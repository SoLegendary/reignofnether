package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.UnitProductionItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.survival.spawners.IllagerWaveSpawner;
import com.solegendary.reignofnether.survival.spawners.MonsterWaveSpawner;
import com.solegendary.reignofnether.survival.spawners.PiglinWaveSpawner;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerProd;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;

public class Factions {
	
	public static final ArrayList<ResourceLocation> CLASSIC_FACTIONS = new ArrayList<>();
	public static final ArrayList<ResourceLocation> SURVIVAL_FACTIONS = new ArrayList<>();
	public static final ArrayList<ResourceLocation> PLAYABLE_FACTIONS = new ArrayList<>();
	
	public static final HashMap<ResourceLocation, ResourceLocation> ENTITY_FACTION = new HashMap<>();
	
	public static Faction VILLAGERS;
	public static Faction MONSTERS;
	public static Faction PIGLINS;
	public static Faction NEUTRAL;
	public static Faction RANDOM;
	public static Faction NONE;
	
	public static void register() {
		VILLAGERS = register("villagers", new Faction()
			.setWorkerEntityType(EntityRegistrar.VILLAGER_UNIT.get())
			.setScoutEntityType(EntityRegistrar.SCOUT_DOG_UNIT.get())
			.setWorkerIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"))
			.setIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"))
			.setSound(SoundRegistrar.VILLAGER_CALM_THEME_SONG.get())
			.setSpawnWave(IllagerWaveSpawner::spawnIllagerWave)
			.setCustomBuildingCondition((cb) -> cb.buildableByVillagers)
		);
		
		MONSTERS = register("monsters", new Faction()
			.setScoutEntityType(EntityRegistrar.BAT_UNIT.get())
			.setWorkerIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/zombie_villager.png"))
			.setIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"))
			.setSound(SoundRegistrar.MONSTER_CALM_THEME_SONG.get())
			.setSpawnWave(MonsterWaveSpawner::spawnMonsterWave)
			.setCustomBuildingCondition((cb) -> cb.buildableByMonsters)
		);
		
		PIGLINS = register("piglins", new Faction()
			.setWorkerEntityType(EntityRegistrar.GRUNT_UNIT.get())
			.setScoutEntityType(EntityRegistrar.STRIDER_UNIT.get())
			.setWorkerIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"))
			.setIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"))
			.setSpawnWave(IllagerWaveSpawner::spawnIllagerWave)
			.setSound(SoundRegistrar.PIGLIN_CALM_THEME_SONG.get())
			.setCustomBuildingCondition((cb) -> cb.buildableByPiglins)
			.setSpawnWave(PiglinWaveSpawner::spawnPiglinWave)
		);
		
		NEUTRAL = register("neutral", new Faction()
			.setWorkerIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/sheep.png"))
			.noCubeMap()
		);
		
		RANDOM = register("random", new Faction()
			.setWorkerIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/unknown.png"))
			.setIcon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/question_mark_bg.png"))
			.setUnplayable()
			.noCubeMap()
		);
		
		NONE = register("none", new Faction()
			.setUnplayable()
			.noCubeMap()
		);
		
		registerUnit();
		registerBuilding();
	}
	
	public static void registerUnit() {
		// Villagers
		registerWorkerEntity(VILLAGERS, EntityRegistrar.VILLAGER_UNIT.get(), ProductionItems.VILLAGER);
		registerEntity(VILLAGERS, EntityRegistrar.MILITIA_UNIT.get(), ProductionItems.VILLAGER);
		registerScoutEntity(VILLAGERS, EntityRegistrar.SCOUT_DOG_UNIT.get(), ProductionItems.SCOUT_DOG);
		registerEntity(VILLAGERS, EntityRegistrar.SCOUT_CAT_UNIT.get(), ProductionItems.SCOUT_CAT);
		registerEntity(VILLAGERS, EntityRegistrar.VINDICATOR_UNIT.get(), ProductionItems.VINDICATOR);
		registerEntity(VILLAGERS, EntityRegistrar.PILLAGER_UNIT.get(), ProductionItems.PILLAGER);
		registerEntity(VILLAGERS, EntityRegistrar.IRON_GOLEM_UNIT.get(), ProductionItems.IRON_GOLEM);
		registerEntity(VILLAGERS, EntityRegistrar.WITCH_UNIT.get(), ProductionItems.WITCH);
		registerEntity(VILLAGERS, EntityRegistrar.EVOKER_UNIT.get(), ProductionItems.EVOKER);
		registerEntity(VILLAGERS, EntityRegistrar.WINDCALLER_UNIT.get(), ProductionItems.WINDCALLER);
		registerEntity(VILLAGERS, EntityRegistrar.RAVAGER_UNIT.get(), ProductionItems.RAVAGER);
		registerEntity(VILLAGERS, EntityRegistrar.ROYAL_GUARD_UNIT.get(), ProductionItems.ROYAL_GUARD);
		registerEntity(VILLAGERS, EntityRegistrar.ENCHANTER_UNIT.get(), ProductionItems.ENCHANTER);
		
		// Monsters
		registerWorkerEntity(MONSTERS, EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(), ProductionItems.ZOMBIE_VILLAGER);
		registerScoutEntity(MONSTERS, EntityRegistrar.BAT_UNIT.get(), ProductionItems.BAT);
		registerEntity(MONSTERS, EntityRegistrar.ZOMBIE_UNIT.get(), ProductionItems.ZOMBIE);
		registerEntity(MONSTERS, EntityRegistrar.DROWNED_UNIT.get(), ProductionItems.DROWNED);
		registerEntity(MONSTERS, EntityRegistrar.HUSK_UNIT.get(), ProductionItems.HUSK);
		registerEntity(MONSTERS, EntityRegistrar.SKELETON_UNIT.get(), ProductionItems.SKELETON);
		registerEntity(MONSTERS, EntityRegistrar.BOGGED_UNIT.get(), ProductionItems.BOGGED);
		registerEntity(MONSTERS, EntityRegistrar.STRAY_UNIT.get(), ProductionItems.STRAY);
		registerEntity(MONSTERS, EntityRegistrar.SPIDER_UNIT.get(), ProductionItems.SPIDER);
		registerEntity(MONSTERS, EntityRegistrar.POISON_SPIDER_UNIT.get(), ProductionItems.POISON_SPIDER);
		registerEntity(MONSTERS, EntityRegistrar.CREEPER_UNIT.get(), ProductionItems.CREEPER);
		registerEntity(MONSTERS, EntityRegistrar.WRAITH_UNIT.get(), ProductionItems.WRAITH);
		registerEntity(MONSTERS, EntityRegistrar.SLIME_UNIT.get(), ProductionItems.SLIME);
		registerEntity(MONSTERS, EntityRegistrar.WARDEN_UNIT.get(), ProductionItems.WARDEN);
		registerEntity(MONSTERS, EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get(), ProductionItems.ZOMBIE_PIGLIN);
		registerEntity(MONSTERS, EntityRegistrar.ZOGLIN_UNIT.get(), ProductionItems.ZOGLIN);
		registerEntity(MONSTERS, EntityRegistrar.NECROMANCER_UNIT.get(), ProductionItems.NECROMANCER);
		registerEntity(MONSTERS, EntityRegistrar.WRETCHED_WRAITH_UNIT.get(), ProductionItems.WRETCHED_WRAITH);
		registerEntity(MONSTERS, EntityRegistrar.SILVERFISH_UNIT.get());
		
		// Piglins
		registerWorkerEntity(PIGLINS, EntityRegistrar.GRUNT_UNIT.get(), ProductionItems.GRUNT);
		registerScoutEntity(PIGLINS, EntityRegistrar.STRIDER_UNIT.get(), ProductionItems.STRIDER);
		registerEntity(PIGLINS, EntityRegistrar.BRUTE_UNIT.get(), ProductionItems.BRUTE);
		registerEntity(PIGLINS, EntityRegistrar.HEADHUNTER_UNIT.get(), ProductionItems.HEADHUNTER);
		registerEntity(PIGLINS, EntityRegistrar.MARAUDER_UNIT.get(), ProductionItems.MARAUDER);
		registerEntity(PIGLINS, EntityRegistrar.HOGLIN_UNIT.get(), ProductionItems.HOGLIN);
		registerEntity(PIGLINS, EntityRegistrar.BLAZE_UNIT.get(), ProductionItems.BLAZE);
		registerEntity(PIGLINS, EntityRegistrar.WITHER_SKELETON_UNIT.get(), ProductionItems.WITHER_SKELETON);
		registerEntity(PIGLINS, EntityRegistrar.MAGMA_CUBE_UNIT.get(), ProductionItems.MAGMA_CUBE);
		registerEntity(PIGLINS, EntityRegistrar.GHAST_UNIT.get(), ProductionItems.GHAST);
		registerEntity(PIGLINS, EntityRegistrar.PIGLIN_MERCHANT_UNIT.get(), ProductionItems.PIGLIN_MERCHANT);
		registerEntity(PIGLINS, EntityRegistrar.WILDFIRE_UNIT.get(), ProductionItems.WILDFIRE);
		registerEntity(PIGLINS, EntityRegistrar.ARMOURED_HOGLIN_UNIT.get());
		
		// Neutral
		registerEntity(NEUTRAL, EntityRegistrar.ENDERMAN_UNIT.get(), ProductionItems.ENDERMAN);
		registerEntity(NEUTRAL, EntityRegistrar.POLAR_BEAR_UNIT.get(), ProductionItems.POLAR_BEAR);
		registerEntity(NEUTRAL, EntityRegistrar.GRIZZLY_BEAR_UNIT.get(), ProductionItems.GRIZZLY_BEAR);
		registerEntity(NEUTRAL, EntityRegistrar.PANDA_UNIT.get(), ProductionItems.PANDA);
		registerEntity(NEUTRAL, EntityRegistrar.WOLF_UNIT.get(), ProductionItems.WOLF);
		registerEntity(NEUTRAL, EntityRegistrar.LLAMA_UNIT.get(), ProductionItems.LLAMA);
	}
	
	
	public static void registerBuilding() {
		// Monsters
		registerStartBuilding(MONSTERS, Buildings.MAUSOLEUM, Keybindings.abilitySlot1);
		registerBuilding(MONSTERS, Buildings.SPRUCE_STOCKPILE, Keybindings.abilitySlot2);
		registerBuilding(MONSTERS, Buildings.SCULK_CATALYST, Keybindings.abilitySlot3);
		registerBuilding(MONSTERS, Buildings.PUMPKIN_FARM, Keybindings.abilitySlot4);
		registerBuilding(MONSTERS, Buildings.DARK_WATCHTOWER, Keybindings.abilitySlot5);
		registerBuilding(MONSTERS, Buildings.GRAVEYARD, Keybindings.abilitySlot6);
		registerBuilding(MONSTERS, Buildings.DUNGEON, Keybindings.abilitySlot7);
		registerBuilding(MONSTERS, Buildings.SPIDER_LAIR, Keybindings.abilitySlot8);
		registerBuilding(MONSTERS, Buildings.SLIME_PIT, Keybindings.abilitySlot9);
		registerBuilding(MONSTERS, Buildings.LABORATORY, Keybindings.abilitySlot10);
		registerBuilding(MONSTERS, Buildings.STRONGHOLD, Keybindings.hotkey2);
		registerBuilding(MONSTERS, Buildings.ALTAR_OF_DARKNESS, Keybindings.hotkey3);
		registerBuilding(MONSTERS, Buildings.SPRUCE_BRIDGE, Keybindings.hotkey4);
		registerBuilding(MONSTERS, Buildings.HAUNTED_HOUSE);
		
		registerBuilding(MONSTERS, Buildings.MONSTER_MARKET);
		registerBuilding(MONSTERS, Buildings.BEACON);
		
		//Piglins
		registerStartBuilding(PIGLINS, Buildings.CENTRAL_PORTAL, Keybindings.abilitySlot1);
		registerBuilding(PIGLINS, Buildings.PORTAL_BASIC, Keybindings.abilitySlot2);
		registerBuilding(PIGLINS, Buildings.NETHERWART_FARM, Keybindings.abilitySlot3);
		registerBuilding(PIGLINS, Buildings.BASTION, Keybindings.abilitySlot4);
		registerBuilding(PIGLINS, Buildings.HOGLIN_STABLES, Keybindings.abilitySlot5);
		registerBuilding(PIGLINS, Buildings.FLAME_SANCTUARY, Keybindings.abilitySlot6);
		registerBuilding(PIGLINS, Buildings.WITHER_SHRINE, Keybindings.abilitySlot7);
		registerBuilding(PIGLINS, Buildings.BASALT_SPRINGS, Keybindings.abilitySlot8);
		registerBuilding(PIGLINS, Buildings.FORTRESS, Keybindings.abilitySlot9);
		registerBuilding(PIGLINS, Buildings.INFERNAL_PORTAL, Keybindings.hotkey3);
		registerBuilding(PIGLINS, Buildings.BLACKSTONE_BRIDGE, Keybindings.hotkey4);
		registerBuilding(PIGLINS, Buildings.PIGLIN_MARKET);
		registerBuilding(PIGLINS, Buildings.BEACON);
		
		//VillagersFaction
		registerStartBuilding(VILLAGERS, Buildings.TOWN_CENTRE, Keybindings.abilitySlot1);
		registerBuilding(VILLAGERS, Buildings.OAK_STOCKPILE, Keybindings.abilitySlot2);
		registerBuilding(VILLAGERS, Buildings.VILLAGER_HOUSE, Keybindings.abilitySlot3);
		registerBuilding(VILLAGERS, Buildings.WHEAT_FARM, Keybindings.abilitySlot4);
		registerBuilding(VILLAGERS, Buildings.WATCHTOWER, Keybindings.abilitySlot5);
		registerBuilding(VILLAGERS, Buildings.BARRACKS, Keybindings.abilitySlot6);
		registerBuilding(VILLAGERS, Buildings.BLACKSMITH, Keybindings.abilitySlot7);
		registerBuilding(VILLAGERS, Buildings.WITCH_HUT, Keybindings.abilitySlot8);
		registerBuilding(VILLAGERS, Buildings.ARCANE_TOWER, Keybindings.abilitySlot9);
		registerBuilding(VILLAGERS, Buildings.LIBRARY, Keybindings.abilitySlot10);
		registerBuilding(VILLAGERS, Buildings.CASTLE, Keybindings.hotkey2);
		registerBuilding(VILLAGERS, Buildings.SHRINE_OF_PROSPERITY, Keybindings.hotkey3);
		registerBuilding(VILLAGERS, Buildings.IRON_GOLEM_BUILDING, Keybindings.hotkey9);
		registerBuilding(VILLAGERS, Buildings.OAK_BRIDGE, Keybindings.hotkey4);
		registerBuilding(VILLAGERS, Buildings.VILLAGER_MARKET);
		registerBuilding(VILLAGERS, Buildings.BEACON);
		
		//Neutral
		registerBuilding(NEUTRAL, Buildings.CAPTURABLE_BEACON, Keybindings.abilitySlot1);
		registerBuilding(NEUTRAL, Buildings.HEALING_FOUNTAIN, Keybindings.abilitySlot2);
		registerBuilding(NEUTRAL, Buildings.END_PORTAL, Keybindings.abilitySlot3);
		registerBuilding(NEUTRAL, Buildings.NEUTRAL_TRANSPORT_PORTAL, Keybindings.abilitySlot4);
		
		for (ResourceLocation faction : PLAYABLE_FACTIONS) {
			getFaction(faction).addCustomBuildings();
		}
		
	}
	
	private static Faction register(String name, Faction faction) {
		ResourceLocation key = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, name);
		if (faction.hasCubeMap)
			CLASSIC_FACTIONS.add(key);
		if (faction.playable)
			PLAYABLE_FACTIONS.add(key);
		return Registry.register(ReignOfNetherRegistries.FACTIONS, key, faction).setKey(getKey(faction));
	}
	
	public static <T extends Faction> T register(ResourceLocation key, T faction) {
		
		return Registry.register(ReignOfNetherRegistries.FACTIONS, key, faction);
	}
	
	public static void registerStartBuilding(Faction faction, Building building, Keybinding key) {
		registerBuilding(faction, building, key);
		faction.capitolBuilding = ReignOfNetherRegistries.BUILDING.getKey(building);
	}
	
	public static void registerBuilding(Faction faction, Building building, Keybinding key) {
		building.setFaction(faction.key);
		faction.addBuilding(building, key);
	}
	
	public static <T extends UnitProductionItem> void registerWorkerEntity(Faction faction, EntityType<? extends Unit> unit, T productionItem) {
		registerEntity(faction, unit, productionItem);
		faction.setWorkerEntityType(unit);
	}
	
	public static <T extends UnitProductionItem> void registerScoutEntity(Faction faction, EntityType<? extends Unit> unit, T productionItem) {
		registerEntity(faction, unit, productionItem);
		faction.setScoutEntityType(unit);
	}
	
	public static <T extends UnitProductionItem> void registerEntity(Faction faction, EntityType<? extends Unit> unit, T productionItem) {
		if (unit == EntityRegistrar.MILITIA_UNIT.get())
			faction.addEntityButton(((VillagerProd) productionItem).getMilitiaPlaceButton());
		else
			faction.addEntityButton(productionItem.getPlaceButton());
		ENTITY_FACTION.put(EntityType.getKey(unit), faction.key);
	}
	
	public static void registerEntity(Faction faction, EntityType<? extends Unit> unit) {
		ENTITY_FACTION.put(EntityType.getKey(unit), faction.key);
	}
	
	public static void registerBuilding(Faction faction, Building building) {
		registerBuilding(faction, building, null);
	}
	
	public static Faction getFaction(ResourceLocation pKey) {
		return ReignOfNetherRegistries.FACTIONS.get(pKey);
	}
	
	public static Faction getFaction(Unit unit) {
		return getFaction(ENTITY_FACTION.get(EntityType.getKey(((Entity) unit).getType())));
	}
	
	public static Faction getFaction(int id) {
		return ReignOfNetherRegistries.FACTIONS.byId(id);
	}
	
	public static ResourceLocation getKey(Faction faction) {
		return ReignOfNetherRegistries.FACTIONS.getKey(faction);
	}
}
