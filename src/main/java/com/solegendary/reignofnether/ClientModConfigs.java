package com.solegendary.reignofnether;

import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientModConfigs {

    public static void registerClientConfigs(ModContainer mlctx) {
        mlctx.registerConfig(ModConfig.Type.CLIENT,
                ReignOfNetherClientConfigs.SPEC,
                "reignofnether-client-" + ReignOfNether.VERSION_STRING + ".toml");

        // passing the factory instance (not a method reference) picks the (Class, T) overload; the
        // (Class, Supplier<T>) overload exists too and makes a bare reference ambiguous.
        mlctx.registerExtensionPoint(
                IConfigScreenFactory.class,
                ReignOfNetherClientConfigs.createConfigScreen()
        );
    }
}

