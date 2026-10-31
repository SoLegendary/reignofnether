package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.chat.ChatLog;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    // every new chat line (not the re-adds when chat is resized) goes into RoN's own chat log
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD")
    )
    private void logMessage(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        ChatLog.onAdded(message);
    }
}
