package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.unit.UnitRole;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Turns units into CombatSim fighters, reading the numbers every unit class already declares. For units the bot
// could train but has none of yet, one never-spawned copy per type stands in (created once, never added to the world).
final class BotFighters {

    private static final double CROSSBOW_CHARGE_SECONDS = 1.75; // CrossbowMixin: 35 ticks without Quick Charge
    private static final double HARMING_DPS = 3; // ponytail: a witch's lingering harming clouds, roughly

    private static final Map<EntityType<?>, LivingEntity> templates = new HashMap<>();

    private BotFighters() {}

    static void clear() {
        templates.clear();
    }

    // knownHealth: the unit is in sight; one remembered from earlier counts at full health
    static CombatSim.Fighter of(LivingEntity e, boolean knownHealth) {
        Unit u = (Unit) e;
        double dps = 0, range = 2;
        if (e instanceof AttackerUnit a) {
            double aps = a.getAttacksPerSecond();
            if (e instanceof PillagerUnit && aps > 0) // its attacks/sec leave out the crossbow charge
                aps = 1 / (1 / aps + CROSSBOW_CHARGE_SECONDS);
            dps = a.getUnitAttackDamage() * aps;
            range = a.getAttackRange();
        }
        Set<UnitRole> roles = u.getUnitRoles();
        if (roles.contains(UnitRole.HARMING))
            dps = Math.max(dps, HARMING_DPS);
        return new CombatSim.Fighter(knownHealth ? e.getHealth() : e.getMaxHealth(), dps, range,
                u.getUnitPhysicalArmorPercentage(), u.getUnitRangedArmorPercentage(), u.getUnitMagicArmorPercentage(), roles);
    }

    static List<CombatSim.Fighter> of(Collection<LivingEntity> units, boolean knownHealth) {
        List<CombatSim.Fighter> list = new ArrayList<>(units.size());
        for (LivingEntity e : units)
            if (e instanceof Unit u && !u.getUnitRoles().contains(UnitRole.WORKER) && !u.getUnitRoles().contains(UnitRole.SCOUT))
                list.add(of(e, knownHealth));
        return list;
    }

    // what a production item would add to the army, or null when it doesn't train a unit
    @Nullable
    static CombatSim.Fighter of(ServerLevel level, ProductionItem item) {
        LivingEntity template = template(level, item);
        return template != null ? of(template, false) : null;
    }

    // a unit of this type as the sim sees it at full health, e.g. the militia a Call to Arms would raise
    @Nullable
    static CombatSim.Fighter of(ServerLevel level, EntityType<?> type) {
        LivingEntity template = template(level, type);
        return template instanceof Unit ? of(template, false) : null;
    }

    @Nullable
    private static LivingEntity template(ServerLevel level, ProductionItem item) {
        EntityType<? extends Unit> type = Factions.unitTypeOf(item);
        return type == null ? null : template(level, type);
    }

    @Nullable
    private static LivingEntity template(ServerLevel level, EntityType<?> type) {
        return templates.computeIfAbsent(type, t -> {
            Entity e = t.create(level);
            return e instanceof LivingEntity le ? le : null;
        });
    }
}
