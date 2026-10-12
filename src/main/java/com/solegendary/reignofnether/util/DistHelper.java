package com.solegendary.reignofnether.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

/**
 * Replacement for Forge's {@code DistExecutor}, which NeoForge 21.1 no longer ships.
 *
 * <p>NeoForge injects the physical {@link Dist} straight into the mod constructor and exposes
 * the running one as {@link FMLEnvironment#dist}, so the whole "call this only on the given
 * side" helper collapses to one comparison.
 *
 * <p>Both Forge spellings are kept: {@code safeRunWhenOn} and {@code unsafeRunWhenOn} differed
 * only in how strictly they checked that the returned {@link Runnable} was safe to reference
 * from the other side, which no longer applies now that the lambda is never stored. Keeping the
 * two names means the ~90 existing call sites (every clientbound packet handler uses
 * {@code unsafeRunWhenOn(Dist.CLIENT, ...)}) stay untouched, and a reader who knows Forge still
 * recognises them for what they are.
 */
public final class DistHelper {

    private DistHelper() { }

    public static void safeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        if (FMLEnvironment.dist == dist) {
            Runnable task = toRun.get();
            if (task != null) task.run();
        }
    }

    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        safeRunWhenOn(dist, toRun);
    }
}