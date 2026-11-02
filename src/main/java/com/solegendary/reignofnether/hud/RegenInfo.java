package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ability.heroAbilities.enchanter.MarchOfProgress;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.BattleRagePassive;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.HealingFountainPlacement;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What heals a unit or restores a hero's mana over time, for the HUD. The server applies these in Unit.tick,
 * HeroUnit.tick and the units' and buildings' own ticks: keep this in step with them.
 */
public final class RegenInfo {

    private RegenInfo() { }

    /** One source of regen; key is the end of its lang key. */
    public record Source(String key, float perSecond) { }

    private static final float MONSTER_REGEN = (float) ResourceCost.TICKS_PER_SECOND / Unit.MONSTER_HEALING_TICKS;
    private static final float PIGLIN_REGEN = (float) ResourceCost.TICKS_PER_SECOND / Unit.PIGLIN_HEALING_TICKS;
    private static final int FOUNTAIN_EFFECT_TICKS = 20; // its regeneration effect is too short to ever heal

    public static float total(List<Source> sources) {
        float sum = 0;
        for (Source s : sources)
            sum += s.perSecond();
        return sum;
    }

    public static List<Source> health(LivingEntity le) {
        List<Source> sources = new ArrayList<>();
        if (le instanceof Unit unit) {
            if (Factions.getFaction(unit).equals(Factions.MONSTERS)) {
                if (!TimeUtils.isDay(TimeClientEvents.serverNormDayTime))
                    sources.add(new Source("night", MONSTER_REGEN));
                if (NightUtils.isInRangeOfNightSource(le.position(), true))
                    sources.add(new Source("night_source", MONSTER_REGEN));
            } else if (Factions.getFaction(unit).equals(Factions.PIGLINS)
                    && (MiscUtil.isOnNetherTerrain(le) || unit instanceof GhastUnit)) {
                sources.add(new Source("nether", PIGLIN_REGEN));
                if (unit.hasEffectWithDuration(MobEffectRegistrar.PIGLIN_FERVOR.get()))
                    sources.add(new Source("fervor", PIGLIN_REGEN));
            }
        }
        MobEffectInstance regen = le.getEffect(MobEffects.REGENERATION);
        if (regen != null && regen.getDuration() > FOUNTAIN_EFFECT_TICKS) {
            int interval = 50 >> regen.getAmplifier();
            sources.add(new Source("effect", interval > 0 ? (float) ResourceCost.TICKS_PER_SECOND / interval : ResourceCost.TICKS_PER_SECOND));
        }
        for (BuildingPlacement b : BuildingClientEvents.getBuildings())
            if (b instanceof HealingFountainPlacement && b.isBuilt
                    && le.position().closerThan(Vec3.atCenterOf(b.centrePos), HealingFountainPlacement.RANGE))
                sources.add(new Source("fountain", Math.min(1, le.getMaxHealth() / 100)));
        if (le instanceof RoyalGuardUnit guard) {
            BattleRagePassive rage = guard.getBattleRage();
            if (rage != null && rage.getRank(guard) > 0)
                sources.add(new Source("battle_rage", (float) rage.getHpRegen(guard)));
        }
        return sources;
    }

    public static List<Source> mana(HeroUnit hero) {
        List<Source> sources = new ArrayList<>();
        AttributeInstance attr = ((LivingEntity) hero).getAttribute(AttributeRegistrar.MANA_REGEN_PER_SECOND.get());
        float base = attr != null ? (float) attr.getBaseValue() : 0;
        float bonus = hero.getManaRegenPerSecond() - base;
        if (base != 0)
            sources.add(new Source("base", base));
        if (bonus != 0) // effects, wraith snow
            sources.add(new Source("bonus", bonus));
        if (hero instanceof EnchanterUnit enchanter && enchanter.isAuraEnabled())
            sources.add(new Source("march", -MarchOfProgress.MANA_COST_PER_SECOND));
        return sources;
    }
}
