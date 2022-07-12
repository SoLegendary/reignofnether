package com.solegendary.reignofnether.building;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BuildingServerboundPacket {

    private static Minecraft MC = Minecraft.getInstance();

    public BlockPos pos;
    public int damageAmount;
    public BuildingAction action;

    // packet-handler functions
    public BuildingServerboundPacket(BlockPos bp, int damageAmount, BuildingAction action) {
        this.pos = bp;
        this.damageAmount = damageAmount;
        this.action = action;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.damageAmount = buffer.readInt();
        this.action = buffer.readEnum(BuildingAction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.damageAmount);
        buffer.writeEnum(this.action);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            if (this.action == BuildingAction.PLACE)
                System.out.println("PLACE");
            else if (this.action == BuildingAction.DESTROY)
                System.out.println("DESTROY");
            else if (this.action == BuildingAction.DAMAGE)
                System.out.println("DAMAGE");
            else if (this.action == BuildingAction.REPAIR)
                System.out.println("REPAIR");

            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
