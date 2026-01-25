package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.attackwarnings.AttackWarningClientboundPacket;
import com.solegendary.reignofnether.building.buildings.monsters.DarkWatchtower;
import com.solegendary.reignofnether.building.buildings.piglins.Bastion;
import com.solegendary.reignofnether.building.buildings.piglins.CentralPortal;
import com.solegendary.reignofnether.building.buildings.piglins.FlameSanctuary;
import com.solegendary.reignofnether.building.buildings.piglins.Fortress;
import com.solegendary.reignofnether.building.buildings.piglins.PortalBasic;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.building.buildings.shared.AbstractStockpile;
import com.solegendary.reignofnether.building.buildings.villagers.Watchtower;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.fogofwar.FrozenChunk;
import com.solegendary.reignofnether.fogofwar.FrozenChunkClientboundPacket;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.research.researchItems.ResearchSilverfish;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.SilverfishUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.util.MiscUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;
import static com.solegendary.reignofnether.building.BuildingUtils.getMaxCorner;
import static com.solegendary.reignofnether.building.BuildingUtils.getMinCorner;
import static com.solegendary.reignofnether.building.BuildingUtils.getTotalCompletedBuildingsOwned;
import static com.solegendary.reignofnether.player.PlayerServerEvents.isRTSPlayer;
import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;
import static com.solegendary.reignofnether.resources.ResourcesServerEvents.NEUTRAL_BUILDING_BOUNTY_PERCENT;
import static com.solegendary.reignofnether.survival.SurvivalServerEvents.ENEMY_OWNER_NAME;

public class BuildingPlacement {
    private Building building;
    public Level level; // directly return MC.level if it's clientside to avoid stale references
    public BlockPos originPos;
    public Rotation rotation;

    public boolean isExploredClientside = false; // show on minimap
    public boolean isDestroyedServerside = false;
    public boolean isBuiltServerside = false;

    public final boolean isCapitol;

    public boolean isBuilt; // set true when blocksPercent reaches 100% the first time; the building can then be used

    private final static int BASE_MS_PER_BUILD = 500; // time taken to build each block with 1 villager assigned;
    public float msToNextBuild = BASE_MS_PER_BUILD; // 5ms per tick

    // building collapses at a certain % blocks remaining so players don't have to destroy every single block
    public final float MIN_BLOCKS_PERCENT = 0.5f;

    protected int highestBlockCountReached = 2; // effective max health of the building

    protected ArrayList<BuildingBlock> scaffoldBlocks = new ArrayList<>();
    /**
     * Don't set blocks directly
     * Please use setBlocks()
     */
    protected ArrayList<BuildingBlock> blocks; // positions are absolute
    protected Map<BlockPos, BuildingBlock> blockMap = new HashMap<>();
    protected Set<BlockPos> placedBlockPosSet = new HashSet<>();
    protected int totalBlocks = 0;
    protected ArrayList<BuildingBlock> blockPlaceQueue = new ArrayList<>();
    public String ownerName;
    public int serverBlocksPlaced = 1;
    private int totalBlocksEverBroken = 0;

    private long ticksToExtinguish = 0;
    private final long TICKS_TO_EXTINGUISH = 100;

    private final long TICKS_TO_SPAWN_ANIMALS_MAX = 1800; // how often we attempt to spawn animals around each
    private long ticksToSpawnAnimals = 0; // spawn once soon after placement
    private final int MAX_ANIMALS = 8;
    private final int ANIMAL_SPAWN_BLOCK_RANGE = 70; // block range to check and spawn animals in
    private final int ANIMAL_SPAWN_RANGE_MIN = 15;

    protected long tickAgeAfterBuilt = 0; // not saved
    protected long tickAge = 0; // not saved

    public final BlockPos minCorner;
    public final BlockPos maxCorner;
    public final BlockPos centrePos;

    public boolean isDiagonalBridge = false;

    public boolean selfBuilding = false; // if set to true, will build itself quickly without workers (but not repair)

    protected List<AbilityButton> abilityButtons = new ArrayList<>();
    protected List<Ability> abilities = new ArrayList<>();

    Object2ObjectArrayMap<Ability, Float> cooldowns = new Object2ObjectArrayMap<>();
    Object2ObjectArrayMap<Ability, Integer> charges = new Object2ObjectArrayMap<>();

    public List<AbilityButton> getAbilityButtons() {
        return abilityButtons;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public int getHighestBlockCountReached() {
        return highestBlockCountReached;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Mob lastAttacker = null;

    public boolean allowProdWhileBuilding = false;

    public Ability autocast;

    private ArmorStand targetStand = null;

    public ArmorStand getTargetStand() {
        if (targetStand == null)
            createArmourStandTarget();
        return targetStand;
    }

    private EntityType<? extends Animal> lastAnimalType = null;

    public BuildingPlacement(
        Building building,
        Level level,
        BlockPos originPos,
        Rotation rotation,
        String ownerName,
        ArrayList<BuildingBlock> blocks,
        boolean isCapitol
    ) {
        Objects.requireNonNull(building, "Building can't be null");
        this.building = building;
        this.level = level;
        this.originPos = originPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.isCapitol = isCapitol;

        setBlocks(blocks);

        cooldowns.defaultReturnValue(0F);
        var maxX = Integer.MIN_VALUE;
        var minX = Integer.MAX_VALUE;
        var maxY = Integer.MIN_VALUE;
        var minY = Integer.MAX_VALUE;
        var maxZ = Integer.MIN_VALUE;
        var minZ = Integer.MAX_VALUE;
        for (var block : blocks) {
            var pos = block.getBlockPos();
            maxX = Math.max(pos.getX(), maxX);
            minX = Math.min(pos.getX(), minX);
            maxY = Math.max(pos.getY(), maxY);
            minY = Math.min(pos.getY(), minY);
            maxZ = Math.max(pos.getZ(), maxZ);
            minZ = Math.min(pos.getZ(), minZ);
        }
        this.minCorner = new BlockPos(minX, minY, minZ);
        this.maxCorner = new BlockPos(maxX, maxY, maxZ);
        this.centrePos = new BlockPos((int) ((float) (this.minCorner.getX() + this.maxCorner.getX()) / 2),
            (int) ((float) (this.minCorner.getY() + this.maxCorner.getY()) / 2),
            (int) ((float) (this.minCorner.getZ() + this.maxCorner.getZ()) / 2)
        );

        // re-hide players if they were revealed
        if (this.isCapitol && !this.level.isClientSide()) {
            if (BuildingUtils.getTotalCompletedBuildingsOwned(false, this.ownerName) == 1 &&
                !TutorialServerEvents.isEnabled() && FogOfWarServerEvents.isEnabled()) {
                sendMessageToAllPlayers("hud.reignofnether.placed_capitol", false, this.ownerName);
            }
            FogOfWarClientboundPacket.revealOrHidePlayer(false, this.ownerName);
        }
        for (Ability ability : building.abilities.get()) {
            getAbilities().add(ability);
        }
        updateButtons();
        initPlacedBlocks();
    }

    protected void setBlocks(ArrayList<BuildingBlock> blocks) {
        this.blocks = blocks;
        var index = 0;
        for (BuildingBlock block : this.blocks) {
            blockMap.put(block.getBlockPos(), block);
            if (block.getBlockState().isAir()) continue;
            index++;
        }
        this.totalBlocks = index;
    }

    public float getMeleeDamageMult() {
        return getBuilding().getMeleeDamageMult();
    }

    public Faction getFaction() {
        return getBuilding().getFaction();
    }

    // fully repairs and rebuilds all the blocks in the building
    // usually used when the structure changes (like when upgrading a building)
    public void refreshBlocks() {
        for (BuildingBlock block : blocks)
            if (!block.isPlaced(level) && !block.getBlockState().isAir()) {
                addToBlockPlaceQueue(block);
            }
        initPlacedBlocks();
    }

    public void setServerBlocksPlaced(int blocksPlaced) {
        this.serverBlocksPlaced = blocksPlaced;
        if (this.getBlocksPlaced() > highestBlockCountReached) {
            highestBlockCountReached = this.getBlocksPlaced();
        }
    }

    public void addToBlockPlaceQueue(BuildingBlock block) {
        this.blockPlaceQueue.add(block);
    }

    public ArrayList<WorkerUnit> getBuilders(Level level) {
        ArrayList<WorkerUnit> builders = new ArrayList<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof WorkerUnit workerUnit) {
                BuildRepairGoal goal = workerUnit.getBuildRepairGoal();
                if (goal != null && goal.getBuildingTarget() == this && goal.isBuilding()) {
                    builders.add(workerUnit);
                }
            }
        }
        return builders;
    }

    public ArrayList<BuildingBlock> getBlocks() {
        return blocks;
    }

    public ArrayList<BuildingBlock> getScaffoldBlocks() {
        return scaffoldBlocks;
    }

    public static Button getBuildButton() {
        return null;
    }


    public boolean canAfford(String ownerName) {
        if (SandboxServer.isAnyoneASandboxPlayer() &&
            (ownerName.isEmpty() || ownerName.equals("Enemy")))
            return true;

        if (SurvivalServerEvents.isEnabled() &&
            SurvivalServerEvents.ENEMY_OWNER_NAME.equals(ownerName))
            return true;

        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName)) {
                return (
                    resources.food >= building.cost.food &&
                    resources.wood >= building.cost.wood &&
                    resources.ore >= building.cost.ore
                );
            }
        return false;
    }

    public boolean isPosInsideBuilding(BlockPos bp) {
        return bp.getX() <= this.maxCorner.getX() && bp.getX() >= this.minCorner.getX()
               && bp.getY() <= this.maxCorner.getY() && bp.getY() >= this.minCorner.getY()
               && bp.getZ() <= this.maxCorner.getZ() && bp.getZ() >= this.minCorner.getZ();
    }

    public boolean isPosPartOfBuilding(BlockPos bp, boolean onlyPlacedBlocks) {
        if (!blockMap.containsKey(bp)) return false;
        if (!onlyPlacedBlocks) return true;
        return blockMap.get(bp).isPlaced(level);
    }

    // returns the lowest Y value block in this.blocks to the given blockPos
    // radius offset is the distance away from the building itself to have the returned pos
    // excludes positions inside the building so that workers  move out of the building foundations
    public BlockPos getClosestGroundPos(BlockPos bpTarget, int radiusOffset) {
        float minDist = 999999;
        BlockPos minPos = this.minCorner;
        int minX = minPos.getX() - radiusOffset;
        int minY = minPos.getY();
        int minZ = minPos.getZ() - radiusOffset;
        BlockPos maxPos = this.maxCorner;
        int maxX = maxPos.getX() + radiusOffset + 1;
        int maxZ = maxPos.getZ() + radiusOffset + 1;

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                BlockPos bp = new BlockPos(x, minY, z);
                if (!(getBuilding() instanceof AbstractBridge) && isPosInsideBuilding(bp))
                    continue;

                float dist = (float) bpTarget.distToCenterSqr(bp.getX(), bp.getY(), bp.getZ());

                if (dist < minDist) {
                    minDist = dist;
                    minPos = bp;
                }
            }
        }
        return minPos;
    }

    // does not account for fog of war
    private boolean isFullyLoadedClientSide(ClientLevel level) {
        for (BuildingBlock block : this.blocks)
            if (!level.isLoaded(block.getBlockPos())) {
                return false;
            }
        return true;
    }

    public int getBlocksTotal() {
        return totalBlocks;
    }

    public void initPlacedBlocks() {
        placedBlockPosSet.clear();
        if (!getLevel().isClientSide() || isFullyLoadedClientSide((ClientLevel) getLevel())) {
            var blocksPlaced = 0;
            for (BuildingBlock block : blocks) {
                if (block.isPlaced(getLevel()) && !block.getBlockState().isAir()) {
                    if (blockMap.containsKey(block.getBlockPos())) placedBlockPosSet.add(block.getBlockPos());
                    blocksPlaced++;
                }
            }
            if (blocksPlaced > highestBlockCountReached) {
                highestBlockCountReached = blocksPlaced;
            }
        }
    }

    public int getBlocksPlaced() {
        if (level.isClientSide) return this.serverBlocksPlaced;
        return placedBlockPosSet.size();
    }

    // % of total buildable blocks existing
    public float getBlocksPlacedPercent() {
        return (float) getBlocksPlaced() / (float) getBlocksTotal();
    }

    public float getUnbuiltBlocksPlacedPercent() {
        return (float) getBlocksPlaced() / highestBlockCountReached;
    }

    // health and maxHealth are normalised to 0 being point of destruction
    public int getHealth() {
        return (int) (getBlocksPlaced() / MIN_BLOCKS_PERCENT) - (getHighestBlockCountReached());
    }

    public int getMaxHealth() {
        return (int) (getHighestBlockCountReached() / MIN_BLOCKS_PERCENT) - (getHighestBlockCountReached());
    }

    // place blocks according to the following rules:
    // - block must be connected to something else (not air)
    // - block must be the lowest Y value possible
    public void buildNextBlock(ServerLevel level, String builderName) {
        // if the building is already constructed then start subtracting resources for repairs
        if (isBuilt) {
            if (!ResourcesServerEvents.canAfford(builderName, ResourceName.WOOD, 1)) {
                ResourcesClientboundPacket.warnInsufficientResources(builderName, true, false, true);
                return;
            } else {
                ResourcesServerEvents.addSubtractResources(new Resources(builderName, 0, -1, 0));
            }
        }

        ArrayList<BuildingBlock> unplacedBlocks = new ArrayList<>();
        for (BuildingBlock block : blocks) {
            if (!block.isPlaced(getLevel()) && !block.getBlockState().isAir()) unplacedBlocks.add(block);
        }

        int minY = getMinCorner(unplacedBlocks).getY();
        ArrayList<BuildingBlock> validBlocks = new ArrayList<>();

        // iterate through unplaced blocks and start at the bottom Y values
        // prioritise placing blocks that are connected to other blocks (nonfloating)
        int nonFloatingBlocks = 0;

        if (!(getBuilding() instanceof AbstractBridge)) {
            for (BuildingBlock block : unplacedBlocks) {
                BlockPos bp = block.getBlockPos();
                if ((bp.getY() <= minY) && (
                    !level.getBlockState(bp.below()).isAir() || !level.getBlockState(bp.east()).isAir()
                    || !level.getBlockState(bp.west()).isAir() || !level.getBlockState(bp.south()).isAir()
                    || !level.getBlockState(bp.north()).isAir() || !level.getBlockState(bp.above()).isAir()
                )) {
                    nonFloatingBlocks += 1;
                    validBlocks.add(block);
                }
            }
        }
        // if there were no nonFloating blocks then allow floating blocks
        if (nonFloatingBlocks == 0) {
            for (BuildingBlock block : unplacedBlocks) {
                BlockPos bp = block.getBlockPos();
                if (bp.getY() <= minY || getBuilding() instanceof AbstractBridge) {
                    validBlocks.add(block);
                }
            }
        }
        if (!validBlocks.isEmpty()) {
            if (getBuilding() instanceof AbstractBridge) {
                ArrayList<WorkerUnit> builders = getBuilders(this.level);
                if (!builders.isEmpty()) {
                    BlockPos builderPos = ((LivingEntity) builders.get(new Random().nextInt(builders.size()))).getOnPos();
                    validBlocks.sort(Comparator.comparing(bb -> bb.getBlockPos().distSqr(builderPos)));
                }
            }
            this.blockPlaceQueue.add(validBlocks.get(0));
        }
    }

    private void extinguishFires(ServerLevel level) {
        BlockPos minPos = this.minCorner.offset(-1, -1, -1);
        BlockPos maxPos = this.maxCorner.offset(1, 1, 1);

        for (int x = minPos.getX(); x <= maxPos.getX(); x++)
            for (int y = minPos.getY(); y <= maxPos.getY(); y++)
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++)
                    if (level.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.FIRE) {
                        level.destroyBlock(new BlockPos(x, y, z), false);
                    }
    }

    // are we allowed to destroy this blockPos using destroyRandomBlocks
    public boolean canDestroyBlock(BlockPos relativeBp) {
        return true;
    }

    private boolean isDestroyedAndNotNextToLiquid(BuildingBlock block) {
        if (!(this instanceof BridgePlacement) && this.level.getBlockState(block.getBlockPos()).getFluidState().isEmpty() && (
            !this.level.getBlockState(block.getBlockPos().above()).getFluidState().isEmpty()
            || !this.level.getBlockState(block.getBlockPos().north()).getFluidState().isEmpty()
            || !this.level.getBlockState(block.getBlockPos().south()).getFluidState().isEmpty()
            || !this.level.getBlockState(block.getBlockPos().east()).getFluidState().isEmpty()
            || !this.level.getBlockState(block.getBlockPos().west()).getFluidState().isEmpty()
        )) {
            return false;
        }
        if (!canDestroyBlock(block.getBlockPos().offset(-originPos.getX(), -originPos.getY(), -originPos.getZ()))) {
            return false;
        }
        return block.isPlaced(getLevel());
    }

    public void destroyRandomBlocks(int amount) {
        if (getLevel().isClientSide())
            return;
        if (building.invulnerable)
            return;
        var placedBlocks = new ArrayList<BuildingBlock>();
        for (BuildingBlock block : blocks) {
            if (!isDestroyedAndNotNextToLiquid(block)) continue;
            placedBlocks.add(block);
        }
        Collections.shuffle(placedBlocks);
        for (int i = 0; i < amount && i < placedBlocks.size(); i++) {
            BlockPos bp = placedBlocks.get(i).getBlockPos();
            if (!getLevel().getBlockState(bp).getFluidState().isEmpty()) {
                getLevel().setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
            } else {
                getLevel().destroyBlock(bp, false);
            }
            this.onBlockBreak((ServerLevel) getLevel(), bp, false);
        }
        if (amount > 0) {
            AttackWarningClientboundPacket.sendWarning(ownerName, BuildingUtils.getCentrePos(getBlocks()));
        }
    }

    public boolean shouldBeDestroyed() {
        if (!this.level.getWorldBorder().isWithinBounds(centrePos)) {
            return true;
        }
        if (this.level.isClientSide() && (FogOfWarClientEvents.isBuildingInBrightChunk(this) && isDestroyedServerside)) {
            return true;
        }
        if (this.level.isClientSide() && (!FogOfWarClientEvents.isBuildingInBrightChunk(this) || !isDestroyedServerside)) {
            return false;
        }
        if (!blockPlaceQueue.isEmpty()) {
            return false;
        }
        if (getBlocksPlaced() <= 0) {
            return true;
        }
        if (isBuilt) {
            return getBlocksPlacedPercent() <= this.MIN_BLOCKS_PERCENT;
        } else // if the building is still under construction, we instead use the highest health we've ever reached as
        // the effective max health
        {
            return totalBlocksEverBroken > 0 && getUnbuiltBlocksPlacedPercent() <= this.MIN_BLOCKS_PERCENT;
        }
    }

    // destroy all remaining blocks in a final big explosion
    // only explode a fraction of the blocks to avoid lag and sound spikes
    public void destroy(ServerLevel serverLevel) {
        this.forceChunk(false);

        this.blocks.forEach((BuildingBlock block) -> {
            if ((!block.getBlockState().getFluidState().isEmpty() ||
                 (block.getBlockState().hasProperty(BlockStateProperties.WATERLOGGED) &&
                  block.getBlockState().getValue(BlockStateProperties.WATERLOGGED))) &&
                !block.getBlockState().isAir()) {
                BlockState air = Blocks.AIR.defaultBlockState();
                serverLevel.setBlockAndUpdate(block.getBlockPos(), air);
            }
            // attempt to destroy regardless of whether it's placed since blocks can change state when neighbours change
            int x = block.getBlockPos().getX();
            int y = block.getBlockPos().getY();
            int z = block.getBlockPos().getZ();
            if (block.isPlaced(serverLevel) && x % 2 == 0 && z % 2 != 0) {
                serverLevel.explode(null, null, null, x, y, z, 1.0f, false, Level.ExplosionInteraction.TNT);
            }
            if (!block.getBlockState().isAir())
                serverLevel.destroyBlock(block.getBlockPos(), false);
        });

        this.scaffoldBlocks.forEach((BuildingBlock block) -> {
            if (serverLevel.getBlockState(block.getBlockPos()).getBlock() == Blocks.SCAFFOLDING) {
                serverLevel.destroyBlock(block.getBlockPos(), false);
            }
        });
        // we don't save scaffoldBlocks in saveBuildings() so this covers that
        if (this.scaffoldBlocks.isEmpty()) {
            for (int x = minCorner.getX(); x <= maxCorner.getX(); x++)
                for (int y = minCorner.getY() - 3; y < minCorner.getY(); y++)
                    for (int z = minCorner.getZ(); z <= maxCorner.getZ(); z++)
                        if (serverLevel.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.SCAFFOLDING) {
                            serverLevel.destroyBlock(new BlockPos(x, y, z), false);
                        }
        }

        if (!this.level.isClientSide() && isRTSPlayer(this.ownerName)) {
            if (BuildingUtils.getTotalCompletedBuildingsOwned(false, this.ownerName) == 0 &&
                !SandboxServer.isSandboxPlayer(this.ownerName)) {
                PlayerServerEvents.defeat(this.ownerName, Component.translatable("server.reignofnether.lost_buildings").getString());
            } else if (this.isCapitol) {
                var flag = false;
                for (BuildingPlacement placement : BuildingServerEvents.getBuildings()) {
                    if (placement.ownerName.equals(this.ownerName) && placement.isCapitol) {
                        flag = true;
                        break;
                    }
                }
                if (!flag && FogOfWarServerEvents.isEnabled()) {
                    sendMessageToAllPlayers("server.reignofnether.lost_capitol",
                        false,
                        this.ownerName,
                        PlayerServerEvents.TICKS_TO_REVEAL / ResourceCost.TICKS_PER_SECOND
                    );
                }
            }
        }
        if (targetStand != null)
            targetStand.discard();

        if (ownerName.isEmpty()) {
            awardBounty();
        }
        placedBlockPosSet.clear();
    }

    private void awardBounty() {
        if (lastAttacker instanceof Unit unit && !unit.getOwnerName().isEmpty()) {
            ResourceCost cost = building.cost;
            Resources resources = new Resources(unit.getOwnerName(),
                (int) (cost.food * NEUTRAL_BUILDING_BOUNTY_PERCENT),
                (int) (cost.wood * NEUTRAL_BUILDING_BOUNTY_PERCENT),
                (int) (cost.ore * NEUTRAL_BUILDING_BOUNTY_PERCENT)
            );
            if (resources.getTotalValue() > 0) {
                ResourcesClientboundPacket.showFloatingText(resources, centrePos);
                ResourcesServerEvents.addSubtractResources(resources);
            }
        }
    }

    // should only be run serverside
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        totalBlocksEverBroken += 1;
        Random rand = new Random();

        if (this.getFaction() == Faction.MONSTERS && ResearchServerEvents.playerHasResearch(this.ownerName,
            ProductionItems.RESEARCH_SILVERFISH
        )) {
            randomSilverfishSpawn(pos);
        }

        // when a player breaks a block that's part of the building:
        // - roll explodeChance to cause explosion effects and destroy more blocks
        // - cause fire if < fireThreshold% blocksPercent
        if (rand.nextFloat(1.0f) < this.building.explodeChance) {
            if (!breakBlocks && MiscUtil.isNewYearsSeason()) {
                MiscUtil.doRandomFireworkExplosion(level, Vec3.atCenterOf(pos));
            } else {
                level.explode(null,
                        level.damageSources().generic(),
                        null,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        breakBlocks ? this.building.explodeRadius : 2.0f,
                        this.getBlocksPlacedPercent() < this.building.fireThreshold,
                        // fire
                        breakBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE
                );
            }
        }
        placedBlockPosSet.remove(pos);
    }

    private void randomSilverfishSpawn(BlockPos pos) {
        Random rand = new Random();
        if (rand.nextFloat(1.0f) < ResearchSilverfish.SILVERFISH_SPAWN_CHANCE) {
            Entity entity = EntityRegistrar.SILVERFISH_UNIT.get().create(level);
            if (entity instanceof SilverfishUnit silverfishUnit) {
                ((Unit) entity).setOwnerName(ownerName);
                level.addFreshEntity(entity);

                BlockPos movePos = pos;
                // move down so they're not stuck above ground
                if (pos.getY() > originPos.getY() + 4) {
                    var blockPoses = getBlockPoses();
                    movePos = blockPoses.get(rand.nextInt(blockPoses.size()));
                }
                if (!this.level.getBlockState(movePos).isAir()) {
                    if (this.level.getBlockState(movePos.north()).isAir()) {
                        movePos = movePos.north();
                    } else if (this.level.getBlockState(movePos.south()).isAir()) {
                        movePos = movePos.south();
                    } else if (this.level.getBlockState(movePos.east()).isAir()) {
                        movePos = movePos.east();
                    } else if (this.level.getBlockState(movePos.west()).isAir()) {
                        movePos = movePos.west();
                    }
                }
                entity.moveTo(movePos.getX() + 0.5f, movePos.getY() + 0.5f, movePos.getZ() + 0.5f);
                silverfishUnit.setLimitedLife();
            }
        }
    }

    private @NotNull ArrayList<BlockPos> getBlockPoses() {
        var blockPoses = new ArrayList<BlockPos>();
        for (BuildingBlock buildingBlock : this.blocks) {
            var blockPos = buildingBlock.getBlockPos();
            if (
                blockPos.getY() == originPos.getY() + 1
                && (
                    blockPos.getX() == originPos.getX()
                    || blockPos.getX() == maxCorner.getX()
                    || blockPos.getZ() == originPos.getZ()
                    || blockPos.getZ() == maxCorner.getZ())) {
                blockPoses.add(blockPos);
            }
        }
        return blockPoses;
    }

    public boolean isAbilityOffCooldown(UnitAction action) {
        for (Ability ability : abilities)
            if (ability.action == action && ability.getCooldown(this) <= 0) {
                return true;
            }
        return false;
    }

    public void forceChunk(boolean add) {
        if (!level.isClientSide()) {
            BlockPos centreBp = this.centrePos;
            ChunkAccess chunk = level.getChunk(centreBp);
            ForgeChunkManager.forceChunk((ServerLevel) level,
                ReignOfNether.MOD_ID,
                centreBp,
                chunk.getPos().x,
                chunk.getPos().z,
                add,
                true
            );
        }
    }

    public void onBuilt() {
        isBuilt = true;
        if (!this.level.isClientSide()) {
            FrozenChunkClientboundPacket.setBuildingBuiltServerside(this.originPos);
            if (isCapitol && BuildingUtils.getTotalCompletedBuildingsOwned(false, ownerName) <= 1) {
                for (int i = 0; i < 3; i++)
                    spawnHuntableAnimalsNearby(ANIMAL_SPAWN_BLOCK_RANGE / 2);
            }
            RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(ownerName);
            if (rtsPlayer == null) return;
            rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.TOTAL_BUILDINGS_CONSTRUCTED);

            TutorialClientEvents.updateStage();
            if (this.isCapitol && !SandboxClientEvents.isSandboxPlayer() &&
                getTotalCompletedBuildingsOwned(this.level.isClientSide(), ownerName) == 1)
                SoundClientEvents.playFactionCalmTheme(this.getFaction(), ownerName);
        }

        // prevent showing blocks on minimap unless previously explored
        if (this.level.isClientSide() && !isExploredClientside && !(getBuilding() instanceof CustomBuilding))
            for (BuildingBlock bb : blocks)
                if (!this.level.getBlockState(bb.getBlockPos()).isAir())
                    this.level.setBlockAndUpdate(bb.getBlockPos(), Blocks.AIR.defaultBlockState());

        if (!level.isClientSide() && ownerName.equals(ENEMY_OWNER_NAME)) {
            if (this instanceof GarrisonableBuilding garr && garr.getCapacity() > 0) {
                int numUnits = 7;
                if (getBuilding() instanceof DarkWatchtower || getBuilding() instanceof Watchtower)
                    numUnits = 3;
                else if (getBuilding() instanceof Bastion)
                    numUnits = 4;

                for (int i = 0; i < numUnits; i++) {
                    EntityType<? extends Mob> entityType = null;
                    if (getFaction() == Faction.VILLAGERS)
                        entityType = EntityRegistrar.PILLAGER_UNIT.get();
                    else if (getFaction() == Faction.MONSTERS)
                        entityType = EntityRegistrar.SKELETON_UNIT.get();
                    else if (getFaction() == Faction.PIGLINS)
                        entityType = EntityRegistrar.HEADHUNTER_UNIT.get();

                    if (entityType != null && garr.getEntryPosition() != null) {
                        UnitServerEvents.spawnMob(
                            entityType,
                            (ServerLevel) level,
                            garr.getEntryPosition(),
                            ENEMY_OWNER_NAME
                        );
                    }
                }
            }
        }
    }

    public void onBlockBuilt(BlockPos bp, BlockState bs) {
    }

    public void tick(Level tickLevel) {
        for (Map.Entry<Ability, Float> cooldownEntry : cooldowns.entrySet()) {
            Ability ability = cooldownEntry.getKey();
            float cooldown = cooldownEntry.getValue();
            if (cooldown > 0 || getCharges(ability) < ability.maxCharges) {
                if (level.isClientSide())
                    cooldowns.put(ability, (float) (cooldown - (TPSClientEvents.getCappedTPS() / 20D)));
                else
                    cooldowns.put(ability, cooldown - 1);

                if (cooldown <= 0 && ability.usesCharges() && getCharges(ability) < ability.maxCharges) {
                    setCharges(ability, getCharges(ability) + 1);
                    if (getCharges(ability) < ability.maxCharges)
                        cooldowns.put(ability, ability.cooldownMax);
                    if (getCharges(ability) > ability.maxCharges)
                        setCharges(ability, ability.maxCharges);
                }
            }
        }
        float blocksPlaced = getBlocksPlaced();
        float blocksTotal = getBlocksTotal();

        if (blocksPlaced >= blocksTotal && !isBuilt) {
            this.onBuilt();
        }

        if (tickLevel.isClientSide()) {
            handleClientTick();
        } else {
            handleServerTick((ServerLevel) tickLevel, blocksPlaced, blocksTotal);
        }

        if (this.level.isClientSide && (
            !FogOfWarClientEvents.isEnabled() || FogOfWarClientEvents.isInBrightChunk(originPos)
        )) {
            isExploredClientside = true;
        }

        // check and do animal spawns around capitols for consistent hunting sources
        if (isCapitol && isBuilt) {
            ticksToSpawnAnimals += 1;
            if (ticksToSpawnAnimals >= TICKS_TO_SPAWN_ANIMALS_MAX) {
                ticksToSpawnAnimals = 0;
                spawnHuntableAnimalsNearby(ANIMAL_SPAWN_BLOCK_RANGE);
            }
        }
        if (isBuilt) {
            tickAgeAfterBuilt += 1;
        }
        tickAge += 1;
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClientTick() {
        if (!blockPlaceQueue.isEmpty()) {
            blockPlaceQueue.remove(0);
        }
    }

    private void handleServerTick(ServerLevel serverLevel, float blocksPlaced, float blocksTotal) {
        ArrayList<WorkerUnit> workerUnits = getBuilders(serverLevel);
        int builderCount = workerUnits.size();

        for (WorkerUnit workerUnit : workerUnits) {
            if (workerUnit instanceof VillagerUnit vUnit && vUnit.getUnitProfession() == VillagerUnitProfession.MASON) {
                if (vUnit.isVeteran())
                    builderCount += 2;
                else
                    builderCount += 1;
            }
            if (((Mob) workerUnit).getActiveEffectsMap().containsKey(MobEffects.DIG_SPEED) ||
                ((Mob) workerUnit).getActiveEffectsMap().containsKey(MobEffectRegistrar.TEMPORARY_EFFICIENCY.get()))
                builderCount += 1;
        }

        boolean hasFastBuildCheat = ResearchServerEvents.playerHasCheat(this.ownerName, "warpten");

        // place a block if the tick has run down
        if (blocksPlaced < blocksTotal) {

            if (builderCount > 0) {
                this.ticksToExtinguish += 1;
                if (ticksToExtinguish >= TICKS_TO_EXTINGUISH) {
                    if (!(getBuilding() instanceof FlameSanctuary) && !(getBuilding() instanceof Fortress)) {
                        extinguishFires(serverLevel);
                    }
                    ticksToExtinguish = 0;
                }
                // AoE 2 speed:
                // 1 builder  - 3/3 (100%)
                // 2 builders - 3/4 (75%)
                // 3 builders - 3/5 (60%)
                // 4 builders - 3/6 (50%)
                // 5 builders - 3/7 (43%)
                float msPerBuild = (float) (3 * BASE_MS_PER_BUILD) / (builderCount + 2);
                if (!isBuilt) {
                    msPerBuild *= building.buildTimeModifier;
                    if (isCapitol && BuildingUtils.getTotalCompletedBuildingsOwned(false, ownerName) > 0)
                        msPerBuild *= 2;
                } else {
                    msPerBuild *= building.repairTimeModifier;
                }

                if (getBuilding() instanceof PortalBasic && !BuildingServerEvents.isOnNetherBlocks(blocks, originPos, serverLevel)
                    && !ResearchServerEvents.playerHasResearch(ownerName, ProductionItems.RESEARCH_ADVANCED_PORTALS)) {
                    msPerBuild *= PortalPlacement.NON_NETHER_BUILD_TIME_MODIFIER;
                }

                if (msToNextBuild > msPerBuild) {
                    msToNextBuild = msPerBuild;
                }

                if (hasFastBuildCheat) {
                    msToNextBuild -= 500;
                } else {
                    msToNextBuild -= 50;
                }

                if (msToNextBuild <= 0) {
                    msToNextBuild = msPerBuild;
                    Collections.shuffle(workerUnits);
                    if (!workerUnits.isEmpty()) {
                        WorkerUnit wUnit = workerUnits.get(0);
                        String ownerName = ((Unit) wUnit).getOwnerName();
                        var count = 0;
                        for (BuildingPlacement placement : BuildingServerEvents.getBuildings()) {
                            if (!placement.ownerName.equals(ownerName)) continue;
                            count++;
                            if (count <= 1) continue;
                            if (wUnit instanceof VillagerUnit vUnit) {
                                vUnit.incrementMasonExp();
                                break;
                            }
                        }
                        buildNextBlock(serverLevel, ownerName);
                    }
                }
            } else if ((selfBuilding || hasFastBuildCheat) && !isBuilt) {
                buildNextBlock(serverLevel, ownerName);
            }
        } else {
            this.ticksToExtinguish = 0;
        }

        // blocks that will build themselves on each tick (eg. foundations from placement, upgrade sections)
        if (!blockPlaceQueue.isEmpty()) {
            BuildingBlock nextBlock = blockPlaceQueue.get(0);
            BlockPos bp = nextBlock.getBlockPos();
            BlockState bs = nextBlock.getBlockState();
            CompoundTag bNbt = nextBlock.getBlockNbt();
            if (level.isLoaded(bp)) {
                level.setBlockAndUpdate(bp, bs);
                if (bNbt != null) {
                    BlockEntity be = BlockEntity.loadStatic(bp, bs, bNbt);
                    if (be != null)
                        level.setBlockEntity(be);
                }
                // avoid creating a bubble column block
                if (bs.getFluidState().is(FluidTags.WATER)) {
                    if (level.getBlockState(bp.below()).getBlock() == Blocks.SOUL_SAND) {
                        level.setBlockAndUpdate(bp.below(), Blocks.SOUL_SOIL.defaultBlockState());
                    } else if (level.getBlockState(bp.below()).getBlock() == Blocks.MAGMA_BLOCK ||
                               level.getBlockState(bp.below()).getBlock() == BlockRegistrar.WALKABLE_MAGMA_BLOCK.get()) {
                        level.setBlockAndUpdate(bp.below(), Blocks.COBBLESTONE.defaultBlockState());
                    }
                }
                if (blockMap.containsKey(bp)) placedBlockPosSet.add(bp);
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, bp, Block.getId(bs));
                level.levelEvent(bs.getSoundType().getPlaceSound().hashCode(), bp, Block.getId(bs));
                blockPlaceQueue.removeIf(i -> i.equals(nextBlock));
                onBlockBuilt(bp, bs);
                if (this.getBlocksPlaced() > highestBlockCountReached) {
                    highestBlockCountReached = this.getBlocksPlaced();
                }
            }
        }
        if (isBuilt && tickAgeAfterBuilt % 10 == 0 && getBuilding().capturable) {
            checkIfCaptured(serverLevel);
        }
    }

    // if there aren't already too many animals nearby, spawn some random huntable animals
    private void spawnHuntableAnimalsNearby(int range) {
        if (level.isClientSide()) {
            return;
        }
        int retries = 0;
        final int MAX_RETRIES = 2;

        int numNearbyAnimals = 0;
        for (Animal animal : MiscUtil.getEntitiesWithinRange(new Vector3d(centrePos.getX(),
            centrePos.getY(),
            centrePos.getZ()
        ), range, Animal.class, level)) {
            if (ResourceSources.isHuntableAnimal(animal)) numNearbyAnimals++;
        }
        int numNearbyChickens = MiscUtil.getEntitiesWithinRange(new Vector3d(centrePos.getX(),
                centrePos.getY(),
                centrePos.getZ()
            ), range, Chicken.class, level)
            .size();

        if (numNearbyAnimals - (numNearbyChickens / 2) >= MAX_ANIMALS) {
            return;
        }

        int spawnAttempts = 0;
        BlockState spawnBs;
        BlockPos spawnBp;
        Random random = new Random();

        do {
            int x = centrePos.getX() + random.nextInt(-range / 2, range / 2);
            int z = centrePos.getZ() + random.nextInt(-range / 2, range / 2);
            int y = level.getChunkAt(new BlockPos(x, 0, z)).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockState bs;
            do {
                bs = level.getBlockState(new BlockPos(x, y, z));
                if (!bs.isSolid() && bs.getFluidState().isEmpty() && y > 0) {
                    y -= 1;
                } else {
                    break;
                }
            } while (true);
            spawnBp = new BlockPos(x, y, z);
            spawnBs = level.getBlockState(spawnBp);
            spawnAttempts += 1;
            if (spawnAttempts > 20) {
                if (retries < MAX_RETRIES) {
                    spawnAttempts = 0;
                    retries += 1;
                    range -= (int) (range * 0.35f);
                } else {
                    ReignOfNether.LOGGER.warn("Gave up trying to find a suitable animal spawn!");
                    return;
                }
            }
        } while (!spawnBs.isSolid()
                 || spawnBs.is(BlockTags.LEAVES)
                 || spawnBs.getBlock() == Blocks.BARRIER
                 || spawnBs.is(BlockTags.LOGS) || spawnBs.is(BlockTags.PLANKS)
                 || spawnBp.distSqr(centrePos) < ANIMAL_SPAWN_RANGE_MIN * ANIMAL_SPAWN_RANGE_MIN
                 || spawnBp.distSqr(centrePos) > range * range
                 || Math.abs(spawnBp.getY() - minCorner.getY()) >= 4
                 || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp)
                 || BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), spawnBp.above())
                 || !level.getWorldBorder().isWithinBounds(spawnBp)
                 || spawnBs.is(BlockTags.FENCES)
                 || BlockUtils.isBottomSlab(spawnBs));

        EntityType<? extends Animal> animalType = null;

        int spawnQty = 1;
        if (getBuilding() instanceof CentralPortal && lastAnimalType != EntityType.MOOSHROOM) {
            animalType = EntityType.MOOSHROOM;
        } else {
            switch (random.nextInt(4)) {
                case 0 -> animalType = EntityType.COW;
                case 1 -> animalType = EntityType.PIG;
                case 2 -> animalType = EntityType.SHEEP;
                case 3 -> {
                    animalType = EntityType.CHICKEN;
                    spawnQty = 2;
                }
            }
        }
        UnitServerEvents.spawnMobs(animalType, (ServerLevel) level, spawnBp.above(), spawnQty, "");

        lastAnimalType = animalType;
    }

    // returns each blockpos origin of 16x16x16 renderchunks that this building overlaps
    // extendedRange includes additional chunks to account for nether conversion and/or resource gathering
    public List<BlockPos> getRenderChunkOrigins(boolean extendedRange) {
        double addedRange = 0;

        if (extendedRange) {
            if (this instanceof NetherConvertingBuilding ncb && ncb.getMaxNetherRange() > 0) {
                double range = ncb.getMaxNetherRange();
                addedRange = (16 * Math.ceil(Math.abs(range / 16))) + 16; // round up to next multiple of 16
            } else if (getBuilding() instanceof AbstractStockpile) {
                addedRange = 32;
            }
        }
        List<BlockPos> origins = new ArrayList<>();
        BlockPos minCorner = getMinCorner(getBlocks()).offset((int) (-addedRange / 2), -1, (int) (-addedRange / 2));
        BlockPos maxCorner = getMaxCorner(getBlocks()).offset((int) addedRange / 2, -1, (int) addedRange / 2);

        BlockPos minOrigin = new BlockPos((int) Math.round(Math.floor(minCorner.getX() / 16d) * 16),
            (int) Math.round(Math.floor(minCorner.getY() / 16d) * 16),
            (int) Math.round(Math.floor(minCorner.getZ() / 16d) * 16)
        );
        BlockPos maxOrigin = new BlockPos((int) Math.round(Math.floor(maxCorner.getX() / 16d) * 16),
            (int) Math.round(Math.floor(maxCorner.getY() / 16d) * 16),
            (int) Math.round(Math.floor(maxCorner.getZ() / 16d) * 16)
        );
        for (int x = minOrigin.getX(); x <= maxOrigin.getX(); x += 16)
            for (int y = minOrigin.getY() - 16; y <= maxOrigin.getY(); y += 16)
                for (int z = minOrigin.getZ(); z <= maxOrigin.getZ(); z += 16)
                    origins.add(new BlockPos(x, y, z));
        return origins;
    }

    public void freezeChunks(String localPlayerName, boolean forceFakeBlocks) {
        if (!level.isClientSide) {
            return;
        }
        if (ownerName.equals(localPlayerName)) {
            return;
        }

        for (BlockPos bp : getRenderChunkOrigins(true)) {
            BlockPos roundedOrigin = bp.offset(-bp.getX() % 16, -bp.getY() % 16, -bp.getZ() % 16);

            //ReignOfNether.LOGGER.info("Froze chunk at: " + roundedOrigin);

            FrozenChunk newFrozenChunk = null;
            for (FrozenChunk frozenChunk : FogOfWarClientEvents.frozenChunks) {
                if (roundedOrigin.equals(frozenChunk.origin)) {
                    newFrozenChunk = new FrozenChunk(roundedOrigin, this, frozenChunk);
                    break;
                }
            }
            if (newFrozenChunk == null) {
                newFrozenChunk = new FrozenChunk(roundedOrigin, this, forceFakeBlocks);
            }

            FogOfWarClientEvents.frozenChunks.add(newFrozenChunk);
        }
    }

    public void unFreezeChunks() {
        if (level.isClientSide) {
            for (BlockPos bp : getRenderChunkOrigins(true))
                for (FrozenChunk fc : FogOfWarClientEvents.frozenChunks)
                    if (fc.building != null && fc.building.originPos.equals(originPos)) {
                        fc.removeOnExplore = true;
                    }
        }
    }

    public int getUpgradeLevel() {
        return getBuilding().getUpgradeLevel(this);
    }

    public Building getBuilding() {
        return building;
    }

    public void changeStructure(String newStructureName) {
        ArrayList<BuildingBlock> newBlocks = BuildingBlockData.getBuildingBlocksFromNbt(newStructureName, this.getLevel());
        setBlocks(getAbsoluteBlockData(newBlocks, this.getLevel(), originPos, rotation));
        refreshBlocks();
    }

    private void checkIfCaptured(ServerLevel serverLevel) {
        if (PlayerServerEvents.rtsPlayers.isEmpty())
            return;

        List<Mob> nearbyUnits = MiscUtil.getEntitiesWithinRange(
                new Vector3d(centrePos.getX(), minCorner.getY(), centrePos.getZ()),
                getBuilding().captureRange, Mob.class, serverLevel);

        Map<String, Integer> playerPopCounts = new HashMap<>();
        boolean ownerHasUnit = false;
        for (Mob mob : nearbyUnits) {
            if (mob instanceof Unit unit && !(mob instanceof WorkerUnit)) {
                String uOwner = unit.getOwnerName();
                if (uOwner.equals(ownerName) && !ownerName.isEmpty()) {
                    ownerHasUnit = true;
                }
                if (!uOwner.isEmpty()) {
                    if (!playerPopCounts.containsKey(uOwner))
                        playerPopCounts.put(uOwner, 0);
                    playerPopCounts.put(uOwner, Math.max(1, unit.getCost().population) + playerPopCounts.get(uOwner));
                }
            }
        }
        String highestPopPlayer = null;
        int highestPop = 0;
        if (!ownerHasUnit) {
            for (String playerName : playerPopCounts.keySet()) {
                if (playerPopCounts.get(playerName) > highestPop) {
                    highestPop = playerPopCounts.get(playerName);
                    highestPopPlayer = playerName;
                }
            }
            if (highestPop > 0 && highestPopPlayer != null) {
                ownerName = highestPopPlayer;

                if (this instanceof BeaconPlacement beacon)
                    beacon.sendWarning("capture_warning");
            }
        }
    }

    public String getUpgradedName() {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(getBuilding());
        if (key == null) {
            return "Unknown";
        }
        return I18n.get("buildings." + (getFaction() != null && getFaction() != Faction.NONE ? getFaction().toString().toLowerCase() : "neutral") + "." + key.getNamespace() + "." + key.getPath());
    }

    public float getMagicDamageMult() {
        return getBuilding().getMeleeDamageMult();
    }

    public void updateButtons() {
        abilities = building.getAbilities().get();
        abilityButtons = building.getAbilities().getButtons(this);
    }

    public void setCooldown(Ability abilityClass, float cooldown) {
        cooldowns.put(abilityClass, cooldown);
    }

    public float getCooldown(Ability abilityClass) {
        return cooldowns.get(abilityClass);
    }

    // creates an invisible armour stand that be attacked by non-units to damage the building
    public void createArmourStandTarget() {
        if (targetStand != null && !targetStand.isDeadOrDying() && !targetStand.isRemoved() && isPosInsideBuilding(targetStand.blockPosition()))
            return;

        // find any existing stands
        List<ArmorStand> entities = level.getEntitiesOfClass(ArmorStand.class,
            new AABB(minCorner.getX(), minCorner.getY(), minCorner.getZ(),
                maxCorner.getX(), maxCorner.getY(), maxCorner.getZ())
        );
        if (!entities.isEmpty()) {
            this.targetStand = entities.get(0);
        } else if (!level.isClientSide()) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand != null) {
                //stand.setInvisible(true);
                stand.setNoGravity(true);
                stand.noPhysics = true;
                stand.moveTo(this.centrePos.getCenter());
                level.addFreshEntity(stand);
                this.targetStand = stand;
            }
        }
    }

    public void setBuilding(Building building) {
        Objects.requireNonNull(building, "Building can't be null");
        this.building = building;
        for (Ability ability : getBuilding().getAbilities().get()) {
            getAbilities().add(ability);
        }
    }

    public void setCharges(Ability ability, int cooldown) {
        charges.put(ability, cooldown);
    }

    public int getCharges(Ability ability) {
        if (!charges.containsKey(ability))
            charges.put(ability, ability.maxCharges);
        return charges.get(ability);
    }

    public boolean hasAutocast(Ability ability) {
        return autocast == ability;
    }

    public void setAutocast(Ability autocast) {
        this.autocast = autocast;
    }
}
