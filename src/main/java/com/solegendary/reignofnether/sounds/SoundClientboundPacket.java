package com.solegendary.reignofnether.sounds;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SoundClientboundPacket {

    SoundAction soundAction;
    BlockPos bp;
    String playerName;
    float volume;
    int id;
    int tickDuration;

    public static void playSoundAtPos(SoundAction soundAction, BlockPos bp) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, bp, "", 1.0f, -1));
    }
    public static void playSoundAtPos(SoundAction soundAction, BlockPos bp, float volume) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, bp, "", volume, -1));
    }
    public static void playFadeableLoopingSoundAtPos(SoundAction soundAction, BlockPos bp, float volume, int id, int tickDuration) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, bp, "", volume, id));
    }
    public static void playSoundForAllPlayers(SoundAction soundAction) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, new BlockPos(0,0,0), "", 1.0f, -1));
    }
    public static void playSoundForAllPlayers(SoundAction soundAction, float volume) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, new BlockPos(0,0,0), "", volume, -1));
    }
    public static void playSoundForPlayer(SoundAction soundAction, String name) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, new BlockPos(0,0,0), name, 1.0f, -1));
    }
    public static void playSoundForPlayer(SoundAction soundAction, String name, float volume) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(soundAction, new BlockPos(0,0,0), name, volume, -1));
    }
    public static void stopSoundWithId(int id) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SoundClientboundPacket(SoundAction.STOP_SOUND, new BlockPos(0,0,0), "", 0f, id));
    }

    public SoundClientboundPacket(SoundAction soundAction, BlockPos bp, String playerName, float volume, int id) {
        this.soundAction = soundAction;
        this.bp = bp;
        this.playerName = playerName;
        this.volume = volume;
        this.id = id;
        this.tickDuration = -1;
    }

    public SoundClientboundPacket(SoundAction soundAction, BlockPos bp, String playerName, float volume, int id, int tickDuration) {
        this.soundAction = soundAction;
        this.bp = bp;
        this.playerName = playerName;
        this.volume = volume;
        this.id = id;
        this.tickDuration = tickDuration;
    }

    public SoundClientboundPacket(FriendlyByteBuf buffer) {
        this.soundAction = buffer.readEnum(SoundAction.class);
        this.bp = buffer.readBlockPos();
        this.playerName = buffer.readUtf();
        this.volume = buffer.readFloat();
        this.id = buffer.readInt();
        this.tickDuration = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.soundAction);
        buffer.writeBlockPos(this.bp);
        buffer.writeUtf(this.playerName);
        buffer.writeFloat(this.volume);
        buffer.writeInt(this.id);
        buffer.writeInt(this.tickDuration);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (soundAction == SoundAction.STOP_SOUND) {
                            SoundClientEvents.stopSound(id);
                        } else if (id >= 0) {
                            SoundClientEvents.playFadeableSoundAtPos(soundAction, bp, volume, id, tickDuration);
                        }
                        else if (bp.equals(new BlockPos(0,0,0))) {
                            if (playerName.isBlank())
                                SoundClientEvents.playSoundForLocalPlayer(soundAction, volume);
                            else
                                SoundClientEvents.playSoundIfPlayer(soundAction, playerName, volume);
                        }
                        else
                            SoundClientEvents.playSoundAtPos(soundAction, bp, volume);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
