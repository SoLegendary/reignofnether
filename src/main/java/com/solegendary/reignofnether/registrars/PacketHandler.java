package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.guiscreen.TopdownGuiServerboundPacket;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.unit.AbilityClientboundPacket;
import com.solegendary.reignofnether.unit.UnitClientboundPacket;
import com.solegendary.reignofnether.unit.UnitServerboundPacket;
import com.solegendary.reignofnether.unit.UnitWorkerClientBoundPacket;
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

        INSTANCE.messageBuilder(UnitWorkerClientBoundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UnitWorkerClientBoundPacket::encode).decoder(UnitWorkerClientBoundPacket::new)
                .consumer(UnitWorkerClientBoundPacket::handle).add();

        INSTANCE.messageBuilder(ResearchClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResearchClientboundPacket::encode).decoder(ResearchClientboundPacket::new)
                .consumer(ResearchClientboundPacket::handle).add();

        INSTANCE.messageBuilder(PlayerServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlayerServerboundPacket::encode).decoder(PlayerServerboundPacket::new)
                .consumer(PlayerServerboundPacket::handle).add();

        INSTANCE.messageBuilder(BuildingServerboundPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuildingServerboundPacket::encode).decoder(BuildingServerboundPacket::new)
                .consumer(BuildingServerboundPacket::handle).add();

        INSTANCE.messageBuilder(BuildingClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BuildingClientboundPacket::encode).decoder(BuildingClientboundPacket::new)
                .consumer(BuildingClientboundPacket::handle).add();

        INSTANCE.messageBuilder(ResourcesClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResourcesClientboundPacket::encode).decoder(ResourcesClientboundPacket::new)
                .consumer(ResourcesClientboundPacket::handle).add();

        INSTANCE.messageBuilder(AbilityClientboundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilityClientboundPacket::encode).decoder(AbilityClientboundPacket::new)
                .consumer(AbilityClientboundPacket::handle).add();
    }
}
