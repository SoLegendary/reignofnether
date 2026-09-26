package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.NetherZone;
import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

public class CentralPortal extends ProductionBuilding implements NetherConvertingAddon {

    public final static String buildingName = "Central Portal";
    public final static String structureName = "central_portal";
    public final static ResourceCost cost = ResourceCosts.CENTRAL_PORTAL;

    public CentralPortal() {
        super(structureName, cost, true);
        this.name = buildingName;
        this.portraitBlock = Blocks.OBSIDIAN;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/obsidian.png");

        this.buildTimeModifier = 0.32f; // 60s total build time with 3 villagers
        this.canAcceptResources = true;
        this.maxHealth = 380d;

        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);

        this.productions.add(ProductionItems.GRUNT, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.STRIDER, Keybindings.abilitySlot2);

        setActiveAddon(NetherConvertingAddon.class, this, true);
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/obsidian.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.CENTRAL_PORTAL,
                () -> false,
                () -> true,
                List.of(
                        Component.translatable("buildings.reignofnether.central_portal").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPop(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.central_portal.tooltip1").getVisualOrderText()
                ),
                this
        );
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level, BuildingPlacement placement) {
        return super.getIndoorSpawnPoint(level, placement).offset(0,-5,0);
    }

    @Override
    public double getMaxNetherRange(BuildingPlacement placement) {
        return 30;
    }

    @Override
    public double getStartingNetherRange(BuildingPlacement placement) {
        return 6;
    }

    @Override
    public void tick(Level tickLevel, BuildingPlacement buildingPlacement) {
        if (!buildingPlacement.getLevel().isClientSide() && buildingPlacement.getBlocksPlaced() >= buildingPlacement.getBlocksTotal()) {
            BlockPos bp;
            if (buildingPlacement.rotation == Rotation.CLOCKWISE_90 ||
                    buildingPlacement.rotation == Rotation.COUNTERCLOCKWISE_90) {
                bp = buildingPlacement.centrePos.offset(0,-1,0);
            } else {
                bp = buildingPlacement.centrePos.offset(-1,0,0);
            }
            if (buildingPlacement.getLevel().getBlockState(bp).isAir())
                buildingPlacement.getLevel().setBlockAndUpdate(bp, Blocks.FIRE.defaultBlockState());
        }
    }

    @Override
    public void onBuilt(BuildingPlacement buildingPlacement) {
        if (getMaxNetherRange(buildingPlacement) > 0)
            setNetherZone(buildingPlacement, new NetherZone(buildingPlacement.centrePos.offset(0,-6,0), getMaxNetherRange(buildingPlacement), getStartingNetherRange(buildingPlacement)), true);
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp, BuildingPlacement placement) {
        BlockPos worldBp = relativeBp.offset(placement.originPos);
        Block block = placement.getLevel().getBlockState(worldBp).getBlock();
        return block != Blocks.OBSIDIAN && block != Blocks.NETHER_PORTAL;
    }
}