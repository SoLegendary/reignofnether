package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.debug.RtsDebugPathPreview;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
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

// Sent server→client when a unit gets a fresh path. The client renders the path so the player
// can see the route their units will actually take. Gated by /rts-debug on the sender side.
public class UnitPathClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitPathClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_path_clientbound"));

    @Override
    public CustomPacketPayload.Type<UnitPathClientboundPacket> type() {
        return TYPE;
    }

    private final int entityId;
    private final byte pathType;
    private final List<BlockPos> nodes;

    public static void sendPath(LivingEntity entity, Path path, byte pathType) {
        // disallow with fog since that could be used to see hidden blocks
        if (path == null || path.nodes.isEmpty() || FogOfWarServerEvents.isEnabled())
            return;
        List<BlockPos> bps = new ArrayList<>(path.nodes.size());
        for (var node : path.nodes)
            bps.add(node.asBlockPos());
        PacketHandler.send(PacketHandler.allPlayers(),
                new UnitPathClientboundPacket(entity.getId(), pathType, bps));
    }

    public UnitPathClientboundPacket(int entityId, byte pathType, List<BlockPos> nodes) {
        this.entityId = entityId;
        this.pathType = pathType;
        this.nodes = nodes;
    }

    public UnitPathClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.pathType = buffer.readByte();
        int n = buffer.readVarInt();
        this.nodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            this.nodes.add(buffer.readBlockPos());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeByte(this.pathType);
        buffer.writeVarInt(this.nodes.size());
        for (BlockPos bp : this.nodes)
            buffer.writeBlockPos(bp);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RtsDebugPathPreview.receiveUnitPath(this.entityId, this.pathType, this.nodes));
        });
    }
}
