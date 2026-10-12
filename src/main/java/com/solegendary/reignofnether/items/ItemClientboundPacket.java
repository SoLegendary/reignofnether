package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// syncs the full contents of a unit's inventory to clients
// stacks are serialised via ItemStack.save/of, so tags, enchantments and damage all survive the trip
public class ItemClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ItemClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "item_clientbound"));

    @Override
    public CustomPacketPayload.Type<ItemClientboundPacket> type() {
        return TYPE;
    }

    private final int unitId;
    private final List<ItemStack> items;
    private final BlockPos shopPos;

    public static void syncInventory(int unitId, List<ItemStack> items) {
        PacketHandler.send(PacketHandler.allPlayers(), new ItemClientboundPacket(unitId, items));
    }

    public static void setShopServedUnit(int unitId, BlockPos shopPos) {
        PacketHandler.send(PacketHandler.allPlayers(), new ItemClientboundPacket(unitId, shopPos));
    }

    public ItemClientboundPacket(int unitId, List<ItemStack> items) {
        this.unitId = unitId;
        this.items = new ArrayList<>(items.size());
        for (ItemStack stack : items) // copy so later server-side mutation can't race the encode
            this.items.add(stack.copy());
        this.shopPos = new BlockPos(0,0,0);
    }

    public ItemClientboundPacket(int unitId, BlockPos shopPos) {
        this.unitId = unitId;
        this.items = List.of();
        this.shopPos = shopPos;
    }

    public ItemClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.items = ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buffer);
        this.shopPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buffer, this.items);
        buffer.writeBlockPos(this.shopPos);
    }

    // full-fidelity ItemStack (de)serialisation: item id, count and the entire tag compound
    public static void writeStack(RegistryFriendlyByteBuf buffer, ItemStack stack) {
        buffer.writeNbt(stack.save(buffer.registryAccess()));
    }

    public static ItemStack readStack(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        if (tag == null) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.parseOptional(buffer.registryAccess(), tag);
        return stack.isEmpty() ? ItemStack.EMPTY : stack; // normalise to the singleton
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.items == null) {
                ReignOfNether.LOGGER.warn("ItemClientboundPacket: no items for unitId " + this.unitId);
            }
            else if (!this.items.isEmpty()) {
                ItemClientEvents.syncInventory(this.unitId, this.items);
            } else {
                ItemClientEvents.setShopServedUnit(this.unitId, this.shopPos);
            }
        }));
        return;
    }
}