package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.unit.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

// buildings which can produce units and/or research tech
public abstract class ProductionBuilding extends Building {

    // includes production options
    public List<Button> productionButtons = new ArrayList<>();
    public final List<ProductionItem> productionQueue = new ArrayList<>();
    // spawn point relative to building origin to spawn units
    private final BlockPos relativeSpawnPoint = new BlockPos(0,0,0);
    public BlockPos rallyPoint = null;

    private boolean isProducing() {
        return this.productionQueue.size() > 0;
    }

    public ProductionBuilding() {
        super();
    }

    public void produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName) {
        Entity entity = entityType.create(level);
        if (entity != null) {
            System.out.println("creating unit: " + entityType.getDescription().getString());
            level.addFreshEntity(entity);
            BlockPos minCorner = getMinCorner(this.blocks);
            entity.moveTo(new Vec3(
                minCorner.getX() + relativeSpawnPoint.getX(),
                minCorner.getY() + relativeSpawnPoint.getY(),
                minCorner.getZ() + relativeSpawnPoint.getZ()
            ));
            ((Unit) entity).setOwnerName(ownerName);
            // TODO: doesn't seem to work?
            if (rallyPoint != null)
                ((Unit) entity).setMoveTarget(rallyPoint);
        }
    }

    public static void startProductionItem(List<Building> buildings, String itemName, BlockPos pos) {
        ProductionBuilding building = (ProductionBuilding) findBuilding(buildings, pos);

        if (building != null) {
            if (building.level.isClientSide())
                System.out.println("(client) starting: " + itemName);
            if (!building.level.isClientSide())
                System.out.println("(server) starting: " + itemName);

            ProductionItem prodItem = null;
            switch(itemName) {
                case CreeperUnitProd.itemName -> prodItem = new CreeperUnitProd(building);
                case SkeletonUnitProd.itemName -> prodItem = new SkeletonUnitProd(building);
                case ZombieUnitProd.itemName -> prodItem = new ZombieUnitProd(building);
            }
            if (prodItem != null)
                building.productionQueue.add(prodItem);
        }
    }

    public static void cancelProductionItem(List<Building> buildings, String itemName, BlockPos pos, boolean frontItem) {
        ProductionBuilding building = (ProductionBuilding) findBuilding(buildings, pos);

        if (building != null) {
            if (building.level.isClientSide())
                System.out.println("(client) starting: " + itemName);
            if (!building.level.isClientSide())
                System.out.println("(server) starting: " + itemName);

            if (frontItem)
                building.productionQueue.remove(0);
            else {
                // find first non-started item to remove
                for (int i = 0; i < building.productionQueue.size(); i++) {
                    ProductionItem prodItem = building.productionQueue.get(i);
                    if (prodItem.getItemName().equals(itemName) &&
                            prodItem.ticksLeft >= prodItem.ticksToProduce) {
                        building.productionQueue.remove(prodItem);
                        break;
                    }
                }
            }
        }
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
