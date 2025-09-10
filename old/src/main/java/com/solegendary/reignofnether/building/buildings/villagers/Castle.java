package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.CastlePlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Castle extends ProductionBuilding {

    public final static String buildingName = "Castle";
    public final static String structureName = "castle";
    public final static String upgradedStructureName = "castle_with_flag";
    public final static ResourceCost cost = ResourceCosts.CASTLE;
    public Castle() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.COBBLESTONE;
        this.icon = new ResourceLocation("minecraft", "textures/block/cobblestone.png");

        this.buildTimeModifier = 0.5f;

        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_WALL);
        this.startingBlockTypes.add(Blocks.SPRUCE_SLAB);
        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.DARK_OAK_PLANKS);

        this.productions.add(ProductionItems.RAVAGER, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_RAVAGER_CAVALRY, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_CASTLE_FLAG, Keybindings.keyE);
    }

    public Faction getFaction() {
        return Faction.VILLAGERS;
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new CastlePlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public AbilityButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new AbilityButton(name,
            new ResourceLocation("minecraft", "textures/block/cobblestone.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.CASTLE,
            TutorialClientEvents::isEnabled,
            () -> (
                BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS)
                    && BuildingClientEvents.hasFinishedBuilding(Buildings.BLACKSMITH)
                    && BuildingClientEvents.hasFinishedBuilding(Buildings.ARCANE_TOWER)
            ) || ResearchClient.hasCheat("modifythephasevariance"),
            () -> BuildingClientEvents.setBuildingToPlace(Buildings.CASTLE),
            null,
            List.of(FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.castle"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.castle.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(I18n.get(
                    "buildings.villagers.reignofnether.castle.tooltip2",
                        CastlePlacement.MAX_OCCUPANTS
                ), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.castle.tooltip3"),
                    Style.EMPTY
                )
            ),
            null
        );
    }

    // check that the flag is built based on existing placed blocks
    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.WHITE_WOOL
                || block.getBlockState().getBlock() == Blocks.RED_WOOL
                || block.getBlockState().getBlock() == Blocks.LIGHT_GRAY_WOOL) {
                return 1;
            }
        return 0;
    }
}
