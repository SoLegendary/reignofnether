package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.hud.panels.SpectatorHud;
import com.solegendary.reignofnether.player.PlayerColors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The spectators' match feed, as system lines in their own chat: units lost, buildings finished and
 * research done. Losses of the same unit type are grouped over a few seconds ("lost 3× Zombie").
 */
public final class SpectatorFeed {

    private SpectatorFeed() { }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final long GROUP_MS = 3_000;
    // what is already there when you start watching (buildings counted as built, research synced) is not news
    private static final long QUIET_AFTER_JOIN_MS = 10_000;

    private record Losses(long first, int[] count) { }

    private static final Map<String, Losses> losses = new LinkedHashMap<>(); // "owner\0unit"
    private static long activeSince = -1;

    private static boolean live() {
        return activeSince > 0 && System.currentTimeMillis() - activeSince > QUIET_AFTER_JOIN_MS;
    }

    public static synchronized void unitLost(String owner, String unitName) {
        if (live() && !owner.isBlank())
            losses.computeIfAbsent(owner + "\0" + unitName, k -> new Losses(System.currentTimeMillis(), new int[1])).count()[0]++;
    }

    public static void built(String owner, String buildingName) {
        if (live() && !owner.isBlank())
            post(owner, Component.translatable("hud.reignofnether.spectator.feed.built", buildingName));
    }

    public static void researched(String owner, String researchName) {
        if (live() && !owner.isBlank())
            post(owner, Component.translatable("hud.reignofnether.spectator.feed.researched", researchName));
    }

    public static synchronized void tick() {
        if (!SpectatorHud.isActive()) {
            activeSince = -1;
            losses.clear();
            return;
        }
        if (activeSince < 0)
            activeSince = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Losses>> it = losses.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Losses> e = it.next();
            if (now - e.getValue().first() < GROUP_MS)
                continue;
            String[] key = e.getKey().split("\0", 2);
            int n = e.getValue().count()[0];
            post(key[0], n == 1 ? Component.translatable("hud.reignofnether.spectator.feed.lost_one", key[1])
                    : Component.translatable("hud.reignofnether.spectator.feed.lost", n, key[1]));
            it.remove();
        }
    }

    private static void post(String owner, Component what) {
        MutableComponent name = Component.literal(owner)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(PlayerColors.getPlayerColorHex(owner))));
        MC.gui.getChat().addMessage(SystemMessage.info(Component.empty().append(name).append(" ").append(what)));
    }
}
