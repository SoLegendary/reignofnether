package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TextUtil {
    public static MutableComponent translate(String key, Object... args) {
        return Component.translatable(ReignOfNether.MOD_ID + "." + key, args);
    }

    public static String translateText(String key, Object... args) {
        return translate(key, args).getString();
    }
}
