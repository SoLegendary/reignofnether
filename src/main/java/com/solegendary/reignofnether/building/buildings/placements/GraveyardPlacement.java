package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.GraveyardUnitProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GraveyardPlacement extends ProductionPlacement {

    public boolean autoRelease = true;

    private static List<BlockPos> skullPoses = List.of(
            new BlockPos(1,1,3),
            new BlockPos(2,1,3),
            new BlockPos(1,1,5),
            new BlockPos(2,1,5),
            new BlockPos(3,1,4),
            new BlockPos(4,1,2),
            new BlockPos(4,1,3),
            new BlockPos(5,1,3),
            new BlockPos(4,1,6),
            new BlockPos(5,1,6)
    );

    // unlocked with mass burials
    private static List<BlockPos> wallSkullPoses = List.of(
            new BlockPos(2,1,2),
            new BlockPos(2,2,2),
            new BlockPos(2,3,2),
            new BlockPos(3,2,2),
            new BlockPos(3,3,2),
            new BlockPos(5,1,4),
            new BlockPos(5,2,4),
            new BlockPos(5,3,4),
            new BlockPos(5,2,5),
            new BlockPos(5,3,5)
    );

    private final static HashMap<EntityType<? extends Unit>, Block> ENTITY_SKULL_MAP = new HashMap<>();

    static {
        ENTITY_SKULL_MAP.put(EntityRegistrar.ZOMBIE_UNIT.get(), Blocks.ZOMBIE_HEAD);
        ENTITY_SKULL_MAP.put(EntityRegistrar.DROWNED_UNIT.get(), BlockRegistrar.DROWNED_HEAD.get());
        ENTITY_SKULL_MAP.put(EntityRegistrar.HUSK_UNIT.get(), BlockRegistrar.HUSK_HEAD.get());
        ENTITY_SKULL_MAP.put(EntityRegistrar.SKELETON_UNIT.get(), Blocks.SKELETON_SKULL);
        ENTITY_SKULL_MAP.put(EntityRegistrar.STRAY_UNIT.get(), BlockRegistrar.STRAY_SKULL.get());
        ENTITY_SKULL_MAP.put(EntityRegistrar.BOGGED_UNIT.get(), BlockRegistrar.BOGGED_SKULL.get());
    }

    private final static HashMap<EntityType<? extends Unit>, Block> ENTITY_WALL_SKULL_MAP = new HashMap<>();

    static {
        ENTITY_WALL_SKULL_MAP.put(EntityRegistrar.ZOMBIE_UNIT.get(), Blocks.ZOMBIE_WALL_HEAD);
        ENTITY_WALL_SKULL_MAP.put(EntityRegistrar.DROWNED_UNIT.get(), BlockRegistrar.DROWNED_WALL_HEAD.get());
        ENTITY_WALL_SKULL_MAP.put(EntityRegistrar.HUSK_UNIT.get(), BlockRegistrar.HUSK_WALL_HEAD.get());
        ENTITY_WALL_SKULL_MAP.put(EntityRegistrar.SKELETON_UNIT.get(), Blocks.SKELETON_WALL_SKULL);
        ENTITY_WALL_SKULL_MAP.put(EntityRegistrar.STRAY_UNIT.get(), BlockRegistrar.STRAY_WALL_SKULL.get());
        ENTITY_WALL_SKULL_MAP.put(EntityRegistrar.BOGGED_UNIT.get(), BlockRegistrar.BOGGED_WALL_SKULL.get());
    }

    public GraveyardPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    private EntityType<? extends Unit> getEntityType(Block block, boolean isWallBlock) {
        if (isWallBlock) {
            for (var entry : ENTITY_WALL_SKULL_MAP.entrySet()) {
                if (entry.getValue() == block) return entry.getKey();
            }
        } else {
            for (var entry : ENTITY_SKULL_MAP.entrySet()) {
                if (entry.getValue() == block) return entry.getKey();
            }
        }
        return null;
    }

    private Block getSkullBlock(EntityType<? extends Mob> entityType, boolean isWallBlock) {
        if (isWallBlock) {
            if (ENTITY_WALL_SKULL_MAP.containsKey(entityType))
                return ENTITY_WALL_SKULL_MAP.get(entityType);
        } else {
            if (ENTITY_SKULL_MAP.containsKey(entityType))
                return ENTITY_SKULL_MAP.get(entityType);
        }
        return null;
    }

    public int getTotalSkulls() {
        int totalSkulls = 0;
        for (BlockPos relativePos : skullPoses) {
            BlockPos relativeRotPos = BuildingUtils.rotatePos(relativePos, this.rotation);
            BlockPos worldPos = relativeRotPos.offset(originPos).above();
            if (level.getBlockState(worldPos).getBlock() instanceof SkullBlock)
                totalSkulls++;
        }
        return totalSkulls;
    }

    public int getMaxSkulls() {
        if (this.level.isClientSide()) {
            return ResearchClient.hasResearch(ProductionItems.RESEARCH_MASS_BURIAL) ?
                    Graveyard.OVERFLOW_AMOUNT_UPGRADED : Graveyard.OVERFLOW_AMOUNT;
        } else {
            return ResearchServerEvents.playerHasResearch(this.ownerName, ProductionItems.RESEARCH_MASS_BURIAL) ?
                    Graveyard.OVERFLOW_AMOUNT_UPGRADED : Graveyard.OVERFLOW_AMOUNT;
        }
    }

    public int getSkullsInProgress() {
        if (getUpgradeLevel() <= 0)
            return 0;
        int i = 0;
        for (ActiveProduction activeProd : productionQueue)
            if (activeProd.item instanceof GraveyardUnitProductionItem)
                i++;
        return i;
    }

    // create a skull at the first empty pos
    public void createSkull(EntityType<? extends Mob> entityType) {
        if (getTotalSkulls() >= getMaxSkulls()) return;
        boolean isWallBlock = false;
        boolean is2ndWall = false;

        BlockPos skullPos = null;
        for (int i = 0; i < skullPoses.size(); i++) {
            BlockPos relativeRotPos = BuildingUtils.rotatePos(skullPoses.get(i), this.rotation);
            BlockPos worldPos = relativeRotPos.offset(originPos).above();
            if (!(level.getBlockState(worldPos).getBlock() instanceof SkullBlock)) {
                skullPos = worldPos;
                break;
            }
        }
        if (skullPos == null && getMaxSkulls() > Graveyard.OVERFLOW_AMOUNT) {
            for (int i = 0; i < wallSkullPoses.size(); i++) {
                BlockPos relativeRotPos = BuildingUtils.rotatePos(wallSkullPoses.get(i), this.rotation);
                BlockPos worldPos = relativeRotPos.offset(originPos).above();
                if (!(level.getBlockState(worldPos).getBlock() instanceof WallSkullBlock)) {
                    if (i > 4) is2ndWall = true;
                    skullPos = worldPos;
                    isWallBlock = true;
                    break;
                }
            }
        }
        if (skullPos != null) {
            Block skullBlock = getSkullBlock(entityType, isWallBlock);
            if (skullBlock == null) return;
            BlockState bs;
            if (is2ndWall) {
                bs = skullBlock.defaultBlockState().setValue(WallSkullBlock.FACING, Direction.WEST);
            } else if (isWallBlock) {
                bs = skullBlock.defaultBlockState().setValue(WallSkullBlock.FACING, Direction.SOUTH);
            } else {
                bs = skullBlock.defaultBlockState().setValue(SkullBlock.ROTATION, 8);
            }
            level.setBlockAndUpdate(skullPos, bs);
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, skullPos, Block.getId(Blocks.COBBLESTONE.defaultBlockState()));
        }
    }

    // find first skull in reverse order to createSkull to turn into a unit
    public void releaseNextUnit() {
        if (this.level.isClientSide()) return;
        Block skull = null;
        BlockPos skullPos = null;
        boolean isWallBlock = true;

        for (int i = wallSkullPoses.size() - 1; i >= 0; i--) {
            BlockPos relativeRotPos = BuildingUtils.rotatePos(wallSkullPoses.get(i), this.rotation);
            BlockPos worldPos = relativeRotPos.offset(originPos).above();
            if (level.getBlockState(worldPos).getBlock() instanceof WallSkullBlock skullBlock) {
                skull = skullBlock;
                skullPos = worldPos;
            }
        }
        if (skull == null) {
            for (int i = skullPoses.size() - 1; i >= 0; i--) {
                BlockPos relativeRotPos = BuildingUtils.rotatePos(skullPoses.get(i), this.rotation);
                BlockPos worldPos = relativeRotPos.offset(originPos).above();
                if (level.getBlockState(worldPos).getBlock() instanceof SkullBlock skullBlock) {
                    skull = skullBlock;
                    skullPos = worldPos;
                    isWallBlock = false;
                }
            }
        }
        if (skull != null) {
            level.setBlockAndUpdate(skullPos, Blocks.AIR.defaultBlockState());
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, skullPos, Block.getId(Blocks.COBBLESTONE.defaultBlockState()));
            EntityType<? extends Unit> entityType = getEntityType(skull, isWallBlock);
            if (entityType != null)
                this.produceUnit((ServerLevel) level, entityType, this.ownerName, true);
        }
    }
}
