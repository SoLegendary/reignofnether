package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.platform.Window;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public class ForgeGuiMixin extends Gui {

    public ForgeGuiMixin(Minecraft pMinecraft, ItemRenderer pItemRenderer) {
        super(pMinecraft, pItemRenderer);
    }

    // moves the chat history window up above bottom left hotkeys
    @Inject(
            method = "renderChat",
            at = @At("HEAD"),
            cancellable = true,
            remap=false
    )
    protected void renderChat(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        ci.cancel();

        this.minecraft.getProfiler().push("chat");
        Window window = this.minecraft.getWindow();
        CustomizeGuiOverlayEvent.Chat event = new CustomizeGuiOverlayEvent.Chat(window, guiGraphics, this.minecraft.getFrameTime(), 0, height - 40);
        MinecraftForge.EVENT_BUS.post(event);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((double)event.getPosX(), (double)(event.getPosY() - height - 15) / this.chat.getScale(), 0.0);
        int mouseX = Mth.floor(this.minecraft.mouseHandler.xpos() * (double)window.getGuiScaledWidth() / (double)window.getScreenWidth());
        int mouseY = Mth.floor(this.minecraft.mouseHandler.ypos() * (double)window.getGuiScaledHeight() / (double)window.getScreenHeight());
        this.chat.render(guiGraphics, this.tickCount, mouseX, mouseY);
        guiGraphics.pose().popPose();
        this.minecraft.getProfiler().pop();
    }
}