package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.building.BuildingUtils.getMinCorner;

// buildings which can produce units and/or research tech
public abstract class ProductionBuilding extends Building {

    // includes production options
    public List<Button> productionButtons = new ArrayList<>();
    public final List<ProductionItem> productionQueue = new ArrayList<>();

    // spawn point relative to building origin to spawn units
    private BlockPos rallyPoint;
    protected int spawnRadiusOffset = 0;

    public ProductionBuilding() {
        super();
    }

    public BlockPos getRallyPoint() {
        return this.rallyPoint;
    }

    public void setRallyPoint(BlockPos rallyPoint) {
        if (isPosInsideBuilding(rallyPoint))
            this.rallyPoint = null;
        else
            this.rallyPoint = rallyPoint;
    }

    private boolean isProducing() {
        return this.productionQueue.size() > 0;
    }

    public void produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName) {
        Entity entity = entityType.create(level);
        if (entity != null) {
            level.addFreshEntity(entity);
            ((Unit) entity).setOwnerName(ownerName);
            ((Unit) entity).onBuildingSpawn();

            if (rallyPoint != null) {
                // spawn unit at centre of the block
                BlockPos spawnPoint = getClosestGroundPos(rallyPoint, spawnRadiusOffset);
                entity.moveTo(new Vec3(
                        spawnPoint.getX() + 0.5f,
                        spawnPoint.getY() + 0.5f,
                        spawnPoint.getZ() + 0.5f
                ));
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                    UnitServerEvents.addActionItem(
                        UnitAction.MOVE,
                        -1,
                        new int[] { entity.getId() },
                        this.rallyPoint
                    );
                });
            }
            else {
                BlockPos spawnPoint = getMinCorner(this.blocks);
                entity.moveTo(new Vec3(
                        spawnPoint.getX() + 0.5f - spawnRadiusOffset,
                        spawnPoint.getY() + 0.5f,
                        spawnPoint.getZ() + 0.5f - spawnRadiusOffset
                ));
            }
        }
    }

    // return true if successful
    public static boolean startProductionItem(ProductionBuilding building, String itemName, BlockPos pos) {
        boolean success = false;

        if (building != null) {
            //if (building.level.isClientSide())
            //    System.out.println("(client) starting: " + itemName);
            //if (!building.level.isClientSide())
            //    System.out.println("(server) starting: " + itemName);

            ProductionItem prodItem = null;
            switch(itemName) {
                case CreeperUnitProd.itemName -> prodItem = new CreeperUnitProd(building);
                case SkeletonUnitProd.itemName -> prodItem = new SkeletonUnitProd(building);
                case ZombieUnitProd.itemName -> prodItem = new ZombieUnitProd(building);
            }
            if (prodItem != null) {
                // only worry about checking affordability on serverside
                if (building.level.isClientSide()) {
                    building.productionQueue.add(prodItem);
                    success = true;
                }
                else {
                    if (prodItem.canAfford(building.ownerName)) {
                        building.productionQueue.add(prodItem);
                        ResourcesServerEvents.addSubtractResources(new Resources(
                                building.ownerName,
                                -prodItem.foodCost,
                                -prodItem.woodCost,
                                -prodItem.oreCost
                        ));
                        success = true;
                    }
                    else {
                        if (!prodItem.isBelowMaxPopulation(building.ownerName))
                            ResourcesClientboundPacket.warnMaxPopulation(building.ownerName);
                        else if (!prodItem.canAffordPopulation(building.ownerName))
                            ResourcesClientboundPacket.warnInsufficientPopulation(building.ownerName);
                        else
                            ResourcesClientboundPacket.warnInsufficientResources(building.ownerName,
                                    prodItem.canAffordFood(building.ownerName),
                                    prodItem.canAffordWood(building.ownerName),
                                    prodItem.canAffordOre(building.ownerName)
                            );
                    }
                }
            }
        }
        return success;
    }

    public static boolean cancelProductionItem(ProductionBuilding building, String itemName, BlockPos pos, boolean frontItem) {
        boolean success = false;

        if (building != null) {
            if (building.productionQueue.size() > 0) {
                if (frontItem) {
                    ProductionItem prodItem = building.productionQueue.get(0);
                    building.productionQueue.remove(0);
                    if (!building.level.isClientSide()) {
                        ResourcesServerEvents.addSubtractResources(new Resources(
                                building.ownerName,
                                prodItem.foodCost,
                                prodItem.woodCost,
                                prodItem.oreCost
                        ));
                    }
                    success = true;
                }
                else {
                    // find first non-started item to remove
                    for (int i = 0; i < building.productionQueue.size(); i++) {
                        ProductionItem prodItem = building.productionQueue.get(i);
                        if (prodItem.getItemName().equals(itemName) &&
                                prodItem.ticksLeft >= prodItem.ticksToProduce) {
                            building.productionQueue.remove(prodItem);
                            if (!building.level.isClientSide()) {
                                ResourcesServerEvents.addSubtractResources(new Resources(
                                        building.ownerName,
                                        prodItem.foodCost,
                                        prodItem.woodCost,
                                        prodItem.oreCost
                                ));
                            }
                            success = true;
                            break;
                        }
                    }
                }
            }
        }
        return success;
    }

    public void tick(Level level) {
        super.tick(level);

        if (productionQueue.size() >= 1) {
            ProductionItem nextItem = productionQueue.get(0);
            if (nextItem.tick(level))
                productionQueue.remove(0);
        }
    }
}
