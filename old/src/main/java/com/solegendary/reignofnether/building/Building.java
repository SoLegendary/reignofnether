package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

// Registered object in Buildings
// is a member of BuildingPlacement and is generally not instantiated directly
public abstract class Building {
    public String name;
    public String structureName;
    public ResourceLocation icon;
    public final boolean isCapitol;
    public Block portraitBlock; // block rendered in the portrait GUI to represent this building
    public boolean canAcceptResources = false; // can workers drop off resources here?

    // chance for a mini explosion to destroy extra blocks if a player is breaking it
    // should be higher for large fragile buildings so players don't take ages to destroy it
    protected float explodeChance = 0.3f;
    protected float explodeRadius = 2.0f;
    protected float fireThreshold = 0.75f; // if building has less %hp than this, explosions caused can make fires
    protected float buildTimeModifier = 1.0f; // only affects non-built buildings, not repair times
    protected float repairTimeModifier = 1.25f; // only affects built buildings

    public int captureRange = 20;
    public boolean capturable = false;
    public boolean invulnerable = false;
    public boolean shouldDestroyOnReset = true;

    public ResourceCost cost;
    public boolean selfBuilding = false;

    // blocks types that are placed automatically when the building is placed
    // used to control size of initial foundations while keeping it symmetrical
    public final ArrayList<Block> startingBlockTypes = new ArrayList();

    public Building(String structureName, ResourceCost cost, boolean isCapitol) {
        this.structureName = structureName;
        this.cost = cost;
        this.isCapitol = isCapitol;
    }

    public float getMeleeDamageMult() {
        return 0.2F;
    }

    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocksFromNbt(this.structureName, level);
    }

    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new BuildingPlacement(this, level, pos, rotation, ownerName, BuildingUtils.getAbsoluteBlockData(this.getRelativeBlockData(level), level, pos, rotation), this.isCapitol);
    }

    protected static ArrayList<BuildingBlock> getCulledBlocks(ArrayList<BuildingBlock> blocks, Level level) {
        blocks.removeIf((b) -> shouldCullBlock(new BlockPos(0, 0, 0), b, level));
        return blocks;
    }

    public static boolean shouldCullBlock(BlockPos originPos, BuildingBlock b, Level level) {
        BlockState bs = b.getBlockState();

        boolean isFenceOrAir = b.getBlockState().getBlock() instanceof AirBlock ||
                b.getBlockState().getBlock() instanceof FenceBlock;
        BlockPos bp = b.getBlockPos().offset(originPos);

        // if the block in the world matches this exactly, don't cull it, instead just consider it to be our block too
        BlockState bsWorld = level.getBlockState(bp);

        if (bsWorld.getBlock() == Blocks.OBSIDIAN)
            return false;
        if (bsWorld.equals(bs))
            return false;
        if ((bsWorld.isAir() || !bsWorld.getFluidState().isEmpty()) && !isFenceOrAir)
            return false;

        // cull if overlaps another bridge block that isn't built yet
        if (BuildingUtils.isPosInsideAnyBuilding(level.isClientSide, bp))
            return true;

        // cull if fence is adjacent to another solid block (or a bridge block, even if air)
        for (BlockPos bpAdj : List.of(bp.north(), bp.south(), bp.east(), bp.west())) {
            BlockState bsWorldAdj = level.getBlockState(bpAdj);
            if (isFenceOrAir && !bsWorldAdj.isAir() && BuildingUtils.isPosInsideAnyBuilding(level.isClientSide, bpAdj))
                return true;
        }
        return level.getBlockState(bp).isSolid() || (isFenceOrAir && level.getBlockState(bp.below()).isSolid());
    }

    public abstract Faction getFaction();

    public int getUpgradeLevel(BuildingPlacement placement) {
        return 0;
    }

    public abstract AbilityButton getBuildButton(Keybinding var1);

    public boolean isTypeOf(Building building) {
        return this == building;
    }
}