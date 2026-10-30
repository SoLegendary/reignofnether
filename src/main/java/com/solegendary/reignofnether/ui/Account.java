package com.solegendary.reignofnether.ui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The signed-in player for the menus' account chip: their skin (loaded once, in the background) and account type. */
public final class Account {

    private Account() { }

    private static ResourceLocation skin;

    /** The player's skin; the default one until theirs has loaded. */
    public static ResourceLocation skin() {
        if (skin == null) {
            Minecraft mc = Minecraft.getInstance();
            GameProfile user = mc.getUser().getGameProfile();
            skin = DefaultPlayerSkin.getDefaultSkin(user.getId());
            mc.getSkinManager().registerSkins(new GameProfile(user.getId(), user.getName()), (type, location, texture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN)
                    skin = location;
            }, false);
        }
        return skin;
    }

    public static String name() {
        return Minecraft.getInstance().getUser().getName();
    }

    /** "Microsoft account" or "Offline account". */
    public static String kind() {
        return Component.translatable(Minecraft.getInstance().getUser().getType() == User.Type.MSA
                ? "menu.reignofnether.account_microsoft" : "menu.reignofnether.account_offline").getString();
    }
}
