package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.building.BuildingUtils.getMinCorner;

public class ProductionPlacement extends BuildingPlacement {
    private ArrayList<BlockPos> rallyPoints = new ArrayList<>();
    private LivingEntity rallyPointEntity;
    public List<Button> productionButtons;
    public final List<ActiveProduction> productionQueue = new ArrayList<>();
    public boolean attackRally = false;
    public ResourceName rallyResourceName = ResourceName.NONE; // used to set worker resource targets

    public ProductionPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        if (building instanceof ProductionBuilding productionBuilding) {
            productionButtons = productionBuilding.productions.getButtons(this);
        }
        if (isCapitol)
            this.allowProdWhileBuilding = true;
    }

    @Nullable
    public BlockPos getFinalRallyPoint() {
        if (!this.rallyPoints.isEmpty())
            return this.rallyPoints.get(this.rallyPoints.size() - 1);
        else
            return null;
    }

    public ArrayList<BlockPos> getRallyPoints() {
        return rallyPoints;
    }

    public LivingEntity getRallyPointEntity() {
        if (this.rallyPointEntity == null)
            return null;
        if (!this.rallyPointEntity.isAlive()) {
            this.rallyPointEntity = null;
            return null;
        }
        return this.rallyPointEntity;
    }

    public void setRallyPoint(BlockPos rallyPoint) {
        this.rallyPoints = new ArrayList<>();
        if (!canSetRallyPoint())
            return;
        if (!isPosInsideBuilding(rallyPoint))
            this.rallyPoints.add(rallyPoint);
        this.rallyPointEntity = null;
        rallyResourceName = ResourceSources.getBlockResourceName(rallyPoint, level);
    }

    public void addRallyPoint(BlockPos rallyPoint) {
        if (!canSetRallyPoint())
            return;
        if (!isPosInsideBuilding(rallyPoint))
            this.rallyPoints.add(rallyPoint);
        this.rallyPointEntity = null;
        rallyResourceName = ResourceSources.getBlockResourceName(rallyPoint, level);
    }

    public boolean canSetRallyPoint() {
        return ((ProductionBuilding) getBuilding()).canSetRallyPoint;
    }

    public void setRallyPointEntity(LivingEntity entity) {
        if (!canSetRallyPoint() || entity == null || !entity.isAlive())
            return;
        this.rallyPointEntity = entity;
        this.rallyPoints.clear();
        attackRally = isRallyEntityAttackable();
    }

    private boolean isRallyEntityAttackable() {
        return rallyPointEntity != null && rallyPointEntity.isAlive() &&
                (ResourceSources.isHuntableAnimal(rallyPointEntity) ||
                (rallyPointEntity instanceof Unit unit1 && !AlliancesServerEvents.isAlliedOrOwned(unit1.getOwnerName(), ownerName)));
    }

    private boolean isProducing() {
        return !this.productionQueue.isEmpty();
    }
    // start with the centre pos then go down and look at adjacent blocks until we reach a non-solid block
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        return building.getIndoorSpawnPoint(level, this);
    }

    // start with the centre pos then go down and look at adjacent blocks until we reach a non-solid block
    public BlockPos getDefaultOutdoorSpawnPoint() {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        return building.getDefaultOutdoorSpawnPoint(getMinCorner(blocks), this);
    }

    public Entity produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName, boolean spawnIndoors) {
        return produceUnit(level, entityType, ownerName, spawnIndoors, new Vec3i(0,0,0));
    }

    public Entity produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName, boolean spawnIndoors, Vec3i spawnOffset) {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        LivingEntity rallyEntity = getRallyPointEntity();
        BlockPos spawnPoint;
        if (spawnIndoors) {
            spawnPoint = getIndoorSpawnPoint(level);
            if (entityType == EntityRegistrar.GHAST_UNIT.get())
                spawnPoint = spawnPoint.offset(0,5,0);
        }
        else if (!rallyPoints.isEmpty())
            spawnPoint = getClosestGroundPos(rallyPoints.get(0), (int) building.spawnRadiusOffset);
        else if (rallyPointEntity != null)
            spawnPoint = getClosestGroundPos(rallyPointEntity.getOnPos(), (int) building.spawnRadiusOffset);
        else
            spawnPoint = getDefaultOutdoorSpawnPoint();

        spawnPoint = spawnPoint.offset(spawnOffset);

        Entity entity = entityType.spawn(level, (CompoundTag) null,
                null,
                spawnPoint,
                MobSpawnType.SPAWNER,
                true,
                false
        );
        BlockPos defaultRallyPoint = getDefaultOutdoorSpawnPoint();

        final List<BlockPos> fRallyPoints = this.rallyPoints.isEmpty() ? List.of(defaultRallyPoint) : this.rallyPoints;

        if (entity instanceof Unit unit) {
            unit.setOwnerName(ownerName);
            unit.setupEquipmentAndUpgradesServer();

            if (rallyEntity != null && rallyEntity.isAlive()) {
                if (isRallyEntityAttackable()) {
                    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                        if (!fRallyPoints.isEmpty()) {
                            UnitServerEvents.addActionItem(
                                    this.ownerName,
                                    UnitAction.ATTACK,
                                    rallyEntity.getId(),
                                    new int[] { entity.getId() },
                                    fRallyPoints.get(0),
                                    new BlockPos(0,0,0)
                            );
                        }
                    });
                } else {
                    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                        if (!fRallyPoints.isEmpty()) {
                            UnitServerEvents.addActionItem(
                                    this.ownerName,
                                    UnitAction.FOLLOW,
                                    rallyEntity.getId(),
                                    new int[] { entity.getId() },
                                    fRallyPoints.get(0),
                                    new BlockPos(0,0,0)
                            );
                        }
                    });
                }
            } else {
                for (int i = 0; i < fRallyPoints.size(); i++) {
                    final int fi = i;
                    CompletableFuture.delayedExecutor(500L * fi, TimeUnit.MILLISECONDS).execute(() -> {
                        if (fRallyPoints.size() > fi)
                            UnitServerEvents.addActionItem(
                                    this.ownerName,
                                    attackRally ? UnitAction.ATTACK_MOVE : UnitAction.MOVE,
                                    -1,
                                    new int[] { entity.getId() },
                                    fRallyPoints.get(fi),
                                    new BlockPos(0,0,0),
                                    fi > 0
                            );
                    });
                    CompletableFuture.delayedExecutor(750, TimeUnit.MILLISECONDS).execute(() -> {
                        if (!attackRally && unit instanceof WorkerUnit workerUnit)
                            if (rallyResourceName != ResourceName.NONE)
                                workerUnit.getGatherResourceGoal().setTargetResourceName(rallyResourceName);
                    });
                }
            }
        }
        return entity;
    }

    // return true if successful
    public boolean startProductionItem(ProductionItem prodItem) {
        boolean success = false;

        if (getBuilding() instanceof ProductionBuilding pb && !pb.productions.get().contains(prodItem)) {
            return false;
        }

        if (prodItem != null) {
            // only worry about checking affordability on serverside
            if (getLevel().isClientSide()) {
                ActiveProduction activeProduction = new ActiveProduction(prodItem, true, ownerName);
                productionQueue.add(activeProduction);
                success = true;
            }
            else {
                boolean allow = switch (prodItem.dupeRule) {
                    case DISALLOW -> !prodItem.itemIsBeingProduced(false, ownerName);
                    case DISALLOW_FOR_BUILDING -> !prodItem.itemIsBeingProducedAt(false, this);
                    case ALLOW -> true;
                };

                if (allow && prodItem.canAfford(this)) {
                    ActiveProduction activeProduction = new ActiveProduction(prodItem, false, ownerName);
                    productionQueue.add(activeProduction);
                    ResourcesServerEvents.addSubtractResources(new Resources(
                            ownerName,
                            -prodItem.getCost(level.isClientSide(), ownerName).food,
                            -prodItem.getCost(level.isClientSide(), ownerName).wood,
                            -prodItem.getCost(level.isClientSide(), ownerName).ore,
                            -prodItem.getCost(level.isClientSide(), ownerName).emerald
                    ));
                    success = true;
                }
                else {
                    if (!prodItem.isBelowMaxPopulation(this))
                        ResourcesClientboundPacket.warnMaxPopulation(ownerName);
                    else if (!prodItem.canAffordPopulation(this)) {
                        if (prodItem instanceof GraveyardUnitProductionItem && this instanceof GraveyardPlacement gy && gy.getUpgradeLevel() > 0) {
                            ResourcesClientboundPacket.warnFullGraveyard(ownerName);
                        } else {
                            ResourcesClientboundPacket.warnInsufficientPopulation(ownerName);
                        }
                    }
                    else
                        ResourcesClientboundPacket.warnInsufficientResources(ownerName,
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.FOOD, prodItem.getCost(level.isClientSide(), ownerName).food),
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.WOOD, prodItem.getCost(level.isClientSide(), ownerName).wood),
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.ORE, prodItem.getCost(level.isClientSide(), ownerName).ore),
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.EMERALD, prodItem.getCost(level.isClientSide(), ownerName).emerald)
                        );
                }
            }
        }
        // DISALLOW_FOR_BUILDING items are likely building upgrades, so switch focus to an unupgraded building without any upgrade in progress
        if (success && level.isClientSide() && prodItem.dupeRule == ProdDupeRule.DISALLOW_FOR_BUILDING) {
            for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings()) {
                if (bpl.getBuilding() == this.getBuilding() && bpl != this && bpl instanceof ProductionPlacement ppl &&
                    !ppl.productionQueue.stream().map(ap -> ap.item.dupeRule).toList().contains(ProdDupeRule.DISALLOW_FOR_BUILDING) &&
                    ppl.getUpgradeLevel() == 0) {
                    HudClientEvents.hudSelectedPlacement = bpl;
                    break;
                }
            }
        }
        return success;
    }

    public boolean cancelProductionItem(ProductionItem item, boolean frontItem) {
        boolean success = false;

        if (!productionQueue.isEmpty()) {
            if (frontItem) {
                ActiveProduction prodItem = productionQueue.get(0);
                productionQueue.remove(0);
                if (!getLevel().isClientSide()) {
                    ResourcesServerEvents.addSubtractResources(new Resources(
                            ownerName,
                            prodItem.item.getCost(level.isClientSide(), ownerName).food,
                            prodItem.item.getCost(level.isClientSide(), ownerName).wood,
                            prodItem.item.getCost(level.isClientSide(), ownerName).ore
                    ));
                }
                success = true;
            }
            else {
                // find first non-started item to remove
                for (int i = 0; i < productionQueue.size(); i++) {
                    ActiveProduction prodItem = productionQueue.get(i);
                    if (prodItem.item.equals(item) &&
                            prodItem.ticksLeft >= prodItem.item.getCost(level.isClientSide(), ownerName).ticks) {
                        productionQueue.remove(prodItem);
                        if (!getLevel().isClientSide()) {
                            ResourcesServerEvents.addSubtractResources(new Resources(
                                    ownerName,
                                    prodItem.item.getCost(level.isClientSide(), ownerName).food,
                                    prodItem.item.getCost(level.isClientSide(), ownerName).wood,
                                    prodItem.item.getCost(level.isClientSide(), ownerName).ore
                            ));
                        }
                        success = true;
                        break;
                    }
                }
            }
        }
        return success;
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        while (!productionQueue.isEmpty()) {
            cancelProductionItem(productionQueue.get(0).item, true);
        }
    }

    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (!productionQueue.isEmpty()) {
            ActiveProduction nextItem = productionQueue.get(0);
            if (nextItem.item.tick(this, nextItem)) {
                if (!tickLevel.isClientSide()) {
                    productionQueue.remove(0);
                    if (productionQueue.isEmpty())
                        BuildingClientboundPacket.clearQueue(this.originPos);
                    else
                        BuildingClientboundPacket.completeProduction(this.originPos);
                }
            }
        }
    }

    @Override
    public void updateButtons() {
        if (level.isClientSide()) {
            super.updateButtons();
            if (getBuilding() instanceof ProductionBuilding productionBuilding) {
                productionButtons = productionBuilding.productions.getButtons(this);
            }
        }
    }
}
