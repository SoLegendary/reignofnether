package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class CustomBuilding extends Building {

    public Vec3i structureSize;
    public final CompoundTag structureNbt;
    public Set<Block> portraitBlockOptions;

    public CustomBuilding(String structureName, Vec3i structureSize, Block portraitBlock, CompoundTag nbt) {
        super(structureName, ResourceCost.Building(0,0,0,0), false);
        this.name = WordUtils.capitalize(structureName
                .replace("minecraft:", "")
                .replace("reignofnether:", "")
                .replace("_", " "));
        this.structureSize = structureSize;
        this.structureNbt = nbt;
        this.portraitBlock = portraitBlock;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_front.png");
        this.startingBlockTypes.addAll(BuildingBlockData.getBuildingBlocksFromNbt(structureNbt)
            .stream().filter(bb -> bb.getBlockPos().getY() == 0)
            .map(bb -> bb.getBlockState().getBlock()).toList());
        this.portraitBlockOptions = BuildingBlockData.getBuildingBlocksFromNbt(structureNbt)
                .stream().map(bb -> bb.getBlockState().getBlock())
                .collect(Collectors.toSet());
    }

    @Override
    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocksFromNbt(structureNbt);
    }

    public Faction getFaction() {return Faction.NONE;}

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
        this.portraitBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, blockRegistryKey));
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
