package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.gui.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.units.UnitClientboundPacket;
import com.solegendary.reignofnether.units.UnitServerboundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Initialises all of the client-server packet-sending classes

public final class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ReignOfNether.MOD_ID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private PacketHandler() { }

    public static void init() {
        int index = 0;

        INSTANCE.messageBuilder(TopdownGuiServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TopdownGuiServerboundPacket::encode).decoder(TopdownGuiServerboundPacket::new)
                .consumer(TopdownGuiServerboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnitServerboundPacket::encode).decoder(UnitServerboundPacket::new)
                .consumer(UnitServerboundPacket::handle).add();

        INSTANCE.messageBuilder(UnitClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitClientboundPacket::encode).decoder(UnitClientboundPacket::new)
                .consumer(UnitClientboundPacket::handle).add();
    }
}
