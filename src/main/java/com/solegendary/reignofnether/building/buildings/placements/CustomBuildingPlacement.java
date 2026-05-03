package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingCommand;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class CustomBuildingPlacement extends BuildingPlacement {
    public final ArrayList<BlockPos> garrisonEntries = new ArrayList<>();
    public final ArrayList<BlockPos> garrisonExits = new ArrayList<>();
    public final ArrayList<CustomBuildingCommand> commands = new ArrayList<>();

    public CustomBuildingPlacement(CustomBuilding customBuilding, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(customBuilding, level, originPos, rotation, ownerName, blocks, isCapitol);

        for (BuildingBlock bb : blocks) {
            if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_ENTRY_BLOCK.get()) {
                garrisonEntries.add(bb.getBlockPos());
            } else if (bb.getBlockState().getBlock() == BlockRegistrar.GARRISON_EXIT_BLOCK.get()) {
                garrisonExits.add(bb.getBlockPos());
            }
        }
        for (CustomBuildingCommand command : customBuilding.commands) {
            CustomBuildingCommand newCommand = new CustomBuildingCommand();
            newCommand.tickCooldown = command.tickCooldown;
            newCommand.tickCooldownMax = command.tickCooldownMax;
            newCommand.commandStr = command.commandStr;
            newCommand.condition = command.condition;
            this.commands.add(newCommand);
        }
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        if (!this.level.isClientSide())
            for (CustomBuildingCommand command : commands)
                if (command.condition == CustomBuildingCommand.TriggerCondition.ON_BUILD_COMPLETE)
                    command.run(this);
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        for (CustomBuildingCommand command : commands)
            if (command.condition == CustomBuildingCommand.TriggerCondition.ON_DESTROY)
                command.run(this);
    }

    @Override
    protected boolean checkIfCaptured(ServerLevel serverLevel) {
        boolean captured = super.checkIfCaptured(serverLevel);
        if (captured)
            for (CustomBuildingCommand command : commands)
                if (command.condition == CustomBuildingCommand.TriggerCondition.ON_CAPTURE && command.isOffCooldown())
                    command.run(this);
        return captured;
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        super.onBlockBreak(level, pos, breakBlocks);
        for (CustomBuildingCommand command : commands)
            if (command.condition == CustomBuildingCommand.TriggerCondition.ON_DAMAGE_TAKEN && command.isOffCooldown())
                command.run(this);
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);
        if (!tickLevel.isClientSide())
            for (CustomBuildingCommand command : commands)
                command.tick(this);
    }
}
