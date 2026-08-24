package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ItemServerboundPacket {

    private final String ownerName; // player that is issuing this command
    private final ItemAction action;
    private final int unitId; // unit performing the action
    private final UUID itemUuid; // uuid of the item in the unit's inventory (unused for PICKUP/NONE)
    private final int targetId; // GIVE/USE_ON_ENTITY: target unit, PICKUP: target ItemEntity (-1 if unused)
    private final BlockPos targetPos; // DROP/USE_ON_BLOCK: block, SELL/USE_ON_BUILDING: building pos (null if unused)

    // client-side senders: one per ItemAction, each taking only what that action needs
    public static void drop(String ownerName, int unitId, UUID itemUuid, BlockPos blockPos) {
        send(ownerName, ItemAction.DROP, unitId, itemUuid, -1, blockPos);
    }

    public static void sell(String ownerName, int unitId, UUID itemUuid, BlockPos buildingPos) {
        send(ownerName, ItemAction.SELL, unitId, itemUuid, -1, buildingPos);
    }

    public static void give(String ownerName, int unitId, UUID itemUuid, int targetUnitId) {
        send(ownerName, ItemAction.GIVE, unitId, itemUuid, targetUnitId, null);
    }

    public static void pickup(String ownerName, int unitId, int itemEntityId) {
        send(ownerName, ItemAction.PICKUP, unitId, null, itemEntityId, null);
    }

    public static void useOnBlock(String ownerName, int unitId, UUID itemUuid, BlockPos blockPos) {
        send(ownerName, ItemAction.USE_ON_BLOCK, unitId, itemUuid, -1, blockPos);
    }

    public static void useOnEntity(String ownerName, int unitId, UUID itemUuid, int targetUnitId) {
        send(ownerName, ItemAction.USE_ON_ENTITY, unitId, itemUuid, targetUnitId, null);
    }

    public static void useOnBuilding(String ownerName, int unitId, UUID itemUuid, BlockPos buildingPos) {
        send(ownerName, ItemAction.USE_ON_BUILDING, unitId, itemUuid, -1, buildingPos);
    }

    public static void use(String ownerName, int unitId, UUID itemUuid) {
        send(ownerName, ItemAction.USE, unitId, itemUuid, -1, null);
    }

    public static void none(String ownerName, int unitId) { // clears the unit's current item action
        send(ownerName, ItemAction.NONE, unitId, null, -1, null);
    }

    private static void send(
            String ownerName,
            ItemAction action,
            int unitId,
            UUID itemUuid,
            int targetId,
            BlockPos targetPos
    ) {
        PacketHandler.INSTANCE.sendToServer(new ItemServerboundPacket(ownerName, action, unitId, itemUuid, targetId, targetPos));
    }

    // packet-handler functions
    public ItemServerboundPacket(
            String ownerName,
            ItemAction action,
            int unitId,
            UUID itemUuid,
            int targetId,
            BlockPos targetPos
    ) {
        this.ownerName = ownerName;
        this.action = action;
        this.unitId = unitId;
        this.itemUuid = itemUuid;
        this.targetId = targetId;
        this.targetPos = targetPos;
    }

    public ItemServerboundPacket(FriendlyByteBuf buffer) {
        this.ownerName = buffer.readUtf();
        this.action = buffer.readEnum(ItemAction.class);
        this.unitId = buffer.readInt();
        this.itemUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        this.targetId = buffer.readInt();
        this.targetPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.ownerName);
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeBoolean(this.itemUuid != null);
        if (this.itemUuid != null)
            buffer.writeUUID(this.itemUuid);
        buffer.writeInt(this.targetId);
        buffer.writeBoolean(this.targetPos != null);
        if (this.targetPos != null)
            buffer.writeBlockPos(this.targetPos);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("Sender for item action packet was null");
                success.set(false);
            }
            else if (!player.getName().getString().equals(ownerName) &&
                    !SandboxServer.isSandboxPlayer(ownerName) &&
                    !AlliancesServerEvents.canControlAlly(player.getName().getString(), ownerName)) {
                ReignOfNether.LOGGER.warn("ItemServerboundPacket: Tried to process packet from " + player.getName() + " for " + ownerName);
                success.set(false);
            }
            else {
                // TODO: wire up to goals and run in ItemServerEvents (and reset behaviours before usage)
                /*
                ItemServerEvents.addActionItem(
                        this.ownerName,
                        this.action,
                        this.unitId,
                        this.itemUuid,
                        this.targetId,
                        this.targetPos
                );
                 */
                success.set(true);
            }
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}