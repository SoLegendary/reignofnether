package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.monsters.*;
import com.solegendary.reignofnether.building.buildings.neutral.*;
import com.solegendary.reignofnether.building.buildings.piglins.*;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.*;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.building.data.DataStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class BuildingSaveData extends SavedData {

    public final ArrayList<BuildingSave> buildings = new ArrayList<>();

    private static BuildingSaveData create() {
        return new BuildingSaveData();
    }

    @Nonnull
    public static BuildingSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(tag -> BuildingSaveData.load(tag, server), BuildingSaveData::create, "saved-building-data");
    }

    public static BuildingSaveData load(CompoundTag tag, MinecraftServer server) {
        ReignOfNether.LOGGER.info("BuildingSaveData.load");

        BuildingSaveData data = create();
        ListTag ltag = (ListTag) tag.get("buildings");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag btag = (CompoundTag) ctag;
                BlockPos pos = new BlockPos(btag.getInt("x"), btag.getInt("y"), btag.getInt("z"));
                Level level = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
                Building building;
                if (btag.contains("customStructureName")) {
                    building = CustomBuildingServerEvents.getCustomBuilding(btag.getString("customStructureName"));
                }
                else if (btag.contains("buildingKey")) {
                    building = ReignOfNetherRegistries.BUILDING.get(ResourceLocation.tryParse(btag.getString("buildingKey")));
                } else {
                    building = getOldBuilding(btag.getString("buildingName"));
                }
                String ownerName = btag.getString("ownerName");
                Rotation rotation = Rotation.valueOf(btag.getString("rotation"));
                BlockPos rallyPoint = new BlockPos(btag.getInt("rallyX"), btag.getInt("rallyY"), btag.getInt("rallyZ"));
                boolean isDiagonalBridge = btag.getBoolean("isDiagonalBridge");
                boolean isBuilt = btag.getBoolean("isBuilt");
                int upgradeLevel = btag.getInt("upgradeLevel");
                PortalPlacement.PortalType portalType = PortalPlacement.PortalType.valueOf(btag.getString("portalType"));
                BlockPos portalDestination = new BlockPos(btag.getInt("xp"), btag.getInt("yp"), btag.getInt("zp"));
                int scenarioRoleIndex = btag.getInt("scenarioRoleIndex");
                DataStorage dataStorage;
                if(btag.contains("dataStorage", Tag.TAG_LIST)) {
                    dataStorage = DataStorage.read(btag.getList("dataStorage", Tag.TAG_COMPOUND), server);
                }else {
                    dataStorage = new DataStorage();
                }

                if (building != null) {
                    data.buildings.add(new BuildingSave(pos,
                            level,
                            building,
                            ownerName,
                            rotation,
                            rallyPoint,
                            isDiagonalBridge,
                            isBuilt,
                            upgradeLevel,
                            portalType,
                            portalDestination,
                            scenarioRoleIndex,
                            dataStorage
                    ));
                    ReignOfNether.LOGGER.info("BuildingSaveData.load: " + ownerName + "|" + building.name);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("BuildingSaveData.save");

        ListTag list = new ListTag();
        this.buildings.forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            if (b.building instanceof CustomBuilding) {
                cTag.putString("customStructureName", b.building.structureName);
            }
            if (!(b.building instanceof CustomBuilding)) {
                cTag.putString("buildingKey", ReignOfNetherRegistries.BUILDING.getKey(b.building).toString());
            }
            cTag.putInt("x", b.originPos.getX());
            cTag.putInt("y", b.originPos.getY());
            cTag.putInt("z", b.originPos.getZ());
            cTag.putString("rotation", b.rotation.name());
            cTag.putInt("rallyX", b.rallyPoint != null ? b.rallyPoint.getX() : b.originPos.getX());
            cTag.putInt("rallyY", b.rallyPoint != null ? b.rallyPoint.getY() : b.originPos.getY());
            cTag.putInt("rallyZ", b.rallyPoint != null ? b.rallyPoint.getZ() : b.originPos.getZ());
            cTag.putString("ownerName", b.ownerName);
            cTag.putBoolean("isDiagonalBridge", b.isDiagonalBridge);
            cTag.putBoolean("isBuilt", b.isBuilt);
            cTag.putInt("upgradeLevel", b.upgradeLevel);
            cTag.putString("portalType", b.portalType != null ? b.portalType.name() : PortalPlacement.PortalType.BASIC.name());
            cTag.putInt("xp", b.portalDestination != null ? b.portalDestination.getX() : 0);
            cTag.putInt("yp", b.portalDestination != null ? b.portalDestination.getY() : 0);
            cTag.putInt("zp", b.portalDestination != null ? b.portalDestination.getZ() : 0);
            cTag.putInt("scenarioRoleIndex", b.scenarioRoleIndex);
            cTag.put("dataStorage", b.dataStorage.write());
            list.add(cTag);

            //ReignOfNether.LOGGER.info("BuildingSaveData.save: " + b.ownerName + "|" + buildingName);
        });
        tag.put("buildings", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }

    // backwards compatibility for old saves
    private static Building getOldBuilding(String name) {
        Building building = null;
        switch(name) {
            case OakBridge.buildingName -> building = Buildings.OAK_BRIDGE;
            case SpruceBridge.buildingName -> building = Buildings.SPRUCE_BRIDGE;
            case BlackstoneBridge.buildingName -> building = Buildings.BLACKSTONE_BRIDGE;
            case OakStockpile.buildingName -> building = Buildings.OAK_STOCKPILE;
            case SpruceStockpile.buildingName -> building = Buildings.SPRUCE_STOCKPILE;
            case VillagerHouse.buildingName -> building = Buildings.VILLAGER_HOUSE;
            case Graveyard.buildingName -> building = Buildings.GRAVEYARD;
            case WheatFarm.buildingName -> building = Buildings.WHEAT_FARM;
            case Laboratory.buildingName -> building = Buildings.LABORATORY;
            case Barracks.buildingName -> building = Buildings.BARRACKS;
            case PumpkinFarm.buildingName -> building = Buildings.PUMPKIN_FARM;
            case HauntedHouse.buildingName -> building = Buildings.HAUNTED_HOUSE;
            case Blacksmith.buildingName -> building = Buildings.BLACKSMITH;
            case TownCentre.buildingName -> building = Buildings.TOWN_CENTRE;
            case IronGolemBuilding.buildingName -> building = Buildings.IRON_GOLEM_BUILDING;
            case Mausoleum.buildingName -> building = Buildings.MAUSOLEUM;
            case SculkCatalyst.buildingName -> building = Buildings.SCULK_CATALYST;
            case SpiderLair.buildingName -> building = Buildings.SPIDER_LAIR;
            case SlimePit.buildingName -> building = Buildings.SLIME_PIT;
            case ArcaneTower.buildingName -> building = Buildings.ARCANE_TOWER;
            case Library.buildingName -> building = Buildings.LIBRARY;
            case Dungeon.buildingName -> building = Buildings.DUNGEON;
            case Watchtower.buildingName -> building = Buildings.WATCHTOWER;
            case DarkWatchtower.buildingName -> building = Buildings.DARK_WATCHTOWER;
            case Castle.buildingName -> building = Buildings.CASTLE;
            case Stronghold.buildingName -> building = Buildings.STRONGHOLD;
            case CentralPortal.buildingName -> building = Buildings.CENTRAL_PORTAL;
            case PortalBasic.buildingName -> building = Buildings.PORTAL_BASIC;
            case PortalCivilian.buildingName -> building = Buildings.PORTAL_CIVILIAN;
            case PortalMilitary.buildingName -> building = Buildings.PORTAL_MILITARY;
            case PortalTransport.buildingName -> building = Buildings.PORTAL_TRANSPORT;
            case NetherwartFarm.buildingName -> building = Buildings.NETHERWART_FARM;
            case Bastion.buildingName -> building = Buildings.BASTION;
            case HoglinStables.buildingName -> building = Buildings.HOGLIN_STABLES;
            case FlameSanctuary.buildingName -> building = Buildings.FLAME_SANCTUARY;
            case WitherShrine.buildingName -> building = Buildings.WITHER_SHRINE;
            case BasaltSprings.buildingName -> building = Buildings.BASALT_SPRINGS;
            case Fortress.buildingName -> building = Buildings.FORTRESS;
            case Beacon.buildingName -> building = Buildings.BEACON;
            case CapturableBeacon.buildingName -> building = Buildings.CAPTURABLE_BEACON;
            case EndPortal.buildingName -> building = Buildings.END_PORTAL;
            case HealingFountain.buildingName -> building = Buildings.HEALING_FOUNTAIN;
            case NeutralTransportPortal.buildingName -> building = Buildings.NEUTRAL_TRANSPORT_PORTAL;
        }
        return building;
    }
}
