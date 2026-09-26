package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.survival.Wave;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Faction {
	
	public ResourceLocation idleWorkerIcon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
	public boolean hasCubeMap = true;
	public boolean playable = true;
	public ResourceLocation key;
	public ResourceLocation workerEntityType;
	public ResourceLocation scoutEntityType;
	public ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/sheep.png");
	public ResourceLocation capitolBuilding;
	public Predicate<CustomBuilding> isBuildableCustomBuilding = null;
	public SoundEvent sound;
	public BiConsumer<ServerLevel, Wave> spawnWave;
	ArrayList<BuildingPlaceButton> buildingButtons = new ArrayList<>();
	ArrayList<UnitSpawnButton> entityButtons = new ArrayList<>();
	
	public Faction() {
	}
	
	public void addEntityButton(UnitSpawnButton button) {
		this.entityButtons.add(button);
	}
	
	public Faction setSound(SoundEvent sound) {
		this.sound = sound;
		return this;
	}
	
	public Faction setCustomBuildingCondition(Predicate<CustomBuilding> isBuildableCustomBuilding) {
		this.isBuildableCustomBuilding = isBuildableCustomBuilding;
		return this;
	}
	
	public void addCustomBuildings() {
		if (!this.playable) return;
		CustomBuildingClientEvents.customBuildings.forEach(cb -> {
			if (this.isBuildableCustomBuilding.test(cb))
				this.buildingButtons.add(cb.getWorkerBuildButton(null));
		});
	}
	
	public Faction setSpawnWave(BiConsumer<ServerLevel, Wave> spawnWave) {
		this.spawnWave = spawnWave;
		Factions.SURVIVAL_FACTIONS.add(this.key);
		return this;
	}
	
	public void spawnWave(ServerLevel level, Wave wave) {
		this.spawnWave.accept(level, wave);
	}
	
	public Faction setUnplayable() {
		this.playable = false;
		return this;
	}
	
	public Faction noCubeMap() {
		this.hasCubeMap = false;
		return this;
	}
	
	public Faction setWorkerIcon(ResourceLocation idleWorkerIcon) {
		this.idleWorkerIcon = idleWorkerIcon;
		return this;
	}
	
	public Faction setIcon(ResourceLocation icon) {
		this.icon = icon;
		return this;
	}
	
	public Faction setWorkerEntityType(EntityType<? extends Unit> workerEntityType) {
		this.workerEntityType = EntityType.getKey(workerEntityType);
		return this;
	}
	
	public Faction setScoutEntityType(EntityType<? extends Unit> scoutEntityType) {
		this.scoutEntityType = EntityType.getKey(scoutEntityType);
		return this;
	}
	
	public Faction setKey(ResourceLocation key) {
		this.key = key;
		return this;
	}
	
	public boolean equals(Faction faction) {
		return faction.key.equals(this.key);
	}
	
	public void addBuilding(Building building, Keybinding keybinding) {
		BuildingPlaceButton buildButton = getBuilding(ReignOfNetherRegistries.BUILDING.getKey(building)).getBuildButton(keybinding);
		buildingButtons.add(buildButton);
	}
	
	public List<BuildingPlaceButton> getBuildingButtons() {
		return buildingButtons;
	}
	
	public ArrayList<UnitSpawnButton> getEntityButtons() {
		return entityButtons;
	}
	
	public String getName() {
		return key.getPath();
	}
	
	public Building getBuilding(ResourceLocation rl) {
		return ReignOfNetherRegistries.BUILDING.get(rl);
	}
}
