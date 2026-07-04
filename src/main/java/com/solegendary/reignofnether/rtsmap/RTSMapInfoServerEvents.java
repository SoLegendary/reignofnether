package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RTSMapInfoServerEvents {

    public static RTSMapInfo rtsMapInfo = null;

    public static boolean usingMapInfoStartPositions() {
        return rtsMapInfo != null && rtsMapInfo.supportsMode(rtsMapInfo.getDefaultMode());
    }

    @SubscribeEvent
    public static void loadInfo(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        // read from rtsmap.json, if empty, then ignore saved startPoses and start blocks
        if (level != null) {
            rtsMapInfo = RTSMapInfoLoader.load(level);
            if (rtsMapInfo != null) {
                ReignOfNether.LOGGER.info("Loaded rtsMapInfo: " +
                        rtsMapInfo.getName() + '|' +
                        rtsMapInfo.getDescription() + '|' +
                        rtsMapInfo.getVersion() + '|' +
                        rtsMapInfo.getAuthor());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (rtsMapInfo != null) {
            evt.getEntity().sendSystemMessage(Component.literal(rtsMapInfo.getName() + " " + rtsMapInfo.getVersion()).withStyle(Style.EMPTY.withBold(true)));
            MutableComponent authors = rtsMapInfo.getAuthor().size() > 1
                    ? Component.translatable("message.reignofnether.rts_map_info_author.multiple")
                    : Component.translatable("message.reignofnether.rts_map_info_author.singular");
            for (String author : rtsMapInfo.getAuthor())
                authors.append(" ").append(author);
            evt.getEntity().sendSystemMessage(authors);
            evt.getEntity().sendSystemMessage(Component.literal(""));
            evt.getEntity().sendSystemMessage(Component.literal(rtsMapInfo.getDescription()));

            StringBuilder modesStr = new StringBuilder();
            for (String modeName : rtsMapInfo.getModes().keySet()) {
                if (modeName.equals(rtsMapInfo.getDefaultMode()))
                    modesStr.append("[").append(modeName).append("]").append(", ");
                else
                    modesStr.append(modeName).append(", ");
            }
            if (modesStr.length() > 0) {
                modesStr.setLength(modesStr.length() - 2);
                evt.getEntity().sendSystemMessage(Component.literal(""));
                evt.getEntity().sendSystemMessage(Component.translatable("message.reignofnether.rts_map_info_modes", modesStr));
            }
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MAP_NAME, rtsMapInfo.getName());
            for (String author : rtsMapInfo.getAuthor())
                RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.ADD_AUTHOR, author);
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_DESCRIPTION, rtsMapInfo.getDescription());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MODE, rtsMapInfo.getDefaultMode());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_VERSION, rtsMapInfo.getVersion());
            for (String mode : rtsMapInfo.getModes().keySet())
                RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.ADD_MODE, mode);

            MinecraftServer server = evt.getEntity().level().getServer();
            if (server != null && server.getGameRules().getRule(GameRuleRegistrar.RTS_PATHFINDING).get() && WorldBorderServerEvents.prewarmedNavmesh) {
                evt.getEntity().sendSystemMessage(Component.literal(""));
                evt.getEntity().sendSystemMessage(Component.translatable("server.reignofnether.prewarmed_navmesh"));
            }
        }
    }
}
