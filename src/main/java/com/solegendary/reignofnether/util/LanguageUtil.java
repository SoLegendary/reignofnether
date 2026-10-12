package com.solegendary.reignofnether.util;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class LanguageUtil {
    public static String getTranslation(String translation, Object... objects) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return I18n.get(translation, objects);
        }else {
            return String.format(Language.getInstance().getOrDefault(translation), objects);
        }
    }
}
