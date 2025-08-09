package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ConfigClientEvents {

    private static ArrayList<FormattedCharSequence> tooltipLines = new ArrayList<>();
    public static boolean showDiffsButton = false;

    static {
        resetDiffsTooltips();
    }

    public static void resetDiffsTooltips() {
        tooltipLines.clear();
        tooltipLines.add(FormattedCharSequence.forward(I18n.get("config.reignofnether.warn_config_change"), Style.EMPTY.withBold(true)));
        tooltipLines.add(FormattedCharSequence.forward(I18n.get("config.reignofnether.hide_config_change"), Style.EMPTY));
        tooltipLines.add(FormattedCharSequence.forward("", Style.EMPTY));
        showDiffsButton = false;
    }

    /*
    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn evt) {
        // LOG IN TO SERVER WORLD ONLY
        ReignOfNether.LOGGER.info("Resetting client config diffs");
        resetDiffsTooltips();
    }
     */

    public static Button getDiffsButton() {
        return new Button(
                "Configs Changed Warning",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/warning.png"),
                (Keybinding) null,
                () -> false,
                () -> !showDiffsButton || TutorialClientEvents.isEnabled(),
                () -> true,
                () -> showDiffsButton = false,
                null,
                tooltipLines
        );
    }

    private static final Minecraft MC = Minecraft.getInstance();
    //Load config data from server
    public static void loadConfigData(ClientboundSyncResourceCostPacket msg, Supplier<NetworkEvent.Context> ctx) {
        String key = msg.getId();
        if(ResourceCost.ENTRIES.containsKey(key)) {
            ResourceCost rescost = ResourceCost.ENTRIES.get(key);
            //jank, but this is how we rebake using the values sent from the packet currently
            //we can clean this up later

            int woodDiff = msg.getWood() - rescost.wood;
            int foodDiff = msg.getFood() - rescost.food;
            int oreDiff = msg.getOre() - rescost.ore;
            int ticksDiff = msg.getTicks() - rescost.ticks;
            int popDiff = msg.getPopulation() - rescost.population;

            rescost.wood = msg.getWood();
            rescost.food = msg.getFood();
            rescost.ore = msg.getOre();
            rescost.ticks = msg.getTicks();
            rescost.population = msg.getPopulation();

            if (foodDiff != 0 ||
                woodDiff != 0 ||
                oreDiff != 0 ||
                ticksDiff != 0 ||
                popDiff != 0) {

                String text = rescost.id.replace("reignofnether.", "")
                        .replace("_","    ")
                        .toLowerCase(Locale.ENGLISH);
                text = text.substring(0, 1).toUpperCase() + text.substring(1);
                text += ":";

                if (foodDiff != 0)
                    text += (foodDiff > 0 ? "    +" : "    ") + foodDiff + " \uE000";
                if (woodDiff != 0)
                    text += (woodDiff > 0 ? "    +" : "    ") + woodDiff + " \uE001";
                if (oreDiff != 0)
                    text += (oreDiff > 0 ? "    +" : "    ") + oreDiff + " \uE002";
                if (ticksDiff != 0)
                    text += (ticksDiff > 0 ? "    +" : "    ") + (ticksDiff / 20) + " \uE004";
                if (popDiff != 0)
                    text += (popDiff > 0 ? "    +" : "    ") +  popDiff + " \uE003";

                showDiffsButton = true;

                tooltipLines.add(FormattedCharSequence.forward(text, MyRenderer.iconStyle));

                ReignOfNether.LOGGER.info("Received different config from server: " + text);
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        ReignOfNether.LOGGER.info("onPlayerJoined fired from ClientConfigEvents");
        //If we own this singleplayer world
        if (evt.getEntity().getServer().isSingleplayerOwner(evt.getEntity().getGameProfile())) {
            resetDiffsTooltips();
            //rebake from clientsideside configs
            ReignOfNether.LOGGER.info("Attempting to rebake from client..");
            for(ResourceCostConfigEntry entry : ResourceCostConfigEntry.ENTRIES) {
                String key = entry.id;
                if(ResourceCost.ENTRIES.containsKey(key)) {
                    ResourceCost rescost = ResourceCost.ENTRIES.get(key);
                    ReignOfNether.LOGGER.info("ID found: " + key + ", replacing resourcecost " + ResourceCost.ENTRIES.get(key));
                    rescost.bakeValues(entry);
                }
            }
            ResourceCosts.deferredLoadResourceCosts();
        }
    }
}