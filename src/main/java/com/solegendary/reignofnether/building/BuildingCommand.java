package com.solegendary.reignofnether.building;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingCommand {

    public enum TriggerCondition {
        ON_BUILD_COMPLETE,
        ON_DESTROY,
        ON_DAMAGE_TAKEN,
        ON_CAPTURE,
        OFF_COOLDOWN_IF_COMPLETE,
        OFF_COOLDOWN_IF_GARRISONED,
        OFF_COOLDOWN_IF_CAPTURED,
        NONE
    }

    public boolean hasCooldownCondition() {
        return List.of(
            TriggerCondition.ON_CAPTURE,
            TriggerCondition.ON_DAMAGE_TAKEN,
            TriggerCondition.OFF_COOLDOWN_IF_COMPLETE,
            TriggerCondition.OFF_COOLDOWN_IF_GARRISONED,
            TriggerCondition.OFF_COOLDOWN_IF_CAPTURED
        ).contains(condition);
    }

    public int tickCooldown = 100;
    public int tickCooldownMax = 100;
    public String commandStr = "";
    public TriggerCondition condition = TriggerCondition.NONE;
    public boolean isValid = true; // updated whenever commandStr is updated
    public Map<? extends CommandNode<?>, CommandSyntaxException> exceptions = new HashMap<>();
    public int triggerCount = 0;
    public String file = null; // when set, runs ron/<file>.mcfunction instead of commandStr

    public BuildingCommand() { }
    
    public BuildingCommand(int tickCooldown, int tickCooldownMax, String commandStr, String condition) {
        this.tickCooldown = tickCooldown;
        this.tickCooldownMax = tickCooldownMax;
        this.commandStr = commandStr;
        this.condition = parseCondition(condition);
    }

    public static BuildingCommand getFromNbt(CompoundTag tag) {
        BuildingCommand command = new BuildingCommand();
        command.tickCooldown = tag.getInt("tickCooldown");
        command.tickCooldownMax = tag.getInt("tickCooldownMax");
        command.commandStr = tag.getString("commandStr");
        command.condition = parseCondition(tag.getString("condition"));
        if (tag.contains("triggerCount"))
            command.triggerCount = tag.getInt("triggerCount");
        return command;
    }

    // NONE for triggers that no longer exist (e.g. the old ON_SCENARIO_START), so old saves still load
    private static TriggerCondition parseCondition(String name) {
        try {
            return TriggerCondition.valueOf(name);
        } catch (IllegalArgumentException e) {
            return TriggerCondition.NONE;
        }
    }

    // file name of this trigger's commands in ron/buildings/<id>/
    public String triggerFileName() {
        return condition.name().toLowerCase();
    }

    public boolean isValid() {
        return isValid;
    }

    public Map<? extends CommandNode<?>, CommandSyntaxException> getExceptions() {
        return exceptions;
    }

    public void updateValidityAndExceptions() {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener handler = client.getConnection();
        this.exceptions.clear();

        if (handler == null) {
            this.isValid = false;
            return;
        }
        String stripped = commandStr.startsWith("/") ? commandStr.substring(1) : commandStr;
        ParseResults<?> parsed = handler.getCommands()
                .parse(new StringReader(stripped), handler.getSuggestionsProvider());

        if (!parsed.getExceptions().isEmpty() || parsed.getReader().canRead()) {
            this.isValid = false;
            this.exceptions = parsed.getExceptions();
            return;
        }
        this.isValid = true;
    }

    public boolean isOffCooldown() {
        return tickCooldown <= 0;
    }

    public void setCooldownToMax() {
        tickCooldown = tickCooldownMax;
    }

    public void tick(BuildingPlacement bpl) {
        if (!hasCooldownCondition())
            return;

        if (tickCooldown > 0)
            tickCooldown -= 1;
        if (tickCooldown <= 0 && checkTickingCondition(bpl)) {
            run(bpl);
        }
    }

    public boolean checkTickingCondition(BuildingPlacement bpl) {
        GarrisonableBuildingAddon gba;
        return switch (condition) {
            case OFF_COOLDOWN_IF_COMPLETE -> bpl.isBuilt;
            case OFF_COOLDOWN_IF_GARRISONED -> (gba = bpl.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class)) != null && !gba.getOccupants(bpl).isEmpty();
            case OFF_COOLDOWN_IF_CAPTURED -> !bpl.ownerName.isBlank();
            case ON_CAPTURE -> !bpl.ownerName.isBlank() && triggerCount <= 0;
            default -> false;
        };
    }

    // a building's commands run at its corner (so @s in rtsapi building selectors is the building) as its owner
    public static CommandSourceStack sourceFor(BuildingPlacement bpl, ServerLevel level) {
        return RonFiles.asOwner(level.getServer().createCommandSourceStack()
                .withPosition(bpl.minCorner.offset(-1, 0, -1).getCenter())
                .withLevel(level)
                .withPermission(2)
                .withSuppressedOutput(), bpl.ownerName);
    }

    public void run(BuildingPlacement bpl) {
        if (bpl.level instanceof ServerLevel level) {
            CommandSourceStack source = sourceFor(bpl, level);
            if (file != null)
                RonFiles.run(level.getServer(), file, source, bpl.ownerName);
            else
                level.getServer().getCommands().performPrefixedCommand(source, RonFiles.withOwner(commandStr, bpl.ownerName));

            setCooldownToMax();
            triggerCount += 1;
        }
    }

    public void reset() {
        triggerCount = 0;
        setCooldownToMax();
    }
}
