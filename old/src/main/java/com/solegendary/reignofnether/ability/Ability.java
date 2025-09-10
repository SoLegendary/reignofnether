package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

public class Ability {
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)
    public final Level level;
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
    protected Keybinding defaultHotkey = Keybindings.keyQ;

    private boolean autocast = false;
    public void setAutocast(boolean value) { autocast = value; }
    public boolean isAutocasting() { return autocast; }

    public Ability(UnitAction action, Level level, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        this.action = action;
        this.level = level;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
        this.canTargetEntities = canTargetEntities;
        this.oneClickOneUse = false;
    }

    public Ability(UnitAction action, Level level, int cooldownMax, float range, float radius, boolean canTargetEntities, boolean oneClickOneUse) {
        this.action = action;
        this.level = level;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
        this.canTargetEntities = canTargetEntities;
        this.oneClickOneUse = oneClickOneUse;
    }

    public boolean usesCharges() {
        return maxCharges > 1;
    }

    protected void toggleAutocast() {
        if (!level.isClientSide())
            return;

        if (autocast && autocastDisableAction != null) {
            sendUnitCommand(autocastDisableAction);
        } else if (!autocast && autocastEnableAction != null) {
            sendUnitCommand(autocastEnableAction);
        }
    }

    public void tickCooldown() {
        if (this.cooldown > 0 || charges < maxCharges) {
            if (this.level.isClientSide())
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

    public boolean isCasting() { return false; }

    public float getCooldown() { return this.cooldown; }

    public boolean isOffCooldown() {
        return this.cooldown <= 0 || (usesCharges() && charges > 0);
    }

    public void setToMaxCooldown() {
        this.cooldown = cooldownMax;
        if (usesCharges() && charges > 0)
            charges -= 1;
    }

    public void setCooldown(float cooldown) {
        this.setCooldown(cooldown, true);
    }

    public void setCooldown(float cooldown, boolean useCharge) {
        if (this.level.isClientSide() && cooldown > 0) {
            HudClientEvents.setLowestCdHudEntity();
        }
        this.cooldown = Math.min(cooldown, cooldownMax);
        if (useCharge && usesCharges() && charges > 0)
            charges -= 1;
    }

    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) { }

    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) { }

    public void use(Level level, Unit unitUsing, BlockPos targetBp) { }

    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) { }

    // assigns a default hotkey
    public AbilityButton getButton() {
        return getButton(defaultHotkey);
    }

    public AbilityButton getButton(Keybinding hotkey) {
        return null;
    }

    public boolean canBypassCooldown() { return usesCharges() && charges > 0; }

    public boolean shouldResetBehaviours() { return true; }
}
