package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.HeroProductionItem;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.hero.HeroClientEvents;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hero.HeroServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.HeroUnitSave;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.packets.UnitSyncAbilityClientboundPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface HeroUnit extends Unit {

    float EXP_REQ_MULTIPLIER = 1.6f;

    static void tick(HeroUnit heroUnit) {
        if (((LivingEntity) heroUnit).tickCount % 20 == 0) {
            heroUnit.setMana(heroUnit.getMana() + heroUnit.getManaRegenPerSecond());
        }
    }

    static ResourceCost getReviveCost(int heroLevel) {
        return ResourceCost.Unit(
                ResourceCosts.HERO_BASE_REVIVE_COST.food + (Mth.clamp(heroLevel, 1, 10) * ResourceCosts.HERO_EXTRA_REVIVE_COST_PER_LEVEL.food),
                ResourceCosts.HERO_BASE_REVIVE_COST.wood + (Mth.clamp(heroLevel, 1, 10) * ResourceCosts.HERO_EXTRA_REVIVE_COST_PER_LEVEL.wood),
                ResourceCosts.HERO_BASE_REVIVE_COST.ore + (Mth.clamp(heroLevel, 1, 10) * ResourceCosts.HERO_EXTRA_REVIVE_COST_PER_LEVEL.ore),
                (ResourceCosts.HERO_BASE_REVIVE_COST.ticks + (Mth.clamp(heroLevel, 1, 10) * ResourceCosts.HERO_EXTRA_REVIVE_COST_PER_LEVEL.ticks)) / 20,
                ResourceCosts.HERO_BASE_REVIVE_COST.population);
    }

    static List<HeroUnit> getHeroes(boolean isClientside, String ownerName) {
        return getHeroes(isClientside, ownerName, "");
    }

    static List<HeroUnit> getHeroes(boolean isClientside, String ownerName, String unitName) {
        List<LivingEntity> units = isClientside ? UnitClientEvents.getAllUnits() : UnitServerEvents.getAllUnits();
        List<HeroUnit> list = new ArrayList<>();
        for (LivingEntity e : units) {
            if (e instanceof HeroUnit heroUnit &&
                    heroUnit.getOwnerName().equals(ownerName) &&
                    (e.getName().getString().equals(unitName) || unitName.isBlank())) {
                list.add(heroUnit);
            }
        }
        return list;
    }

    static List<ActiveProduction> getHeroesInTraining(boolean isClientside, String ownerName) {
        List<BuildingPlacement> buildings = isClientside ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();
        List<ActiveProduction> productions = new ArrayList<>();
        for (BuildingPlacement b : buildings)
            if (b instanceof ProductionPlacement pb && pb.ownerName.equals(ownerName))
                for (ActiveProduction prod : pb.productionQueue)
                    if (prod.item instanceof HeroProductionItem)
                        productions.add(prod);
        return productions;
    }

    @Nullable
    static HeroUnitSave getFallenHero(boolean isClientSide, String ownerName, String heroName) {
        ArrayList<HeroUnitSave> heroUnits = isClientSide ? HeroClientEvents.fallenHeroes : HeroServerEvents.fallenHeroes;
        for (HeroUnitSave heroUnit : heroUnits) {
            if (heroUnit.ownerName.equals(ownerName) && heroUnit.name.equals(heroName))
                return heroUnit;
        }
        return null;
    }

    static int getNumHeroesOwnedOrInTraining(boolean isClientside, String ownerName) {
        return HeroUnit.getHeroes(isClientside, ownerName).size() +
                HeroUnit.getFallenHeroes(isClientside, ownerName).size() +
                HeroUnit.getHeroesInTraining(isClientside, ownerName).size();
    }

    static List<HeroUnitSave> getFallenHeroes(boolean isClientSide, String ownerName) {
        ArrayList<HeroUnitSave> heroUnits = isClientSide ? HeroClientEvents.fallenHeroes : HeroServerEvents.fallenHeroes;
        heroUnits.removeIf(h -> !h.ownerName.equals(ownerName));
        return heroUnits;
    }

    int MAX_LEVEL = 10;
    int MAX_NEUTRAL_EXP_LEVEL = 5; // cannot gain exp from neutral enemies at or past this level

    float getHealthBonusPerLevel();
    float getAttackBonusPerLevel();
    float getBaseHealth();
    float getBaseAttack();

    float getBaseMaxMana();
    float getMana();
    void setMana(float amount);
    float getMaxMana();
    void setMaxMana(float amount);
    float getManaRegenPerSecond();
    float getManaBonusPerLevel();

    int getSkillPoints();
    void setSkillPoints(int points);
    boolean isRankUpMenuOpen();
    void showRankUpMenu(boolean show);
    int getExperience();
    void setExperience(int experience);
    default int getChargesForSaveData() { return 0; } // track stats like necromancer souls in save data
    default void setChargesFromSaveData(int charges) { }

    default void setStatsForLevel() {
        setStatsForLevel(false);
    }

    default void setStatsForLevel(boolean heal) {
        AttributeInstance aiMaxHealth = ((LivingEntity) this).getAttribute(Attributes.MAX_HEALTH);
        if (aiMaxHealth != null)
            aiMaxHealth.setBaseValue(getBaseHealth() + ((getHeroLevel() - 1) * getHealthBonusPerLevel()));
        AttributeInstance aiAttackDamage = ((LivingEntity) this).getAttribute(Attributes.ATTACK_DAMAGE);
        if (aiAttackDamage != null)
            aiAttackDamage.setBaseValue(getBaseAttack() + ((getHeroLevel() - 1) * getAttackBonusPerLevel()));
        this.setMaxMana(getBaseMaxMana() + ((getHeroLevel() - 1) * getManaBonusPerLevel()));
        if (heal)
            ((LivingEntity) this).setHealth(((LivingEntity) this).getMaxHealth());
    }

    default void addExperience(int amount) {
        if (((LivingEntity) this).level().isClientSide())
            return;
        int levelBefore = getHeroLevel();
        if (levelBefore >= MAX_LEVEL)
            return;

        setExperience(getExperience() + amount);
        int levelDiff = getHeroLevel() - levelBefore;

        HeroClientboundPacket.setExperience(((LivingEntity) this).getId(), getExperience());
        if (levelDiff > 0) {
            setSkillPoints(getSkillPoints() + levelDiff);
            HeroClientboundPacket.setSkillPoints(((LivingEntity) this).getId(), getSkillPoints());
            SoundClientboundPacket.playSoundAtPos(SoundAction.LEVEL_UP, ((LivingEntity) this).getOnPos());
            setStatsForLevel();
            ((LivingEntity) this).heal(levelDiff * getHealthBonusPerLevel());
        }
    }

    // we always track total exp and then reduce down for the UI
    default int getHeroLevel() {
        return getHeroLevel(getExperience());
    }

    static int getHeroLevel(int exp) {
        int level = 0;
        int expToNextLevel = (int) (200 * EXP_REQ_MULTIPLIER);
        do {
            level += 1;
            exp -= expToNextLevel;
            expToNextLevel += (100 * EXP_REQ_MULTIPLIER);
        } while (exp >= 0 && level < MAX_LEVEL);
        return level;
    }

    // @ 4000 exp, show 500/900 (level 8)
    default int getExpOnCurrentLevel() {
        if (getHeroLevel() >= MAX_LEVEL)
            return 0;
        int expToNextLevel = (int) (200 * EXP_REQ_MULTIPLIER);
        int expCount = 0;
        int exp = getExperience();
        while (expCount < exp) {
            if (expCount + expToNextLevel > exp) {
                return exp - expCount;
            }
            expCount += expToNextLevel;
            expToNextLevel += (100 * EXP_REQ_MULTIPLIER);
        }
        return 0;
    }

    default int getExpToNextlevel() {
        if (getHeroLevel() >= MAX_LEVEL)
            return 0;
        return (int) ((getHeroLevel() + 1) * (100 * EXP_REQ_MULTIPLIER));
    }

    default List<HeroAbility> getHeroAbilities() {
        List<HeroAbility> list = new ArrayList<>();
        for (Ability a : getAbilities().get()) {
            if (a instanceof HeroAbility heroAbility) {
                list.add(heroAbility);
            }
        }
        return list;
    }

    // call from addAdditionalSaveData
    default void addHeroUnitSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putInt("experience", getExperience());
        pCompound.putInt("skillPoints", getSkillPoints());
        pCompound.putInt("charges", getChargesForSaveData());
        pCompound.putFloat("mana", getMana());
        pCompound.putFloat("maxMana", getMaxMana());

        List<HeroAbility> abls = getHeroAbilities();
        pCompound.putInt("ability1Rank", abls.size() > 0 ? getHeroAbilityRank(abls.get(0)) : 0);
        pCompound.putInt("ability2Rank", abls.size() > 1 ? getHeroAbilityRank(abls.get(1)) : 0);
        pCompound.putInt("ability3Rank", abls.size() > 2 ? getHeroAbilityRank(abls.get(2)) : 0);
        pCompound.putInt("ability4Rank", abls.size() > 3 ? getHeroAbilityRank(abls.get(3)) : 0);
    }

    // call from readAdditionalSaveData
    default void readHeroUnitSaveData(@NotNull CompoundTag pCompound) {
        setExperience(pCompound.getInt("experience"));
        setSkillPoints(pCompound.getInt("skillPoints"));
        setChargesFromSaveData(pCompound.getInt("charges"));
        setMana(pCompound.getFloat("mana"));
        setMaxMana(pCompound.getFloat("maxMana"));

        List<HeroAbility> abls = getHeroAbilities();
        if (abls.size() > 0) {
            setHeroAbilityRank(abls.get(0), pCompound.getInt("ability1Rank"));
        }
        if (abls.size() > 1) {
            setHeroAbilityRank(abls.get(1), pCompound.getInt("ability2Rank"));
        }
        if (abls.size() > 2) {
            setHeroAbilityRank(abls.get(2), pCompound.getInt("ability3Rank"));
        }
        if (abls.size() > 3) {
            setHeroAbilityRank(abls.get(3), pCompound.getInt("ability4Rank"));
        }
        for (HeroAbility abl : abls)
            abl.updateStatsForRank(this);
    }

    default void activateAbilityClientside(int abilityIndex) { }

    default void deactivateAbilityClientside(int abilityIndex) { }

    default int getHeroAbilityRank(HeroAbility ability) {
        return getHeroAbilityRanks().getOrDefault(ability, 0);
    }

    public default void syncToClients() {
        LivingEntity entity = (LivingEntity) this;
        if (!entity.level().isClientSide()) {
            HeroClientboundPacket.setExperience(entity.getId(), this.getExperience());
            HeroClientboundPacket.setSkillPoints(entity.getId(), this.getSkillPoints());
            HeroClientboundPacket.setCharges(entity.getId(), this.getChargesForSaveData());
            List<HeroAbility> abls = this.getHeroAbilities();
            if (abls.size() > 0)
                HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(0).getRank(this), 0);
            if (abls.size() > 1)
                HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(1).getRank(this), 1);
            if (abls.size() > 2)
                HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(2).getRank(this), 2);
            if (abls.size() > 3)
                HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(3).getRank(this), 3);
        }
    }

    default void setHeroAbilityRank(HeroAbility ability, int rank) {
        getHeroAbilityRanks().put(ability, rank);
    }

    Object2ObjectArrayMap<HeroAbility,Integer> getHeroAbilityRanks();
}


