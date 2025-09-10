package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.guiscreen.TopdownGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {


    @Shadow @Nullable private Overlay overlay;

    @Shadow @Nullable public Screen screen;

    @Shadow protected abstract void handleKeybinds();

    @Shadow protected int missTime;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;tick(Z)V"))
    public void tick(CallbackInfo ci){
        if (this.overlay == null && (this.screen instanceof TopdownGui)) {
            this.handleKeybinds();
            if (this.missTime > 0) {
                --this.missTime;
            }
        }
    }
}
