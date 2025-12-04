package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.RTSStructureBlockEntity;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CustomBuildingServerEvents {

    // since every custom building has a different structure, we need to maintain a list of them here
    private static final Set<CustomBuilding> customBuildings = new HashSet<>();

    public static CustomBuilding getCustomBuilding(String name) {
        for (CustomBuilding building : customBuildings)
            if (building.name.equals(name))
                return building;
        return null;
    }

    public static void deregisterCustomBuilding(String buildingName) {
        customBuildings.removeIf(b -> b.name.equals(buildingName));
        BuildingServerEvents.getBuildings().removeIf(b -> b.getBuilding().name.equals(buildingName));
        saveCustomBuildings(BuildingServerEvents.getServerLevel());
    }

    public static boolean createAndRegisterNewCustomBuilding(ResourceLocation structureRL, String structureName, ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RTSStructureBlockEntity rtsBe) {
            return createAndRegisterNewCustomBuilding(structureRL, structureName, level, pos.offset(1,0,1), rtsBe.getStructureSize());
        }
        return true;
    }

    // registers and places a new custom building on server and client
    public static boolean createAndRegisterNewCustomBuilding(ResourceLocation structureRL, String structureName, ServerLevel level,
                                                             BlockPos pos, Vec3i structureSize) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(structureRL);
        CompoundTag structureNbt = null;
        if (template.isPresent()) {
            structureNbt = template.get().save(new CompoundTag());
        }
        if (structureNbt != null) {
            BuildingServerEvents.getBuildings().removeIf(b -> b.originPos.equals(pos));

            ArrayList<BuildingBlock> blocks = BuildingUtils.getAbsoluteBlockData(
                    BuildingBlockData.getBuildingBlocksFromNbt(structureNbt),
                    level, pos, Rotation.NONE
            );
            int numSolidBlocks = 0;
            Block portraitBlock = Blocks.COMMAND_BLOCK;
            for (BuildingBlock bb : blocks) {
                BlockState bs = bb.getBlockState();
                if (!bs.isAir() && bs.getFluidState().isEmpty() &&
                    !CustomBuildingPlacement.INVULNERABLE_BLOCKS.contains(bb.getBlockState().getBlock())) {
                    numSolidBlocks += 1;
                    portraitBlock = bs.getBlock();
                }
            }
            if (numSolidBlocks == 0) {
                PlayerServerEvents.sendMessageToAllPlayers("ERROR (server): cannot register custom building with no solid blocks");
            } else {
                CustomBuilding building = new CustomBuilding(structureName, structureSize, portraitBlock, structureNbt);
                for (CustomBuilding customBuilding : customBuildings) {
                    if (customBuilding.name.equals(building.name)) {
                        PlayerServerEvents.sendMessageToAllPlayers("ERROR (server): custom building " + building.name + " already exists");
                        return false;
                    }
                }
                customBuildings.add(building);
                BuildingPlacement placement = new CustomBuildingPlacement(building, level, pos, Rotation.NONE, "", blocks, false);
                BuildingServerEvents.getBuildings().add(placement);
                CustomBuildingClientboundPacket.registerCustomBuilding(building);
                saveCustomBuildings(level);
                BuildingServerEvents.saveBuildings(level);
                BuildingServerEvents.placeBuildingClientside(pos);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            saveCustomBuildings(level);
        }
    }

    public static void saveCustomBuildings(ServerLevel level) {
        CustomBuildingSaveData customBuildingData = CustomBuildingSaveData.getInstance(level);
        customBuildingData.customBuildings.clear();
        customBuildings.forEach(b -> {
            b.packAttributesNbt();
            customBuildingData.customBuildings.add(new CustomBuildingSave(
                    b.structureNbt,
                    b.name,
                    b.structureSize,
                    b.attributesNbt
            ));
        });
        customBuildingData.save();
        level.getDataStorage().save();
    }

    public static void loadCustomBuildings(ServerLevel level) {
        CustomBuildingSaveData customBuildingData = CustomBuildingSaveData.getInstance(level);
        customBuildingData.customBuildings.forEach(bSave -> {
            CustomBuilding building = new CustomBuilding(bSave.buildingName, bSave.structureSize, Blocks.COMMAND_BLOCK, bSave.structureNbt, bSave.attributesNbt);
            boolean buildingExists = false;
            for (CustomBuilding customBuilding : customBuildings) {
                if (customBuilding.name.equals(building.name)) {
                    buildingExists = true;
                    break;
                }
            }
            if (!buildingExists)
                customBuildings.add(building);
            ReignOfNether.LOGGER.info("loaded custom building in serverevents: " + bSave.buildingName);
        });
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!PlayerServerEvents.rtsSyncingEnabled) {
            return;
        }
        MinecraftServer server = evt.getEntity().level().getServer();
        if (server == null || !server.isDedicatedServer()) {
            CompletableFuture.delayedExecutor(1000,  TimeUnit.MILLISECONDS).execute(() -> syncCustomBuildings(evt.getEntity().getName().getString()));
        } else {
            syncCustomBuildings(evt.getEntity().getName().getString());
        }
        //ReignOfNether.LOGGER.info("Synced " + buildings.size() + " custom buildings with player logged in");
    }

    private static void syncCustomBuildings(String playerName) {
        for (CustomBuilding customBuilding : customBuildings) {
            CustomBuildingClientboundPacket.registerCustomBuilding(playerName, customBuilding);
        }
    }
}
