package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Locale;

// the voice channel key cycles your channels during a match; a small tag shows the current one.
// Only with Simple Voice Chat installed
public class VoiceClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static VoiceChannel channel = VoiceChannel.TEAM;
    private static boolean keyWasDown = false;

    public static void reset() {
        channel = VoiceChannel.TEAM;
    }

    private static boolean active() {
        return MC.player != null && ModList.get().isLoaded("voicechat") && !PlayerClientEvents.rtsPlayers.isEmpty();
    }

    // the channel the server will use: ours if the role allows it, else the role's default
    private static VoiceChannel current() {
        PlayerRole role = PlayerRolesClient.myRole();
        return VoiceChannel.allowedFor(role).contains(channel) ? channel : VoiceChannel.defaultFor(role);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        boolean down = Keybindings.voiceChannel.isDown() && MC.screen == null;
        if (down && !keyWasDown && active()) {
            List<VoiceChannel> allowed = VoiceChannel.allowedFor(PlayerRolesClient.myRole());
            channel = allowed.get((allowed.indexOf(current()) + 1) % allowed.size());
            VoiceChannelServerboundPacket.choose(channel);
        }
        keyWasDown = down;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post evt) {
        if (!active() || MC.options.hideGui)
            return;
        GuiGraphics g = evt.getGuiGraphics();
        HudScale scale = HudGeometry.scale();
        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        String label = I18n.get("voice.reignofnether.tag", I18n.get("voice.reignofnether.channel." + current().name().toLowerCase(Locale.ROOT)));
        String key = Keybindings.voiceChannel.getCurrentLabel();
        int w = Text.smallWidth(label) + Text.smallWidth(key) + 14;
        int x = (scale.width() - w) / 2;
        int y = 34;
        g.fill(x, y, x + w, y + 11, 0xD90B0A0F);
        int kw = Theme.keyCap(g, key, x + 3, y + 2, Theme.GOLD);
        Text.small(g, label, x + 3 + kw + 4, y + 2, Theme.MUTED);
        g.pose().popPose();
    }
}
