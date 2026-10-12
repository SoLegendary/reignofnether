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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;

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

    /**
     * The mode list of the join message: the map's default mode in brackets, then the rest.
     *
     * <p>Returns a String on purpose. This value ends up as a translatable argument, and in 1.21.1
     * {@code TranslatableContents.ARG_CODEC} only accepts Number/Boolean/String/Component - passing the
     * StringBuilder itself made the system chat packet fail to encode, which disconnected every player
     * on join to a map that has modes (i.e. every packaged map).
     */
    public static String modesLabel(RTSMapInfo info) {
        if (info.getModes() == null)
            return "";
        StringBuilder modesStr = new StringBuilder();
        for (String modeName : info.getModes().keySet()) {
            if (modeName.equals(info.getDefaultMode()))
                modesStr.append("[").append(modeName).append("]").append(", ");
            else
                modesStr.append(modeName).append(", ");
        }
        if (modesStr.length() > 0)
            modesStr.setLength(modesStr.length() - 2);
        return modesStr.toString();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (rtsMapInfo != null) {
            evt.getEntity().sendSystemMessage(Component.literal(rtsMapInfo.getName() + " " + rtsMapInfo.getVersion()).withStyle(Style.EMPTY.withBold(true)));

            // rtsmap.json is hand-written map data, so tolerate any field being absent: a null here
            // would throw on the server thread while the player is joining.
            var authors = rtsMapInfo.getAuthor();
            if (authors != null && !authors.isEmpty()) {
                MutableComponent authorsLine = authors.size() > 1
                        ? Component.translatable("message.reignofnether.rts_map_info_author.multiple")
                        : Component.translatable("message.reignofnether.rts_map_info_author.singular");
                for (String author : authors)
                    authorsLine.append(" ").append(author);
                evt.getEntity().sendSystemMessage(authorsLine);
            }
            evt.getEntity().sendSystemMessage(Component.literal(""));
            evt.getEntity().sendSystemMessage(Component.literal(rtsMapInfo.getDescription() == null ? "" : rtsMapInfo.getDescription()));

            var modes = rtsMapInfo.getModes();
            if (modes != null && !modes.isEmpty()) {
                String modesLabel = modesLabel(rtsMapInfo);
                if (!modesLabel.isEmpty()) {
                    evt.getEntity().sendSystemMessage(Component.literal(""));
                    // TranslatableContents.ARG_CODEC (1.21.1) only accepts Number/Boolean/String/Component.
                    // A StringBuilder arg cannot be encoded, so the chat packet throws an
                    // EncoderException and the player is kicked the moment they open the map.
                    evt.getEntity().sendSystemMessage(Component.translatable("message.reignofnether.rts_map_info_modes", modesLabel));
                }
                for (String mode : modes.keySet())
                    RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.ADD_MODE, mode);
            }
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MAP_NAME, rtsMapInfo.getName());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_DESCRIPTION, rtsMapInfo.getDescription());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MODE, rtsMapInfo.getDefaultMode());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_VERSION, rtsMapInfo.getVersion());

            MinecraftServer server = evt.getEntity().level().getServer();
            if (server != null && server.getGameRules().getRule(GameRuleRegistrar.RTS_PATHFINDING).get() && WorldBorderServerEvents.prewarmedNavmesh) {
                evt.getEntity().sendSystemMessage(Component.literal(""));
                evt.getEntity().sendSystemMessage(Component.translatable("server.reignofnether.prewarmed_navmesh"));
            }
        }
    }
}
