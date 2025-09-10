package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.RTSStructureBlockEntity;
import com.solegendary.reignofnether.building.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.Optional;

public class CustomBuildingServerEvents {

    // since every custom building has a different structure, we need to maintain a list of them here
    private static final ArrayList<CustomBuilding> customBuildings = new ArrayList<>();

    public static Building getCustomBuilding(String structureName) {
        for (CustomBuilding building : customBuildings)
            if (building.structureName.equals(structureName))
                return building;
        return null;
    }

    public static boolean createNewCustomBuilding(ResourceLocation structureRL, String structureName, ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RTSStructureBlockEntity rtsBe) {
            return createNewCustomBuilding(structureRL, structureName, level, pos, rtsBe.getStructurePos(), rtsBe.getStructureSize(), true);
        }
        return true;
    }

    public static boolean createNewCustomBuilding(ResourceLocation structureRL, String structureName, ServerLevel level,
                                                  BlockPos pos, BlockPos structurePos, Vec3i structureSize, boolean save) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(structureRL);
        CompoundTag structureNbt = null;
        if (template.isPresent()) {
            structureNbt = template.get().save(new CompoundTag());
        }
        if (structureNbt != null) {
            boolean buildingExists = false;
            for (BuildingPlacement existingPlacement : BuildingServerEvents.getBuildings()) {
                if (existingPlacement.originPos.equals(pos)) {
                    buildingExists = true;
                    break;
                }
            }
            if (!buildingExists) {
                ArrayList<BuildingBlock> blocks = BuildingUtils.getAbsoluteBlockData(
                        BuildingBlockData.getBuildingBlocksFromNbt(structureNbt),
                        level, pos, Rotation.NONE, new Vec3i(1,0,1)
                );
                int numSolidBlocks = 0;
                Block portraitBlock = Blocks.COMMAND_BLOCK;
                for (BuildingBlock bb : blocks) {
                    BlockState bs = bb.getBlockState();
                    if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                        numSolidBlocks += 1;
                        portraitBlock = bs.getBlock();
                    }
                }
                if (numSolidBlocks == 0) {
                    ReignOfNether.LOGGER.error("ERROR (server): cannot register custom building with no solid blocks");
                } else {
                    CustomBuilding building = new CustomBuilding(structureName, structurePos, structureSize, portraitBlock);
                    customBuildings.add(building);
                    BuildingPlacement placement = new BuildingPlacement(building, level, pos, Rotation.NONE, "", blocks, false);
                    BuildingServerEvents.getBuildings().add(placement);
                    CustomBuildingClientboundPacket.registerCustomBuilding(structureName, pos, structurePos, structureSize);
                    //if (save)
                    //    saveBuildings(level);
                    return true;
                }
            } else {
                ReignOfNether.LOGGER.error("ERROR (server): cannot register custom building at same origin as another building");
            }
        }
        return false;
    }

    /*
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            saveBuildings(level);
        }
    }

    public static void saveBuildings(ServerLevel level) {
        CustomBuildingSaveData customBuildingData = CustomBuildingSaveData.getInstance(level);
        customBuildingData.customBuildings.clear();

        BuildingServerEvents.getBuildings().stream()
                .filter(b -> b.getBuilding() instanceof CustomBuilding)
                .toList()
                .forEach(b -> {
            customBuildingData.customBuildings.add(new CustomBuildingSave(
                    b.originPos,
                    level,
                    b.ownerName,
                    b.getBuilding().structureName,
                    b.getBuilding().name,
                    ((CustomBuilding) b.getBuilding()).structurePos,
                    ((CustomBuilding) b.getBuilding()).structureSize
            ));
        });
        customBuildingData.save();
        level.getDataStorage().save();
    }

    @SubscribeEvent
    public static void loadBuildings(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            CustomBuildingSaveData customBuildingData = CustomBuildingSaveData.getInstance(level);
            customBuildingData.customBuildings.forEach(b -> {
                boolean result = createNewCustomBuilding(
                        new ResourceLocation(b.structureName.toLowerCase()),
                        b.buildingName,
                        level,
                        b.originPos,
                        b.structurePos,
                        b.structureSize,
                        false
                );
                if (result) {
                    ReignOfNether.LOGGER.info("loaded custom building in serverevents: " + "" + "|" + b.originPos);
                }
            });
        }
    }
     */
}
