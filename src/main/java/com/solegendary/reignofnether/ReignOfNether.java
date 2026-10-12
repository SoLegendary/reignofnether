package com.solegendary.reignofnether;

import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.commands.rtsapi.ResourceObjectiveCriteria;
import com.solegendary.reignofnether.commands.rtsapi.argument.options.BuildingSelectorOptions;
import com.solegendary.reignofnether.config.ReignOfNetherCommonConfigs;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.hud.custombutton.CustomButton;
import com.solegendary.reignofnether.hud.custombutton.CustomButtonActions;
import com.solegendary.reignofnether.hud.custombutton.CustomButtonMappingManager;
import com.solegendary.reignofnether.hud.custombutton.CustomButtonServerEvents;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.BlockEntityRegistrar;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.ClientEventRegistrar;
import com.solegendary.reignofnether.registrars.CommandArgumentRegistrar;
import com.solegendary.reignofnether.registrars.ContainerRegistrar;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.registrars.ServerEventRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.DistHelper;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ReignOfNether.MOD_ID)
public class ReignOfNether {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "reignofnether";
    public static final String VERSION_STRING = "1.4.4d";

    static final Logger logger = LogManager.getLogger();

    // NeoForge 21.1 no longer injects FMLJavaModLoadingContext; the mod event bus and the
    // container (needed for registerConfig / extension points) are constructor parameters.
    public ReignOfNether(IEventBus modBus, ModContainer container) {
        // Registering all components
        EnchantmentRegistrar.init(container);
        AttributeRegistrar.init(container);
        ItemRegistrar.init(container);
        EntityRegistrar.init(container);
        ContainerRegistrar.init(container);
        SoundRegistrar.init(container);
        BlockRegistrar.init(container);
        BlockEntityRegistrar.init(container);
        GameRuleRegistrar.init();
        Buildings.init();
        ProductionItems.init();
        MobEffectRegistrar.init(container);
        ParticleRegistrar.init(container);
        CommandArgumentRegistrar.init(container);
        CustomButtonActions.init(container);
        BuildingSelectorOptions.bootStrap();
        ResourceObjectiveCriteria.init();

        final ClientEventRegistrar clientRegistrar = new ClientEventRegistrar();
        DistHelper.safeRunWhenOn(Dist.CLIENT, () -> clientRegistrar::registerClientEvents);

        final ServerEventRegistrar serverRegistrar = new ServerEventRegistrar();
        DistHelper.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> serverRegistrar::registerServerEvents);

        modBus.addListener(ReignOfNether::init);
        modBus.addListener(ReignOfNether::loadDatapacks);
        // Payload registration is handled once, by CommonModEvents#registerPayloads; adding
        // PacketHandler::registerPayloads here as well registers every payload twice and NeoForge
        // rejects the duplicate ("Cannot register payload ... as it is already registered").
        NeoForge.EVENT_BUS.addListener(ReignOfNether::reloadListener);
        container.registerConfig(ModConfig.Type.COMMON, ReignOfNetherCommonConfigs.SPEC,
                "reignofnether-common-" + VERSION_STRING + ".toml");

        // client-only config
        DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientModConfigs.registerClientConfigs(container));

        // Command argument types are registered from a static initialiser in each argument class:
        // Commands.validate() runs during bootstrap, before NeoForge constructs the mods, so doing it
        // here (or from FMLCommonSetupEvent#enqueueWork) is too late and startup fails with
        // "Unregistered argument types".

        // NeoForge 21.1 dropped FML's IExtensionPoint.DisplayTest, which the Forge build used
        // to refuse connections to servers without the mod. Servers without Reign of Nether
        // simply have nothing to send, so the handshake check is gone; see PORT_ANALYSIS.md.
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        ResourceCosts.deferredLoadResourceCosts();
        // Faction registration reads DeferredHolder values (sounds, entity types, production items),
        // which are only bound once the register events have run - the mod constructor is too early.
        event.enqueueWork(() -> Factions.register());
    }

    @SubscribeEvent
    public static void loadDatapacks(DataPackRegistryEvent.NewRegistry evt) {
        evt.dataPackRegistry(
            CustomButtonServerEvents.CUSTOM_BUTTON_REGISTRY_KEY,
            CustomButton.CODEC,
            CustomButton.CODEC
        );
    }

    public static void reloadListener(AddReloadListenerEvent evt) {
        evt.addListener(new CustomButtonMappingManager());
    }
}