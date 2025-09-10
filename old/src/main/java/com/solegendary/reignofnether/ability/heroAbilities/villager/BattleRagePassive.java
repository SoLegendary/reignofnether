package com.solegendary.reignofnether.ability.heroAbilities.villager;

//The lower the guard's health, the more damage, resistance and life regen he gains
//Higher levels increase the amount of damage and resistance gained

// show vanilla villager angry clouds when the guard is hit

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
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

    public BattleRagePassive(HeroUnit hero) {
        super(hero, 3, 0, UnitAction.NONE, 0, 0, 0, false);
    }

    public boolean rankUp() {
        if (super.rankUp()) {
            updateStatsForRank();
            return true;
        }
        return false;
    }

    public void updateStatsForRank() {
        if (rank == 1) {
            maxHpRegen = 1.2f;
            manaPerDmgTaken = 0.4f;
        } else if (rank == 2) {
            maxHpRegen = 1.8f;
            manaPerDmgTaken = 0.6f;
        } else if (rank == 3) {
            maxHpRegen = 2.4f;
            manaPerDmgTaken = 0.8f;
        }
    }

    public double getHpRegen() {
        float healthRatio = 1f - (((LivingEntity) hero).getHealth() / ((LivingEntity) hero).getMaxHealth());
        return MyMath.round(healthRatio * maxHpRegen, 1);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton("Battle Rage",
                new ResourceLocation("minecraft", "textures/block/redstone_block.png"),
                hotkey,
                () -> false,
                () -> rank == 0,
                () -> true,
                null,
                null,
                getTooltipLines(),
                this
        );
    }

    @Override
    public Button getRankUpButton() {
        return super.getRankUpButtonProtected(
                "Battle Rage",
                new ResourceLocation("minecraft", "textures/block/redstone_block.png")
        );
    }

    public List<FormattedCharSequence> getTooltipLines() {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.battle_rage") + " " + rankString(), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip1", maxHpRegen)),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip2", manaPerDmgTaken)),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip3", getHpRegen()))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines() {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.battle_rage"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle()),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip1", maxHpRegen)),
                fcs(I18n.get("abilities.reignofnether.battle_rage.tooltip2", manaPerDmgTaken)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.battle_rage.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.battle_rage.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.battle_rage.rank3"), rank == 2)
        );
    }
}
