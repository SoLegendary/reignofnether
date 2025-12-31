package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.resources.BlockUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

<<<<<<< HEAD
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
=======
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
>>>>>>> 02d047be (Graveyard crash fix)

// a class for static functions related to reading building NBT data (as created by Structure Blocks)

public class BuildingBlockData {

    private static final Set<String> warnedMissingStructures = ConcurrentHashMap.newKeySet();

    public static ArrayList<BuildingBlock> getBuildingBlocksFromNbt(String structureName, LevelAccessor level) {
        ResourceManager resourceManager;
        if (level.isClientSide())
            resourceManager = Minecraft.getInstance().getResourceManager();
        else
            resourceManager = level.getServer().getResourceManager();

        Optional<CompoundTag> nbtOpt = tryGetBuildingNbt(structureName, resourceManager);
        if (nbtOpt.isEmpty()) {
            warnMissingStructureOnce(structureName, "getBuildingBlocksFromNbt(structureName, level)");
            return new ArrayList<>();
        }

        return getBuildingBlocksFromNbt(nbtOpt.get());
    }

    public static ArrayList<BuildingBlock> getBuildingBlocksFromNbt(CompoundTag nbt) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();
        if (nbt == null) {
            ReignOfNether.LOGGER.warn("BuildingBlockData: getBuildingBlocksFromNbt called with null NBT; returning empty block list to avoid crash");
            return blocks;
        }

        // load in blocks (list of blockPos and their palette index)
        ListTag blocksNbt = nbt.getList("blocks", 10);

        ArrayList<BlockState> palette = getBuildingPalette(nbt);

        for(int i = 0; i < blocksNbt.size(); i++) {
            CompoundTag blockNbt = blocksNbt.getCompound(i);
            ListTag blockPosNbt = blockNbt.getList("pos", 3);

            BlockPos bp = new BlockPos(
                    blockPosNbt.getInt(0),
                    blockPosNbt.getInt(1),
                    blockPosNbt.getInt(2)
            );
            BlockState bs = palette.get(blockNbt.getInt("state"));
            CompoundTag bNbt = null;
            if (blockNbt.contains("nbt")) {
                bNbt = blockNbt.getCompound("nbt");
            }
            if (BlockUtils.isFallingLogBlock(bs))
                bs = BlockUtils.getNonFallingLog(bs);

            if (bs.getBlock() != Blocks.WATER || bs.getFluidState().isSource())
                blocks.add(new BuildingBlock(bp, bs, bNbt));
        }
        return blocks;
    }

    public static CompoundTag getBuildingNbt(String structureName, ResourceManager resManager) {
<<<<<<< HEAD

=======
        return tryGetBuildingNbt(structureName, resManager).orElse(null);
    }

    /**
     * Attempts to load a building structure NBT.\n
     *\n
     * Primary path: via the provided {@link ResourceManager} (allows resource-pack overrides client-side).\n
     * Fallback path: bundled mod resource under /assets/reignofnether/structures/*.nbt (works server-side too).\n
     */
    public static Optional<CompoundTag> tryGetBuildingNbt(String structureName, ResourceManager resManager) {
        // 1) Try through resource manager (client-side resource packs, etc.)
>>>>>>> 02d047be (Graveyard crash fix)
        try {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "structures/" + structureName + ".nbt");
            Optional<Resource> rs = resManager.getResource(rl);
<<<<<<< HEAD
            if (rs.isEmpty()) return null;
            return NbtIo.readCompressed(rs.get().open());
        } catch (IOException e) {
            ReignOfNether.LOGGER.error(e.getMessage(), e);
            return null;
=======
            if (rs.isPresent()) {
                try (InputStream in = rs.get().open()) {
                    return Optional.of(NbtIo.readCompressed(in));
                }
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.warn("BuildingBlockData: failed reading structure NBT '{}' via ResourceManager", structureName, e);
        }

        // 2) Fallback: read bundled asset directly (important for dedicated server / server ResourceManager differences)
        String cpPath = "/assets/" + ReignOfNether.MOD_ID + "/structures/" + structureName + ".nbt";
        try (InputStream in = ReignOfNether.class.getResourceAsStream(cpPath)) {
            if (in != null) {
                return Optional.of(NbtIo.readCompressed(in));
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.warn("BuildingBlockData: failed reading structure NBT '{}' from classpath '{}'", structureName, cpPath, e);
>>>>>>> 02d047be (Graveyard crash fix)
        }

        return Optional.empty();
    }

    public static ArrayList<BlockState> getBuildingPalette(CompoundTag nbt) {
        ArrayList<BlockState> palette = new ArrayList<>();
        // load in palette (list of unique block states)
        ListTag paletteNbt = nbt.getList("palette", 10);
        for(int i = 0; i < paletteNbt.size(); i++) {
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteNbt.getCompound(i)));
        }
        return palette;
    }

    public static BuildingBlock getBuildingBlockByPos(ArrayList<BuildingBlock> blocks, BlockPos bp) {
        for (BuildingBlock block : blocks) {
            if (block.getBlockPos().equals(bp)) return block;
        }
        return null;
    }

    private static void warnMissingStructureOnce(String structureName, String context) {
        if (!warnedMissingStructures.add(structureName)) {
            return;
        }
        ReignOfNether.LOGGER.warn(
            "BuildingBlockData: missing/unreadable structure NBT '{}' (context: {}). " +
                "This would have crashed previously; now returning empty data. " +
                "Verify the structure exists at '/assets/{}/structures/{}.nbt' (or is provided by a resource pack).",
            structureName,
            context,
            ReignOfNether.MOD_ID,
            structureName
        );
    }

    /**
     * Simple regression signal for release builds: verifies the NBT exists as a bundled mod asset.\n
     *\n
     * Note: this does not validate the NBT contents, only that the resource path is present.
     */
    public static boolean bundledStructureExists(String structureName) {
        String cpPath = "/assets/" + ReignOfNether.MOD_ID + "/structures/" + structureName + ".nbt";
        return ReignOfNether.class.getResource(cpPath) != null;
    }
}
