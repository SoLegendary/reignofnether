package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

public class AbilityButton extends Button {

    public final int cooldownMax;
    public int cooldown = 0;
    public final float range; // if <= 0, is melee
    public final float radius; // if <= 0, is single target
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)

    public AbilityButton(String abilityName, int iconSize, ResourceLocation rl,
                         Keybinding hotkey, Supplier<Boolean> isSelected, Supplier<Boolean> isHidden,
                         Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick, List<FormattedCharSequence> tooltipLines,
                         UnitAction action, int cooldownMax, float range, float radius) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(abilityName, iconSize, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);

        Runnable originalOnLeftClick = this.onLeftClick;
        this.onLeftClick = () -> {
            if (this.cooldown > 0)
                HudClientEvents.showTemporaryMessage("This ability is still on cooldown");
            else
                originalOnLeftClick.run();
        };
        this.action = action;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
    }

    @Override
    public void render(PoseStack poseStack, int x, int y, int mouseX, int mouseY) {
        if (cooldownMax > 0)
            this.greyPercent = 1.0f - ((float) cooldown / (float) cooldownMax);
        super.render(poseStack, x, y, mouseX, mouseY);

        // TODO: render number of seconds left on cooldown
    }

    public void tickCooldown() {
        if (this.cooldown > 0)
            this.cooldown -= 1;
    }

    public void setCooldown() {
        this.cooldown = cooldownMax;
    }
}
