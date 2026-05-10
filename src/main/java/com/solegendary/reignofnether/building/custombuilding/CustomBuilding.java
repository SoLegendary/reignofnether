package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class CustomBuilding extends Building implements GarrisonableBuildingAddon, NetherConvertingAddon, NightSourceAddon, RangeIndicatorAddon {
    public static final List<Block> INVULNERABLE_BLOCKS = List.of(
            BlockRegistrar.GARRISON_EXIT_BLOCK.get(),
            BlockRegistrar.GARRISON_ENTRY_BLOCK.get(),
            BlockRegistrar.GARRISON_ZONE_BLOCK.get(),
            Blocks.NETHER_PORTAL,
            Blocks.LIGHT,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK
    );

    public static final List<Block> INVULNERABLE_ABOVE_BLOCKS = List.of(
            BlockRegistrar.GARRISON_ENTRY_BLOCK.get(),
            BlockRegistrar.GARRISON_ZONE_BLOCK.get()
    );

    public Vec3i structureSize;
    public final CompoundTag structureNbt;
    public Set<Block> portraitBlockOptions = new HashSet<>();
    public CompoundTag attributesNbt = new CompoundTag(); // NBT containing all the below fields (including portrait block key)
    public ListTag commandsNbt = new ListTag();
    public int nightRadius = 0;
    public int netherRadius = 0;
    public boolean buildableByVillagers = false;
    public boolean buildableByMonsters = false;
    public boolean buildableByPiglins = false;
    public boolean netherTerrainOnly = false;
    public int garrisonCapacity = 0;
    public int garrisonRange = 20;
    public int numGarrisonZones = 0;
    public int numGarrisonEntries = 0;
    public int numGarrisonExits = 0;
    public ArrayList<CustomBuildingCommand> commands = new ArrayList<>(List.of(new CustomBuildingCommand()));
    private final Random random = new Random();

    public CustomBuilding(String structureName, Vec3i structureSize, Block portraitBlock, CompoundTag structureNbt) {
        this(structureName, structureSize, portraitBlock, structureNbt, null, null);
    }

    public CustomBuilding(String structureName, Vec3i structureSize, Block portraitBlock, CompoundTag structureNbt, CompoundTag attributesNbt, ListTag commandsNbt) {
        super(structureName, ResourceCost.Building(0,0,0,0), false);
        this.name = WordUtils.capitalize(structureName
                .replace("minecraft:", "")
                .replace("reignofnether:", "")
                .replace("_", " "));
        this.structureSize = structureSize;
        this.structureNbt = structureNbt;
        this.portraitBlock = portraitBlock;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_front.png");
        for (BuildingBlock buildingBlock : BuildingBlockData.getBuildingBlocksFromNbt(this.structureNbt)) {
            if (buildingBlock.getBlockPos().getY() == 0) {
                Block block = buildingBlock.getBlockState().getBlock();
                this.startingBlockTypes.add(block);
            }
        }
        for (BuildingBlock buildingBlock : BuildingBlockData.getBuildingBlocksFromNbt(this.structureNbt)) {
            if (!List.of(
                BlockRegistrar.GARRISON_EXIT_BLOCK.get(),
                BlockRegistrar.GARRISON_ENTRY_BLOCK.get(),
                BlockRegistrar.GARRISON_ZONE_BLOCK.get()
            ).contains(buildingBlock.getBlockState().getBlock())) {
                Block block = buildingBlock.getBlockState().getBlock();
                this.portraitBlockOptions.add(block);
            }
        }
        this.packAttributesNbt();
        if (attributesNbt != null) {
            this.attributesNbt = attributesNbt;
            this.unpackAttributesNbt();
        }
        this.packCommandsNbt();
        if (commandsNbt != null) {
            this.commandsNbt = commandsNbt;
            this.unpackCommandsNbt();
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

        //TODO made this toggelable
        setActiveAddon(GarrisonableBuildingAddon.class, this, true);
        setActiveAddon(NetherConvertingAddon.class, this, true);
        setActiveAddon(NightSourceAddon.class, this, true);
        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    public void packAttributesNbt() {
        attributesNbt.putString("portraitBlockRegistryKey", this.getPortraitBlockRegistryKey());
        attributesNbt.putBoolean("capturable", this.capturable);
        attributesNbt.putBoolean("invulnerable", this.invulnerable);
        attributesNbt.putBoolean("repairable", this.repairable);
        attributesNbt.putBoolean("shouldDestroyOnReset", this.shouldDestroyOnReset);
        attributesNbt.putInt("nightRadius", this.nightRadius);
        attributesNbt.putInt("netherRadius", this.netherRadius);
        attributesNbt.putBoolean("buildableByVillagers", this.buildableByVillagers);
        attributesNbt.putBoolean("buildableByMonsters", this.buildableByMonsters);
        attributesNbt.putBoolean("buildableByPiglins", this.buildableByPiglins);
        attributesNbt.putBoolean("netherTerrainOnly", this.netherTerrainOnly);
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
        this.repairable = attributesNbt.getBoolean("repairable");
        this.shouldDestroyOnReset = attributesNbt.getBoolean("shouldDestroyOnReset");
        this.nightRadius = attributesNbt.getInt("nightRadius");
        this.netherRadius = attributesNbt.getInt("netherRadius");
        this.buildableByVillagers = attributesNbt.getBoolean("buildableByVillagers");
        this.buildableByMonsters = attributesNbt.getBoolean("buildableByMonsters");
        this.buildableByPiglins = attributesNbt.getBoolean("buildableByPiglins");
        this.netherTerrainOnly = attributesNbt.getBoolean("netherTerrainOnly");
        this.cost.food = attributesNbt.getInt("foodCost");
        this.cost.wood = attributesNbt.getInt("woodCost");
        this.cost.ore = attributesNbt.getInt("oreCost");
        this.garrisonCapacity = attributesNbt.getInt("garrisonCapacity");
        this.garrisonRange = attributesNbt.getInt("garrisonRange");
    }

    public void packCommandsNbt() {
        this.commandsNbt.clear();
        for (CustomBuildingCommand command : commands) {
            CompoundTag ctag = new CompoundTag();
            ctag.putInt("tickCooldown", command.tickCooldown);
            ctag.putInt("tickCooldownMax", command.tickCooldownMax);
            ctag.putString("commandStr", command.commandStr);
            ctag.putString("condition", command.condition.toString());
            this.commandsNbt.add(ctag);
        }
    }

    private void unpackCommandsNbt() {
        this.commands.clear();
        for (Tag tag : this.commandsNbt) {
            this.commands.add(CustomBuildingCommand.getFromNbt((CompoundTag) tag));
        }
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
        if (!repairable)
            tooltips.add(fcs(I18n.get("sandbox.reignofnether.custom_buildings.not_repairable.label")));
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
        this.portraitBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockRegistryKey));

    }

    public void cycleIconAndPortrait(boolean reverse) {
        List<Block> list = new ArrayList<>();
        for (Block b : portraitBlockOptions) {
            if (!b.defaultBlockState().isAir()) {
                list.add(b);
            }
        }
        ArrayList<Block> blockOptions = new ArrayList<>(list);
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

    public void addCommand() {
        this.commands.add(new CustomBuildingCommand());
    }

    public void deleteCommand(int index) {
        try {
            this.commands.remove(index);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException in deleteCommand");
        }
    }

    public void setCommandText(int index, String text) {
        try {
            this.commands.get(index).commandStr = text;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException in setCommandText");
        }
    }

    public void setCommandCooldownTicks(int index, String cooldownTicksStr) {
        int cooldownTicks;
        try {
            cooldownTicks = Integer.parseInt(cooldownTicksStr);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException in setCommandCooldown");
            return;
        }
        try {
            this.commands.get(index).tickCooldownMax = cooldownTicks;
            this.commands.get(index).tickCooldown = cooldownTicks;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException in setCommandCooldown");
        }
    }

    public void setCommandTrigger(int index, String triggerStr) {
        CustomBuildingCommand.TriggerCondition triggerCond;
        try {
            triggerCond = CustomBuildingCommand.TriggerCondition.valueOf(triggerStr);
        } catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException in setCommandTrigger");
            return;
        }
        try {
            this.commands.get(index).condition = triggerCond;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException in setCommandTrigger");
        }
    }
    // GarrisonableBuilding
    @Override
    public int getAttackRange() { return garrisonRange; }

    @Override
    public int getExternalAttackRangeBonus() { return Math.min(15, garrisonRange / 2); }

    @Override
    public int getCapacity() { return garrisonCapacity; }

    @Override
    public BlockPos getEntryPosition(BuildingPlacement placement) {
        CustomBuildingPlacement cbp = (CustomBuildingPlacement) placement;
        if (!cbp.garrisonEntries.isEmpty()) {
            return cbp.garrisonEntries.get(random.nextInt(cbp.garrisonEntries.size())).above();
        }
        return null;
    }

    @Override
    public BlockPos getExitPosition(BuildingPlacement placement) {
        CustomBuildingPlacement cbp = (CustomBuildingPlacement) placement;
        if (!cbp.garrisonExits.isEmpty()) {
            return cbp.garrisonExits.get(random.nextInt(cbp.garrisonExits.size())).above();
        }
        return null;
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp, BuildingPlacement placement) {
        if (getCapacity() <= 0)
            return true;
        BlockPos worldBp = relativeBp.offset(placement.originPos);
        Block block = placement.getLevel().getBlockState(worldBp).getBlock();
        BlockPos worldBpAbove = relativeBp.offset(placement.originPos.above());
        Block blockAbove = placement.getLevel().getBlockState(worldBpAbove).getBlock();
        return !INVULNERABLE_BLOCKS.contains(block) && !INVULNERABLE_ABOVE_BLOCKS.contains(blockAbove);
    }

    // NetherConvertingBuilding
    @Override public double getMaxNetherRange(BuildingPlacement placement) { return this.netherRadius; }
    @Override public double getStartingNetherRange(BuildingPlacement placement) { return 3; }

    @Override
    public void onBuilt(BuildingPlacement buildingPlacement) {
        super.onBuilt(buildingPlacement);
        updateHighlightBps(buildingPlacement);
        if (getMaxNetherRange(buildingPlacement) > 0)
            setNetherZone(buildingPlacement, new NetherZone(new BlockPos(buildingPlacement.centrePos.getX(), buildingPlacement.originPos.getY() + 1, buildingPlacement.centrePos.getZ()), getMaxNetherRange(buildingPlacement), getStartingNetherRange(buildingPlacement)), true);
    }

    @Nullable
    @Override
    public NetherZone getNetherZone(BuildingPlacement placement) {
        if (this.netherRadius > 0)
            return NetherConvertingAddon.super.getNetherZone(placement);
        return null;
    }

    // NightSource
    @Override
    public int getNightRange(BuildingPlacement placement) {
        return placement.isBuilt ? nightRadius : 0;
    }

    @Override
    public int getRange(BuildingPlacement placement) {
        return getNightRange(placement);
    }

    // RangeIndicator
    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide() || this.getNightRange(placement) <= 0) {
            return;
        }
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).clear();
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                getNightRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ,
                placement.level, true
        ));
    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return false;
    }
}
