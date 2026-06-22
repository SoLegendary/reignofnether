package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// Wide RTS units now follow their path loosely (vanilla following) - safe because wideFits clears a 3x3 around
// every node, so the body has room and doesn't need strict centring (which only caused freezes). The single
// thing we still relax is the VERTICAL advance gate: a wide unit perched on a stair step is held up by its width
// ~1.0 above the next (lower) node and can't satisfy vanilla's `|Y - nodeY| < 1.0`, so it would spin on the
// ledge. Widening it lets the unit commit to the drop and step down.
@Mixin(PathNavigation.class)
public class PathNavigationMixin {

    @Shadow @Final protected Mob mob;

    @Unique
    private boolean reignofnether$isWideUnit() {
        return mob instanceof Unit && mob.getBbWidth() > 1.0f;
    }

    @ModifyConstant(method = "followThePath", constant = @Constant(doubleValue = 1.0))
    private double reignofnether$widenVerticalReachForWideUnits(double original) {
        return reignofnether$isWideUnit() ? 1.5 : original;
    }
}
