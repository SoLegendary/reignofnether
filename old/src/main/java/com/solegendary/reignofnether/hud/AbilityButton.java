package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.ability.Ability;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class AbilityButton extends Button {

    // can be null for stuff like production buttons (handled separately)
    // or simple abilities with no cooldown and the logic can be handled entirely in onLeftClick()
    public Ability ability;
    public String extraLabel = "";
    public int extraLabelColour = 0xFFFFFF;

    public AbilityButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected,
                         Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick,
                         List<FormattedCharSequence> tooltipLines, @Nullable Ability ability) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);

        this.ability = ability;

        Runnable originalOnLeftClick = this.onLeftClick;
        this.onLeftClick = () -> {
            if (this.ability != null && (this.ability.getCooldown() > 0 && !this.ability.canBypassCooldown()))
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.on_cooldown"));
            if (this.ability instanceof HeroAbility heroAbility && heroAbility.manaCost > 0 && heroAbility.hero.getMana() < heroAbility.manaCost)
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.not_enough_mana"));
            else if (originalOnLeftClick != null)
                originalOnLeftClick.run();
        };
    }

    @Override
    protected void renderHotkey(GuiGraphics guiGraphics, int x, int y) {
        if (this.ability == null || !this.ability.usesCharges())
            super.renderHotkey(guiGraphics, x, y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (this.ability != null && ability.cooldownMax > 0)
            this.greyPercent = 1.0f - ((float) ability.getCooldown() / (float) ability.cooldownMax);
        super.render(guiGraphics, x, y, mouseX, mouseY);

        // charges remaining number
        if (this.ability != null && this.ability.usesCharges()) {
            String chargeStr = String.valueOf(this.ability.charges);
            guiGraphics.pose().translate(0,0,2);

            int colour = 0xFFFFFF;
            if (this.ability.charges >= this.ability.maxCharges)
                colour = 0x00FF00;
            else if (this.ability.charges <= 0)
                colour = 0xFF0000;
            else if (this.ability.charges == 1)
                colour = 0xFFFF00;

            guiGraphics.drawCenteredString(MC.font,
                    chargeStr,
                    x + iconSize - 5 - (chargeStr.length() * 4),
                    y + iconSize - 1,
                    colour);

            super.renderHotkey(guiGraphics, x, y);
        } else if (this.ability != null && !extraLabel.isBlank()) {
            guiGraphics.pose().translate(0,0,2);
            guiGraphics.drawCenteredString(MC.font,
                    extraLabel,
                    x + iconSize - 7 - (extraLabel.length() >= 1 ? 0 : 5),
                    y + iconSize - 1,
                    extraLabelColour);
        }
    }
}
