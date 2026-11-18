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
    public record Result(int width, int height, int[] argb) {}

    private ImageImport() {}

    public static Result load(Path file, boolean removeBackground) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("that file is too big (over 40 MB)");
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("that is not a PNG or JPG picture");
        }

        int sw = image.getWidth();
        int sh = image.getHeight();
        float scale = (float) MAX_RES / Math.max(sw, sh);
        int w = Math.max(4, Math.min(MAX_RES, Math.round(sw * scale)));
        int h = Math.max(4, Math.min(MAX_RES, Math.round(sh * scale)));

        int[] argb = downsample(image, sw, sh, w, h);
        boolean[] filled = computeFilled(argb, w, h, removeBackground);
        for (int k = 0; k < argb.length; k++) {
            argb[k] = filled[k] ? (0xFF000000 | (argb[k] & 0xFFFFFF)) : 0;
        }
        return new Result(w, h, argb);
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
    private static boolean[] computeFilled(int[] argb, int w, int h, boolean removeBackground) {
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
        } else if (!removeBackground) {
            Arrays.fill(filled, true);
        } else {
            long br = 0;
            long bg = 0;
            long bb = 0;
            int bn = 0;
            for (int i = 0; i < w; i++) {
                for (int j : new int[]{0, h - 1}) {
                    int p = argb[j * w + i];
                    br += (p >> 16) & 255;
                    bg += (p >> 8) & 255;
                    bb += p & 255;
                    bn++;
                }
            }
            for (int j = 1; j < h - 1; j++) {
                for (int i : new int[]{0, w - 1}) {
                    int p = argb[j * w + i];
                    br += (p >> 16) & 255;
                    bg += (p >> 8) & 255;
                    bb += p & 255;
                    bn++;
                }
            }
            int avg = ((int) (br / bn) << 16) | ((int) (bg / bn) << 8) | (int) (bb / bn);

            boolean[] background = new boolean[total];
            int[] queue = new int[total];
            int head = 0;
            int tail = 0;
            for (int i = 0; i < w; i++) {
                for (int j : new int[]{0, h - 1}) {
                    int k = j * w + i;
                    if (!background[k] && distSq(argb[k], avg) < 120 * 120) {
                        background[k] = true;
                        queue[tail++] = k;
                    }
                }
            }
            for (int j = 0; j < h; j++) {
                for (int i : new int[]{0, w - 1}) {
                    int k = j * w + i;
                    if (!background[k] && distSq(argb[k], avg) < 120 * 120) {
                        background[k] = true;
                        queue[tail++] = k;
                    }
                }
            }
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            while (head < tail) {
                int k = queue[head++];
                int x = k % w;
                int y = k / w;
                for (int d = 0; d < 4; d++) {
                    int nx = x + dx[d];
                    int ny = y + dy[d];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                        continue;
                    }
                    int nk = ny * w + nx;
                    // Joins the background if it is close to the cell it spreads from (so soft gradients work)
                    // and not wildly different from the border colour.
                    if (!background[nk] && distSq(argb[nk], argb[k]) < 30 * 30 && distSq(argb[nk], avg) < 120 * 120) {
                        background[nk] = true;
                        queue[tail++] = nk;
                    }
                }
            }
            int inside = 0;
            for (int k = 0; k < total; k++) {
                filled[k] = !background[k];
                if (filled[k]) {
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
