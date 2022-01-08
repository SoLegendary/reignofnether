package com.solegendary.ageofcraft;

import com.solegendary.ageofcraft.registrars.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("ageofcraft")
public class AgeOfCraft
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "ageofcraft";

    public AgeOfCraft() {
        ItemRegistrar.init();
        EntityRegistrar.init();

        // Use MinecraftForge.EVENT_BUS.register() for non-mod events (eg. onKeyInput, onServerChat)
        // and FMLJavaModLoadingContext...register() for IModEventBus events (eg, FMLClientSetupEvent)
        //
        // If these anything mixed up, you can usually get crashes saying "Has @SubscribeEvent annotation, but..."

        CommonEventRegistrar commonRegistrar = new CommonEventRegistrar();
        commonRegistrar.registerCommonEvents();

        final ClientSideEventRegistrar clientRegistrar = new ClientSideEventRegistrar();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> clientRegistrar::registerClientEvents);

        final ServerSideEventRegistrar serverRegistrar = new ServerSideEventRegistrar();
        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> serverRegistrar::registerServerEvents);
    }
}
