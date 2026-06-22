package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// No unit follows its path strictly: they aim at cell centres (PathConverter.CenteredPath, so they don't hug
// walls) but follow LOOSELY - vanilla corner-cutting and reach tolerance, no per-node lock, no stuck-recovery
// oscillation. Keeping the body off walls/edges/ceilings is the pathfinder's job (2x2 footprint + crowdingMalus),
// not the follower's. The ONLY thing we still relax is the VERTICAL advance gate, so a unit perched on a stair
// step (held up by its width ~1.0 above the next node) commits to the drop instead of spinning on the ledge.
@Mixin(PathNavigation.class)
public class PathNavigationMixin {

    @Shadow @Final protected Mob mob;

    @Unique
    private boolean reignofnether$isWideUnit() {
        if (mob instanceof SpiderUnit spider && spider.isWallClimbing()) return false;
        return mob instanceof Unit && mob.getBbWidth() > 1.0f;
    }

    @Unique
    private boolean reignofnether$isClimbingSpider() {
        return mob instanceof SpiderUnit spider && spider.isWallClimbing();
    }

    @ModifyConstant(method = "followThePath", constant = @Constant(doubleValue = 1.0))
    private double reignofnether$widenVerticalReach(double original) {
        // Climbing spiders barely gate on Y at all: a climb is a straight column (same X/Z), so the horizontal
        // reach already keeps them on the wall. With a big vertical window they advance to the far end of the
        // column right away and just go for it up/down, instead of fussing over each block of the climb. Wide
        // units get a small relax so a body perched on a stair step still drops instead of spinning on the ledge.
        if (reignofnether$isClimbingSpider()) return 16.0;
        return reignofnether$isWideUnit() ? 1.5 : original;
    }
}
