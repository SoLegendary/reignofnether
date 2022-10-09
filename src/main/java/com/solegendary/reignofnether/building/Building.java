package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Random;

import static com.solegendary.reignofnether.building.BuildingUtils.getMaxCorner;
import static com.solegendary.reignofnether.building.BuildingUtils.getMinCorner;

public abstract class Building {

    public String name;
    public LevelAccessor level;

    public boolean isBuilt; // set true when blocksPercent reaches 100% the first time
    public boolean isBuilding = true; // TODO: only true if // a builder is assigned and actively building or repairing
    public int ticksPerBuild = 1; // ticks taken to place a single block while isBuilding
    public int ticksToNextBuild = ticksPerBuild;
    // building collapses at a certain % blocks remaining so players don't have to destroy every single block
    public final float minBlocksPercent = 0.25f;
    // chance for a mini explosion to destroy extra blocks if a player is breaking it
    // should be higher for large fragile buildings so players don't take ages to destroy it
    public int explosionCount = 0;
    public float explodeChance = 0.5f;
    public float explodeRadius = 2.0f;
    public float fireThreshold = 0.75f; // if building has less %hp than this, explosions caused can make fires

    protected ArrayList<BuildingBlock> blocks = new ArrayList<>();
    public String ownerName;
    public Block portraitBlock; // block rendered in the portrait GUI to represent this building
    public int tickAge = 0; // how many ticks ago this building was placed
    public boolean canAcceptResources = false; // can workers drop off resources here?

    public Building() { }

    public ArrayList<BuildingBlock> getBlocks() {
        return blocks;
    }

    public boolean isPosInsideBuilding(BlockPos bp) {
        BlockPos min = getMinCorner(this.blocks);
        BlockPos max = getMaxCorner(this.blocks);

        return bp.getX() <= max.getX() && bp.getX() >= min.getX() &&
                bp.getY() <= max.getY() && bp.getY() >= min.getY() &&
                bp.getZ() <= max.getZ() && bp.getZ() >= min.getZ();
    }

    public boolean isPosPartOfBuilding(BlockPos bp, boolean onlyPlacedBlocks) {
        for (BuildingBlock block : this.blocks)
            if ((block.isPlaced || !onlyPlacedBlocks) && block.getBlockPos().equals(bp))
                return true;
        return false;
    }

    // returns the lowest Y value block in this.blocks to the given blockPos
    // radius offset is the distance away from the
    public BlockPos getClosestGroundPos(BlockPos bpTarget, int radiusOffset) {
        float minDist = 999999;
        BlockPos minPos = BuildingUtils.getMinCorner(this.blocks);
        int minX = minPos.getX() - radiusOffset;
        int minY = minPos.getY();
        int minZ = minPos.getZ() - radiusOffset;
        BlockPos maxPos = BuildingUtils.getMaxCorner(this.blocks);
        int maxX = maxPos.getX() + radiusOffset + 1;
        int maxZ = maxPos.getZ() + radiusOffset + 1;

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                BlockPos bp = new BlockPos(x,minY,z);
                float dist = (float) bpTarget.distToCenterSqr(bp.getX(), bp.getY(), bp.getZ());

                if (dist < minDist) {
                    minDist = dist;
                    minPos = bp;
                }
            }
        }
        return minPos;
    }


    public int getBlocksTotal() {
        return blocks.stream().filter(b -> !b.getBlockState().isAir()).toList().size();
    }
    public int getBlocksPlaced() {
        return blocks.stream().filter(b -> b.isPlaced && !b.getBlockState().isAir()).toList().size();
    }
    public float getBlocksPlacedPercent() {
        return (float) getBlocksPlaced() / (float) getBlocksTotal();
    }

    // TODO: add some temporary scaffolding blocks if !isBuilt

    // place blocks according to the following rules:
    // - block must be connected to something else (not air)
    // - block must be the lowest Y value possible
    private void buildNextBlock(Level level) {
        ArrayList<BuildingBlock> unplacedBlocks = new ArrayList<>(blocks.stream().filter(b -> !b.isPlaced).toList());
        int minY = getMinCorner(unplacedBlocks).getY();
        ArrayList<BuildingBlock> validBlocks = new ArrayList<>();

        // iterate through unplaced blocks and start at the bottom Y values
        for (BuildingBlock block : unplacedBlocks) {
            BlockPos bp = block.getBlockPos();
            if ((bp.getY() <= minY) &&
                (!level.getBlockState(bp.below()).isAir() ||
                 !level.getBlockState(bp.east()).isAir() ||
                 !level.getBlockState(bp.west()).isAir() ||
                 !level.getBlockState(bp.south()).isAir() ||
                 !level.getBlockState(bp.north()).isAir() ||
                 !level.getBlockState(bp.above()).isAir()))
                validBlocks.add(block);
        }
        if (validBlocks.size() > 0) {
            validBlocks.get(0).place();
            /*
            Random rand = new Random();
            validBlocks.get(rand.nextInt(validBlocks.size())).place();
            */
        }
    }

    public boolean shouldBeDestroyed() {
        if (tickAge < 10)
            return false;
        if (getBlocksPlaced() <= 0)
            return true;
        if (isBuilt)
            return getBlocksPlacedPercent() <= this.minBlocksPercent;
        else
            return explosionCount >= 3;
    }

    // destroy all remaining blocks in a final big explosion
    // only explode a quarter of the blocks to avoid lag
    private void destroy(ServerLevel level) {
        this.blocks.forEach((BuildingBlock block) -> {
            level.destroyBlock(block.getBlockPos(), false);
            if (block.isPlaced) {
                int x = block.getBlockPos().getX();
                int y = block.getBlockPos().getY();
                int z = block.getBlockPos().getZ();
                if (x % 2 == 0 && z % 2 != 0) {
                    level.explode(null, null, null,
                            x,y,z,
                            1.0f,
                            false,
                            Explosion.BlockInteraction.BREAK);
                }
            }
        });
    }

    // should only be run serverside
    public void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (evt.getLevel().isClientSide())
            return;

        ServerLevel level = (ServerLevel) evt.getLevel();

        // when a player breaks a block that's part of the building:
        // - roll explodeChance to cause explosion effects and destroy more blocks
        // - cause fire if < fireThreshold% blocksPercent
        Random rand = new Random();
        if (rand.nextFloat(1.0f) < this.explodeChance) {
            level.explode(null, null, null,
                    evt.getPos().getX(),
                    evt.getPos().getY(),
                    evt.getPos().getZ(),
                    this.explodeRadius,
                    this.getBlocksPlacedPercent() < this.fireThreshold, // fire
                    Explosion.BlockInteraction.BREAK);
            explosionCount += 1;
        }
    }

    public void tick(Level level) {
        this.tickAge += 1;

        // update all the BuildingBlock.isPlaced booleans to match what the world actually has
        for (BuildingBlock block : blocks) {
            BlockPos bp = block.getBlockPos();
            BlockState bs = block.getBlockState();
            BlockState bsWorld = level.getBlockState(bp);
            block.isPlaced = bsWorld.equals(bs);
        }
        float blocksPercent = getBlocksPlacedPercent();
        float blocksPlaced = getBlocksPlaced();
        float blocksTotal = getBlocksTotal();

        if (blocksPlaced >= blocksTotal) {
            isBuilding = false;
            isBuilt = true;
        }

        if (!level.isClientSide()) {

            // TODO: if builder is assigned, set isBuilding true

            // TODO: keep the surrounding chunks loaded or else the building becomes unselectable when unloaded

            // place a block if the tick has run down
            if (isBuilding && blocksPlaced < blocksTotal) {
                ticksToNextBuild -= 1;
                if (ticksToNextBuild <= 0) {
                    ticksToNextBuild = ticksPerBuild;
                    buildNextBlock(level);
                }
            }

            // TODO: if fires exist, put them out one by one (or gradually remove them if blocksPercent > fireThreshold%)

            if (this.shouldBeDestroyed()) {
                this.destroy((ServerLevel) level);
            }
        }


    }
}
