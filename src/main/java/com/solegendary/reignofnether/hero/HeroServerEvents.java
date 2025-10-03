package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.HeroUnitSave;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;

public class HeroServerEvents {

    public static ArrayList<HeroUnitSave> fallenHeroes = new ArrayList<>();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent evt) {
        if (evt.getEntity().level().isClientSide())
            return;

        Level level = evt.getEntity().level();
        if (evt.getEntity() instanceof Unit deadUnit) {
            int popCost = deadUnit.getCost().population;
            if (popCost > 0) {
                for (LivingEntity unit : UnitServerEvents.getAllUnits()) {
                    boolean inRange = unit.distanceToSqr((LivingEntity) deadUnit) < HeroExperienceOrb.RANGE * HeroExperienceOrb.RANGE;
                    if (unit instanceof HeroUnit heroUnit && inRange && heroUnit != evt.getEntity()) {
                        String heroOwner = ((Unit) heroUnit).getOwnerName();
                        String deadOwner = deadUnit.getOwnerName();

                        if (!AlliancesServerEvents.isAllied(heroOwner, deadOwner) && !heroOwner.equals(deadOwner) &&
                                heroUnit.getHeroLevel() < HeroUnit.MAX_LEVEL &&
                                (heroUnit.getHeroLevel() < HeroUnit.MAX_NEUTRAL_EXP_LEVEL || !deadOwner.isBlank())) {

                            int expValue = (popCost + 1) * 5;
                            if (evt.getEntity() instanceof HeroUnit killedHero)
                                expValue += killedHero.getHeroLevel() * 5;

                            while (expValue > 0) {
                                HeroExperienceOrb expOrb = HeroExperienceOrb.newOrb(level,
                                        heroUnit,
                                        deadOwner.isBlank(),
                                        evt.getEntity().getX(),
                                        evt.getEntity().getY(),
                                        evt.getEntity().getZ(),
                                        expValue >= 2 ? 2 : 1
                                );
                                expValue -= expValue >= 2 ? 2 : 1;
                                evt.getEntity().level().addFreshEntity(expOrb);
                            }
                        }
                    }
                }
            }
        }
        // save killed hero unit for revival
        if (evt.getEntity() instanceof HeroUnit heroUnit) {
            String heroName = ((LivingEntity) heroUnit).getName().getString();
            fallenHeroes.removeIf(fHero -> fHero.ownerName.equals(heroUnit.getOwnerName()) && fHero.name.equals(heroName));
            HeroUnitSave fallenHero = new HeroUnitSave(
                    ((Entity) heroUnit).getStringUUID(),
                    heroName,
                    heroUnit.getOwnerName(),
                    heroUnit.getExperience(),
                    heroUnit.getSkillPoints(),
                    0,
                    heroUnit.getHeroAbilities().size() > 0 ? heroUnit.getHeroAbilities().get(0).getRank(heroUnit) : 0,
                    heroUnit.getHeroAbilities().size() > 1 ? heroUnit.getHeroAbilities().get(1).getRank(heroUnit) : 0,
                    heroUnit.getHeroAbilities().size() > 2 ? heroUnit.getHeroAbilities().get(2).getRank(heroUnit) : 0,
                    heroUnit.getHeroAbilities().size() > 3 ? heroUnit.getHeroAbilities().get(3).getRank(heroUnit) : 0
            );
            fallenHeroes.add(fallenHero);
            FallenHeroClientboundPacket.addFallenHero(fallenHero);
            UnitServerEvents.saveFallenHeroUnits((ServerLevel) evt.getEntity().level());

            for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers) {
                if (rtsPlayer.name.equals(heroUnit.getOwnerName()) ||
                    AlliancesServerEvents.isAllied(rtsPlayer.name, heroUnit.getOwnerName())) {
                    PlayerServerEvents.sendMessageToPlayer(rtsPlayer.name, "hud.hero.reignofnether.death", true,
                            heroUnit.getOwnerName(),
                            WordUtils.capitalize(MiscUtil.getSimpleEntityName(evt.getEntity()).replace("_", " ")),
                            heroUnit.getHeroLevel());
                    SoundClientboundPacket.playSoundForPlayer(SoundAction.ENEMY, rtsPlayer.name);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        for (HeroUnitSave fallenHero : fallenHeroes) {
            if (fallenHero.ownerName.equals(evt.getEntity().getName().getString()))
                FallenHeroClientboundPacket.addFallenHero(fallenHero);
        }
    }
}
