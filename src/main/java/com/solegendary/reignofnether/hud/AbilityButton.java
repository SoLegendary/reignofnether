package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.abilities.Sacrifice;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
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
    @Nullable private Unit unit;
    @Nullable private BuildingPlacement placement;

    public AbilityButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected,
                         Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick,
                         List<FormattedCharSequence> tooltipLines, @Nullable Ability ability, Unit unit) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);

        this.ability = ability;

        Runnable originalOnLeftClick = this.onLeftClick;
        this.onLeftClick = () -> {
            if (this.ability != null && (this.ability.getCooldown(unit) > 0 && !this.ability.canBypassCooldown(unit)))
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.on_cooldown"));
            else if (this.ability instanceof HeroAbility heroAbility && heroAbility.manaCost > 0 && unit instanceof HeroUnit hero && hero.getMana() < heroAbility.manaCost)
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.not_enough_mana"));
            else if (originalOnLeftClick != null)
                originalOnLeftClick.run();
        };
        this.unit = unit;
    }

    public AbilityButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected,
                         Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick,
                         List<FormattedCharSequence> tooltipLines, @Nullable Ability ability, BuildingPlacement placement) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);

        this.ability = ability;

        Runnable originalOnLeftClick = this.onLeftClick;
        this.onLeftClick = () -> {
            if (this.ability != null && (this.ability.getCooldown(placement) > 0 && !this.ability.canBypassCooldown(building)))
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.on_cooldown"));
            else if (originalOnLeftClick != null)
                originalOnLeftClick.run();
        };

        this.placement = placement;
    }

    @Override
    protected void renderHotkey(GuiGraphics guiGraphics, int x, int y) {
        if (this.ability == null || !this.ability.usesCharges())
            super.renderHotkey(guiGraphics, x, y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        float cooldown;

        if (this.ability != null && ability.cooldownMax > 0) {
            if (unit != null) {
                cooldown = ability.getCooldown(unit);
            }else {
                cooldown = ability.getCooldown(placement);
            }
            this.greyPercent = 1.0f - (cooldown / (float) ability.cooldownMax);
        }
        super.render(guiGraphics, x, y, mouseX, mouseY);

        // charges remaining number
        if (this.ability != null && this.ability.usesCharges()) {
            String chargeStr = String.valueOf(unit != null ? unit.getCharges(ability) : building.getCharges(ability));
            guiGraphics.pose().translate(0,0,2);

            int colour = 0xFFFFFF;
            if ((unit != null ? unit.getCharges(ability) : building.getCharges(ability)) >= this.ability.maxCharges)
                colour = 0x00FF00;
            else if ((unit != null ? unit.getCharges(ability) : building.getCharges(ability)) <= 0)
                colour = 0xFF0000;
            else if ((unit != null ? unit.getCharges(ability) : building.getCharges(ability)) == 1)
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
