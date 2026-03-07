package com.solegendary.reignofnether.ability.heroAbilities.royalguard;

//The lower the guard's health, the more damage, resistance and life regen he gains
//Higher levels increase the amount of damage and resistance gained

// show vanilla villager angry clouds when the guard is hit

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BattleRagePassive extends HeroAbility {

    public float maxHpRegen = 1.2f;
    public float manaPerDmgTaken = 0.4f;

    public BattleRagePassive() {
        super(3, 0, UnitAction.NONE, 0, 0, 0, false);
    }

    public boolean rankUp(HeroUnit hero) {
        if (super.rankUp(hero)) {
            updateStatsForRank(hero);
            return true;
        }
        return false;
    }

    @Override
    public void updateStatsForRank(HeroUnit hero) {
        if (getRank(hero) == 1) {
            maxHpRegen = 1.2f;
            manaPerDmgTaken = 0.3f;
        } else if (getRank(hero) == 2) {
            maxHpRegen = 1.8f;
            manaPerDmgTaken = 0.4f;
        } else if (getRank(hero) == 3) {
            maxHpRegen = 2.4f;
            manaPerDmgTaken = 0.5f;
        }
    }

    public double getHpRegen(HeroUnit hero) {
        float healthRatio = 1f - (((LivingEntity) hero).getHealth() / ((LivingEntity) hero).getMaxHealth());
        return MyMath.round(healthRatio * maxHpRegen, 1);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Battle Rage",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/battle_rage.png"),
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
                "Battle Rage",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/battle_rage.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.battle_rage") + " " + rankString(hero), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip1", maxHpRegen)),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip2", manaPerDmgTaken)),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip3", getHpRegen(hero)))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.battle_rage"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip1", maxHpRegen)),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip2", manaPerDmgTaken)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.battle_rage.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.battle_rage.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.battle_rage.rank3"), getRank(hero) == 2)
        );
    }
}
