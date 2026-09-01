package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.BuildingCommand;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Random;

public class CustomBuildingPlacement extends ProductionPlacement {
    public final ArrayList<BlockPos> garrisonEntries = new ArrayList<>();
    public final ArrayList<BlockPos> garrisonExits = new ArrayList<>();
    public final ArrayList<BlockPos> spawnBlocks = new ArrayList<>();
    public final ArrayList<BuildingCommand> commands = new ArrayList<>();
    public ListTag commandsNbt = new ListTag();
    private final Random random = new Random();

    public CustomBuildingPlacement(CustomBuilding customBuilding, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(customBuilding, level, originPos, rotation, ownerName, blocks, isCapitol);

        for (BuildingBlock bb : blocks) {
            if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_ENTRY_BLOCK.get()) {
                garrisonEntries.add(bb.getBlockPos());
            } else if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_EXIT_BLOCK.get()) {
                garrisonExits.add(bb.getBlockPos());
            } else if (bb.getBlockState().getBlock() == BlockRegistrar.PRODUCTION_SPAWN_BLOCK.get()) {
                spawnBlocks.add(bb.getBlockPos());
            }
        }
        for (BuildingCommand command : customBuilding.commands) {
            BuildingCommand newCommand = new BuildingCommand();
            newCommand.tickCooldown = command.tickCooldown;
            newCommand.tickCooldownMax = command.tickCooldownMax;
            newCommand.commandStr = command.commandStr;
            newCommand.condition = command.condition;
            this.commands.add(newCommand);
        }
    }

    public void packCommandsNbt() {
        this.commandsNbt.clear();
        for (BuildingCommand command : commands) {
            CompoundTag ctag = new CompoundTag();
            ctag.putInt("tickCooldown", command.tickCooldown);
            ctag.putInt("tickCooldownMax", command.tickCooldownMax);
            ctag.putString("commandStr", command.commandStr);
            ctag.putString("condition", command.condition.toString());
            ctag.putInt("triggerCount", command.triggerCount);
            this.commandsNbt.add(ctag);
        }
    }

    @Override
    public Entity produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName, boolean spawnIndoors, Vec3i spawnOffset) {
        BlockPos spawnPoint = getDefaultOutdoorSpawnPoint();
        if (!spawnBlocks.isEmpty()) {
            spawnPoint = spawnBlocks.get(random.nextInt(spawnBlocks.size()));
        }
        CompoundTag nbt = null;
        if (this.getBuilding() instanceof CustomBuilding cb && cb.unitProductionNbts.containsKey(entityType))
            nbt = cb.unitProductionNbts.get(entityType);

        Entity entity = entityType.spawn(level, (CompoundTag) null,
                null,
                spawnPoint,
                MobSpawnType.SPAWNER,
                true,
                false
        );
        if (entity != null && nbt != null) {
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(entity.getX()));
            pos.add(DoubleTag.valueOf(entity.getY()));
            pos.add(DoubleTag.valueOf(entity.getZ()));
            nbt.put("Pos", pos);
            entity.load(nbt);
        }
        if (entity instanceof Unit unit) {
            unit.setOwnerName(ownerName);
            unit.setupEquipmentAndUpgradesServer();
            setDelayedRally(unit);
        }
        return entity;
    }

    public void setAndUnpackCommandsNbt(ListTag nbt) {
        if (nbt.isEmpty())
            return;
        this.commands.clear();
        this.commandsNbt = nbt;
        for (Tag tag : this.commandsNbt) {
            this.commands.add(BuildingCommand.getFromNbt((CompoundTag) tag));
        }
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        if (!this.level.isClientSide())
            for (BuildingCommand command : commands)
                if (command.condition == BuildingCommand.TriggerCondition.ON_BUILD_COMPLETE)
                    command.run(this);
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        for (BuildingCommand command : commands)
            if (command.condition == BuildingCommand.TriggerCondition.ON_DESTROY)
                command.run(this);
    }

    @Override
    protected boolean checkAndDoCapture(ServerLevel serverLevel) {
        boolean captured = super.checkAndDoCapture(serverLevel);
        if (captured) {
            for (BuildingCommand command : commands) {
                if (command.condition == BuildingCommand.TriggerCondition.ON_CAPTURE ||
                    command.condition == BuildingCommand.TriggerCondition.OFF_COOLDOWN_IF_CAPTURED) {
                    command.triggerCount = 0; // allow retrigger after the cooldown is done
                    command.setCooldownToMax();
                }
            }
        }
        return captured;
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        super.onBlockBreak(level, pos, breakBlocks);
        for (BuildingCommand command : commands)
            if (command.condition == BuildingCommand.TriggerCondition.ON_DAMAGE_TAKEN && command.isOffCooldown())
                command.run(this);
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);
        if (!tickLevel.isClientSide())
            for (BuildingCommand command : commands)
                command.tick(this);
    }

    public void resetAllCommands() {
        for (BuildingCommand command : commands) {
            command.reset();
        }
    }
}
