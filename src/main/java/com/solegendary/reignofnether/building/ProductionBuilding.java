package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.research.researchItems.ResearchVindicatorAxes;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnitProd;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnitProd;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnitProd;
import com.solegendary.reignofnether.unit.units.villagers.IronGolemProdItem;
import com.solegendary.reignofnether.unit.units.villagers.PillagerProdItem;
import com.solegendary.reignofnether.unit.units.villagers.VillagerProdItem;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnitProd;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorProdItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
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

    public ProductionBuilding(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
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

    // start with the centre pos then go down and look at adjacent blocks until we reach a non-solid block
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        BlockPos spawnPoint = BuildingUtils.getCentrePos(blocks);

        while (level.getBlockState(spawnPoint.below()).isAir())
            spawnPoint = spawnPoint.offset(0,-1,0);

        return spawnPoint;
    }

    public void produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName, boolean spawnIndoors) {
        Entity entity = entityType.create(level);
        if (entity != null) {
            level.addFreshEntity(entity);
            ((Unit) entity).setOwnerName(ownerName);

            if (entity instanceof AttackerUnit)
                ((AttackerUnit) entity).setupEquipmentAndUpgrades();

            BlockPos defaultRallyPoint = getMinCorner(this.blocks).offset(
                    0.5f - spawnRadiusOffset,
                    0.5f,
                    0.5f - spawnRadiusOffset);

            BlockPos rallyPoint = this.rallyPoint == null ? defaultRallyPoint : this.rallyPoint;

            if (spawnIndoors) {
                BlockPos spawnPoint = getIndoorSpawnPoint(level);
                entity.moveTo(new Vec3(
                        spawnPoint.getX() + 0.5f,
                        spawnPoint.getY() + 0.5f,
                        spawnPoint.getZ() + 0.5f
                ));
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                    UnitServerEvents.addActionItem(
                            this.ownerName,
                            UnitAction.MOVE,
                            -1,
                            new int[] { entity.getId() },
                            rallyPoint
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
            ProductionItem prodItem = null;
            switch(itemName) {
                case CreeperUnitProd.itemName -> prodItem = new CreeperUnitProd(building);
                case SkeletonUnitProd.itemName -> prodItem = new SkeletonUnitProd(building);
                case ZombieUnitProd.itemName -> prodItem = new ZombieUnitProd(building);
                case VillagerProdItem.itemName -> prodItem = new VillagerProdItem(building);
                case ZombieVillagerUnitProd.itemName -> prodItem = new ZombieVillagerUnitProd(building);
                case VindicatorProdItem.itemName -> prodItem = new VindicatorProdItem(building);
                case PillagerProdItem.itemName -> prodItem = new PillagerProdItem(building);
                case IronGolemProdItem.itemName -> prodItem = new IronGolemProdItem(building);

                case ResearchVindicatorAxes.itemName -> prodItem = new ResearchVindicatorAxes(building);
            }
            if (prodItem != null) {
                // only worry about checking affordability on serverside
                if (building.getLevel().isClientSide()) {
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
                                ResourcesServerEvents.canAfford(building.ownerName, ResourceName.FOOD, prodItem.foodCost),
                                ResourcesServerEvents.canAfford(building.ownerName, ResourceName.WOOD, prodItem.woodCost),
                                ResourcesServerEvents.canAfford(building.ownerName, ResourceName.ORE, prodItem.oreCost)
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
                    if (!building.getLevel().isClientSide()) {
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
                            if (!building.getLevel().isClientSide()) {
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

    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (productionQueue.size() >= 1) {
            ProductionItem nextItem = productionQueue.get(0);
            if (nextItem.tick(tickLevel))
                productionQueue.remove(0);
        }
    }
}
