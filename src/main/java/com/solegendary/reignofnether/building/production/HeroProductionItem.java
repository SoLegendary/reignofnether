package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class HeroProductionItem extends ProductionItem {

    public final ResourceLocation iconRl;
    public final String itemName;

    public HeroProductionItem(ResourceCost cost, String itemName, ResourceLocation iconRl) {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            boolean notClientSide = !level.isClientSide();
            boolean noHeroOwned = !heroOwned(level.isClientSide(), placement.ownerName);
            if (notClientSide && noHeroOwned)
                placement.produceUnit((ServerLevel) level, getHeroEntityType(), placement.ownerName, true);
        };
        this.itemName = itemName;
        this.iconRl = iconRl;
    }

    public String getItemName() {
        return itemName;
    }

    protected boolean heroOwned(boolean isClientside, String ownerName) {
        String heroName = getHeroEntityType().getDescriptionId();
        return !HeroUnit.getHeroes(isClientside, ownerName, heroName).isEmpty() ||
                HeroUnit.getFallenHero(isClientside, ownerName, heroName) != null;
    }

    // can't make this a member as we can't refer to other registered objects at init time
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return null;
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                getItemName(),
                iconRl,
                prodBuilding,
                this,
                first
        );
    }
}
