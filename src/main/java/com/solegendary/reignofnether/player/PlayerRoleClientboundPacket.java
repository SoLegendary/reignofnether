package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// syncs soldiers and co-commanders; plain commanders come from the rtsPlayers list as before
public class PlayerRoleClientboundPacket {

    private final boolean clear;
    private final String name;
    private final PlayerRole role;
    private final String owner;

    public static void set(String name, PlayerRole role, String owner) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PlayerRoleClientboundPacket(false, name, role, owner));
    }

    public static void setFor(ServerPlayer player, String name, PlayerRole role, String owner) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlayerRoleClientboundPacket(false, name, role, owner));
    }

    public static void clear() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PlayerRoleClientboundPacket(true, "", PlayerRole.SPECTATOR, ""));
    }

    public PlayerRoleClientboundPacket(boolean clear, String name, PlayerRole role, String owner) {
        this.clear = clear;
        this.name = name;
        this.role = role;
        this.owner = owner;
    }

    public PlayerRoleClientboundPacket(FriendlyByteBuf buffer) {
        this.clear = buffer.readBoolean();
        this.name = buffer.readUtf();
        this.role = buffer.readEnum(PlayerRole.class);
        this.owner = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(clear);
        buffer.writeUtf(name);
        buffer.writeEnum(role);
        buffer.writeUtf(owner);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (clear)
                PlayerRolesClient.clear();
            else
                PlayerRolesClient.set(name, role, owner);
        }));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
