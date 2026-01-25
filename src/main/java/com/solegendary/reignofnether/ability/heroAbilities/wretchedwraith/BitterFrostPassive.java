package com.solegendary.reignofnether.ability.heroAbilities.wretchedwraith;

import com.solegendary.reignofnether.ReignOfNether;
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

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BitterFrostPassive extends HeroAbility {

    public static final int SNOW_NO_MELT_RANGE = 5;

    public BitterFrostPassive() {
        super(3, 35, UnitAction.NONE, 0, 0, 0, false);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Bitter Frost",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bitter_frost.png"),
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
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Bitter Frost",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/bitter_frost.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        ArrayList<FormattedCharSequence> tooltipLines = new ArrayList<>(List.of(
                fcs(I18n.get("abilities.reignofnether.bitter_frost") + " " + rankString(hero), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.tooltip2")),
                fcs("")
        ));
        if (getRank(hero) >= 1) {
            tooltipLines.add(fcs(I18n.get("abilities.reignofnether.bitter_frost.rank1")));
        }
        if (getRank(hero) >= 2) {
            tooltipLines.add(fcs(I18n.get("abilities.reignofnether.bitter_frost.rank2")));
        }
        if (getRank(hero) >= 3) {
            tooltipLines.add(fcs(I18n.get("abilities.reignofnether.bitter_frost.rank3")));
        }
        return tooltipLines;
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.bitter_frost"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.bitter_frost.rank3"), getRank(hero) == 2)
        );
    }
}
