package com.solegendary.reignofnether.ability.heroAbilities.monster;

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
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SoulSiphonPassive extends HeroAbility {

    public static int RANGE = 16;
    public float souls = 0;
    public float soulsPerCast = 0;
    public float soulsMax = 0;

    public SoulSiphonPassive() {
        super(3, UnitAction.NONE, 0, 0, 0, false);
        this.autocastEnableAction = UnitAction.ENABLE_SOUL_SIPHON_PASSIVE;
        this.autocastDisableAction = UnitAction.DISBLE_SOUL_SIPHON_PASSIVE;
        this.setDefaultAutocast(true);
    }

    public boolean rankUp(HeroUnit hero) {
        if (super.rankUp(hero)) {
            updateStatsForRank();
            return true;
        }
        return false;
    }

    public void updateStatsForRank() {
        if (rank == 1) {
            soulsPerCast = 4;
            soulsMax = 20;
        } else if (rank == 2) {
            soulsPerCast = 7;
            soulsMax = 30;
        } else if (rank == 3) {
            soulsPerCast = 10;
            soulsMax = 40;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit hero) {
        AbilityButton button = new AbilityButton("Soul Siphon",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
            hotkey,
            () -> this.getAutocast(hero),
            () -> rank == 0,
            () -> true,
            () -> toggleAutocast(hero),
            null,
            getTooltipLines((HeroUnit) hero),
            this,
            hero
        );
        button.extraLabel = String.valueOf((int) souls);
        if (((int) souls) <= 0)
            button.extraLabelColour = 0xFF0000;
        else if (((int) souls) >= ((int) soulsMax))
            button.extraLabelColour = 0x00FF00;
        else if (((int) souls) <= ((int) soulsPerCast))
            button.extraLabelColour = 0xFFFF00;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
            "Soul Siphon",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
            hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.soul_siphon") + " " + rankString(), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip3", soulsPerCast, soulsMax)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip4"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.soul_siphon"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip3", soulsPerCast, soulsMax)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.can_be_toggled")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.rank3"), rank == 2)
        );
    }

    // returns amount of souls consumed
    public boolean consumeSouls(HeroUnit unit) {
        if (getAutocast(unit) && souls >= soulsPerCast) {
            souls -= soulsPerCast;
            return true;
        }
        return false;
    }

    private int lastEntityKilledId = -1; // LivingDeathEvent sometimes fires twice

    public void checkAndGainSouls(LivingEntity entityKilled, int splitAmount, HeroUnit hero) {
        if (soulsMax > 0 && entityKilled.getId() != lastEntityKilledId) {
            if (entityKilled instanceof Unit unit && !unit.getOwnerName().equals(hero.getOwnerName()))
                souls += unit.getCost().population;
            lastEntityKilledId = entityKilled.getId();
        }
        if (souls > soulsMax)
            souls = soulsMax;
        if (((LivingEntity) hero).level().isClientSide())
            hero.updateAbilityButtons();
    }
}
