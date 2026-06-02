package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.PromoteIllager;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Castle extends ProductionBuilding implements GarrisonableBuildingAddon {
    public final static int MAX_OCCUPANTS = 7;

    public final static String buildingName = "Castle";
    public final static String structureName = "castle";
    public final static String upgradedStructureName = "castle_with_flag";
    public final static ResourceCost cost = ResourceCosts.CASTLE;

    public final static DataType<LivingEntity> PROMOTED_ILLAGER = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "promoted_illager"), (tag, server) -> {
        UUID uuid = tag.getUUID("entity_uuid");
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }, livingEntity -> {
        CompoundTag tag = new CompoundTag();
        if (livingEntity != null)
            tag.putUUID("entity_uuid", livingEntity.getUUID());
        return tag;
    });

    public Castle() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.COBBLESTONE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobblestone.png");

        this.buildTimeModifier = 0.5f;

        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_WALL);
        this.startingBlockTypes.add(Blocks.SPRUCE_SLAB);
        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.DARK_OAK_PLANKS);

        Ability promoteIllager = new PromoteIllager();
        this.abilities.add(promoteIllager, Keybindings.abilitySlot3);

        this.productions.add(ProductionItems.RAVAGER, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.RESEARCH_RAVAGER_CAVALRY, Keybindings.abilitySlot2);
        this.productions.add(ProductionItems.RESEARCH_CASTLE_FLAG, Keybindings.abilitySlot3);

        setActiveAddon(GarrisonableBuildingAddon.class, this, true);
    }

    public Faction getFaction() {
        return Faction.VILLAGERS;
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobblestone.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.CASTLE,
            TutorialClientEvents::isEnabled,
            () -> (
                BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS)
                    && BuildingClientEvents.hasFinishedBuilding(Buildings.BLACKSMITH)
                    && BuildingClientEvents.hasFinishedBuilding(Buildings.ARCANE_TOWER)
            ) || ResearchClient.hasCheat("modifythephasevariance"),
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
                        MAX_OCCUPANTS
                ), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.castle.tooltip3"),
                    Style.EMPTY
                )
            ),
            this
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

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }

    public int getAttackRange() {
        return 30;
    }

    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() {
        return 15;
    }
    public boolean canDestroyBlock(BlockPos relativeBp) {
        return relativeBp.getY() != 15 && relativeBp.getY() != 17;
    }

    @Override
    public BlockPos getEntryPosition(BuildingPlacement placement) {
        return placement.originPos.offset(BuildingUtils.rotatePos(new BlockPos(5, 16, 5), placement.rotation));
    }

    @Override
    public BlockPos getExitPosition(BuildingPlacement placement) {
        return placement.originPos.offset(BuildingUtils.rotatePos(new BlockPos(5, 2, 5), placement.rotation));
    }

    @Override
    public int getCapacity() { return MAX_OCCUPANTS; }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level, BuildingPlacement placement) {
        return getExitPosition(placement);
    }
}
