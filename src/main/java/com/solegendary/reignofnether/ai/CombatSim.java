package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.UnitRole;

import java.util.List;
import java.util.Set;

// A quick fake fight between two groups, in plain numbers, so the bot knows how a fight goes before it takes it.
// Every simulated second each unit hits the weakest enemy it can actually damage; dead units stop hitting.
// Damage follows the real rules through the units' roles: melee can't reach flyers, ranged damage is cut by
// ranged resist, harming potions do nothing to undead, area damage is worth more against a big group.
// ponytail: no positions, no move speed, no abilities beyond their roles, area damage judged on the starting group;
// good enough to rank fights, not replay them
public final class CombatSim {

    // one unit as the sim sees it; dps already includes reload time
    public record Fighter(double hp, double dps, double range, double armor, double rangedResist, double magicResist,
                          Set<UnitRole> roles) {
        boolean has(UnitRole r) {
            return roles.contains(r);
        }
    }

    // ourLeft / theirLeft: share of each side's starting health still standing at the end (0..1)
    public record Result(double ourLeft, double theirLeft) {
        // > 0: we win, by how much; 1 = flawless win, -1 = flawless loss
        public double margin() {
            return ourLeft - theirLeft;
        }
    }

    private static final int MAX_SECONDS = 90;
    private static final int RANGED_HEAD_START = 2; // seconds ranged units shoot before melee closes in
    private static final double LONG_RANGE = 6;
    private static final double AOE_GROUP = 5, AOE_BONUS = 1.5;
    private static final double HEAL_PER_SECOND = 2; // ponytail: a witch's healing potions, roughly

    private CombatSim() {}

    public static Result fight(List<Fighter> ours, List<Fighter> theirs) {
        double ourStart = totalHp(ours), theirStart = totalHp(theirs);
        if (ourStart == 0 || theirStart == 0)
            return new Result(ourStart > 0 ? 1 : 0, theirStart > 0 ? 1 : 0);
        double[] a = hpOf(ours), b = hpOf(theirs);
        // who does how much to whom never changes during a fight: worked out once, not every simulated second
        double[][] oursToTheirs = dpsTable(ours, theirs), theirsToOurs = dpsTable(theirs, ours);
        for (int t = 0; t < MAX_SECONDS && alive(a) && alive(b); t++) {
            boolean closing = t < RANGED_HEAD_START;
            double[] toB = damage(ours, a, oursToTheirs, b, closing);
            double[] toA = damage(theirs, b, theirsToOurs, a, closing);
            if (isZero(toA) && isZero(toB) && !closing)
                break; // nobody can hurt anybody: a standoff
            apply(b, toB);
            apply(a, toA);
            heal(ours, a);
            heal(theirs, b);
        }
        return new Result(sum(a) / ourStart, sum(b) / theirStart);
    }

    // dps of each attacker against each target, area damage counted for the whole starting group
    private static double[][] dpsTable(List<Fighter> attackers, List<Fighter> targets) {
        double[][] table = new double[attackers.size()][targets.size()];
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < targets.size(); j++)
                table[i][j] = dpsAgainst(attackers.get(i), targets.get(j), targets.size());
        return table;
    }

    // what `attacker` deals to `target` per second, by the game's damage rules
    static double dpsAgainst(Fighter attacker, Fighter target, int targetGroupSize) {
        if (target.has(UnitRole.FLYER) && !attacker.has(UnitRole.ANTI_AIR) && !attacker.has(UnitRole.FLYER))
            return 0;
        if (attacker.has(UnitRole.HARMING) && target.has(UnitRole.UNDEAD))
            return 0;
        double dps = attacker.dps() * (1 - target.armor());
        if (attacker.has(UnitRole.RANGED))
            dps *= 1 - target.rangedResist();
        if (attacker.has(UnitRole.CASTER) || attacker.has(UnitRole.HARMING))
            dps *= 1 - target.magicResist();
        if (attacker.has(UnitRole.AOE) && targetGroupSize >= AOE_GROUP)
            dps *= AOE_BONUS;
        return dps;
    }

    // each living attacker focuses the weakest enemy it can damage
    private static double[] damage(List<Fighter> attackers, double[] attackerHp, double[][] dps, double[] targetHp, boolean closing) {
        double[] dealt = new double[targetHp.length];
        for (int i = 0; i < attackers.size(); i++) {
            if (attackerHp[i] <= 0 || (closing && attackers.get(i).range() < LONG_RANGE))
                continue;
            int best = -1;
            double bestDps = 0;
            for (int j = 0; j < targetHp.length; j++) {
                if (targetHp[j] - dealt[j] <= 0)
                    continue;
                double d = dps[i][j];
                if (d > 0 && (best < 0 || targetHp[j] < targetHp[best])) {
                    best = j;
                    bestDps = d;
                }
            }
            if (best >= 0)
                dealt[best] += bestDps;
        }
        return dealt;
    }

    private static void heal(List<Fighter> side, double[] hp) {
        for (int i = 0; i < side.size(); i++) {
            if (hp[i] <= 0 || !side.get(i).has(UnitRole.HEALER))
                continue;
            int worst = -1;
            for (int j = 0; j < side.size(); j++)
                if (hp[j] > 0 && hp[j] < side.get(j).hp() && (worst < 0 || hp[j] / side.get(j).hp() < hp[worst] / side.get(worst).hp()))
                    worst = j;
            if (worst >= 0)
                hp[worst] = Math.min(side.get(worst).hp(), hp[worst] + HEAL_PER_SECOND);
        }
    }

    private static double[] hpOf(List<Fighter> side) {
        double[] hp = new double[side.size()];
        for (int i = 0; i < hp.length; i++)
            hp[i] = side.get(i).hp();
        return hp;
    }

    private static void apply(double[] hp, double[] dealt) {
        for (int i = 0; i < hp.length; i++)
            hp[i] = Math.max(0, hp[i] - dealt[i]);
    }

    private static double totalHp(List<Fighter> side) {
        double total = 0;
        for (Fighter f : side)
            total += f.hp();
        return total;
    }

    private static double sum(double[] hp) {
        double total = 0;
        for (double h : hp)
            total += h;
        return total;
    }

    private static int count(double[] hp) {
        int n = 0;
        for (double h : hp)
            if (h > 0)
                n++;
        return n;
    }

    private static boolean alive(double[] hp) {
        return count(hp) > 0;
    }

    private static boolean isZero(double[] dealt) {
        for (double d : dealt)
            if (d > 0)
                return false;
        return true;
    }
}
