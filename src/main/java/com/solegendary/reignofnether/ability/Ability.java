package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

public class Ability {
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)
    public float cooldownMax;
    private float cooldown = 0;
    public final float range; // if <= 0, is melee
    public final float radius; // if <= 0, is single target
    public final boolean canTargetEntities;
    public boolean oneClickOneUse; // if true, a group of units/buildings will use their abilities one by one
    public UnitAction autocastEnableAction = null;
    public UnitAction autocastDisableAction = null;
    public int maxCharges = 1;
    public int charges = 1;

    private boolean defaultAutocast = false;
    public void setAutocast(boolean value, Unit unit) { unit.setAutocast(value ? this : null); }
    public boolean getAutocast(Unit unit) { return unit.hasAutocast(this); }

    public Ability(UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        this.action = action;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
        this.canTargetEntities = canTargetEntities;
        this.oneClickOneUse = false;
    }

    public Ability(UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities, boolean oneClickOneUse) {
        this.action = action;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
        this.canTargetEntities = canTargetEntities;
        this.oneClickOneUse = oneClickOneUse;
    }

    public boolean usesCharges() {
        return maxCharges > 1;
    }

    protected void toggleAutocast(Unit unit) {
        if (!unit.level().isClientSide())
            return;

        if (getAutocast(unit) && autocastDisableAction != null) {
            sendUnitCommand(autocastDisableAction);
        } else if (!getAutocast(unit) && autocastEnableAction != null) {
            sendUnitCommand(autocastEnableAction);
        }
    }

    public void tickCooldown(Level level) {
        if (this.cooldown > 0 || charges < maxCharges) {
            if (level.isClientSide())
                this.cooldown -= (TPSClientEvents.getCappedTPS() / 20D);
            else
                this.cooldown -= 1;

            if (this.cooldown <= 0 && usesCharges() && charges < maxCharges) {
                charges += 1;
                if (charges < maxCharges)
                    cooldown = cooldownMax;
                if (charges > maxCharges)
                    charges = maxCharges;
            }
        }
    }

    public boolean isChanneling(Unit unit) { return false; }

    public float getCooldown(Unit unit) { return unit.getCooldown(getClass()); }
    public float getCooldown(BuildingPlacement placement) { return placement.getCooldown(getClass()); }

    public boolean isOffCooldown(Unit unit) { return getCooldown(unit) <= 0 || (usesCharges() && charges > 0); }
    public boolean isOffCooldown(BuildingPlacement placement) { return getCooldown(placement) <= 0 || (usesCharges() && charges > 0); }

    public void setToMaxCooldown(Unit unit) {
        unit.setCooldown(getClass(), cooldownMax);
        if (usesCharges() && charges > 0)
            charges -= 1;
    }

    public void setToMaxCooldown(BuildingPlacement building) {
        building.setCooldown(getClass(), cooldownMax);
        if (usesCharges() && charges > 0)
            charges -= 1;
    }

    public void setCooldown(float cooldown, Unit unit) {
        this.setCooldown(cooldown, true, unit);
    }

    public void setCooldown(float cooldown, boolean useCharge, Unit unit) {
        if (unit.level().isClientSide() && cooldown > 0) {
            HudClientEvents.setLowestCdHudEntity();
        }
        unit.setCooldown(this.getClass(), Math.min(cooldown, cooldownMax));
        if (useCharge && usesCharges() && charges > 0)
            charges -= 1;
    }

    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) { }

    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) { }

    public void use(Level level, Unit unitUsing, BlockPos targetBp) { }

    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) { }

    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return null;
    }
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return null;
    }

    public boolean canBypassCooldown() { return usesCharges() && charges > 0; }

    public boolean shouldResetBehaviours() { return true; }

    protected void setDefaultAutocast(boolean b) {
        defaultAutocast = b;
    }

    public boolean isDefaultAutocast() {
        return defaultAutocast;
    }
}
