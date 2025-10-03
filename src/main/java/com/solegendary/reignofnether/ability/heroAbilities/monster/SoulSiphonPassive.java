package com.solegendary.reignofnether.ability.heroAbilities.monster;

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
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SoulSiphonPassive extends HeroAbility {

    public static int RANGE = 16;
    public float souls = 0;
    public float soulsPerCast = 0;
    public float soulsMax = 0;

    public float soulsConsumedForHealth = 10;
    public float healthPerSoul = 1.5f;

    public SoulSiphonPassive() {
        super(3, 0, UnitAction.SOUL_SIPHON_HEAL, 0, 0, 0, false);
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
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Soul Siphon",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
            hotkey,
            () -> this.isAutocasting(hero),
            () -> getRank(hero) == 0,
            () -> true,
            () -> toggleAutocast(hero),
            () -> UnitClientEvents.sendUnitCommand(UnitAction.SOUL_SIPHON_HEAL),
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
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
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
    public boolean consumeSoulsForCast(HeroUnit hero) {
        if (isAutocasting(hero) && souls >= soulsPerCast) {
            souls -= soulsPerCast;
            if (!hero.level().isClientSide())
                AbilityClientboundPacket.doAbility(((LivingEntity) hero).getId(), UnitAction.SOUL_SIPHON_UPDATE, souls);
            addUnitPoofs((int) (soulsPerCast * 2), hero);
            return true;
        }
        return false;
    }

    // consume souls for health
    @Override
    public void use(Level level, Unit hero, BlockPos targetBp) {
        int soulsConsumed = (int) Math.min(souls, soulsConsumedForHealth);
        if (soulsConsumed > 0) {
            LivingEntity entity = (LivingEntity) hero;
            souls -= soulsConsumed;
            if (!level.isClientSide()) {
                AbilityClientboundPacket.doAbility(entity.getId(), UnitAction.SOUL_SIPHON_UPDATE, souls);
                UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.CAST_SPELL, entity);
            }
            entity.heal(soulsConsumed * healthPerSoul);
            addUnitPoofs(soulsConsumed * 2, hero);
        }
    }

    private void addUnitPoofs(int amount, Unit hero) {
        RandomSource rand = RandomSource.create();
        LivingEntity entity = (LivingEntity) hero;
        for(int j = 0; j < amount; ++j) {
            double d0 = rand.nextGaussian() * 0.2;
            double d1 = rand.nextGaussian() * 0.2;
            double d2 = rand.nextGaussian() * 0.2;
            hero.level().addParticle(ParticleTypes.POOF, entity.getX(), entity.getY(), entity.getZ(), d0, d1, d2);
        }
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
