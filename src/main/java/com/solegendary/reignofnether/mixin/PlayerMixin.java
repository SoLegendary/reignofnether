package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PlayerMixin {

    @Shadow public boolean noPhysics;
    @Shadow public abstract int getId();
    @Shadow public Level level;

    @Inject(
            method = "tick()V",
            at = @At("HEAD")
    )
    // noclip for orthoview players
    // tick() naturally reverses all of this so no need for reversing it here when leaving orthoView
    private void tick(CallbackInfo ci) {
        for (ServerPlayer serverPlayer : PlayerServerEvents.orthoviewPlayers) {
            if (serverPlayer.getId() == this.getId() && !this.noPhysics) {
                this.noPhysics = true;
                Entity entity = this.level.getEntity(this.getId());
                if (entity instanceof Player player) {
                    if (!player.getAbilities().flying) {
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                    }
                }
            }
        }
    }
}
