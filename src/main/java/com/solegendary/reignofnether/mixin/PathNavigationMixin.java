package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No unit follows its path strictly: they aim at cell centres (PathConverter.CenteredPath, so they don't hug
// walls) but follow LOOSELY - vanilla corner-cutting and reach tolerance, no per-node lock, no stuck-recovery
// oscillation. Keeping the body off walls/edges/ceilings is the pathfinder's job (2x2 footprint + crowdingMalus),
// not the follower's. The ONLY thing we still relax is the VERTICAL advance gate, so a unit perched on a stair
// step (held up by its width ~1.0 above the next node) commits to the drop instead of spinning on the ledge.
@Mixin(PathNavigation.class)
public class PathNavigationMixin {

    @Shadow @Final protected Mob mob;
    @Shadow protected Path path;

    @Unique
    private boolean reignofnether$isWideUnit() {
        if (mob instanceof SpiderUnit spider && spider.isWallClimbing()) return false;
        return mob instanceof Unit && mob.getBbWidth() > 1.0f;
    }

    @Unique
    private boolean reignofnether$isClimbingSpider() {
        return mob instanceof SpiderUnit spider && spider.isWallClimbing();
    }

    // Vanilla auto-recompute must never touch an RTS-pathed unit. On any block change,
    // ServerLevel.sendBlockUpdated asks every mob's navigation shouldRecomputePath (true within
    // remaining-path-length blocks of the path midpoint - a huge sphere for a long march) and then
    // recomputePath() replaces the path with a synchronous vanilla createPath(targetPos). targetPos is
    // whatever the last vanilla createPath set (eg. an attack goal's chase target) and stop() never clears
    // it, so the unit veers back toward a STALE position - the multiplayer "units walk backwards /
    // oscillate" bug. Block-change reactions are the RTS system's job (dirty-chunk reclassify + the move
    // goal's own repath); vanilla mode (gamerule off) keeps stock behaviour.
    @Unique
    private boolean reignofnether$usesRtsPaths() {
        return mob instanceof Unit && UnitServerEvents.rtsPathfinding;
    }

    @Inject(method = "shouldRecomputePath", at = @At("HEAD"), cancellable = true)
    private void reignofnether$noBlockUpdateRecompute(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (reignofnether$usesRtsPaths()) cir.setReturnValue(false);
    }

    // Also gate recomputePath itself: it can still fire via a pending hasDelayedRecomputation from before
    // the gamerule turned on, or any direct caller.
    @Inject(method = "recomputePath", at = @At("HEAD"), cancellable = true)
    private void reignofnether$noRecompute(CallbackInfo ci) {
        if (reignofnether$usesRtsPaths()) ci.cancel();
    }

    @ModifyConstant(method = "followThePath", constant = @Constant(doubleValue = 1.0))
    private double reignofnether$widenVerticalReach(double original) {
        // Climbing spiders barely gate on Y at all: a climb is a straight column (same X/Z), so the horizontal
        // reach already keeps them on the wall. With a big vertical window they advance to the far end of the
        // column right away and just go for it up/down, instead of fussing over each block of the climb. Wide
        // units get the gate relaxed to cover a full MAX_FALL_DROP: the pathfinder now emits multi-block drop
        // nodes (see GridAStar fall pass), so a body perched at the lip of a 2-3 block drop must be able to
        // validate the landing node below it and commit to the fall, instead of spinning on the ledge unable to
        // reach a node it's standing right above. +0.5 so a drop of exactly MAX_FALL_DROP still validates.
        if (reignofnether$isClimbingSpider()) return 16.0;
        return reignofnether$isWideUnit() ? PathfinderConfig.MAX_FALL_DROP + 0.5 : original;
    }

    // The other half of the descent deadlock: vanilla only advances to the next node once the body is within
    // maxDistanceToWaypoint (~bbWidth/2) HORIZONTALLY of it. A wide body perched on a block can never get its
    // centre that close to a lower-forward node while it's still grounded (its base overhangs the lip, so it
    // never falls into reach) - so the path never advances and it spins on the edge. When the next node is a drop
    // below the mob and the mob is roughly over the lip, advance anyway: the follow target jumps to the node
    // BEYOND the drop, giving a forward pull that walks the body off the ledge. One advance per tick (a staircase
    // steps down one node at a time); never past the final target node. Narrow units / climbing spiders excluded.
    @Inject(method = "followThePath", at = @At("HEAD"))
    private void reignofnether$commitDescent(CallbackInfo ci) {
        if (!reignofnether$isWideUnit()) return;
        Path p = this.path;
        if (p == null || p.isDone()) return;
        int idx = p.getNextNodeIndex();
        if (idx >= p.getNodeCount() - 1) return; // keep the final target node; only commit intermediate drops
        Node next = p.getNode(idx);
        if (next.y >= mob.getY() - 0.5) return; // not a drop below the mob's feet
        double dx = (next.x + 0.5) - mob.getX();
        double dz = (next.z + 0.5) - mob.getZ();
        double reach = mob.getBbWidth() / 2.0 + 1.0;
        if (dx * dx + dz * dz <= reach * reach)
            p.advance();
    }
}
