package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class CustomBuilding extends Building {

    public Vec3i structureSize;
    public final CompoundTag structureNbt;
    public Set<Block> portraitBlockOptions;
    public CompoundTag attributesNbt = new CompoundTag(); // NBT containing all the below fields (including portrait block key)
    public int nightRadius = 0;
    public int netherRadius = 0;
    public boolean buildableByVillagers = false;
    public boolean buildableByMonsters = false;
    public boolean buildableByPiglins = false;
    public int garrisonCapacity = 0;
    public int garrisonRange = 20;
    public int numGarrisonZones = 0;
    public int numGarrisonEntries = 0;
    public int numGarrisonExits = 0;

    public CustomBuilding(String structureName, Vec3i structureSize, Block portraitBlock, CompoundTag structureNbt) {
        this(structureName, structureSize, portraitBlock, structureNbt, null);
    }

    public CustomBuilding(String structureName, Vec3i structureSize, Block portraitBlock, CompoundTag structureNbt, CompoundTag attributesNbt) {
        super(structureName, ResourceCost.Building(0,0,0,0), false);
        this.name = WordUtils.capitalize(structureName
                .replace("minecraft:", "")
                .replace("reignofnether:", "")
                .replace("_", " "));
        this.structureSize = structureSize;
        this.structureNbt = structureNbt;
        this.portraitBlock = portraitBlock;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_front.png");
        this.startingBlockTypes.addAll(BuildingBlockData.getBuildingBlocksFromNbt(this.structureNbt)
            .stream().filter(bb -> bb.getBlockPos().getY() == 0)
            .map(bb -> bb.getBlockState().getBlock()).toList());
        this.portraitBlockOptions = BuildingBlockData.getBuildingBlocksFromNbt(this.structureNbt)
                .stream().filter(bb -> !List.of(
                            BlockRegistrar.GARRISON_EXIT_BLOCK.get(),
                            BlockRegistrar.GARRISON_ENTRY_BLOCK.get(),
                            BlockRegistrar.GARRISON_ZONE_BLOCK.get()
                        ).contains(bb.getBlockState().getBlock()))
                .map(bb -> bb.getBlockState().getBlock())
                .collect(Collectors.toSet());
        this.packAttributesNbt();
        if (attributesNbt != null) {
            this.attributesNbt = attributesNbt;
            this.unpackAttributesNbt();
        }

        for (BuildingBlock bb : BuildingBlockData.getBuildingBlocksFromNbt(structureNbt)) {
            if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_ZONE_BLOCK.get()) {
                numGarrisonZones += 1;
            } else if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_ENTRY_BLOCK.get()) {
                numGarrisonEntries += 1;
            } else if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_EXIT_BLOCK.get()) {
                numGarrisonExits += 1;
            }
        }
    }

    public void packAttributesNbt() {
        attributesNbt.putString("portraitBlockRegistryKey", this.getPortraitBlockRegistryKey());
        attributesNbt.putBoolean("capturable", this.capturable);
        attributesNbt.putBoolean("invulnerable", this.invulnerable);
        attributesNbt.putInt("nightRadius", this.nightRadius);
        attributesNbt.putInt("netherRadius", this.netherRadius);
        attributesNbt.putBoolean("buildableByVillagers", this.buildableByVillagers);
        attributesNbt.putBoolean("buildableByMonsters", this.buildableByMonsters);
        attributesNbt.putBoolean("buildableByPiglins", this.buildableByPiglins);
        attributesNbt.putInt("foodCost", this.cost.food);
        attributesNbt.putInt("woodCost", this.cost.wood);
        attributesNbt.putInt("oreCost", this.cost.ore);
        attributesNbt.putInt("garrisonCapacity", this.garrisonCapacity);
        attributesNbt.putInt("garrisonRange", this.garrisonRange);
    }

    private void unpackAttributesNbt() {
        this.setIconAndPortrait(attributesNbt.getString("portraitBlockRegistryKey"));
        this.capturable = attributesNbt.getBoolean("capturable");
        this.invulnerable = attributesNbt.getBoolean("invulnerable");
        this.nightRadius = attributesNbt.getInt("nightRadius");
        this.netherRadius = attributesNbt.getInt("netherRadius");
        this.buildableByVillagers = attributesNbt.getBoolean("buildableByVillagers");
        this.buildableByMonsters = attributesNbt.getBoolean("buildableByMonsters");
        this.buildableByPiglins = attributesNbt.getBoolean("buildableByPiglins");
        this.cost.food = attributesNbt.getInt("foodCost");
        this.cost.wood = attributesNbt.getInt("woodCost");
        this.cost.ore = attributesNbt.getInt("oreCost");
        this.garrisonCapacity = attributesNbt.getInt("garrisonCapacity");
        this.garrisonRange = attributesNbt.getInt("garrisonRange");
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new CustomBuildingPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), true);
    }

    @Override
    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocksFromNbt(structureNbt);
    }

    public Faction getFaction() {return Faction.NONE;}

    public BuildingPlaceButton getWorkerBuildButton(Keybinding hotkey) {
        return new BuildingPlaceButton(
                this.name,
                MiscUtil.getTextureForBlock(portraitBlock),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> false,
                () -> true,
                getWorkerBuildTooltips(),
                this
        );
    }

    private List<FormattedCharSequence> getWorkerBuildTooltips() {
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>();
        tooltips.add(fcs(this.name, true));
        if (cost.food > 0 || cost.ore > 0 || cost.wood > 0)
            tooltips.add(ResourceCosts.getFormattedCost(cost));
        if (capturable)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.set_capturable.label")));
        if (invulnerable)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.set_invulnerable.label")));
        if (nightRadius > 0)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.set_night_radius.label") + ": " + nightRadius));
        if (netherRadius > 0)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.set_nether_radius.label") + ": " + netherRadius));
        if (garrisonCapacity > 0)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.set_garrison_capacity.label") + ": " + garrisonCapacity));
        if (garrisonRange > 0)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.set_garrison_range.label") + ": " + garrisonRange));
        return tooltips;
    }

    @Override
    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        BuildingPlaceButton button = new BuildingPlaceButton(
                this.name,
                MiscUtil.getTextureForBlock(portraitBlock),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> false,
                () -> true,
                List.of(
                        fcs(this.name, true),
                        fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.building_menu"))
                ),
                this
        );
        button.onRightClick = () -> CustomBuildingClientEvents.setCustomBuildingToEdit(this);
        return button;
    }

    public String getPortraitBlockRegistryKey() {
        return BuiltInRegistries.BLOCK.getKey(portraitBlock).toString();
    }

    public void setIconAndPortrait(String blockRegistryKey) {
        this.portraitBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(blockRegistryKey));
    }

    public void cycleIconAndPortrait(boolean reverse) {
        ArrayList<Block> blockOptions = new ArrayList<>(portraitBlockOptions.stream().filter(b -> !b.defaultBlockState().isAir()).toList());
        if (reverse)
            Collections.reverse(blockOptions);
        boolean foundCurrentPortrait = false;
        boolean changedPortrait = false;
        for (Block block : blockOptions) {
            if (foundCurrentPortrait) {
                portraitBlock = block;
                changedPortrait = true;
                break;
            } else if (block == portraitBlock) {
                foundCurrentPortrait = true;
            }
        }
        if (!changedPortrait)
            portraitBlock = blockOptions.get(0);

        CustomBuildingServerboundPacket.customiseBuilding(CustomBuildingAction.SET_PORTRAIT_BLOCK, name, getPortraitBlockRegistryKey());
    }
}
