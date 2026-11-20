package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The editor's ▶ Test, with the faction to play when the seat doesn't fix one. */
public class EditorTestServerboundPacket {

    private final ResourceLocation faction;

    public static void start(Faction faction) {
        PacketHandler.INSTANCE.sendToServer(new EditorTestServerboundPacket(faction.key));
    }

    public EditorTestServerboundPacket(ResourceLocation faction) {
        this.faction = faction;
    }

    public EditorTestServerboundPacket(FriendlyByteBuf buffer) {
        this.faction = buffer.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(faction);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                EditorTestServer.start(player, Factions.getFaction(faction)); // checks the sender hosts the world
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
