package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

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

        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);

        this.productions.add(ProductionItems.GRUNT, Keybindings.keyQ);

        setActiveAddon(NetherConvertingAddon.class, this, true);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/obsidian.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.CENTRAL_PORTAL,
                () -> false,
                () -> true,
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.central_portal"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPop(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.central_portal.tooltip1"), Style.EMPTY)
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