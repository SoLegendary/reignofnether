package com.solegendary.reignofnether.ability.heroAbilities.villager;

//Forces all nearby enemy units to lose control for a few seconds and target the royal guard
//While active, the guard gains knockback and push immunity
//Higher levels incease the taunt duration

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class TauntingCry extends HeroAbility {

    public static final int RANGE = 6;
    private static final int CD_MAX_SECONDS = 50 * ResourceCost.TICKS_PER_SECOND;
    public int duration = 4 * ResourceCost.TICKS_PER_SECOND;
    public static final float DAMAGE_MULT = 0.5f;

    public TauntingCry(HeroUnit hero) {
        super(hero, 3, 60, UnitAction.TAUNTING_CRY, CD_MAX_SECONDS, RANGE, 0, false);
    }

    @Override
    public boolean rankUp() {
        if (super.rankUp()) {
            updateStatsForRank();
            return true;
        }
        return false;
    }

    public void updateStatsForRank() {
        if (rank == 1) {
            duration = 4 * ResourceCost.TICKS_PER_SECOND;
        } else if (rank == 2) {
            duration = 6 * ResourceCost.TICKS_PER_SECOND;
        } else if (rank == 3) {
            duration= 8 * ResourceCost.TICKS_PER_SECOND;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton("Taunting Cry",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/ominous_banner.png"),
                hotkey,
                () -> false,
                () -> rank <= 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.TAUNTING_CRY),
                null,
                getTooltipLines(),
                this
        );
    }

    @Override
    public Button getRankUpButton() {
        return super.getRankUpButtonProtected(
                "Taunting Cry",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/ominous_banner.png")
        );
    }

    public List<FormattedCharSequence> getTooltipLines() {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.taunting_cry") + " " + rankString(), true),
                fcsIcons(I18n.get("abilities.reignofnether.taunting_cry.stats", CD_MAX_SECONDS / 20, range, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip2", duration / 20)),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines() {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.taunting_cry"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle()),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip2", duration / 20)),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.tooltip3")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.taunting_cry.rank3"), rank == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().setAbility(this);
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().setAbility(this);
        ((RoyalGuardUnit) unitUsing).getCastTauntingCryGoal().startCasting();
    }
}
