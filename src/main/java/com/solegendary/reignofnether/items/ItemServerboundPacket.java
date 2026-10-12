package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;
import java.util.function.Supplier;

public class ItemServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ItemServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "item_serverbound"));

    @Override
    public CustomPacketPayload.Type<ItemServerboundPacket> type() {
        return TYPE;
    }

    private static final int NO_INDEX = -1;

    private final ItemAction action;
    private final int unitId; // unit performing the action
    private final UUID itemUuid; // uuid of the item in the unit's inventory (unused for PICKUP/NONE/SWAP)
    private final int targetId; // GIVE/USE_ON_ENTITY: target unit, PICKUP: target ItemEntity (-1 if unused)
    private final BlockPos targetPos; // DROP/USE_ON_BLOCK: block, SELL/USE_ON_BUILDING: building pos (null if unused)
    private final int invIndex1; // SWAP: first inventory slot (-1 if unused)
    private final int invIndex2; // SWAP: second inventory slot (-1 if unused)

    // client-side senders: one per ItemAction, each taking only what that action needs
    public static void drop(int unitId, UUID itemUuid, BlockPos blockPos) {
        send(ItemAction.DROP, unitId, itemUuid, -1, blockPos);
    }

    public static void sell(int unitId, UUID itemUuid, BlockPos buildingPos) {
        send(ItemAction.SELL, unitId, itemUuid, -1, buildingPos);
    }

    public static void give(int unitId, UUID itemUuid, int targetUnitId) {
        send(ItemAction.GIVE, unitId, itemUuid, targetUnitId, null);
    }

    public static void pickup(int unitId, int itemEntityId) {
        send(ItemAction.PICKUP, unitId, null, itemEntityId, null);
    }

    public static void useOnBlock(int unitId, UUID itemUuid, BlockPos blockPos) {
        send(ItemAction.USE_ON_BLOCK, unitId, itemUuid, -1, blockPos);
    }

    public static void useOnEntity(int unitId, UUID itemUuid, int targetUnitId) {
        send(ItemAction.USE_ON_ENTITY, unitId, itemUuid, targetUnitId, null);
    }

    public static void useOnBuilding(int unitId, UUID itemUuid, BlockPos buildingPos) {
        send(ItemAction.USE_ON_BUILDING, unitId, itemUuid, -1, buildingPos);
    }

    public static void use(int unitId, UUID itemUuid) {
        send(ItemAction.USE, unitId, itemUuid, -1, null);
    }

    public static void swap(int unitId, int invIndex1, int invIndex2) { // swaps two slots in the unit's inventory
        send(ItemAction.SWAP, unitId, null, -1, null, invIndex1, invIndex2);
    }

    public static void openShop(int unitId, BlockPos buildingPos) {
        send(ItemAction.OPEN_SHOP, unitId, null, -1, buildingPos);
    }

    public static void buy(int unitId, UUID itemUuid, BlockPos buildingPos) { // buys an item from a shop (UUID here is Item UUID, not inventory UUID)
        send(ItemAction.BUY, unitId, itemUuid, -1, buildingPos, -1, -1);
    }

    /**
     * 1.5.0 addresses shop stock by item id rather than the per-stack UUID, so the HUD button
     * does not have to track one. The UUID form above stays for the server-side paths.
     */
    public static void buy(int unitId, String descId, BlockPos buildingPos) {
        UnitItem unitItem = ItemUtil.getUnitItem(descId);
        send(ItemAction.BUY, unitId, unitItem != null ? unitItem.uuid : null, -1, buildingPos, -1, -1);
    }

    private static void send(
            ItemAction action,
            int unitId,
            UUID itemUuid,
            int targetId,
            BlockPos targetPos
    ) {
        send(action, unitId, itemUuid, targetId, targetPos, NO_INDEX, NO_INDEX);
    }

    private static void send(
            ItemAction action,
            int unitId,
            UUID itemUuid,
            int targetId,
            BlockPos targetPos,
            int invIndex1,
            int invIndex2
    ) {
        PacketHandler.sendToServer(new ItemServerboundPacket(
                action, unitId, itemUuid, targetId, targetPos, invIndex1, invIndex2
        ));
    }

    public ItemServerboundPacket(
            ItemAction action,
            int unitId,
            UUID itemUuid,
            int targetId,
            BlockPos targetPos,
            int invIndex1,
            int invIndex2
    ) {
        this.action = action;
        this.unitId = unitId;
        this.itemUuid = itemUuid;
        this.targetId = targetId;
        this.targetPos = targetPos;
        this.invIndex1 = invIndex1;
        this.invIndex2 = invIndex2;
    }

    public ItemServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(ItemAction.class);
        this.unitId = buffer.readInt();
        this.itemUuid = buffer.readBoolean() ? buffer.readUUID() : null;
        this.targetId = buffer.readInt();
        this.targetPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
        if (buffer.readBoolean()) {
            this.invIndex1 = buffer.readInt();
            this.invIndex2 = buffer.readInt();
        }
        else {
            this.invIndex1 = NO_INDEX;
            this.invIndex2 = NO_INDEX;
        }
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeBoolean(this.itemUuid != null);
        if (this.itemUuid != null)
            buffer.writeUUID(this.itemUuid);
        buffer.writeInt(this.targetId);
        buffer.writeBoolean(this.targetPos != null);
        if (this.targetPos != null)
            buffer.writeBlockPos(this.targetPos);
        boolean hasIndices = this.invIndex1 != NO_INDEX || this.invIndex2 != NO_INDEX;
        buffer.writeBoolean(hasIndices);
        if (hasIndices) {
            buffer.writeInt(this.invIndex1);
            buffer.writeInt(this.invIndex2);
        }
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            Unit actionableUnit = null;
            for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                if (le.getId() == unitId && le instanceof Unit unit) {
                    actionableUnit = unit;
                    break;
                }
            }

            if (player == null) {
                ReignOfNether.LOGGER.warn("Sender for item action packet was null");
            }
            else if (actionableUnit == null) {
                ReignOfNether.LOGGER.warn("Unit for item action packet was null");
            }
            else if (!player.getName().getString().equals(actionableUnit.getOwnerName()) &&
                    !SandboxServer.isSandboxPlayer(actionableUnit.getOwnerName()) &&
                    !AlliancesServerEvents.canControlAlly(player.getName().getString(), actionableUnit.getOwnerName())) {
                ReignOfNether.LOGGER.warn("ItemServerboundPacket: Tried to process packet from " + player.getName() + " for " + actionableUnit.getOwnerName());
            }
            else {
                if (this.action == ItemAction.BUY) {
                    ItemServerEvents.buyItem(actionableUnit, this.itemUuid, this.targetPos);
                } else if (this.action == ItemAction.SWAP) {
                    ItemServerEvents.swapItems(actionableUnit, this.invIndex1, this.invIndex2);
                } else {
                    ItemServerEvents.doAction(
                            this.action,
                            actionableUnit,
                            this.itemUuid,
                            this.targetId,
                            this.targetPos
                    );
                }
            }
        });
        return;
    }
}