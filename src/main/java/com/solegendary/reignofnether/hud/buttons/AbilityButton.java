package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

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
    @Nullable private Unit unit;
    @Nullable private BuildingPlacement placement;
    public final boolean isPassive; // nothing happens on left click

    public AbilityButton(String name, ResourceLocation rl, Keybinding hotkey, Supplier<Boolean> isSelected,
                         Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick,
                         List<FormattedCharSequence> tooltipLines, @Nullable Ability ability, Unit unit) {

        // generate x/y based on given position (starting at 0 which is bottom left 1 row above generic action buttons)
        super(name, Button.itemIconSize, rl, hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);

        this.ability = ability;
        this.isPassive = onLeftClick == null;

        Runnable originalOnLeftClick = this.onLeftClick;
        this.onLeftClick = () -> {
            if (this.ability != null && (this.ability.getCooldown(unit) > 0 && !this.ability.canBypassCooldown(unit)))
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.on_cooldown", getCooldown()));
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
        this.isPassive = onLeftClick == null;

        Runnable originalOnLeftClick = this.onLeftClick;
        this.onLeftClick = () -> {
            if (this.ability != null && (this.ability.getCooldown(placement) > 0 && !this.ability.canBypassCooldown(placement)))
                HudClientEvents.showTemporaryMessage(I18n.get("hud.buttons.reignofnether.on_cooldown", getCooldown()));
            else if (originalOnLeftClick != null)
                originalOnLeftClick.run();
        };

        this.placement = placement;
    }

    @Nullable
    public Unit getUnit() {
        return unit;
    }

    public boolean isAutocasting() {
        return unit != null ? ability.isAutocasting(unit) : placement != null && ability.isAutocasting(placement);
    }

    /** Remaining cooldown and its maximum in ticks, or 0 when the button has no cooldown. */
    public float getCooldownTicks() {
        if (ability == null || ability.cooldownMax <= 0)
            return 0;
        if (unit != null)
            return ability.getCooldown(unit);
        return placement != null ? ability.getCooldown(placement) : 0;
    }

    public int getCharges() {
        if (ability == null || !ability.usesCharges())
            return -1;
        return unit != null ? unit.getAbilityCharges(ability) : placement != null ? placement.getCharges(ability) : -1;
    }

    private int getCooldown() {
        if (unit != null) {
            float timeMult = 1 / (1 + unit.getFasterCooldownsPercent());
            return Math.round((this.ability.getCooldown(unit) * timeMult) / 20);
        }
        else if (placement != null)
            return Math.round(this.ability.getCooldown(placement) / 20);
        return 0;
    }

}
