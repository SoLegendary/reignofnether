package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Bridge extends Building {

    public final static String buildingName = "Bridge";
    public final static String structureName = "bridge_orthogonal";
    public final static ResourceCost cost = ResourceCosts.BRIDGE;

    public Bridge(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.OAK_FENCE;
        this.icon = new ResourceLocation("minecraft", "textures/block/oak_fence.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.SPRUCE_FENCE);

        // remove any blocks that are clipping solid blocks
        blocks.removeIf(b -> shouldBlockExist(new BlockPos(0,0,0), b, level));
    }

    public static boolean shouldBlockExist(BlockPos originPos, BuildingBlock b, Level level) {
        BlockState bs = b.getBlockState();
        boolean isFenceOrWall = b.getBlockState().getBlock() instanceof FenceBlock ||
                b.getBlockState().getBlock() instanceof WallBlock;
        Material bm = level.getBlockState(b.getBlockPos().offset(originPos)).getMaterial();
        Material bmBelow = level.getBlockState(b.getBlockPos().offset(originPos).below()).getMaterial();
        return bm.isSolidBlocking() || (isFenceOrWall && bmBelow.isSolidBlocking());
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        Minecraft MC = Minecraft.getInstance();
        return new AbilityButton(
                Bridge.buildingName,
                new ResourceLocation("minecraft", "textures/block/oak_fence.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Bridge.class,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(TownCentre.buildingName) ||
                        BuildingClientEvents.hasFinishedBuilding(Mausoleum.buildingName) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                () -> BuildingClientEvents.setBuildingToPlace(Bridge.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Bridge.buildingName, Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A bridge you can build to cross water.", Style.EMPTY)
                ),
                null
        );
    }
}
