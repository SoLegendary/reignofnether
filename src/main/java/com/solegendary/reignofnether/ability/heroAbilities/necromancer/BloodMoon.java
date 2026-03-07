package com.solegendary.reignofnether.ability.heroAbilities.necromancer;

//Forces day to night under a blood red moon for the entire world temporarily
//Raises the movement and attack speed of all of your units while active (other monster players' units are unaffected)
//Soul Siphon extends the duration

// play a cave sound and announce "A blood moon rises" in global chat

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.time.TimeServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
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

public class BloodMoon extends HeroAbility {

    public static final String ENEMY_NAME = "Blood Moon";

    public static final int SPAWN_INTERVAL_TICKS = 120; // how often to spawn a unit
    public static final int CHANNEL_TICKS = 0;
    private static final int CD_MAX = 420 * ResourceCost.TICKS_PER_SECOND;
    public static final int DURATION = 75 * ResourceCost.TICKS_PER_SECOND;
    public static final int BONUS_DURATION_PER_SOUL_RANK = 10 * ResourceCost.TICKS_PER_SECOND;
    public static final int RADIUS = 25;

    public BloodMoon() {
        super(1, 175, UnitAction.BLOOD_MOON, CD_MAX, 0, 0, false);
    }

    @Override
    public boolean isCasting(Unit hero) {
        if (hero instanceof NecromancerUnit necromancerUnit) {
            GenericTargetedSpellGoal goal = necromancerUnit.getCastBloodMoonGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Blood Moon",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/blood_moon.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.BLOOD_MOON,
            () -> getRank(hero) == 0,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.BLOOD_MOON),
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
                "Blood Moon",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/blood_moon.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.blood_moon"), true),
                fcsIcons(I18n.get("abilities.reignofnether.blood_moon.stats", CD_MAX / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip3")),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip4", DURATION / 20, BONUS_DURATION_PER_SOUL_RANK / 20))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.blood_moon"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip3")),
                fcs(I18n.get("abilities.reignofnether.blood_moon.tooltip4", DURATION / 20, BONUS_DURATION_PER_SOUL_RANK / 20))
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        BuildingPlacement bpl = BuildingUtils.findBuilding(level.isClientSide(), targetBp);

        if (bpl != null && !bpl.ownerName.isBlank() && !bpl.ownerName.equals(unitUsing.getOwnerName()))
            use(level, unitUsing, bpl);
        else if (level.isClientSide() && (bpl == null || bpl.ownerName.isBlank() || bpl.ownerName.equals(unitUsing.getOwnerName())))
            HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.blood_moon.error"));
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (level.isClientSide())
            HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.blood_moon.error"));
    }

    private void use(Level level, Unit unitUsing, BuildingPlacement targetBpl) {
        boolean isBloodMoonActive;
        if (level.isClientSide()) {
            isBloodMoonActive = TimeClientEvents.isBloodMoonActive();
            if (isBloodMoonActive) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.blood_moon.already_active"));
            }
        } else {
            isBloodMoonActive = TimeServerEvents.isBloodMoonActive();
        }

        if (!isBloodMoonActive) {
            ((NecromancerUnit) unitUsing).getCastBloodMoonGoal().setAbility(this);
            ((NecromancerUnit) unitUsing).getCastBloodMoonGoal().startCasting();
            ((NecromancerUnit) unitUsing).getCastBloodMoonGoal().setTarget(targetBpl);
        }
    }
}
