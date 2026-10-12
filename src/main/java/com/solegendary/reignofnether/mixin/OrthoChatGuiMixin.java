package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.platform.Window;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.5.0 moved the chat history window up above the bottom left hotkeys while the RTS camera is on.
 * Upstream did that in a mixin on Forge's ForgeGui; 1.21.1 NeoForge has no ForgeGui, but it posts
 * its own chat overlay position event from {@code Gui#renderChat}, so the same offset can be applied
 * to the event's Y before it is constructed.
 */
@Mixin(Gui.class)
public class OrthoChatGuiMixin {

    @Redirect(
            method = "renderChat",
            at = @At(
                    value = "NEW",
                    target = "net/neoforged/neoforge/client/event/CustomizeGuiOverlayEvent$Chat"
            )
    )
    private CustomizeGuiOverlayEvent.Chat ron$shiftChatUp(Window window, GuiGraphics guiGraphics,
                                                          DeltaTracker partialTick, int posX, int posY) {
        if (OrthoviewClientEvents.isEnabled())
            posY += OrthoviewClientEvents.CHAT_Y_OFFSET;
        return new CustomizeGuiOverlayEvent.Chat(window, guiGraphics, partialTick, posX, posY);
    }
}
