package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// syncs the full contents of a unit's inventory to clients
// stacks are serialised via ItemStack.save/of, so tags, enchantments and damage all survive the trip
public class ItemClientboundPacket {

    private final int unitId;
    private final List<ItemStack> items;
    private final BlockPos shopPos;

    public static void syncInventory(int unitId, List<ItemStack> items) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ItemClientboundPacket(unitId, items));
    }

    public static void setShopServedUnit(int unitId, BlockPos shopPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ItemClientboundPacket(unitId, shopPos));
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

    public ItemClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.items = buffer.readList(ItemClientboundPacket::readStack);
        this.shopPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeCollection(this.items, ItemClientboundPacket::writeStack);
        buffer.writeBlockPos(this.shopPos);
    }

    // full-fidelity ItemStack (de)serialisation: item id, count and the entire tag compound
    public static void writeStack(FriendlyByteBuf buffer, ItemStack stack) {
        buffer.writeNbt(stack.save(new CompoundTag()));
    }

    public static ItemStack readStack(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        if (tag == null) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.of(tag);
        return stack.isEmpty() ? ItemStack.EMPTY : stack; // normalise to the singleton
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.items == null) {
                ReignOfNether.LOGGER.warn("ItemClientboundPacket: no items for unitId " + this.unitId);
                success.set(false);
            }
            else if (!this.items.isEmpty()) {
                ItemClientEvents.syncInventory(this.unitId, this.items);
                success.set(true);
            } else {
                ItemClientEvents.setShopServedUnit(this.unitId, this.shopPos);
            }
        }));
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}