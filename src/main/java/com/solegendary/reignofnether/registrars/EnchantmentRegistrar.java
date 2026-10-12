package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Handles of the mod's seven enchantments.
 *
 * <p>On 1.20.1 these were Java classes extending {@code Enchantment} and registered through a
 * {@code DeferredRegister}. 1.21.1 made {@code Enchantment} a final record and moved its
 * definition into data, so the enchantments now live in
 * {@code data/reignofnether/enchantment/*.json} and this class only hands out the registry
 * holders.
 *
 * <p>That has one consequence worth spelling out: the enchantment registry is a <em>datapack</em>
 * registry, so unlike blocks or items it has no field on {@code BuiltInRegistries} - it only exists
 * once a {@code RegistryAccess} has been built for it, and every world load builds a new one. So the
 * registry is bound per logical side on each load ({@link #bindServer} / {@link #bindClient}) and
 * holders are resolved from it on every call: a holder kept past the world it came from cannot be
 * saved and cannot be encoded into a packet.
 *
 * <p>Because of that, the seven mod enchantments are {@link Supplier}s rather than plain fields -
 * touching one before the registry exists would fail. Call sites read {@code .get()}, which is the
 * same shape the pre-port code had.
 *
 * <p>Vanilla's own enchantments moved the other way: {@code Enchantments.SHARPNESS} and friends are
 * now {@link ResourceKey}s, so {@link #vanilla(ResourceKey)} is the bridge from a key to the holder
 * that {@code ItemStack#getEnchantmentLevel} and friends want.
 */
public class EnchantmentRegistrar {

    // One registry per logical side. Single player runs both sides in one JVM and they build
    // separate registry accesses, so a server-side item written with the client's holder failed to
    // save ("Element Reference{...} is not valid in current registry set") and failed to be sent
    // ("Failed to encode packet 'clientbound/minecraft:set_equipment'"). Gameplay call sites take
    // the server's registry; a dedicated client, which never runs one, falls back to its own.
    private static volatile Registry<Enchantment> serverRegistry;
    private static volatile Registry<Enchantment> clientRegistry;

    public static final Supplier<Holder<Enchantment>> VIGOR = mod("vigor");
    public static final Supplier<Holder<Enchantment>> BREACHING = mod("breaching");
    public static final Supplier<Holder<Enchantment>> FORTYIFYING = mod("fortifying");
    public static final Supplier<Holder<Enchantment>> MAIMING = mod("maiming");
    public static final Supplier<Holder<Enchantment>> ZEAL = mod("zeal");
    public static final Supplier<Holder<Enchantment>> GUST = mod("gust");
    public static final Supplier<Holder<Enchantment>> LONGSHOT = mod("longshot");

    private EnchantmentRegistrar() { }

    /**
     * Captures the server's enchantment registry from a {@code RegistryAccess} that has it built.
     *
     * <p>Called on every world load, not just the first one: a datapack registry is rebuilt per
     * world, and a holder resolved from a previous instance is dead in the new one. Held onto, such
     * a holder made {@code ClientboundSetEquipmentPacket} fail to encode ("Can't find id for
     * 'Reference{... enchantment / minecraft:thorns}' in map net.minecraft.core.Registry$1") and
     * kicked every player in range, and it made the entity's items unsaveable. So the newest
     * binding always wins, and {@link #holder} resolves against it on every call.
     */
    public static void bindServer(RegistryAccess access) {
        serverRegistry = access.registryOrThrow(Registries.ENCHANTMENT);
    }

    /**
     * Captures the client's enchantment registry. A dedicated client never runs a server, so this
     * is the only binding it has; in single player it is the fallback for holders the server cannot
     * provide (a dedicated client, or code that runs before a world exists).
     */
    public static void bindClient(RegistryAccess access) {
        clientRegistry = access.registryOrThrow(Registries.ENCHANTMENT);
    }

    /** The registry gameplay code should resolve holders from, or null before a world exists. */
    private static @Nullable Registry<Enchantment> current() {
        return serverRegistry != null ? serverRegistry : clientRegistry;
    }

    /**
     * Resolves one of vanilla's {@code Enchantments.*} keys to its holder.
     *
     * <p>A few vanilla enchantments were also renamed on the way: {@code POWER_ARROWS} is now
     * {@code POWER}, {@code PUNCH_ARROWS} is {@code PUNCH}, {@code FLAMING_ARROWS} is
     * {@code FLAME}, {@code MOB_LOOTING} is {@code LOOTING} and {@code BLOCK_EFFICIENCY} is
     * {@code EFFICIENCY}.
     */
    public static Holder<Enchantment> vanilla(ResourceKey<Enchantment> key) {
        return holder(key);
    }

    /**
     * Re-resolves a holder that was captured earlier - typically in a static field built before the
     * current world existed - against the registry in use right now.
     *
     * <p>Static item definitions ({@code UnitItems.MERCHANT_TRIDENT} and friends) bake their
     * enchantments in at class load, so without this a trident enchanted in the first world keeps a
     * dead thorns/looting holder forever and every unit that is handed one disconnects the players
     * it is sent to.
     */
    public static Holder<Enchantment> current(Holder<Enchantment> holder) {
        ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
        Registry<Enchantment> reg = current();
        return key == null || reg == null ? holder : reg.getHolderOrThrow(key);
    }

    /**
     * Like {@link #vanilla(ResourceKey)} but null instead of throwing when no world exists yet.
     *
     * <p>Client-only: the title screen renders before a level is joined, and the client has no
     * enchantment registry at that point, so a caller that only wants the key for a cosmetic effect
     * (an enchanted-looking item, say) has to be able to skip it.
     */
    public static @Nullable Holder<Enchantment> tryVanilla(ResourceKey<Enchantment> key) {
        Registry<Enchantment> reg = current();
        return reg == null ? null : reg.getHolderOrThrow(key);
    }

    /** Kept so the mod constructor stays unchanged; there is nothing to register any more. */
    public static void init(net.neoforged.fml.ModContainer container) { }

    private static Supplier<Holder<Enchantment>> mod(String path) {
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path));
        return () -> holder(key);
    }

    /**
     * Resolves a key against the registry in use right now.
     *
     * <p>Deliberately uncached: a key-to-holder lookup is a hash lookup, while a cached holder
     * outlives the world it came from and takes the item holding it down with it.
     */
    private static Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
        Registry<Enchantment> reg = current();
        if (reg == null)
            throw new IllegalStateException(
                    "Enchantment " + key.location() + " was requested before the enchantment registry"
                            + " was bound; bindServer/bindClient has to run first, and it only"
                            + " can once a world with datapacks exists");
        return reg.getHolderOrThrow(key);
    }
}
