package com.solegendary.reignofnether.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// The supervisor's net, trained by RoN-Bots (ronbots/net.py, same shapes): a small MLP over the game's numbers with
// one head per decision, and two pointer heads that score places on the map (attack targets, expansion patches).
// Weights come from a JSON file: {"inputs": n, "layers": {"trunk1": {"w": [[..]], "b": [..]}, ...}}, w as [out][in].
public final class BotNet {

    record Linear(double[][] w, double[] b) {
        double[] apply(double[] x) {
            double[] y = b.clone();
            for (int o = 0; o < w.length; o++) {
                double[] row = w[o];
                double s = 0;
                for (int i = 0; i < row.length; i++)
                    s += row[i] * x[i];
                y[o] += s;
            }
            return y;
        }

        int in() {
            return w.length == 0 ? 0 : w[0].length;
        }
    }

    // everything the heads say for one decision: logits per head, a score per place, and how likely a win is
    public record Output(double[] army, double[] unit, double[] gather, double[] targets, double[] expands, double value) {}

    final int inputs;
    private final Linear trunk1, trunk2, army, unit, gather, value, target1, target2, expand1, expand2, expandNone;

    private BotNet(JsonObject json) {
        JsonObject l = json.getAsJsonObject("layers");
        inputs = json.get("inputs").getAsInt();
        trunk1 = linear(l, "trunk1");
        trunk2 = linear(l, "trunk2");
        army = linear(l, "army");
        unit = linear(l, "unit");
        gather = linear(l, "gather");
        value = linear(l, "value");
        target1 = linear(l, "target1");
        target2 = linear(l, "target2");
        expand1 = linear(l, "expand1");
        expand2 = linear(l, "expand2");
        expandNone = linear(l, "expandNone");
        if (trunk1.in() != inputs)
            throw new IllegalArgumentException("net expects " + trunk1.in() + " inputs, file says " + inputs);
    }

    public static BotNet parse(JsonObject json) {
        return new BotNet(json);
    }

    private static Linear linear(JsonObject layers, String name) {
        JsonObject o = layers.getAsJsonObject(name);
        if (o == null)
            throw new IllegalArgumentException("net has no layer " + name);
        JsonArray rows = o.getAsJsonArray("w");
        double[][] w = new double[rows.size()][];
        for (int r = 0; r < w.length; r++)
            w[r] = doubles(rows.get(r).getAsJsonArray());
        return new Linear(w, doubles(o.getAsJsonArray("b")));
    }

    private static double[] doubles(JsonArray a) {
        double[] d = new double[a.size()];
        int i = 0;
        for (JsonElement e : a)
            d[i++] = e.getAsDouble();
        return d;
    }

    public Output run(double[] global, List<double[]> targets, List<double[]> expands) {
        double[] h = relu(trunk2.apply(relu(trunk1.apply(global))));
        double[] t = new double[targets.size()];
        for (int i = 0; i < t.length; i++)
            t[i] = target2.apply(relu(target1.apply(concat(targets.get(i), h))))[0];
        // the last expansion choice is "not now"
        double[] e = new double[expands.size() + 1];
        for (int i = 0; i < expands.size(); i++)
            e[i] = expand2.apply(relu(expand1.apply(concat(expands.get(i), h))))[0];
        e[expands.size()] = expandNone.apply(h)[0];
        return new Output(army.apply(h), unit.apply(h), gather.apply(h), t, e, Math.tanh(value.apply(h)[0]));
    }

    // ---------------------------------------------------------------- picking

    public record Pick(int index, double logProb) {}

    // the best allowed choice, or a draw by the odds while training. mask null: everything allowed. -1 when nothing is
    // self-play draws from sharper odds than the net's own: drawn as they are, an unsure head (a third right on
    // buildings) scrambled the whole build order every game. The trainer uses the same (ppo.py TEMPERATURE)
    static final double TEMPERATURE = 0.5;

    static Pick pick(double[] logits, boolean[] mask, boolean sample, Random random) {
        if (sample) {
            double[] sharp = new double[logits.length];
            for (int i = 0; i < logits.length; i++)
                sharp[i] = logits[i] / TEMPERATURE;
            logits = sharp;
        }
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logits.length; i++)
            if (mask == null || mask[i])
                max = Math.max(max, logits[i]);
        if (max == Double.NEGATIVE_INFINITY)
            return new Pick(-1, 0);
        double sum = 0;
        double[] p = new double[logits.length];
        for (int i = 0; i < logits.length; i++)
            if (mask == null || mask[i])
                sum += p[i] = Math.exp(logits[i] - max);
        int best = -1;
        double roll = random.nextDouble() * sum;
        for (int i = 0; i < p.length; i++) {
            if (p[i] == 0)
                continue;
            if (sample ? (roll -= p[i]) <= 0 : best < 0 || p[i] > p[best]) {
                best = i;
                if (sample)
                    break;
            }
        }
        if (best < 0) // rounding left the roll just over: the last allowed one
            for (int i = p.length - 1; i >= 0 && best < 0; i--)
                if (p[i] > 0)
                    best = i;
        return new Pick(best, Math.log(p[best] / sum));
    }

    // allowed choices from the most to the least likely
    static List<Integer> ranked(double[] logits, boolean[] mask) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < logits.length; i++)
            if (mask == null || mask[i])
                order.add(i);
        order.sort((a, b) -> Double.compare(logits[b], logits[a]));
        return order;
    }

    private static double[] relu(double[] x) {
        for (int i = 0; i < x.length; i++)
            x[i] = Math.max(0, x[i]);
        return x;
    }

    private static double[] concat(double[] a, double[] b) {
        double[] c = new double[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
