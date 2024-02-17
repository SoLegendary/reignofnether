package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.piglins.GruntProd;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class CentralPortal extends ProductionBuilding implements NetherConvertingBuilding {

    public final static String buildingName = "Central Portal";
    public final static String structureName = "central_portal";
    public final static ResourceCost cost = ResourceCosts.CENTRAL_PORTAL;

    private final double NETHER_CONVERT_RANGE_MAX = 30;
    private double netherConvertRange = 6;
    private int netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
    private int convertsAfterMaxRange = 0;

    public double getMaxRange() { return NETHER_CONVERT_RANGE_MAX; }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (isBuilt) {
            netherConvertTicksLeft -= 1;
            if (netherConvertTicksLeft <= 0 && convertsAfterMaxRange < MAX_CONVERTS_AFTER_MAX_RANGE) {
                netherConvertTick(this, netherConvertRange, NETHER_CONVERT_RANGE_MAX);
                if (netherConvertRange < NETHER_CONVERT_RANGE_MAX)
                    netherConvertRange += 0.1f;
                else
                    convertsAfterMaxRange += 1;
                netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
            }
        }
    }

    public CentralPortal(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), true);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.OBSIDIAN;
        this.icon = new ResourceLocation("minecraft", "textures/block/obsidian.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 0.4f;
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                    GruntProd.getStartButton(this, Keybindings.keyQ)
            );
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp) {
        BlockPos worldBp = relativeBp.offset(this.originPos);
        Block block = this.getLevel().getBlockState(worldBp).getBlock();
        return block != Blocks.OBSIDIAN && block != Blocks.NETHER_PORTAL;
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        if (!this.getLevel().isClientSide()) {
            if (this.rotation == Rotation.CLOCKWISE_90 ||
                this.rotation == Rotation.COUNTERCLOCKWISE_90) {
                this.getLevel().setBlockAndUpdate(this.centrePos.offset(0,-1,0), Blocks.FIRE.defaultBlockState());
            } else {
                this.getLevel().setBlockAndUpdate(this.centrePos.offset(-1,0,0), Blocks.FIRE.defaultBlockState());
            }
        }
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                CentralPortal.buildingName,
                new ResourceLocation("minecraft", "textures/block/obsidian.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == CentralPortal.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(CentralPortal.class),
                null,
                List.of(
                        FormattedCharSequence.forward(CentralPortal.buildingName + " (Capitol)", Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPop(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("The primary portal to transport piglin grunts from the nether.", Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("You may only have one capitol building at any time.", Style.EMPTY)
                ),
                null
        );
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        return super.getIndoorSpawnPoint(level).offset(0,-5,0);
    }
}