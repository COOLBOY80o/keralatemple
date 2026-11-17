package com.example.keralatemple.client;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Turns a photo into a 3D relief model.
 *
 * A single photo cannot contain the real shape of a statue, so this does what a sculptor's "inflate" tool does:
 *  1. shrinks the photo to at most 64 cells on its long side (the Minecraft look),
 *  2. finds the idol's outline: the transparent area of a cut-out PNG, or else the background, found by
 *     flood-filling inwards from the photo's border,
 *  3. gives every cell inside the outline a depth that is greatest in the middle and rounds off towards the edge,
 *  4. builds the front (the photo), a darker back and solid sides.
 *
 * It is pure Java (no OpenGL), so it can run anywhere. Coordinates: height 0..1, x centred, z is depth with the
 * front of the idol at negative z.
 */
public final class IdolMeshBuilder {
    private static final int MAX_RES = 64;
    private static final float HALF_DEPTH = 0.14F;
    public static final int STRIDE = 9; // x y z nx ny nz u v shade

    public record MeshData(float[] verts, int vertexCount, int texW, int texH, int[] argb) {}

    private IdolMeshBuilder() {}

    public static MeshData build(BufferedImage src) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        float scale = Math.min(1.0F, (float) MAX_RES / Math.max(sw, sh));
        int w = Math.max(2, Math.round(sw * scale));
        int h = Math.max(2, Math.round(sh * scale));

        int[] argb = downsample(src, w, h);
        boolean[] mask = computeMask(argb, w, h);
        float[] depth = depthField(mask, w, h);

        FloatBuf buf = new FloatBuf();
        float s = 1.0F / h;
        float halfW = w * s / 2.0F;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (!mask[j * w + i]) {
                    continue;
                }
                float hz = depth[j * w + i];
                // Image columns run left to right; the viewer in front of the idol (at -z) sees +x on their left.
                float xa = halfW - i * s;
                float xb = halfW - (i + 1) * s;
                float ya = 1.0F - j * s;
                float yb = 1.0F - (j + 1) * s;
                float u = (i + 0.5F) / w;
                float v = (j + 0.5F) / h;

                quad(buf, xa, ya, -hz, xa, yb, -hz, xb, yb, -hz, xb, ya, -hz, 0, 0, -1, u, v, 1.0F);
                quad(buf, xa, ya, hz, xb, ya, hz, xb, yb, hz, xa, yb, hz, 0, 0, 1, u, v, 0.6F);

                float right = depthAt(depth, mask, w, h, i + 1, j);
                if (right < hz) {
                    wall(buf, xb, ya, xb, yb, hz, right, -1, 0, u, v);
                }
                float left = depthAt(depth, mask, w, h, i - 1, j);
                if (left < hz) {
                    wall(buf, xa, ya, xa, yb, hz, left, 1, 0, u, v);
                }
                float up = depthAt(depth, mask, w, h, i, j - 1);
                if (up < hz) {
                    wall(buf, xa, ya, xb, ya, hz, up, 0, 1, u, v);
                }
                float down = depthAt(depth, mask, w, h, i, j + 1);
                if (down < hz) {
                    wall(buf, xa, yb, xb, yb, hz, down, 0, -1, u, v);
                }
            }
        }
        return new MeshData(buf.toArray(), buf.count / STRIDE, w, h, argb);
    }

    // ------------------------------------------------------------------ image processing

    /** Box-filters the photo down to w x h. Returns ARGB. */
    private static int[] downsample(BufferedImage src, int w, int h) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        int[] out = new int[w * h];
        for (int j = 0; j < h; j++) {
            int y0 = j * sh / h;
            int y1 = Math.max(y0 + 1, (j + 1) * sh / h);
            int ys = Math.max(1, (y1 - y0) / 6);
            for (int i = 0; i < w; i++) {
                int x0 = i * sw / w;
                int x1 = Math.max(x0 + 1, (i + 1) * sw / w);
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
    private static boolean[] computeMask(int[] argb, int w, int h) {
        int total = w * h;
        boolean[] mask = new boolean[total];

        int transparent = 0;
        for (int p : argb) {
            if ((p >>> 24) < 128) {
                transparent++;
            }
        }

        if (transparent * 100 >= total) {
            // A cut-out PNG: the alpha channel is the outline.
            for (int k = 0; k < total; k++) {
                mask[k] = (argb[k] >>> 24) >= 128;
            }
        } else {
            // A normal photo: treat whatever connects to the border and looks like it as background.
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
            int avg = (int) (br / bn) << 16 | (int) (bg / bn) << 8 | (int) (bb / bn);

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
                mask[k] = !background[k];
                if (mask[k]) {
                    inside++;
                }
            }
            if (inside * 100 < total * 4) {
                Arrays.fill(mask, true);   // the guess failed: keep the whole picture
            }
        }

        int count = 0;
        for (boolean m : mask) {
            if (m) {
                count++;
            }
        }
        if (count == 0) {
            Arrays.fill(mask, true);
        }
        return mask;
    }

    private static int distSq(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255);
        int dg = ((a >> 8) & 255) - ((b >> 8) & 255);
        int db = (a & 255) - (b & 255);
        return dr * dr + dg * dg + db * db;
    }

    /** Half-thickness per cell: rounded like a pillow, thickest furthest from the outline. */
    private static float[] depthField(boolean[] mask, int w, int h) {
        int pw = w + 2;
        int ph = h + 2;
        final float inf = 1.0e9F;
        float[] d = new float[pw * ph];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                d[(j + 1) * pw + (i + 1)] = mask[j * w + i] ? inf : 0.0F;
            }
        }
        for (int j = 1; j <= h; j++) {
            for (int i = 1; i <= w; i++) {
                int k = j * pw + i;
                if (d[k] == 0.0F) {
                    continue;
                }
                float m = d[k];
                m = Math.min(m, d[k - 1] + 1.0F);
                m = Math.min(m, d[k - pw] + 1.0F);
                m = Math.min(m, d[k - pw - 1] + 1.414F);
                m = Math.min(m, d[k - pw + 1] + 1.414F);
                d[k] = m;
            }
        }
        float max = 0.0F;
        for (int j = h; j >= 1; j--) {
            for (int i = w; i >= 1; i--) {
                int k = j * pw + i;
                if (d[k] == 0.0F) {
                    continue;
                }
                float m = d[k];
                m = Math.min(m, d[k + 1] + 1.0F);
                m = Math.min(m, d[k + pw] + 1.0F);
                m = Math.min(m, d[k + pw + 1] + 1.414F);
                m = Math.min(m, d[k + pw - 1] + 1.414F);
                d[k] = m;
                max = Math.max(max, m);
            }
        }
        float r = Math.max(2.0F, max);
        float[] out = new float[w * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (!mask[j * w + i]) {
                    continue;
                }
                float t = Math.min(d[(j + 1) * pw + (i + 1)], r) / r;
                out[j * w + i] = HALF_DEPTH * (float) Math.sqrt(1.0 - (1.0 - t) * (1.0 - t));
            }
        }
        return out;
    }

    private static float depthAt(float[] depth, boolean[] mask, int w, int h, int i, int j) {
        if (i < 0 || j < 0 || i >= w || j >= h || !mask[j * w + i]) {
            return 0.0F;
        }
        return depth[j * w + i];
    }

    // ------------------------------------------------------------------ geometry

    /** A side wall along the edge (x1,y1)-(x2,y2), between this cell's depth and the neighbour's. */
    private static void wall(FloatBuf b, float x1, float y1, float x2, float y2, float hz, float hn,
                             float nx, float ny, float u, float v) {
        if (hn <= 0.0001F) {
            quad(b, x1, y1, -hz, x2, y2, -hz, x2, y2, hz, x1, y1, hz, nx, ny, 0, u, v, 0.8F);
        } else {
            quad(b, x1, y1, -hz, x2, y2, -hz, x2, y2, -hn, x1, y1, -hn, nx, ny, 0, u, v, 0.8F);
            quad(b, x1, y1, hn, x2, y2, hn, x2, y2, hz, x1, y1, hz, nx, ny, 0, u, v, 0.7F);
        }
    }

    private static void quad(FloatBuf b,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz, float u, float v, float shade) {
        vertex(b, x1, y1, z1, nx, ny, nz, u, v, shade);
        vertex(b, x2, y2, z2, nx, ny, nz, u, v, shade);
        vertex(b, x3, y3, z3, nx, ny, nz, u, v, shade);
        vertex(b, x4, y4, z4, nx, ny, nz, u, v, shade);
    }

    private static void vertex(FloatBuf b, float x, float y, float z, float nx, float ny, float nz,
                               float u, float v, float shade) {
        b.add(x);
        b.add(y);
        b.add(z);
        b.add(nx);
        b.add(ny);
        b.add(nz);
        b.add(u);
        b.add(v);
        b.add(shade);
    }

    /** A growable float array. */
    private static final class FloatBuf {
        private float[] data = new float[1 << 16];
        private int count = 0;

        void add(float f) {
            if (count == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[count++] = f;
        }

        float[] toArray() {
            return Arrays.copyOf(data, count);
        }
    }
}
