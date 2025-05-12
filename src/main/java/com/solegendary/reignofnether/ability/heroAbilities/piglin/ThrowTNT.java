package com.solegendary.reignofnether.ability.heroAbilities.piglin;

//Throws a TNT item which, when it hits the ground, becomes a full block of TNT that explodes after a delay
//Higher levels raise the damage
//Greed is Good reduces the cooldown

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class ThrowTNT extends HeroAbility {

    public static final int RANGE = 12;
    private static float explosionPower = 2;
    private static int LESS_COOLDOWN_PER_100_RESOURCES = 5 * ResourceCost.TICKS_PER_SECOND;

    public ThrowTNT() {
        super(3, UnitAction.THROW_TNT, 25 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
    }

    @Override
    public boolean rankUp(HeroUnit hero) {
        if (super.rankUp(hero)) {
            updateStatsForRank();
            return true;
        }
        return false;
    }

    public void updateStatsForRank() {
        if (rank == 1) {
            explosionPower = 2;
        } else if (rank == 2) {
            explosionPower = 3;
        } else if (rank == 3) {
            explosionPower = 4;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit hero) {
        return new AbilityButton("Throw TNT",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/tnt.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.THROW_TNT,
                () -> rank == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.THROW_TNT),
                null,
                getTooltipLines((HeroUnit) hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Throw TNT",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/tnt.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.throw_tnt") + " " + rankString(), true),
                fcsIcons(I18n.get("abilities.reignofnether.throw_tnt.stats", Math.round(explosionPower * 6.67f), cooldownMax / 20, RANGE)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.tooltip2", LESS_COOLDOWN_PER_100_RESOURCES / 20))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.throw_tnt"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.tooltip2", LESS_COOLDOWN_PER_100_RESOURCES / 20)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.throw_tnt.rank3"), rank == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((PiglinMerchantUnit) unitUsing).getCastTNTGoal().setAbility(this);
        ((PiglinMerchantUnit) unitUsing).getCastTNTGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((PiglinMerchantUnit) unitUsing).getCastTNTGoal().setAbility(this);
        ((PiglinMerchantUnit) unitUsing).getCastTNTGoal().setTarget(targetEntity);
    }
}
