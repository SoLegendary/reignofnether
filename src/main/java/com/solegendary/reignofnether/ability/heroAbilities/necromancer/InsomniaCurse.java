package com.solegendary.reignofnether.ability.heroAbilities.necromancer;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class InsomniaCurse extends HeroAbility {

    public static final int RANGE = 12;
    public static final float PHANTOM_DAMAGE = 6;
    public static final float PHANTOM_DAMAGE_BONUS_PER_SOUL_RANK = 2f;
    public static final int PHANTOM_MAX_ATTACKS = 6;

    public InsomniaCurse() {
        super(3, 15, UnitAction.INSOMNIA_CURSE, 20 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
        maxCharges = 3;
    }

    @Override
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
            maxCharges = 3;
            cooldownMax = 20 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 2) {
            maxCharges = 4;
            cooldownMax = 17 * ResourceCost.TICKS_PER_SECOND;
        } else if (getRank(hero) == 3) {
            maxCharges = 5;
            cooldownMax= 14 * ResourceCost.TICKS_PER_SECOND;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Curse of Insomnia",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/curse_of_insomnia.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.INSOMNIA_CURSE,
            () -> getRank(hero) == 0,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.INSOMNIA_CURSE),
            null,
            getTooltipLines((HeroUnit) hero),
            this,
            hero
        );
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Curse of Insomnia",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/curse_of_insomnia.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.insomnia_curse") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.insomnia_curse.stats", PHANTOM_DAMAGE, cooldownMax / 20, RANGE, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.insomnia_curse.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.insomnia_curse.tooltip2", PHANTOM_DAMAGE, PHANTOM_DAMAGE_BONUS_PER_SOUL_RANK)),
                fcs(I18n.get("abilities.reignofnether.insomnia_curse.tooltip3", PHANTOM_MAX_ATTACKS)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.charges", maxCharges))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
            fcs(I18n.get("abilities.reignofnether.insomnia_curse"), true),
            fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
            fcs(""),
            fcs(I18n.get("abilities.reignofnether.insomnia_curse.tooltip1")),
            fcs(I18n.get("abilities.reignofnether.insomnia_curse.tooltip2", PHANTOM_DAMAGE, PHANTOM_DAMAGE_BONUS_PER_SOUL_RANK)),
            fcs(I18n.get("abilities.reignofnether.insomnia_curse.tooltip3", PHANTOM_MAX_ATTACKS)),
            fcs(""),
            fcs(I18n.get("abilities.reignofnether.insomnia_curse.rank1"), getRank(hero) == 0),
            fcs(I18n.get("abilities.reignofnether.insomnia_curse.rank2"), getRank(hero) == 1),
            fcs(I18n.get("abilities.reignofnether.insomnia_curse.rank3"), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity == unitUsing)
            return;
        ((NecromancerUnit) unitUsing).getCastPhantomGoal().setAbility(this);
        ((NecromancerUnit) unitUsing).getCastPhantomGoal().setTarget(targetEntity);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), targetBp)) {
            ((NecromancerUnit) unitUsing).getCastPhantomGoal().setAbility(this);
            ((NecromancerUnit) unitUsing).getCastPhantomGoal().setTarget(targetBp);
        }
    }
}
