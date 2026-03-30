package com.solegendary.reignofnether;

import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.config.ReignOfNetherCommonConfigs;
import com.solegendary.reignofnether.faction.FactionRegistries;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.mixin.DownloadPackSourceAccessor;
import com.solegendary.reignofnether.network.S2CReset;
import com.solegendary.reignofnether.registrars.*;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.GameData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("reignofnether")
public class ReignOfNether {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "reignofnether";
    public static final String VERSION_STRING = "1.3.1";

    // Fields from ClientReset
    public static final Field handshakeField;
    public static final Constructor<?> contextConstructor;
    static final Logger logger = LogManager.getLogger();
    static final Marker RESETMARKER = MarkerManager.getMarker("RESETPACKET")
        .setParents(MarkerManager.getMarker("FMLNETWORK"));
    public static SimpleChannel handshakeChannel;

    public ReignOfNether(FMLJavaModLoadingContext mlctx) {
        // Registering all components
        EnchantmentRegistrar.init(mlctx);

        ItemRegistrar.init(mlctx);
        EntityRegistrar.init(mlctx);
        ContainerRegistrar.init(mlctx);
        SoundRegistrar.init(mlctx);
        BlockRegistrar.init(mlctx);
        BlockEntityRegistrar.init(mlctx);
        GameRuleRegistrar.init();
        Buildings.init();
        FactionRegistries.register();
        ProductionItems.init();
        MobEffectRegistrar.init(mlctx);
        ParticleRegistrar.init(mlctx);

        final ClientEventRegistrar clientRegistrar = new ClientEventRegistrar();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> clientRegistrar::registerClientEvents);

        final ServerEventRegistrar serverRegistrar = new ServerEventRegistrar();
        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> serverRegistrar::registerServerEvents);

        // Registering ClientReset's init
        IEventBus bus = mlctx.getModEventBus();
        bus.addListener(ReignOfNether::init);
        mlctx.registerConfig(ModConfig.Type.COMMON, ReignOfNetherCommonConfigs.SPEC, "reignofnether-common-" + VERSION_STRING + ".toml");
        // client-only config
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {  //workaround to prevent Unsafe Referent usage; See DistExecutor.validateSafeReferent
            ClientModConfigs.registerClientConfigs(mlctx);
        });
        mlctx.registerExtensionPoint(
            DisplayTest.class,
            () -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true)
        );
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        if (handshakeField == null) {
            logger.error(RESETMARKER, "Failed to find FML's handshake channel. Disabling mod.");
            return;
        }
        if (contextConstructor == null) {
            logger.error(RESETMARKER, "Failed to find FML's network event context constructor. Disabling mod.");
            return;
        }
        try {
            Object handshake = handshakeField.get(null);
            if (handshake instanceof SimpleChannel) {
                handshakeChannel = (SimpleChannel) handshake;
                logger.info(RESETMARKER, "Registering forge reset packet.");
                handshakeChannel.messageBuilder(S2CReset.class, 98)
                    .loginIndex(S2CReset::getLoginIndex, S2CReset::setLoginIndex)
                    .decoder(S2CReset::decode)
                    .encoder(S2CReset::encode)
                    .consumerNetworkThread(HandshakeHandler.biConsumerFor(ReignOfNether::handleReset))
                    .add();
                logger.info(RESETMARKER, "Registered forge reset packet successfully.");
            }
        } catch (Exception e) {
            logger.error(
                RESETMARKER,
                "Caught exception when attempting to utilize FML's handshake. Disabling mod. Exception: "
                    + e.getMessage()
            );
        }
        ResourceCosts.deferredLoadResourceCosts();
    }

    public static void handleReset(
        HandshakeHandler handler,
        S2CReset msg,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        Connection connection = context.getNetworkManager();

        if (context.getDirection() != NetworkDirection.LOGIN_TO_CLIENT
            && context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            connection.disconnect(Component.literal("Illegal packet received, terminating connection"));
            throw new IllegalStateException("Invalid packet received, aborting connection");
        }

        logger.info(RESETMARKER, "Received reset packet from server.");

        if (!handleClear(context)) {
            return;
        }

        NetworkHooks.registerClientLoginChannel(connection);
        connection.setProtocol(ConnectionProtocol.LOGIN);
        connection.setListener(new ClientHandshakePacketListenerImpl(
            connection,
            Minecraft.getInstance(),
            null,
            null,
            false,
            Duration.ZERO,
            statusMessage -> {
            }
        ));
        Minecraft.getInstance().pendingConnection = connection;
        context.setPacketHandled(true);
        try {
            handshakeChannel.reply(new HandshakeMessages.C2SAcknowledge(),
                (NetworkEvent.Context) contextConstructor.newInstance(connection, NetworkDirection.LOGIN_TO_CLIENT, 98)
            );
        } catch (Exception e) {
            logger.error(
                RESETMARKER,
                "Exception occurred when attempting to reply to reset packet.  Exception: " + e.getMessage()
            );
            context.setPacketHandled(false);
            return;
        }
        logger.info(RESETMARKER, "Reset complete.");
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean handleClear(NetworkEvent.Context context) {
        CompletableFuture<Void> future = context.enqueueWork(() -> {
            logger.debug(RESETMARKER, "Clearing");

            ServerData serverData = Minecraft.getInstance().getCurrentServer();
            Pack serverPack = ((DownloadPackSourceAccessor)Minecraft.getInstance().getDownloadedPackSource()).getServerPack();

            if (Minecraft.getInstance().level == null) {
                GameData.revertToFrozen();
            }
            ((DownloadPackSourceAccessor)Minecraft.getInstance().getDownloadedPackSource()).setServerPack(null);

            Minecraft.getInstance()
                .clearLevel(new GenericDirtMessageScreen(Component.translatable("connect.negotiating")));
            try {
                context.getNetworkManager().channel().pipeline().remove("forge:forge_fixes");
            } catch (NoSuchElementException ignored) {
            }
            try {
                context.getNetworkManager().channel().pipeline().remove("forge:vanilla_filter");
            } catch (NoSuchElementException ignored) {
            }

            ((DownloadPackSourceAccessor)Minecraft.getInstance().getDownloadedPackSource()).setServerPack(serverPack);
            //Minecraft.getInstance().setCurrentServer(serverData); TODO find out why, almost impossible to reproduce
        });

        logger.debug(RESETMARKER, "Waiting for clear to complete");
        try {
            future.get();
            logger.debug("Clear complete, continuing reset");
            return true;
        } catch (Exception ex) {
            logger.error(RESETMARKER, "Failed to clear, closing connection", ex);
            context.getNetworkManager().disconnect(Component.literal("Failed to clear, closing connection"));
            return false;
        }
    }

    private static Field fetchHandshakeChannel() {
        try {
            return ObfuscationReflectionHelper.findField(NetworkConstants.class, "handshakeChannel");
        } catch (Exception e) {
            logger.error("Exception occurred while accessing handshakeChannel: " + e.getMessage());
            return null;
        }
    }

    private static Constructor<?> fetchNetworkEventContext() {
        try {
            return ObfuscationReflectionHelper.findConstructor(
                NetworkEvent.Context.class,
                Connection.class,
                NetworkDirection.class,
                int.class
            );
        } catch (Exception e) {
            logger.error("Exception occurred while accessing getLoginIndex: " + e.getMessage());
            return null;
        }
    }

    static {
        handshakeField = fetchHandshakeChannel();
        contextConstructor = fetchNetworkEventContext();
    }
}
