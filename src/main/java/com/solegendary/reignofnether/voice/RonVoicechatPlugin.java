package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.ReignOfNether;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

// Simple Voice Chat finds this through its annotation and only when it's installed; nothing else in RoN
// refers to it. During a match every voice packet goes where the speaker's channel allows (VoiceChannels);
// outside a match Simple Voice Chat works as usual.
@ForgeVoicechatPlugin
public class RonVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return ReignOfNether.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent evt) {
        VoicechatConnection senderConnection = evt.getSenderConnection();
        if (senderConnection == null || !(senderConnection.getPlayer().getPlayer() instanceof ServerPlayer speaker))
            return;
        if (!VoiceChannels.isMatch())
            return;
        evt.cancel();
        VoicechatServerApi api = evt.getVoicechat();
        VoiceChannel channel = VoiceChannels.get(speaker.getUUID());
        EntitySoundPacket local = channel == VoiceChannel.LOCAL ? evt.getPacket().toEntitySoundPacket(speaker.getUUID(), false) : null;
        StaticSoundPacket fixed = channel != VoiceChannel.LOCAL ? evt.getPacket().toStaticSoundPacket() : null;
        for (UUID uuid : VoiceChannels.players()) {
            VoicechatConnection connection = api.getConnectionOf(uuid);
            if (uuid.equals(speaker.getUUID()) || connection == null || !connection.isConnected() ||
                    !(connection.getPlayer().getPlayer() instanceof ServerPlayer listener) ||
                    !VoiceChannels.canHear(speaker, listener, channel))
                continue;
            if (local != null)
                api.sendEntitySoundPacketTo(connection, local);
            else
                api.sendStaticSoundPacketTo(connection, fixed);
        }
    }
}
