package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.researchItems.ResearchLabLightningRod;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnitProd;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Laboratory extends ProductionBuilding {

    public final static String buildingName = "Laboratory";
    public final static String structureName = "laboratory";

    public Laboratory(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.BREWING_STAND;
        this.spawnRadiusOffset = 1;
        this.icon = new ResourceLocation("minecraft", "textures/block/brewing_stand.png");

        this.foodCost = ResourceCosts.Laboratory.FOOD;
        this.woodCost = ResourceCosts.Laboratory.WOOD;
        this.oreCost = ResourceCosts.Laboratory.ORE;
        this.popSupply = ResourceCosts.Laboratory.SUPPLY;

        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.BLACKSTONE);

        Ability callLightning = new Ability(
            UnitAction.CALL_LIGHTNING,
            ResourceCosts.TICKS_PER_SECOND * 60, 20, 0
        );
        this.abilities.add(callLightning);

        if (level.isClientSide()) {
            this.productionButtons = Arrays.asList(
                CreeperUnitProd.getStartButton(this, Keybindings.keyQ),
                ResearchLabLightningRod.getStartButton(this, Keybindings.keyW)
            );

            this.abilityButtons.add(
                new AbilityButton(
                    "Call Lightning",
                    14,
                    new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/lightbulb_on.png"),
                    Keybindings.keyL,
                    () -> false,
                    () -> !this.isUpgraded(),
                    () -> this.getLightningRodPos() != null,
                    () -> CursorClientEvents.setLeftClickAction(UnitAction.CALL_LIGHTNING),
                    null,
                    List.of(
                        FormattedCharSequence.forward("Call Lightning", Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE004  " + 60 + "s", MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Summon a bolt of lightning at the targeted location.", Style.EMPTY),
                        FormattedCharSequence.forward("Can be used to charge creepers and damage enemies.", Style.EMPTY)
                    ),
                    callLightning
                )
            );
        }
    }

    // return the lightning rod is built based on existing placed blocks
    // returns null if it is not build or is damaged
    // also will return null if outside of render range, but shouldn't matter since it'd be out of ability range anyway
    public BlockPos getLightningRodPos() {
        for (BuildingBlock block : blocks) {
            if (this.getLevel().getBlockState(block.getBlockPos()).getBlock() == Blocks.LIGHTNING_ROD &&
                this.getLevel().getBlockState(block.getBlockPos().below()).getBlock() == Blocks.WAXED_COPPER_BLOCK)
                return block.getBlockPos();
        }
        return null;
    }

    // check that the lightning rod is built based on existing placed blocks
    public boolean isUpgraded() {
        for (BuildingBlock block : blocks)
            if (block.getBlockState().getBlock() == Blocks.LIGHTNING_ROD)
                return true;
        return false;
    }

    public void changeStructure(String newStructureName) {
        ArrayList<BuildingBlock> newBlocks = BuildingBlockData.getBuildingBlocks(newStructureName, this.getLevel());
        this.blocks = getAbsoluteBlockData(newBlocks, this.getLevel(), originPos, rotation);
        super.refreshBlocks();
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Laboratory.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/brewing_stand.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Laboratory.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Laboratory.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Laboratory.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Laboratory.WOOD + "  \uE002  " + ResourceCosts.Laboratory.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A sinister lab that can research new technologies and", Style.EMPTY),
                        FormattedCharSequence.forward("produce creepers. Can be upgraded to have a lightning rod.", Style.EMPTY)
                ),
                null
        );
    }
}
