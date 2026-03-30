package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class CustomBuildingCommand {

    public enum TriggerCondition {
        ON_BUILD_COMPLETE,
        ON_DESTROY,
        ON_DAMAGE_TAKEN,
        ON_CAPTURE,
        OFF_COOLDOWN_IF_COMPLETE,
        OFF_COOLDOWN_IF_GARRISONED,
        NONE
    }

    private int tickCooldown = 0;
    public int tickCooldownMax = 20;
    public String command = "";
    public TriggerCondition condition = TriggerCondition.NONE;

    public CustomBuildingCommand() { }

    public void tick(BuildingPlacement bpl) {
        if (bpl.tickAge % 20 == 0 && tickCooldown > 0)
            tickCooldown -= 20;
        if (tickCooldown <= 0 && checkTickingCondition(bpl)) {
            tickCooldown = tickCooldownMax;
            run(bpl);
        }
    }

    public boolean checkTickingCondition(BuildingPlacement bpl) {
        return switch (condition) {
            case OFF_COOLDOWN_IF_COMPLETE -> bpl.isBuilt;
            case OFF_COOLDOWN_IF_GARRISONED -> bpl instanceof GarrisonableBuilding garr && !garr.getOccupants().isEmpty();
            default -> false;
        };
    }

    public void run(BuildingPlacement bpl) {
        if (bpl.level instanceof ServerLevel level) {
            CommandSourceStack source = level.getServer()
                    .createCommandSourceStack()
                    .withPosition(Vec3.atCenterOf(bpl.originPos))
                    .withLevel(level)
                    .withSuppressedOutput();
            level.getServer().getCommands().performPrefixedCommand(source, command);
        }
    }
}
