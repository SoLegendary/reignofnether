package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static com.solegendary.reignofnether.building.BuildingUtils.getMaxCorner;
import static com.solegendary.reignofnether.building.BuildingUtils.getMinCorner;

public abstract class Building {

    private final static int BASE_MS_PER_BUILD = 5; // time taken to build each block with 1 villager assigned;
    public final float MELEE_DAMAGE_MULTIPLIER = 0.5f; // damage multiplier applied to melee attackers

    public String name;
    public static String structureName;
    private Level level; // directly return MC.level if it's clientside to avoid stale references
    public BlockPos originPos;
    public Rotation rotation;
    public ResourceLocation icon;

    public boolean isBuilt; // set true when blocksPercent reaches 100% the first time; the building can then be used
    public int msToNextBuild = BASE_MS_PER_BUILD; // 5ms per tick

    // building collapses at a certain % blocks remaining so players don't have to destroy every single block
    protected float minBlocksPercent = 0.5f;
    // chance for a mini explosion to destroy extra blocks if a player is breaking it
    // should be higher for large fragile buildings so players don't take ages to destroy it
    protected float explodeChance = 0.3f;
    protected float explodeRadius = 2.0f;
    protected float fireThreshold = 0.75f; // if building has less %hp than this, explosions caused can make fires
    protected int unrepairedBlocksDestroyed = 0; // ticks up on a block being destroyed, ticks down on a block being built

    protected ArrayList<BuildingBlock> blocks = new ArrayList<>();
    protected ArrayList<BuildingBlock> blockPlaceQueue = new ArrayList<>();
    public String ownerName;
    public Block portraitBlock; // block rendered in the portrait GUI to represent this building
    public int tickAge = 0; // how many ticks ago this building was placed
    public boolean canAcceptResources = false; // can workers drop off resources here?
    public int serverBlocksPlaced = 1;

    public int foodCost;
    public int woodCost;
    public int oreCost;
    public int popSupply; // max population this building provides

    public Level getLevel() {
        return level;
    }
    public void setLevel(Level level) { this.level = level; }

    public Building(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        this.level = level;
        this.originPos = originPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
    }

    public void addToBlockPlaceQueue(BuildingBlock block) {
        this.blockPlaceQueue.add(block);
    }

    public ArrayList<WorkerUnit> getBuilders(Level level) {
        ArrayList<WorkerUnit> builders = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof WorkerUnit workerUnit) {
                BuildRepairGoal goal = workerUnit.getBuildRepairGoal();
                if (goal != null && goal.getBuildingTarget() == this && goal.isBuilding())
                    builders.add(workerUnit);
            }
        }
        return builders;
    }

    public ArrayList<BuildingBlock> getBlocks() {
        return blocks;
    }

    public static Button getBuildButton() { return null; }

    public boolean canAfford(String ownerName) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName))
                return (resources.food >= foodCost &&
                        resources.wood >= woodCost &&
                        resources.ore >= oreCost);
        return false;
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
            if ((block.isPlaced(getLevel()) || !onlyPlacedBlocks) && block.getBlockPos().equals(bp))
                return true;
        return false;
    }

    // returns the lowest Y value block in this.blocks to the given blockPos
    // radius offset is the distance away from the building itself to have the returned pos
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

    private boolean isFullyLoadedClientSide(ClientLevel level) {
        for (BuildingBlock block : this.blocks)
            if (!level.isLoaded(block.getBlockPos()))
                return false;
        return true;
    }

    public int getBlocksTotal() {
        return blocks.stream().filter(b -> !b.getBlockState().isAir()).toList().size();
    }

    public int getBlocksPlaced() {
        // on clientside a building outside of render view would always be 0
        if (!getLevel().isClientSide() || isFullyLoadedClientSide((ClientLevel) getLevel()))
            return blocks.stream().filter(b -> b.isPlaced(getLevel()) && !b.getBlockState().isAir()).toList().size();
        else
            return this.serverBlocksPlaced;
    }
    public float getBlocksPlacedPercent() {
        return (float) getBlocksPlaced() / (float) getBlocksTotal();
    }

    // TODO: add some temporary scaffolding blocks if !isBuilt

    // place blocks according to the following rules:
    // - block must be connected to something else (not air)
    // - block must be the lowest Y value possible
    private void buildNextBlock(ServerLevel level) {

        // if the building is already constructed then start subtracting resources for repairs
        if (isBuilt) {
            if (!ResourcesServerEvents.canAfford(ownerName, ResourceName.WOOD, 1)) {
                ResourcesClientboundPacket.warnInsufficientResources(ownerName, true, false, true);
                return;
            }
            else
                ResourcesServerEvents.addSubtractResources(new Resources(ownerName,0,-1,0));
        }

        ArrayList<BuildingBlock> unplacedBlocks = new ArrayList<>(blocks.stream().filter(
                b -> !b.isPlaced(getLevel()) && !b.getBlockState().isAir()
        ).toList());

        int minY = getMinCorner(unplacedBlocks).getY();
        ArrayList<BuildingBlock> validBlocks = new ArrayList<>();

        // iterate through unplaced blocks and start at the bottom Y values
        // prioritise placing blocks that are connected to other blocks (nonfloating)
        int nonFloatingBlocks = 0;
        for (BuildingBlock block : unplacedBlocks) {
            BlockPos bp = block.getBlockPos();
            if ((bp.getY() <= minY) &&
                    (!level.getBlockState(bp.below()).isAir() ||
                            !level.getBlockState(bp.east()).isAir() ||
                            !level.getBlockState(bp.west()).isAir() ||
                            !level.getBlockState(bp.south()).isAir() ||
                            !level.getBlockState(bp.north()).isAir() ||
                            !level.getBlockState(bp.above()).isAir())) {
                nonFloatingBlocks += 1;
                validBlocks.add(block);
            }
        }
        // if there were no nonFloating blocks then allow floating blocks
        if (nonFloatingBlocks == 0) {
            for (BuildingBlock block : unplacedBlocks) {
                BlockPos bp = block.getBlockPos();
                if (bp.getY() <= minY)
                    validBlocks.add(block);
            }
        }

        if (validBlocks.size() > 0) {
            this.blockPlaceQueue.add(validBlocks.get(0));
            if (unrepairedBlocksDestroyed > 0)
                unrepairedBlocksDestroyed -= 1;
        }
    }

    private void extinguishRandomFire(ServerLevel level) {
        BlockPos minPos = BuildingUtils.getMinCorner(blocks).offset(-1,-1,-1);
        BlockPos maxPos = BuildingUtils.getMaxCorner(blocks).offset(1,1,1);

        for (int x = minPos.getX(); x <= maxPos.getX(); x++)
            for (int y = minPos.getY(); y <= maxPos.getY(); y++)
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
                    if (level.getBlockState(new BlockPos(x,y,z)).getBlock() == Blocks.FIRE) {
                        level.destroyBlock(new BlockPos(x,y,z), false);
                        return;
                    }
    }

    public void destroyRandomBlocks(int amount) {
        if (getLevel().isClientSide())
            return;
        ArrayList<BuildingBlock> placedBlocks = new ArrayList<>(blocks.stream().filter(b -> b.isPlaced(getLevel())).toList());
        Collections.shuffle(placedBlocks);
        for (int i = 0; i < amount && i < placedBlocks.size(); i++)
            getLevel().destroyBlock(placedBlocks.get(i).getBlockPos(), false);
    }

    public boolean shouldBeDestroyed() {
        if (tickAge < 10)
            return false;
        if (getBlocksPlaced() <= 0)
            return true;
        if (isBuilt)
            return getBlocksPlacedPercent() <= this.minBlocksPercent;
        else
            return unrepairedBlocksDestroyed >= 10;
    }

    // destroy all remaining blocks in a final big explosion
    // only explode a fraction of the blocks to avoid lag and sound spikes
    public void destroy(ServerLevel serverLevel) {
        BuildingClientboundPacket.destroyBuilding(this.originPos);

        this.blocks.forEach((BuildingBlock block) -> {
            if (block.getBlockState().getMaterial().isLiquid()) {
                BlockState air = Blocks.AIR.defaultBlockState();
                serverLevel.setBlockAndUpdate(block.getBlockPos(), air);
            }
            // attempt to destroy regardless of whether it's placed since blocks can change state when neighbours change
            int x = block.getBlockPos().getX();
            int y = block.getBlockPos().getY();
            int z = block.getBlockPos().getZ();
            if (block.isPlaced(serverLevel) && x % 2 == 0 && z % 2 != 0) {
                serverLevel.explode(null, null, null,
                        x,y,z,
                        1.0f,
                        false,
                        Explosion.BlockInteraction.BREAK);
            }
            serverLevel.destroyBlock(block.getBlockPos(), false);
        });
    }

    // should only be run serverside
    public void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (evt.getLevel().isClientSide())
            return;

        unrepairedBlocksDestroyed += 1;
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
        }
    }

    public void onBuilt() {
        isBuilt = true;
    }

    public void tick(Level tickLevel) {

        this.tickAge += 1;

        for (BuildingBlock block : blocks) {
            BlockPos bp = block.getBlockPos();
            BlockState bs = block.getBlockState();
            BlockState bsWorld = tickLevel.getBlockState(bp);
        }
        float blocksPercent = getBlocksPlacedPercent();
        float blocksPlaced = getBlocksPlaced();
        float blocksTotal = getBlocksTotal();

        if (blocksPlaced >= blocksTotal && !isBuilt)
            onBuilt();

        if (!tickLevel.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) tickLevel;
            int builderCount = getBuilders(serverLevel).size();

            // place a block if the tick has run down
            if (blocksPlaced < blocksTotal && builderCount > 0) {
                int msPerBuild = (3 * BASE_MS_PER_BUILD) / (builderCount + 2);
                if (msToNextBuild > msPerBuild)
                    msToNextBuild = msPerBuild;

                msToNextBuild -= 5;
                if (msToNextBuild <= 0) {
                    msToNextBuild = msPerBuild;
                    extinguishRandomFire(serverLevel);
                    buildNextBlock(serverLevel);
                }
            }
            if (this.shouldBeDestroyed())
                this.destroy(serverLevel);

            // blocks that will build themselves on each tick (eg. foundations from placement, upgrade sections)
            if (blockPlaceQueue.size() > 0) {
                BuildingBlock nextBlock = blockPlaceQueue.get(0);
                BlockPos bp = nextBlock.getBlockPos();
                BlockState bs = nextBlock.getBlockState();
                if (level.isLoaded(bp)) {
                    level.setBlockAndUpdate(bp, bs);
                    level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, bp, Block.getId(bs));
                    level.levelEvent(bs.getSoundType().getPlaceSound().hashCode(), bp, Block.getId(bs));
                    blockPlaceQueue.removeIf(i -> i.equals(nextBlock));
                }
            }
        }
    }
}
