package com.solegendary.reignofnether;

import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ClientModConfigs {

    public static void registerClientConfigs() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,
                ReignOfNetherClientConfigs.SPEC,
                "reignofnether-client-" + ReignOfNether.VERSION_STRING + ".toml");

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                ReignOfNetherClientConfigs::createConfigScreen
        );
    }
}

