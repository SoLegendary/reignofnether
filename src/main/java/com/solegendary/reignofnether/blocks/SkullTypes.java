package com.solegendary.reignofnether.blocks;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.SkullBlock;

/**
 * The four mob-head variants the mod adds support for.
 *
 * <p>1.21.1 made {@code SkullBlock.Type} extend {@link StringRepresentable}, so the anonymous
 * implementations the 1.20.1 build used no longer compile. An enum gives each one the
 * serialised name the codec round-trips.
 */
public enum SkullTypes implements SkullBlock.Type {

    STRAY,
    BOGGED,
    DROWNED,
    HUSK;

    public static final Codec<SkullTypes> CODEC = StringRepresentable.fromEnum(SkullTypes::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
