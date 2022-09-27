package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.unit.Unit;
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
    private Vec3 relativeSpawnPoint = new Vec3(0,0,0);

    private boolean isProducing() {
        return this.productionQueue.size() > 0;
    }

    public ProductionBuilding() {
        super();
    }

    public void produceUnit(ServerLevel level, EntityType<? extends Unit> entityType) {
        Entity unit = entityType.create(level);
        if (unit != null) {
            level.addFreshEntity(unit);
            Vec3i minCorner = getMinCorner(this.blocks);
            unit.moveTo(new Vec3(
                    minCorner.getX() + relativeSpawnPoint.x,
                    minCorner.getX() + relativeSpawnPoint.y,
                    minCorner.getX() + relativeSpawnPoint.z
            ));
        }
    }

    public void tick(Level level) {
        super.tick(level);

        if (!level.isClientSide()) {
            if (productionQueue.size() >= 1) {
                ProductionItem nextItem = productionQueue.get(0);
                if (nextItem.tick((ServerLevel) level))
                    productionQueue.remove(0);
            }
        }
    }
}
