package com.solegendary.reignofnether.mixin;

import net.minecraft.network.Connection;
import net.minecraftforge.network.filters.VanillaPacketFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VanillaPacketFilter.class)
public interface VanillaPacketFilterAccessor {

    @Invoker("isNecessary")
    boolean invokeIsNecessary(Connection manager);
}