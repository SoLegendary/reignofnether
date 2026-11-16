package com.solegendary.reignofnether.guide;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

// what map scripts show a player: dialogue, highlights and the cinematic camera. Built with the factories below,
// each action only uses some of the fields
public class GuideClientboundPacket {

    private static final int MAX_LENGTH = 4096;

    private final GuideAction action;
    private final String head;   // SAY: the speaker's portrait
    private final String name;   // SAY: the speaker's name
    private final String text;   // SAY: the line, HIGHLIGHT: the callout (lang keys or plain text)
    private final String target; // HIGHLIGHT: a HUD region, "button:<name>", "entity:<id>", "building" or "pos"; LISTEN: events, comma-separated
    private final BlockPos pos;  // HIGHLIGHT
    private final int number;    // SAY: callback id (-1: none), FOLLOW: entity id (-1: stop)
    private final float amount;  // ROTATE: degrees, SHAKE: strength
    private final int ticks;     // FOLLOW (0: until stopped), ROTATE, SHAKE
    private final boolean flag;  // SAY: waits for the script, CONTROL: the player has control

    public static void say(ServerPlayer player, String head, String name, String text, boolean waits, int callback) {
        send(List.of(player), new GuideClientboundPacket(GuideAction.SAY, head, name, text, "", BlockPos.ZERO, callback, 0, 0, waits));
    }

    public static void highlight(Collection<ServerPlayer> players, String target, String text, BlockPos pos) {
        send(players, new GuideClientboundPacket(GuideAction.HIGHLIGHT, "", "", text, target, pos, 0, 0, 0, false));
    }

    // the actions with no fields: CLEAR_DIALOGUE, NEXT, CLEAR_HIGHLIGHT
    public static void send(Collection<ServerPlayer> players, GuideAction action) {
        send(players, new GuideClientboundPacket(action, "", "", "", "", BlockPos.ZERO, 0, 0, 0, false));
    }

    public static void listen(ServerPlayer player, String events) {
        send(List.of(player), new GuideClientboundPacket(GuideAction.LISTEN, "", "", "", events, BlockPos.ZERO, 0, 0, 0, false));
    }

    public static void control(Collection<ServerPlayer> players, boolean control) {
        send(players, new GuideClientboundPacket(GuideAction.CONTROL, "", "", "", "", BlockPos.ZERO, 0, 0, 0, control));
    }

    public static void follow(Collection<ServerPlayer> players, int entityId, int ticks) {
        send(players, new GuideClientboundPacket(GuideAction.FOLLOW, "", "", "", "", BlockPos.ZERO, entityId, 0, ticks, false));
    }

    // ROTATE or SHAKE
    public static void camera(Collection<ServerPlayer> players, GuideAction action, float amount, int ticks) {
        send(players, new GuideClientboundPacket(action, "", "", "", "", BlockPos.ZERO, 0, amount, ticks, false));
    }

    private static void send(Collection<ServerPlayer> players, GuideClientboundPacket packet) {
        for (ServerPlayer player : players)
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private GuideClientboundPacket(GuideAction action, String head, String name, String text, String target, BlockPos pos,
                                   int number, float amount, int ticks, boolean flag) {
        this.action = action;
        this.head = cap(head);
        this.name = cap(name);
        this.text = cap(text);
        this.target = cap(target);
        this.pos = pos;
        this.number = number;
        this.amount = amount;
        this.ticks = ticks;
        this.flag = flag;
    }

    // a script line can be longer than the packet allows, which would throw on send
    private static String cap(String s) {
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) : s;
    }

    public GuideClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(GuideAction.class);
        this.head = buffer.readUtf(MAX_LENGTH);
        this.name = buffer.readUtf(MAX_LENGTH);
        this.text = buffer.readUtf(MAX_LENGTH);
        this.target = buffer.readUtf(MAX_LENGTH);
        this.pos = buffer.readBlockPos();
        this.number = buffer.readInt();
        this.amount = buffer.readFloat();
        this.ticks = buffer.readInt();
        this.flag = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeUtf(head, MAX_LENGTH);
        buffer.writeUtf(name, MAX_LENGTH);
        buffer.writeUtf(text, MAX_LENGTH);
        buffer.writeUtf(target, MAX_LENGTH);
        buffer.writeBlockPos(pos);
        buffer.writeInt(number);
        buffer.writeFloat(amount);
        buffer.writeInt(ticks);
        buffer.writeBoolean(flag);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            switch (action) {
                case SAY -> GuideClientEvents.say(head, name, text, flag, number);
                case CLEAR_DIALOGUE -> GuideClientEvents.clearDialogue();
                case HIGHLIGHT -> GuideClientEvents.highlight(target, text, pos);
                case CLEAR_HIGHLIGHT -> GuideClientEvents.clearHighlight();
                case NEXT -> GuideClientEvents.forceNext();
                case LISTEN -> GuideClientEvents.listen(target);
                case CONTROL -> CinematicClient.setControl(flag);
                case FOLLOW -> CinematicClient.follow(number, ticks);
                case ROTATE -> CinematicClient.rotate(amount, ticks);
                case SHAKE -> CinematicClient.shake(amount, ticks);
            }
        }));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
