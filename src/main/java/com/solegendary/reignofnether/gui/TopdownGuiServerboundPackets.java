package com.solegendary.reignofnether.gui;

import com.solegendary.reignofnether.registrars.PacketHandler;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class TopdownGuiServerboundPackets {

    private static final Minecraft MC = Minecraft.getInstance();

    /* TODO: convenience functions that just send packets out to enact the server function instead of having to
        write something like: PacketHandler.INSTANCE.sendToServer(new PacketSenderClass(data)); every time
        we can also enact some clientside logic too before asking the server to do stuff
     */

    public static void openTopdownGui() {
        PacketHandler.INSTANCE.sendToServer(new TopdownGuiToggler(true));
    }

    public static void closeTopdownGui() {
        MC.popGuiLayer();
        PacketHandler.INSTANCE.sendToServer(new TopdownGuiToggler(false));
    }


}
