package com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ChillingPresencePassive extends HeroAbility {

    public ChillingPresencePassive() {
        super(3, 35, UnitAction.NONE, 0, 0, 0, false);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        return new AbilityButton("Chilling Presence",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/snow.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) == 0,
                () -> true,
                null,
                null,
                getTooltipLines(hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Chilling Presence",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/snow.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.chilling_presence") + " " + rankString(hero), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.chilling_presence"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.tooltip3")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.chilling_presence.rank3"), getRank(hero) == 2)
        );
    }
}
