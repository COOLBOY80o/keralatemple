package com.example.keralatemple.client;

import java.util.Arrays;

/**
 * Turns the idol's front picture into a 3D relief, the way a sculptor's "inflate" tool does:
 * every filled cell gets a depth that is greatest in the middle of the shape and rounds off towards the outline.
 * It builds the front (carrying the picture), a darker back and solid sides.
 *
 * Pure Java (no OpenGL), so it can be tested anywhere. Coordinates: height 0..1, x centred, z is depth with the
 * front of the idol at negative z.
 */
public final class IdolMeshBuilder {
    private static final float HALF_DEPTH = 0.14F;
    public static final int STRIDE = 9; // x y z nx ny nz u v shade

    public record MeshData(float[] verts, int vertexCount) {}

    private IdolMeshBuilder() {}

    /** @param argb w*h cells; a cell is filled when its alpha is 128 or more. */
    public static MeshData build(int w, int h, int[] argb, float thickness) {
        boolean[] mask = new boolean[w * h];
        for (int k = 0; k < mask.length; k++) {
            mask[k] = (argb[k] >>> 24) >= 128;
        }
        float[] depth = depthField(mask, w, h, thickness);

        FloatBuf buf = new FloatBuf();
        float s = 1.0F / h;
        float halfW = w * s / 2.0F;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (!mask[j * w + i]) {
                    continue;
                }
                float hz = depth[j * w + i];
                // Image columns run left to right; a viewer in front of the idol (at -z) sees +x on their left.
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
        return new MeshData(buf.toArray(), buf.count / STRIDE);
    }

    /** Half-thickness per cell: rounded like a pillow, thickest furthest from the outline. */
    private static float[] depthField(boolean[] mask, int w, int h, float thickness) {
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
                out[j * w + i] = HALF_DEPTH * thickness * (float) Math.sqrt(1.0 - (1.0 - t) * (1.0 - t));
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
