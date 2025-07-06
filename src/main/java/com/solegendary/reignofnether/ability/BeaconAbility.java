package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;

import static com.solegendary.reignofnether.resources.ResourceCost.TICKS_PER_SECOND;

public abstract class BeaconAbility extends Ability {
    protected final MobEffect effect;

    public static final int CD_MAX = 5 * TICKS_PER_SECOND;

    public BeaconAbility(UnitAction action, MobEffect effect) {
        super(
                action,
                CD_MAX,
                0,
                0,
                false,
                true
        );
        this.effect = effect;
    }

    private void setToMaxCooldownAllAbiltities(BeaconPlacement beacon) {
        for (Ability ability : beacon.getAbilities())
            ability.setToMaxCooldown(beacon);
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos bp) {
        if (!(buildingUsing instanceof BeaconPlacement)) return;
        BeaconPlacement beacon = (BeaconPlacement) buildingUsing;
        beacon.setAuraEffect(effect);
        setToMaxCooldownAllAbiltities(beacon);
    }
}
