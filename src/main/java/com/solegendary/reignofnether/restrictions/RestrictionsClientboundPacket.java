package com.solegendary.reignofnether.restrictions;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

// the whole restrictions map: it's small, and co-commanders need their owner's entry
public class RestrictionsClientboundPacket {

    public final Map<String, Set<ResourceLocation>> denied;

    public static void syncAll(Map<String, Set<ResourceLocation>> denied) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new RestrictionsClientboundPacket(denied));
    }

    public static void syncTo(ServerPlayer player, Map<String, Set<ResourceLocation>> denied) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RestrictionsClientboundPacket(denied));
    }

    public RestrictionsClientboundPacket(Map<String, Set<ResourceLocation>> denied) {
        this.denied = denied;
    }

    public RestrictionsClientboundPacket(FriendlyByteBuf buffer) {
        this.denied = buffer.readMap(HashMap::new, FriendlyByteBuf::readUtf,
                b -> b.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeMap(denied, FriendlyByteBuf::writeUtf,
                (b, ids) -> b.writeCollection(ids, FriendlyByteBuf::writeResourceLocation));
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RestrictionsClient.set(denied)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
