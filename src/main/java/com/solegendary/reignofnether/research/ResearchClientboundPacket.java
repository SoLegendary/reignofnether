package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ResearchClientboundPacket {

    // pos is used to identify the building object serverside
    public String playerName;
    public String itemName;
    public boolean add; // false for remove

    public static void addCheat(String playerName, String itemName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new ResearchClientboundPacket(playerName, itemName, true));
    }
    public static void removeCheat(String playerName, String itemName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new ResearchClientboundPacket(playerName, itemName, false));
    }

    public ResearchClientboundPacket(String playerName, String itemName, boolean add) {
        this.playerName = playerName;
        this.itemName = itemName;
        this.add = add;
    }

    public ResearchClientboundPacket(FriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.itemName = buffer.readUtf();
        this.add = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeUtf(this.itemName);
        buffer.writeBoolean(this.add);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (Minecraft.getInstance().player.getName().getString().equals(this.playerName)) {
                            if (add)
                                ResearchClient.addCheat(this.itemName);
                            else
                                ResearchClient.removeCheat(this.itemName);
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
