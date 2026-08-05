package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FogNeutralUnitClientboundPacket {

    public int unitId;
    public Vector3f vec3fMin;
    public Vector3f vec3fMax;
    public boolean remove;

    public static void sendNeutralFogUnitToAll(int unitId, AABB aabb) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new FogNeutralUnitClientboundPacket(unitId, aabb, false));
    }

    public static void sendNeutralFogUnit(ServerPlayer serverPlayer, int unitId, AABB aabb) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new FogNeutralUnitClientboundPacket(unitId, aabb, false));
    }

    // remove if the player has explored the pos and the unit is dead
    public static void removeNeutralFogUnit(ServerPlayer serverPlayer, int unitId) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new FogNeutralUnitClientboundPacket(unitId, new AABB(0,0,0,0,0,0), true));
    }

    public FogNeutralUnitClientboundPacket(int unitId, AABB aabb, boolean remove) {
        this.unitId = unitId;
        this.vec3fMin = new Vector3f((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ);
        this.vec3fMax = new Vector3f((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ);
        this.remove = remove;
    }

    public FogNeutralUnitClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.vec3fMin = buffer.readVector3f();
        this.vec3fMax = buffer.readVector3f();
        this.remove = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeVector3f(this.vec3fMin);
        buffer.writeVector3f(this.vec3fMax);
        buffer.writeBoolean(this.remove);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    if (remove)
                        MinimapClientEvents.removeNeutralFogUnit(this.unitId);
                    else
                        MinimapClientEvents.addNeutralFogUnit(this.unitId, this.vec3fMin, this.vec3fMax);
                    success.set(true);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
