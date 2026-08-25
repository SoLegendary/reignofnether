package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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

    // server-side senders
    public static void syncToAll(int unitId, List<ItemStack> items) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ItemClientboundPacket(unitId, items));
    }

    // packet-handler functions
    public ItemClientboundPacket(int unitId, List<ItemStack> items) {
        this.unitId = unitId;
        this.items = new ArrayList<>(items.size());
        for (ItemStack stack : items) // copy so later server-side mutation can't race the encode
            this.items.add(stack.copy());
    }

    public ItemClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.items = buffer.readList(ItemClientboundPacket::readStack);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeCollection(this.items, ItemClientboundPacket::writeStack);
    }

    // full-fidelity ItemStack (de)serialisation: item id, count and the entire tag compound
    public static void writeStack(FriendlyByteBuf buffer, ItemStack stack) {
        buffer.writeNbt(stack.save(new CompoundTag()));
    }

    public static ItemStack readStack(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (this.items == null) {
                ReignOfNether.LOGGER.warn("ItemClientboundPacket: no items for unitId " + this.unitId);
                success.set(false);
            }
            else {
                ItemClientEvents.syncInventory(this.unitId, this.items);
                success.set(true);
            }
        }));
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}