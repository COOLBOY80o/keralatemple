package com.example.keralatemple.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Turns a dropped photo into the small picture the idol designer works with:
 * shrinks it to at most 64 cells on the long side and (optionally) removes the background.
 *
 * The background is whatever touches the photo's border and looks like it, found by flood-filling inwards.
 * A PNG that already has a transparent background is used as it is.
 */
@OnlyIn(Dist.CLIENT)
public final class ImageImport {
    public static final int MAX_RES = 64;
    private static final long MAX_FILE_BYTES = 40L * 1024 * 1024;

    /** argb: alpha is either 0 (empty) or 255 (filled). */
    public record Result(int width, int height, int[] argb, int[] original) {}

    private ImageImport() {}

    /** @param cutMode 0 keep the whole picture, 1 normal cut-out, 2 generous cut-out (keeps more of the picture) */
    public static Result load(Path file, int cutMode) throws IOException {
        return load(file, cutMode, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    /** As above, but only the part of the picture inside the given rectangle (fractions 0..1 of the picture). */
    public static Result load(Path file, int cutMode, float cx0, float cy0, float cx1, float cy1) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("that file is too big (over 40 MB)");
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("that is not a PNG or JPG picture");
        }

        int fw = image.getWidth();
        int fh = image.getHeight();
        int x0 = Math.max(0, Math.min(fw - 4, Math.round(Math.min(cx0, cx1) * fw)));
        int y0 = Math.max(0, Math.min(fh - 4, Math.round(Math.min(cy0, cy1) * fh)));
        int x1 = Math.max(x0 + 4, Math.min(fw, Math.round(Math.max(cx0, cx1) * fw)));
        int y1 = Math.max(y0 + 4, Math.min(fh, Math.round(Math.max(cy0, cy1) * fh)));
        if (x0 != 0 || y0 != 0 || x1 != fw || y1 != fh) {
            image = image.getSubimage(x0, y0, x1 - x0, y1 - y0);
        }
        int sw = image.getWidth();
        int sh = image.getHeight();
        float scale = (float) MAX_RES / Math.max(sw, sh);
        int w = Math.max(4, Math.min(MAX_RES, Math.round(sw * scale)));
        int h = Math.max(4, Math.min(MAX_RES, Math.round(sh * scale)));

        int[] argb = downsample(image, sw, sh, w, h);
        int[] original = new int[argb.length];
        for (int k = 0; k < original.length; k++) {
            original[k] = 0xFF000000 | (argb[k] & 0xFFFFFF);
        }
        boolean[] filled = computeFilled(argb, w, h, cutMode);
        for (int k = 0; k < argb.length; k++) {
            argb[k] = filled[k] ? (0xFF000000 | (argb[k] & 0xFFFFFF)) : 0;
        }
        return new Result(w, h, argb, original);
    }

    /** Box-filters the photo down to w x h. */
    private static int[] downsample(BufferedImage src, int sw, int sh, int w, int h) {
        int[] out = new int[w * h];
        for (int j = 0; j < h; j++) {
            int y0 = Math.min(sh - 1, j * sh / h);
            int y1 = Math.max(y0 + 1, Math.min(sh, (j + 1) * sh / h));
            int ys = Math.max(1, (y1 - y0) / 6);
            for (int i = 0; i < w; i++) {
                int x0 = Math.min(sw - 1, i * sw / w);
                int x1 = Math.max(x0 + 1, Math.min(sw, (i + 1) * sw / w));
                int xs = Math.max(1, (x1 - x0) / 6);
                long a = 0;
                long r = 0;
                long g = 0;
                long b = 0;
                int n = 0;
                for (int y = y0; y < y1; y += ys) {
                    for (int x = x0; x < x1; x += xs) {
                        int p = src.getRGB(x, y);
                        int pa = p >>> 24;
                        a += pa;
                        r += (long) ((p >> 16) & 255) * pa;
                        g += (long) ((p >> 8) & 255) * pa;
                        b += (long) (p & 255) * pa;
                        n++;
                    }
                }
                if (a == 0 || n == 0) {
                    out[j * w + i] = 0;
                } else {
                    int oa = (int) (a / n);
                    out[j * w + i] = (oa << 24) | ((int) (r / a) << 16) | ((int) (g / a) << 8) | (int) (b / a);
                }
            }
        }
        return out;
    }

    /** true = part of the idol. */
    private static boolean[] computeFilled(int[] argb, int w, int h, int cutMode) {
        int total = w * h;
        boolean[] filled = new boolean[total];

        int transparent = 0;
        for (int p : argb) {
            if ((p >>> 24) < 128) {
                transparent++;
            }
        }
        if (transparent * 100 >= total) {
            // A cut-out PNG: its transparency is the outline.
            for (int k = 0; k < total; k++) {
                filled[k] = (argb[k] >>> 24) >= 128;
            }
        } else if (cutMode == 0) {
            Arrays.fill(filled, true);
        } else {
            boolean[] grab = grabCut(argb, w, h, cutMode == 2 ? 1.7F : 1.0F);
            System.arraycopy(grab, 0, filled, 0, total);
            int inside = 0;
            for (boolean f : filled) {
                if (f) {
                    inside++;
                }
            }
            if (inside * 100 < total * 4) {
                Arrays.fill(filled, true);   // the guess failed: keep the whole picture
            }
        }

        cleanup(filled, w, h);

        boolean any = false;
        for (boolean f : filled) {
            if (f) {
                any = true;
                break;
            }
        }
        if (!any) {
            Arrays.fill(filled, true);
        }
        return filled;
    }

    /**
     * Separates the subject from the background by learning two colour palettes: the border of the photo is the
     * background, the middle is the (rough) subject. Every cell is then given to the palette it matches better, the
     * palettes are re-learnt from the result and the process repeats. Gaps are closed so the idol stays in one piece.
     */
    private static boolean[] grabCut(int[] argb, int w, int h, float bias) {
        int total = w * h;
        int[] bin = new int[total];
        for (int k = 0; k < total; k++) {
            int p = argb[k];
            bin[k] = (((p >> 21) & 7) << 6) | (((p >> 13) & 7) << 3) | ((p >> 5) & 7);
        }
        int band = Math.max(2, Math.min(w, h) / 12);
        boolean[] border = new boolean[total];
        boolean[] fg = new boolean[total];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                border[k] = i < band || j < band || i >= w - band || j >= h - band;
                double dx = (i + 0.5D) / w - 0.5D;
                double dy = (j + 0.5D) / h - 0.5D;
                fg[k] = !border[k] && (dx * dx) / (0.30D * 0.30D) + (dy * dy) / (0.40D * 0.40D) <= 1.0D;
            }
        }
        for (int iter = 0; iter < 6; iter++) {
            float[] fh = new float[512];
            float[] bh = new float[512];
            float nf = 1;
            float nb = 1;
            for (int k = 0; k < total; k++) {
                if (fg[k]) {
                    fh[bin[k]] += 1;
                    nf += 1;
                } else if (border[k] || iter > 0) {
                    bh[bin[k]] += 1;
                    nb += 1;
                }
            }
            // Smooth the palettes a little so near colours count too.
            float[] fs = blurBins(fh);
            float[] bs = blurBins(bh);
            boolean[] next = new boolean[total];
            for (int k = 0; k < total; k++) {
                if (border[k]) {
                    next[k] = false;
                    continue;
                }
                float pf = (fs[bin[k]] + 0.02F) / nf;
                float pb = (bs[bin[k]] + 0.02F) / nb;
                next[k] = pf * bias > pb;
            }
            // Majority vote over the neighbours keeps the regions solid.
            boolean[] vote = new boolean[total];
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int c = 0;
                    for (int dj = -1; dj <= 1; dj++) {
                        for (int di = -1; di <= 1; di++) {
                            int x = i + di;
                            int y = j + dj;
                            if (x >= 0 && y >= 0 && x < w && y < h && next[y * w + x]) {
                                c++;
                            }
                        }
                    }
                    vote[j * w + i] = !border[j * w + i] && c >= 5;
                }
            }
            fg = vote;
        }
        return close(fg, w, h, 2);
    }

    private static float[] blurBins(float[] h) {
        float[] out = new float[512];
        for (int r = 0; r < 8; r++) {
            for (int g = 0; g < 8; g++) {
                for (int b = 0; b < 8; b++) {
                    float sum = 0;
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dg = -1; dg <= 1; dg++) {
                            for (int db = -1; db <= 1; db++) {
                                int rr = r + dr;
                                int gg = g + dg;
                                int bb = b + db;
                                if (rr >= 0 && gg >= 0 && bb >= 0 && rr < 8 && gg < 8 && bb < 8) {
                                    float wgt = (dr == 0 && dg == 0 && db == 0) ? 1.0F : 0.25F;
                                    sum += h[(rr << 6) | (gg << 3) | bb] * wgt;
                                }
                            }
                        }
                    }
                    out[(r << 6) | (g << 3) | b] = sum;
                }
            }
        }
        return out;
    }

    /** Grows the shape by n cells then shrinks it by n cells: narrow gaps (a neck, a thin arm) fill up. */
    private static boolean[] close(boolean[] in, int w, int h, int n) {
        boolean[] cur = in;
        for (int pass = 0; pass < n; pass++) {
            cur = morph(cur, w, h, true);
        }
        for (int pass = 0; pass < n; pass++) {
            cur = morph(cur, w, h, false);
        }
        return cur;
    }

    private static boolean[] morph(boolean[] in, int w, int h, boolean grow) {
        boolean[] out = new boolean[in.length];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                boolean any = false;
                boolean all = true;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        int x = i + di;
                        int y = j + dj;
                        boolean v = x >= 0 && y >= 0 && x < w && y < h && in[y * w + x];
                        any |= v;
                        all &= v;
                    }
                }
                out[j * w + i] = grow ? any : all;
            }
        }
        return out;
    }

    /**
     * Tidies the cut-out: smooths ragged edges, drops small floating islands (bits of background that survived)
     * and fills small holes.
     */
    private static void cleanup(boolean[] filled, int w, int h) {
        // 1. One pass of a majority filter rounds corners and removes single stray cells.
        boolean[] smooth = new boolean[filled.length];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int n = 0;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        int x = i + di;
                        int y = j + dj;
                        if (x >= 0 && y >= 0 && x < w && y < h && filled[y * w + x]) {
                            n++;
                        }
                    }
                }
                smooth[j * w + i] = n >= 5;
            }
        }
        boolean anySmooth = false;
        for (boolean f : smooth) {
            anySmooth |= f;
        }
        if (anySmooth) {
            System.arraycopy(smooth, 0, filled, 0, filled.length);
        }

        // 2. Keep the biggest piece and any piece that is at least 5% of it.
        int[] label = new int[filled.length];
        java.util.List<Integer> sizes = new java.util.ArrayList<>();
        int[] queue = new int[filled.length];
        for (int k = 0; k < filled.length; k++) {
            if (!filled[k] || label[k] != 0) {
                continue;
            }
            int id = sizes.size() + 1;
            int head = 0;
            int tail = 0;
            queue[tail++] = k;
            label[k] = id;
            while (head < tail) {
                int c = queue[head++];
                int x = c % w;
                int y = c / w;
                for (int dj = -1; dj <= 1; dj++) {
                    for (int di = -1; di <= 1; di++) {
                        int nx = x + di;
                        int ny = y + dj;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                            continue;
                        }
                        int nk = ny * w + nx;
                        if (filled[nk] && label[nk] == 0) {
                            label[nk] = id;
                            queue[tail++] = nk;
                        }
                    }
                }
            }
            sizes.add(tail);
        }
        int largest = 0;
        for (int sz : sizes) {
            largest = Math.max(largest, sz);
        }
        for (int k = 0; k < filled.length; k++) {
            if (filled[k] && sizes.get(label[k] - 1) * 20 < largest) {
                filled[k] = false;
            }
        }

        // 3. Fill empty pockets that do not touch the border and are small (under 2% of the picture).
        boolean[] seen = new boolean[filled.length];
        for (int k = 0; k < filled.length; k++) {
            if (filled[k] || seen[k]) {
                continue;
            }
            int head = 0;
            int tail = 0;
            boolean touchesBorder = false;
            queue[tail++] = k;
            seen[k] = true;
            while (head < tail) {
                int c = queue[head++];
                int x = c % w;
                int y = c / w;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    touchesBorder = true;
                }
                int[] nx = {x + 1, x - 1, x, x};
                int[] ny = {y, y, y + 1, y - 1};
                for (int d = 0; d < 4; d++) {
                    if (nx[d] < 0 || ny[d] < 0 || nx[d] >= w || ny[d] >= h) {
                        continue;
                    }
                    int nk = ny[d] * w + nx[d];
                    if (!filled[nk] && !seen[nk]) {
                        seen[nk] = true;
                        queue[tail++] = nk;
                    }
                }
            }
            if (!touchesBorder && tail * 50 < filled.length) {
                for (int q = 0; q < tail; q++) {
                    filled[queue[q]] = true;
                }
            }
        }
    }

    private static int distSq(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255);
        int dg = ((a >> 8) & 255) - ((b >> 8) & 255);
        int db = (a & 255) - (b & 255);
        return dr * dr + dg * dg + db * db;
    }
}
