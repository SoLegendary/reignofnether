package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


// disable spectator GUI entirely when in orthoview mode as it has cheaty functions like tp to player

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(
            method = "isOnGround()Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHotbarSelected(
            float f, PoseStack ps, CallbackInfo ci
    ) {
        if (OrthoviewClientEvents.isEnabled())
            ci.cancel();
    }
}
