package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CustomBuildingServerboundPacket {

    public CustomBuildingAction action;
    public String buildingName;
    public boolean boolValue;
    public int intValue;
    public String strValue;

    public static void deregisterBuilding(String buildingName) {
        if (!MiscUtil.isConnected()) return;
        PacketHandler.INSTANCE.sendToServer(new CustomBuildingServerboundPacket(CustomBuildingAction.DEREGISTER, buildingName, false, 0, ""));
    }

    public static void customiseBuilding(CustomBuildingAction action, String buildingName, boolean boolValue) {
        if (!MiscUtil.isConnected()) return;
        PacketHandler.INSTANCE.sendToServer(new CustomBuildingServerboundPacket(action, buildingName, boolValue, 0, ""));
    }

    public static void customiseBuilding(CustomBuildingAction action, String buildingName, int intValue) {
        if (!MiscUtil.isConnected()) return;
        PacketHandler.INSTANCE.sendToServer(new CustomBuildingServerboundPacket(action, buildingName, false, intValue, ""));
    }

    public static void customiseBuilding(CustomBuildingAction action, String buildingName, String strValue) {
        if (!MiscUtil.isConnected()) return;
        PacketHandler.INSTANCE.sendToServer(new CustomBuildingServerboundPacket(action, buildingName, false, 0, strValue));
    }

    public static void customiseBuilding(CustomBuildingAction action, String buildingName) {
        if (!MiscUtil.isConnected()) return;
        PacketHandler.INSTANCE.sendToServer(new CustomBuildingServerboundPacket(action, buildingName, false, 0, ""));
    }

    public static void customiseBuilding(CustomBuildingAction action, String buildingName, int intValue, String strValue) {
        if (!MiscUtil.isConnected()) return;
        PacketHandler.INSTANCE.sendToServer(new CustomBuildingServerboundPacket(action, buildingName, false, intValue, strValue));
    }

    public CustomBuildingServerboundPacket(CustomBuildingAction action,
                                           String buildingName,
                                           boolean boolValue,
                                           int intValue,
                                           String strValue) {
        this.action = action;
        this.buildingName = buildingName;
        this.boolValue = boolValue;
        this.intValue = intValue;
        this.strValue = strValue;
    }

    public CustomBuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(CustomBuildingAction.class);
        this.buildingName = buffer.readUtf();
        this.boolValue = buffer.readBoolean();
        this.intValue = buffer.readInt();
        this.strValue = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.buildingName);
        buffer.writeBoolean(this.boolValue);
        buffer.writeInt(this.intValue);
        buffer.writeUtf(this.strValue);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            if (!SandboxServer.isAnyoneASandboxPlayer())
                return;

            ReignOfNether.LOGGER.info("[CustomBuilding] action={}, buildingName={}, boolValue={}, intValue={}, strValue={}", this.action, this.buildingName, this.boolValue, this.intValue, this.strValue);

            CustomBuilding customBuilding = CustomBuildingServerEvents.getCustomBuilding(this.buildingName);

            if (customBuilding != null) {
                switch (this.action) {
                    case DEREGISTER -> CustomBuildingServerEvents.deregisterCustomBuilding(this.buildingName);
                    case SET_PORTRAIT_BLOCK -> customBuilding.setIconAndPortrait(this.strValue);
                    case SET_CAPTURABLE -> customBuilding.capturable = this.boolValue;
                    case SET_INVULNERABLE -> customBuilding.invulnerable = this.boolValue;
                    case SET_REPAIRABLE -> customBuilding.repairable = this.boolValue;
                    case SET_DESTROY_ON_RESET -> customBuilding.shouldDestroyOnReset = this.boolValue;
                    case SET_NIGHT_RADIUS -> customBuilding.nightRadius = this.intValue;
                    case SET_NETHER_RADIUS -> customBuilding.netherRadius = this.intValue;
                    case SET_BUILDABLE_BY_VILLAGERS -> customBuilding.buildableByVillagers = this.boolValue;
                    case SET_BUILDABLE_BY_MONSTERS -> customBuilding.buildableByMonsters = this.boolValue;
                    case SET_BUILDABLE_BY_PIGLINS -> customBuilding.buildableByPiglins = this.boolValue;
                    case SET_NETHER_TERRAIN_ONLY -> customBuilding.netherTerrainOnly = this.boolValue;
                    case SET_FOOD_COST -> customBuilding.cost.food = this.intValue;
                    case SET_WOOD_COST -> customBuilding.cost.wood = this.intValue;
                    case SET_ORE_COST -> customBuilding.cost.ore = this.intValue;
                    case SET_GARRISON_CAPACITY -> customBuilding.garrisonCapacity = this.intValue;
                    case SET_GARRISON_RANGE -> customBuilding.garrisonRange = this.intValue;
                    case ADD_COMMAND -> customBuilding.addCommand();
                    case DELETE_COMMAND -> customBuilding.deleteCommand(this.intValue);
                    case SET_COMMAND_TEXT -> customBuilding.setCommandText(this.intValue, this.strValue);
                    case SET_COMMAND_COOLDOWN -> customBuilding.setCommandCooldownTicks(this.intValue, this.strValue);
                    case SET_COMMAND_TRIGGER -> customBuilding.setCommandTrigger(this.intValue, this.strValue);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
