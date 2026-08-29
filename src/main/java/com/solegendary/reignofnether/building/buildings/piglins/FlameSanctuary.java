package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FlameSanctuary extends ProductionBuilding {

    public final static String buildingName = "Flame Sanctuary";
    public final static String structureName = "flame_sanctuary";
    public final static ResourceCost cost = ResourceCosts.FLAME_SANCTUARY;

    public FlameSanctuary() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.MAGMA_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/magma.png");

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.RED_NETHER_BRICK_STAIRS);

        this.explodeChance = 0.2f;
        this.maxHealth = 150d;

        this.productions.add(ProductionItems.RESEARCH_BLAZE_FIREWALL, Keybindings.abilitySlot1);
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/magma.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.FLAME_SANCTUARY,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.HOGLIN_STABLES) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                Component.translatable("buildings.reignofnether.flame_sanctuary").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.flame_sanctuary.tooltip1").getVisualOrderText(),
                Component.translatable("buildings.reignofnether.flame_sanctuary.tooltip2").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.flame_sanctuary.tooltip3").getVisualOrderText()
            ),
            this
        );
    }

    @Override
    public void onBlockBuilt(BlockPos bp, BlockState bs, BuildingPlacement placement) {
        if (!placement.getLevel().isClientSide()) {
            if (bs.hasBlockEntity()) {
                BlockEntity be = placement.getLevel().getBlockEntity(bp);
                if (be instanceof SpawnerBlockEntity sbe)
                    sbe.getSpawner().setEntityId(EntityType.BLAZE, placement.getLevel(), placement.getLevel().random, bp);
            }
        }
    }
}
