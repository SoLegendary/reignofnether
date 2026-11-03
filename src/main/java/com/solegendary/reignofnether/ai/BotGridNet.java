package com.solegendary.reignofnether.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The grid supervisor's net (RoN-Bots FORMAT.md, "grid-1"): a small U-Net over the 18x32x32 map grid plus the global
// inputs. Cell heads score every grid cell, the other heads pick from a fixed list, and value guesses the outcome.
// Picking goes through BotNet.pick / BotNet.ranked. run() reuses its buffers, so one net must not run on two threads.
public final class BotGridNet {

    public static final String FORMAT = "grid-1";
    static final int LAYERS = 18, GRID = 32, CELLS = GRID * GRID;
    public static final List<String> CELL_HEADS = List.of("mainCell", "secondCell", "hero0Cell", "hero1Cell", "raidCell", "scoutCell", "buildCell");
    // head -> its size, or -1 when it depends on the vocab
    private static final Map<String, Integer> HEADS = new LinkedHashMap<>();
    static {
        HEADS.put("mainOrder", 4);
        HEADS.put("secondSplit", 3);
        HEADS.put("secondOrder", 4);
        HEADS.put("hero0Order", 4);
        HEADS.put("hero1Order", 4);
        HEADS.put("raidOrder", 2);
        HEADS.put("scoutOrder", 2);
        HEADS.put("build", -1);
        HEADS.put("research", -1);
        HEADS.put("workers", 2);
        HEADS.put("gather", 5);
        HEADS.put("unit", -1);
    }

    // w flattened from [out][in][k][k]
    private record Conv(float[] w, float[] b, int in, int out, int k) {}

    private record Linear(float[] w, float[] b, int in, int out) {
        void apply(float[] x, float[] y, boolean relu) {
            for (int o = 0; o < out; o++) {
                float s = b[o];
                for (int i = 0, r = o * in; i < in; i++)
                    s += w[r + i] * x[i];
                y[o] = relu ? Math.max(0, s) : s;
            }
        }
    }

    public record Output(Map<String, double[]> heads, double value) {
        public double[] logits(String head) {
            return heads.get(head);
        }
    }

    public final int inputs;
    public final List<String> buildings, research, units;
    private final Conv enc1, enc2, enc3, dec2, dec1;
    private final Linear glob, trunk, bcast, value;
    private final Map<String, Conv> cellHeads = new LinkedHashMap<>();
    private final Map<String, Linear> heads = new LinkedHashMap<>();

    // padded buffers ([c][h+2][w+2]) are the inputs of 3x3 convs; their zero border is never written
    private final float[] x = new float[LAYERS * 34 * 34];
    private final float[] dec1In = new float[96 * 34 * 34]; // up2(d2) ++ e1
    private final float[] dec2In = new float[128 * 18 * 18]; // up2(e3b) ++ e2
    private final float[] acc0 = new float[32 * 34], acc1 = new float[32 * 34];
    private final float[] e3 = new float[64 * 8 * 8], d2 = new float[64 * 16 * 16], d1 = new float[32 * CELLS];
    private final float[] g, gv = new float[128], tIn = new float[192], t = new float[256], b = new float[64];

    private BotGridNet(JsonObject json) {
        if (!FORMAT.equals(json.has("format") ? json.get("format").getAsString() : null))
            throw new IllegalArgumentException("net format isn't " + FORMAT);
        if (json.get("layers").getAsInt() != LAYERS || json.get("grid").getAsInt() != GRID)
            throw new IllegalArgumentException("net expects a " + LAYERS + "x" + GRID + "x" + GRID + " grid");
        inputs = json.get("inputs").getAsInt();
        g = new float[inputs];
        JsonObject h = json.getAsJsonObject("heads");
        buildings = strings(h.getAsJsonArray("build"));
        research = strings(h.getAsJsonArray("research"));
        units = strings(h.getAsJsonArray("units"));
        JsonObject w = json.getAsJsonObject("weights");
        enc1 = conv(w, "enc1", LAYERS, 32, 3);
        enc2 = conv(w, "enc2", 32, 64, 3);
        enc3 = conv(w, "enc3", 64, 64, 3);
        dec2 = conv(w, "dec2", 128, 64, 3);
        dec1 = conv(w, "dec1", 96, 32, 3);
        glob = linear(w, "glob", inputs, 128);
        trunk = linear(w, "trunk", 192, 256);
        bcast = linear(w, "bcast", 256, 64);
        value = linear(w, "value", 256, 1);
        for (String k : CELL_HEADS)
            cellHeads.put(k, conv(w, k, 32, 1, 1));
        for (Map.Entry<String, Integer> e : HEADS.entrySet()) {
            int n = switch (e.getKey()) {
                case "build" -> 1 + buildings.size();
                case "research" -> 1 + research.size();
                case "unit" -> units.size();
                default -> e.getValue();
            };
            heads.put(e.getKey(), linear(w, e.getKey(), 256, n));
        }
    }

    public static BotGridNet parse(JsonObject json) {
        return new BotGridNet(json);
    }

    // grid: 18*32*32 layer-major, already /255. globals: the inputs in BotSupervisor.INPUTS order
    public Output run(float[] grid, double[] globals) {
        if (grid.length != LAYERS * CELLS || globals.length != inputs)
            throw new IllegalArgumentException("net wants " + LAYERS * CELLS + " grid values and " + inputs + " globals");
        for (int c = 0; c < LAYERS; c++)
            for (int y = 0; y < GRID; y++)
                System.arraycopy(grid, (c * GRID + y) * GRID, x, (c * 34 + y + 1) * 34 + 1, GRID);
        for (int i = 0; i < inputs; i++)
            g[i] = (float) globals[i];

        conv3(enc1, x, 0, GRID, 1, dec1In, 64, 1);  // e1
        conv3(enc2, dec1In, 64, GRID, 2, dec2In, 64, 1); // e2
        conv3(enc3, dec2In, 64, 16, 2, e3, 0, 0);

        glob.apply(g, gv, true);
        for (int c = 0; c < 64; c++) {
            float s = 0;
            for (int i = 0; i < 64; i++)
                s += e3[c * 64 + i];
            tIn[c] = s / 64;
        }
        System.arraycopy(gv, 0, tIn, 64, 128);
        trunk.apply(tIn, t, true);
        bcast.apply(t, b, false);
        for (int c = 0; c < 64; c++)
            for (int i = 0; i < 64; i++)
                e3[c * 64 + i] = Math.max(0, e3[c * 64 + i] + b[c]);

        up2(e3, 64, 8, dec2In, 18);
        conv3(dec2, dec2In, 0, 16, 1, d2, 0, 0);
        up2(d2, 64, 16, dec1In, 34);
        conv3(dec1, dec1In, 0, GRID, 1, d1, 0, 0);

        Map<String, double[]> out = new LinkedHashMap<>();
        for (Map.Entry<String, Conv> e : cellHeads.entrySet()) {
            Conv c = e.getValue();
            double[] l = new double[CELLS];
            for (int i = 0; i < CELLS; i++) {
                float s = c.b[0];
                for (int ch = 0; ch < 32; ch++)
                    s += c.w[ch] * d1[ch * CELLS + i];
                l[i] = s;
            }
            out.put(e.getKey(), l);
        }
        for (Map.Entry<String, Linear> e : heads.entrySet()) {
            Linear l = e.getValue();
            float[] y = new float[l.out];
            l.apply(t, y, false);
            double[] d = new double[y.length];
            for (int i = 0; i < y.length; i++)
                d[i] = y[i];
            out.put(e.getKey(), d);
        }
        float[] v = new float[1];
        value.apply(t, v, false);
        return new Output(out, Math.tanh(v[0]));
    }

    // relu(3x3 conv, padding 1) from a padded src (size x size, channels from srcCh) into dst from dstCh, whose
    // planes have a border of dstPad
    private void conv3(Conv c, float[] src, int srcCh, int size, int stride, float[] dst, int dstCh, int dstPad) {
        int sp = size + 2, n = size / stride, dp = n + 2 * dstPad;
        if (stride == 1) {
            // two output channels at a time, accumulated in rows sp wide so each input channel is one long contiguous
            // loop over all 9 taps; the 2 extra columns per row are dropped
            int len = n * sp - 2;
            float[] w = c.w;
            for (int o = 0; o < c.out; o += 2) {
                java.util.Arrays.fill(acc0, 0, len, c.b[o]);
                java.util.Arrays.fill(acc1, 0, len, c.b[o + 1]);
                for (int i = 0; i < c.in; i++) {
                    int a = (o * c.in + i) * 9, b = a + c.in * 9;
                    float a0 = w[a], a1 = w[a + 1], a2 = w[a + 2], a3 = w[a + 3], a4 = w[a + 4], a5 = w[a + 5], a6 = w[a + 6], a7 = w[a + 7], a8 = w[a + 8];
                    float b0 = w[b], b1 = w[b + 1], b2 = w[b + 2], b3 = w[b + 3], b4 = w[b + 4], b5 = w[b + 5], b6 = w[b + 6], b7 = w[b + 7], b8 = w[b + 8];
                    int r0 = (srcCh + i) * sp * sp, r1 = r0 + sp, r2 = r1 + sp;
                    for (int j = 0; j < len; j++) {
                        float s0 = src[r0 + j], s1 = src[r0 + j + 1], s2 = src[r0 + j + 2];
                        float s3 = src[r1 + j], s4 = src[r1 + j + 1], s5 = src[r1 + j + 2];
                        float s6 = src[r2 + j], s7 = src[r2 + j + 1], s8 = src[r2 + j + 2];
                        acc0[j] += a0 * s0 + a1 * s1 + a2 * s2 + a3 * s3 + a4 * s4 + a5 * s5 + a6 * s6 + a7 * s7 + a8 * s8;
                        acc1[j] += b0 * s0 + b1 * s1 + b2 * s2 + b3 * s3 + b4 * s4 + b5 * s5 + b6 * s6 + b7 * s7 + b8 * s8;
                    }
                }
                for (int k = 0; k < 2; k++) {
                    float[] ac = k == 0 ? acc0 : acc1;
                    int dBase = (dstCh + o + k) * dp * dp + dstPad * dp + dstPad;
                    for (int y = 0; y < n; y++)
                        for (int xx = 0; xx < n; xx++)
                            dst[dBase + y * dp + xx] = Math.max(0, ac[y * sp + xx]);
                }
            }
            return;
        }
        // stride 2: the same, but row by row
        for (int o = 0; o < c.out; o++) {
            int dBase = (dstCh + o) * dp * dp + dstPad * dp + dstPad;
            java.util.Arrays.fill(acc0, 0, n * n, c.b[o]);
            for (int i = 0; i < c.in; i++) {
                int a = (o * c.in + i) * 9, sBase = (srcCh + i) * sp * sp;
                float[] w = c.w;
                float a0 = w[a], a1 = w[a + 1], a2 = w[a + 2], a3 = w[a + 3], a4 = w[a + 4], a5 = w[a + 5], a6 = w[a + 6], a7 = w[a + 7], a8 = w[a + 8];
                for (int y = 0; y < n; y++) {
                    int r0 = sBase + y * stride * sp, r1 = r0 + sp, r2 = r1 + sp, d = y * n;
                    for (int xx = 0; xx < n; xx++) {
                        int k = xx * stride;
                        acc0[d + xx] += a0 * src[r0 + k] + a1 * src[r0 + k + 1] + a2 * src[r0 + k + 2]
                                + a3 * src[r1 + k] + a4 * src[r1 + k + 1] + a5 * src[r1 + k + 2]
                                + a6 * src[r2 + k] + a7 * src[r2 + k + 1] + a8 * src[r2 + k + 2];
                    }
                }
            }
            for (int y = 0; y < n; y++)
                for (int xx = 0; xx < n; xx++)
                    dst[dBase + y * dp + xx] = Math.max(0, acc0[y * n + xx]);
        }
    }

    // nearest x2 of [ch][size][size] into channels 0.. of a padded dst with planes of dp x dp
    private static void up2(float[] src, int ch, int size, float[] dst, int dp) {
        for (int c = 0; c < ch; c++)
            for (int y = 0; y < size * 2; y++)
                for (int xx = 0; xx < size * 2; xx++)
                    dst[c * dp * dp + (y + 1) * dp + xx + 1] = src[(c * size + y / 2) * size + xx / 2];
    }

    // ---------------------------------------------------------------- parsing

    private static JsonObject layer(JsonObject weights, String name) {
        JsonObject o = weights.getAsJsonObject(name);
        if (o == null)
            throw new IllegalArgumentException("net has no layer " + name);
        return o;
    }

    private static Conv conv(JsonObject weights, String name, int in, int out, int k) {
        JsonObject o = layer(weights, name);
        float[] w = floats(o.get("w"), out * in * k * k, name);
        JsonArray rows = o.getAsJsonArray("w");
        if (rows.size() != out || rows.get(0).getAsJsonArray().size() != in || rows.get(0).getAsJsonArray().get(0).getAsJsonArray().size() != k)
            throw new IllegalArgumentException(name + " isn't [" + out + "][" + in + "][" + k + "][" + k + "]");
        return new Conv(w, floats(o.get("b"), out, name), in, out, k);
    }

    private static Linear linear(JsonObject weights, String name, int in, int out) {
        JsonObject o = layer(weights, name);
        JsonArray rows = o.getAsJsonArray("w");
        if (rows.size() != out || rows.get(0).getAsJsonArray().size() != in)
            throw new IllegalArgumentException(name + " isn't [" + out + "][" + in + "]");
        return new Linear(floats(rows, out * in, name), floats(o.get("b"), out, name), in, out);
    }

    private static float[] floats(JsonElement e, int n, String name) {
        List<Float> l = new ArrayList<>(n);
        flatten(e, l);
        if (l.size() != n)
            throw new IllegalArgumentException(name + " has " + l.size() + " numbers, wants " + n);
        float[] f = new float[n];
        for (int i = 0; i < n; i++)
            f[i] = l.get(i);
        return f;
    }

    private static void flatten(JsonElement e, List<Float> into) {
        if (e.isJsonArray())
            for (JsonElement c : e.getAsJsonArray())
                flatten(c, into);
        else
            into.add((float) e.getAsDouble());
    }

    private static List<String> strings(JsonArray a) {
        List<String> l = new ArrayList<>();
        if (a != null)
            for (JsonElement e : a)
                l.add(e.getAsString());
        return List.copyOf(l);
    }
}
