package com.solegendary.reignofnether;

import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientModConfigs {

    public static void registerClientConfigs(FMLJavaModLoadingContext mlctx) {
        mlctx.registerConfig(ModConfig.Type.CLIENT,
                ReignOfNetherClientConfigs.SPEC,
                "reignofnether-client-" + ReignOfNether.VERSION_STRING + ".toml");

        mlctx.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                ReignOfNetherClientConfigs::createConfigScreen
        );
    }
}

