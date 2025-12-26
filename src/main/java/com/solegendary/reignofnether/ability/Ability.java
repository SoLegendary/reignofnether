package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

public class Ability {
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)
    public float cooldownMax;
    public float range; // if <= 0, is melee
    public float radius; // if <= 0, is single target
    public final boolean canTargetEntities;
    public boolean oneClickOneUse; // if true, a group of units/buildings will use their abilities one by one
    public UnitAction autocastEnableAction = null;
    public UnitAction autocastDisableAction = null;
    public int maxCharges = 1;
    private boolean defaultAutocast = false;
    public void setAutocast(boolean value, Unit unit) { unit.setAutocast(value ? this : null); }
    public void setAutocast(boolean value, BuildingPlacement placement) { placement.setAutocast(value ? this : null); }
    public boolean isAutocasting(Unit unit) { return unit.hasAutocast(this); }
    public boolean isAutocasting(BuildingPlacement placement) { return placement.hasAutocast(this); }
    protected Keybinding defaultHotkey = Keybindings.keyQ;

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
        if (!((Entity) unit).level().isClientSide())
            return;

        if (isAutocasting(unit) && autocastDisableAction != null) {
            sendUnitCommand(autocastDisableAction);
        } else if (!isAutocasting(unit) && autocastEnableAction != null) {
            sendUnitCommand(autocastEnableAction);
        }
    }

    protected void toggleAutocast(BuildingPlacement placement) {
        if (!placement.level.isClientSide())
            return;

        if (isAutocasting(placement) && autocastDisableAction != null) {
            sendUnitCommand(autocastDisableAction);
        } else if (!isAutocasting(placement) && autocastEnableAction != null) {
            sendUnitCommand(autocastEnableAction);
        }
    }

    public boolean isCasting(Unit unit) { return false; }

    public float getCooldown(Unit unit) { return unit.getCooldown(this); }
    public float getCooldown(BuildingPlacement placement) { return placement.getCooldown(this); }

    public boolean isOffCooldown(Unit unit) { return getCooldown(unit) <= 0 || (usesCharges() && getCharges(unit) > 0); }
    public boolean isOffCooldown(BuildingPlacement placement) { return getCooldown(placement) <= 0 || (usesCharges() && getCharges(placement) > 0); }

    public void setToMaxCooldown(Unit unit) {
        unit.setCooldown(this, cooldownMax);
        if (usesCharges() && unit.getCharges(this) > 0)
            unit.setCharges(this, unit.getCharges(this) - 1);
    }

    public void setToMaxCooldown(BuildingPlacement building) {
        building.setCooldown(this, cooldownMax);
        if (usesCharges() && building.getCharges(this) > 0)
            building.setCharges(this, building.getCharges(this) - 1);
    }

    public void setCooldown(float cooldown, Unit unit) {
        this.setCooldown(cooldown, true, unit);
    }

    public void setCooldown(float cooldown, boolean useCharge, Unit unit) {
        if (((Entity) unit).level().isClientSide() && cooldown > 0) {
            HudClientEvents.setLowestCdHudEntity();
        }
        unit.setCooldown(this, Math.min(cooldown, cooldownMax));
        if (useCharge && usesCharges() && unit.getCharges(this) > 0)
            unit.setCharges(this, unit.getCharges(this) - 1);
    }

    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) { }

    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) { }

    public void use(Level level, Unit unitUsing, BlockPos targetBp) { }

    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) { }

    // assigns a default hotkey
    public AbilityButton getButton(BuildingPlacement placement) {
        return getButton(defaultHotkey, placement);
    }

    public AbilityButton getButton(Unit unit) {
        return getButton(defaultHotkey, unit);
    }

    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return null;
    }
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return null;
    }

    public boolean canBypassCooldown(Unit unit) { return usesCharges() && getCharges(unit) > 0; }
    public boolean canBypassCooldown(BuildingPlacement buildingPlacement) { return usesCharges() && getCharges(buildingPlacement) > 0; }

    public boolean shouldResetBehaviours() { return true; }

    protected void setDefaultAutocast(boolean b) {
        defaultAutocast = b;
    }

    public boolean isDefaultAutocast() {
        return defaultAutocast;
    }

    public int getCharges(Unit unit) {
        return unit.getCharges(this);
    }

    public int getCharges(BuildingPlacement placement) {
        return placement.getCharges(this);
    }

    public void setCharges(Unit unit, int charges) {
        unit.setCharges(this, charges);
    }
}
