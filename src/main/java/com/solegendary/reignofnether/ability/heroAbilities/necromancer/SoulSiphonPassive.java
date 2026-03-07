package com.solegendary.reignofnether.ability.heroAbilities.necromancer;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SoulSiphonPassive extends HeroAbility {

    public static int RANGE = 16;
    public int soulsPerCast = 0;
    public int soulsMax = 0;

    public int soulsConsumedForHealth = 10;
    public float healthPerSoul = 1.5f;

    public SoulSiphonPassive() {
        super(3, 0, UnitAction.SOUL_SIPHON_HEAL, 0, 0, 0, false);
        this.autocastEnableAction = UnitAction.ENABLE_SOUL_SIPHON_PASSIVE;
        this.autocastDisableAction = UnitAction.DISBLE_SOUL_SIPHON_PASSIVE;
        this.setDefaultAutocast(true);
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
            soulsPerCast = 4;
            soulsMax = 20;
        } else if (getRank(hero) == 2) {
            soulsPerCast = 7;
            soulsMax = 30;
        } else if (getRank(hero) == 3) {
            soulsPerCast = 10;
            soulsMax = 40;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof NecromancerUnit necro)) return null;
        AbilityButton button = new AbilityButton("Soul Siphon",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/soul_siphon.png"),
            hotkey,
            () -> this.isAutocasting(necro),
            () -> getRank(necro) == 0,
            () -> true,
            () -> toggleAutocast(necro),
            () -> UnitClientEvents.sendUnitCommand(UnitAction.SOUL_SIPHON_HEAL),
            getTooltipLines((HeroUnit) necro),
            this,
            necro
        );
        button.extraLabel = String.valueOf(necro.souls);
        if ((necro.souls) <= 0)
            button.extraLabelColour = 0xFF0000;
        else if ((necro.souls) >= ((int) soulsMax))
            button.extraLabelColour = 0x00FF00;
        else if ((necro.souls) <= ((int) soulsPerCast))
            button.extraLabelColour = 0xFFFF00;
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
            "Soul Siphon",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/soul_siphon.png"),
            hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.soul_siphon") + " " + rankString(hero), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip3", (int) soulsPerCast, (int) soulsMax)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip4", (int) soulsConsumedForHealth, healthPerSoul)),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip5"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.soul_siphon"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.tooltip3", (int) soulsPerCast, (int) soulsMax)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.can_be_toggled")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.soul_siphon.rank3"), getRank(hero) == 2)
        );
    }

    // returns whether the consumption was successful
    public boolean consumeSoulsForCast(NecromancerUnit hero) {
        if (isAutocasting(hero) && hero.souls >= soulsPerCast) {
            hero.souls -= soulsPerCast;
            if (!hero.level().isClientSide())
                AbilityClientboundPacket.doAbility(hero.getId(), UnitAction.SOUL_SIPHON_UPDATE, hero.souls);
            addUnitPoofs(soulsPerCast * 3, hero);
            return true;
        }
        return false;
    }

    // consume souls for health
    @Override
    public void use(Level level, Unit hero, BlockPos targetBp) {
        if (!(hero instanceof NecromancerUnit necro))
            return;
        int soulsConsumed = Math.min(necro.souls, soulsConsumedForHealth);
        if (soulsConsumed > 0) {
            LivingEntity entity = (LivingEntity) hero;
            necro.souls -= soulsConsumed;
            if (!level.isClientSide()) {
                AbilityClientboundPacket.doAbility(entity.getId(), UnitAction.SOUL_SIPHON_UPDATE, necro.souls);
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.CAST_SPELL, entity);
            }
            entity.heal(soulsConsumed * healthPerSoul);
            addUnitPoofs(soulsConsumed * 3, hero);
        }
    }

    private void addUnitPoofs(int amount, Unit hero) {
        MiscUtil.addParticleExplosion(ParticleTypes.WITCH, amount, ((Entity) hero).level(), ((Entity) hero).getEyePosition());
    }

    public void checkAndGainSouls(LivingEntity entityKilled, int splitAmount, NecromancerUnit necro) {
        if (soulsMax > 0) {
            if (entityKilled instanceof Unit unit && !unit.getOwnerName().equals(necro.getOwnerName()))
                necro.souls += (unit.getCost().population / splitAmount);
        }
        if (necro.souls > soulsMax)
            necro.souls = soulsMax;
        if (necro.level().isClientSide())
            necro.updateAbilityButtons();
    }
}
