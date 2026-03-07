package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.enchantments.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EnchantmentRegistrar {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ReignOfNether.MOD_ID);

    public static final RegistryObject<Enchantment> VIGOR = ENCHANTMENTS.register("vigor", VigorEnchantment::new);

    public static final RegistryObject<Enchantment> BREACHING = ENCHANTMENTS.register("breaching", BreachingEnchantment::new);

    public static final RegistryObject<Enchantment> FORTYIFYING = ENCHANTMENTS.register("fortifying", FortifyingEnchantment::new);

    public static final RegistryObject<Enchantment> MAIMING = ENCHANTMENTS.register("maiming", MaimingEnchantment::new);

    public static final RegistryObject<Enchantment> ZEAL = ENCHANTMENTS.register("zeal", ZealEnchantment::new);

    public static void init(FMLJavaModLoadingContext context) {
        ENCHANTMENTS.register(context.getModEventBus());
    }
}