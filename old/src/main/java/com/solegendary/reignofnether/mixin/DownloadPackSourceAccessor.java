package com.solegendary.reignofnether.mixin;

import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DownloadedPackSource.class)
public interface DownloadPackSourceAccessor {

    @Accessor
    Pack getServerPack();

    @Accessor
    void setServerPack(Pack pack);
}
