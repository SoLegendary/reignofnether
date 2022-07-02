package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BuildingServerboundPacket {

    private static Minecraft MC = Minecraft.getInstance();

    public BlockPos bp;
    public CompoundTag nbt;
    public BuildingAction action;

    public static void placeBlock(BlockPos bp, CompoundTag nbt) {
        if (MC.level != null) {
            PacketHandler.INSTANCE.sendToServer(
                    new BuildingServerboundPacket(bp, nbt, BuildingAction.PLACE)
            );
            // place and destroy dummy block clientside with drops disabled to get particle effects
            MC.level.setBlock(bp, Blocks.OAK_LOG.defaultBlockState(), 1);
            MC.level.destroyBlock(bp, false);
            MC.level.setBlock(bp, Blocks.OAK_LOG.defaultBlockState(), 1);
        }
    }
    public static void destroyBlock(BlockPos bp) {
        if (MC.level != null) {
            PacketHandler.INSTANCE.sendToServer(
                    new BuildingServerboundPacket(bp, null, BuildingAction.DESTROY)
            );
            MC.level.destroyBlock(bp, false);
        }
    }

    // packet-handler functions
    public BuildingServerboundPacket(BlockPos bp, CompoundTag nbt, BuildingAction action) {
        this.bp = bp;
        this.nbt = nbt;
        this.action = action;
    }

    public BuildingServerboundPacket(FriendlyByteBuf buffer) {
        this.bp = buffer.readBlockPos();
        this.nbt = buffer.readNbt();
        this.action = buffer.readEnum(BuildingAction.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.bp);
        buffer.writeNbt(this.nbt);
        buffer.writeEnum(this.action);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            if (this.action == BuildingAction.PLACE)
                BuildingServerEvents.placeBlock(this.bp, this.nbt);
            else if (this.action == BuildingAction.DESTROY)
                BuildingServerEvents.destroyBlock(this.bp);

            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
