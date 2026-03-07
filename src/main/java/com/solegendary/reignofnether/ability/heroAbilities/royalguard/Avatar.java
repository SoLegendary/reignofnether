package com.solegendary.reignofnether.ability.heroAbilities.royalguard;

//Temporarily gain bonus damage, health and movespeed
//While active, the guard's model becomes larger and has a shimmering enchanted effect
// Duration is extended whenever damage is taken from an enemy

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
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

public class Avatar extends HeroAbility {

    private static final int CD_MAX_SECONDS = 300 * ResourceCost.TICKS_PER_SECOND;
    public static final int DURATION = 60 * ResourceCost.TICKS_PER_SECOND;
    public static final float ATTACK_SPLASH_RADIUS = 2.5f;
    public static final float ATTACK_SPLASH_MULT = 0.5f;
    public static final float BONUS_HEALTH = 100;
    public static final float KNOCKBACK = 0.5f;

    public Avatar() {
        super(1, 100, UnitAction.AVATAR, CD_MAX_SECONDS, 0, 0, false);
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof RoyalGuardUnit royalGuardUnit) {
            GenericUntargetedSpellGoal goal = royalGuardUnit.getCastAvatarGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Avatar",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/avatar.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) == 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.AVATAR),
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
                "Avatar",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/avatar.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.avatar"), true),
                fcsIcons(I18n.get("abilities.reignofnether.avatar.stats", CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.avatar.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.avatar.tooltip2", BONUS_HEALTH)),
                fcs(I18n.get("abilities.reignofnether.avatar.tooltip3", DURATION / 20))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.avatar"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.avatar.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.avatar.tooltip2", BONUS_HEALTH)),
                fcs(I18n.get("abilities.reignofnether.avatar.tooltip3", DURATION / 20))
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        use(level, unitUsing);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        use(level, unitUsing);
    }

    private void use(Level level, Unit unitUsing) {
        boolean isAvatarActive = ((RoyalGuardUnit) unitUsing).avatarTicksLeft > 0;
        if (level.isClientSide()) {
            if (isAvatarActive) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.avatar.already_active"));
            }
        }
        if (!isAvatarActive) {
            ((RoyalGuardUnit) unitUsing).getCastAvatarGoal().setAbility(this);
            ((RoyalGuardUnit) unitUsing).getCastAvatarGoal().startCasting();
            ((RoyalGuardUnit) unitUsing).avatarScalingStarted = true;
            if (!level.isClientSide()) {
                SoundClientboundPacket.playSoundAtPos(SoundAction.BLOODLUST, ((LivingEntity) unitUsing).getOnPos().above());
                HeroClientboundPacket.activateAbilityClientside(((RoyalGuardUnit) unitUsing).getId(), 3);
            }
        }
    }
}
