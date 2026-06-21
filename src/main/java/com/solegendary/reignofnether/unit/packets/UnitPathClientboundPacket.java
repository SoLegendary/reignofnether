package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Sent server→client when a unit gets a fresh A* path. The client renders the path
// briefly so the player can see the route their units will actually take.
public class UnitPathClientboundPacket {

    private final int entityId;
    private final List<BlockPos> nodes;

    public static void sendPath(LivingEntity entity, Path path) {
        if (path == null || path.nodes == null || path.nodes.isEmpty())
            return;
        List<BlockPos> bps = new ArrayList<>(path.nodes.size());
        for (var node : path.nodes)
            bps.add(node.asBlockPos());
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitPathClientboundPacket(entity.getId(), bps));
    }

    public UnitPathClientboundPacket(int entityId, List<BlockPos> nodes) {
        this.entityId = entityId;
        this.nodes = nodes;
    }

    public UnitPathClientboundPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        int n = buffer.readVarInt();
        this.nodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            this.nodes.add(buffer.readBlockPos());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeVarInt(this.nodes.size());
        for (BlockPos bp : this.nodes)
            buffer.writeBlockPos(bp);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> UnitClientEvents.receiveUnitPath(this.entityId, this.nodes));
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
