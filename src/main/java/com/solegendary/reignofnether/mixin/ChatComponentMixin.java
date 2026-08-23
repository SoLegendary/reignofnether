package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    // Match the change to ForgeGuiMixin to also move up components like buttons and links
    @ModifyVariable(
            method = "getClickedComponentStyleAt(DD)Lnet/minecraft/network/chat/Style;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1   // 0 = pMouseX, 1 = pMouseY (ordinal counts doubles only)
    )
    private double modifyMouseY(double pMouseY) {
        return OrthoviewClientEvents.isEnabled() ? pMouseY - OrthoviewClientEvents.CHAT_Y_OFFSET : pMouseY;
    }
}