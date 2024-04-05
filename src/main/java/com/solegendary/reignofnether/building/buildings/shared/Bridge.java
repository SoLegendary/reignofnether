package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ReignOfNether;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.WaterFluid;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Bridge extends Building {

    public final static String buildingName = "Bridge";
    public final static String structureName = "bridge_orthogonal";
    public final static ResourceCost cost = ResourceCosts.BRIDGE;

    public Bridge(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getCulledBlocks(getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), level), false);

        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.OAK_FENCE;
        this.icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/oak_fence.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.SPRUCE_FENCE);
    }

    public static boolean shouldCullBlock(BlockPos originPos, BuildingBlock b, Level level) {
        BlockState bs = b.getBlockState();
        boolean isFenceWallOrAir = b.getBlockState().getBlock() instanceof AirBlock ||
                b.getBlockState().getBlock() instanceof FenceBlock ||
                b.getBlockState().getBlock() instanceof WallBlock;
        Material bm = level.getBlockState(b.getBlockPos().offset(originPos)).getMaterial();
        Material bmBelow = level.getBlockState(b.getBlockPos().offset(originPos).below()).getMaterial();
        return bm.isSolidBlocking() || (isFenceWallOrAir && bmBelow.isSolidBlocking());
    }

    private static ArrayList<BuildingBlock> getCulledBlocks(ArrayList<BuildingBlock> blocks, Level level) {
        blocks.removeIf(b -> shouldCullBlock(new BlockPos(0,0,0), b, level));
        return blocks;
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        super.onBlockBreak(level, pos, breakBlocks);
        BlockState bsBelow = level.getBlockState(pos.below());
        if (bsBelow.getMaterial().isLiquid())
            level.setBlockAndUpdate(pos, bsBelow);
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        Minecraft MC = Minecraft.getInstance();
        return new AbilityButton(
                Bridge.buildingName,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/oak_fence.png"),
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
                        FormattedCharSequence.forward("A bridge used to traverse water or lava. Must be built", Style.EMPTY),
                        FormattedCharSequence.forward("over water or lava connecting to land or another bridge.", Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Bridges can be repaired or attacked by anyone.", Style.EMPTY)
                ),
                null
        );
    }
}
