package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AttributeRegistrar {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ReignOfNether.MOD_ID);

    public static final RegistryObject<Attribute> ATTACK_DAMAGE =
            ATTRIBUTES.register("attack_damage",
                    () -> new RangedAttribute("attribute.reignofnether.attack_damage", 1, 0, 999999.0)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> ATTACKS_PER_SECOND =
            ATTRIBUTES.register("attacks_per_second",
                    () -> new RangedAttribute("attribute.reignofnether.attacks_per_second", 0.5, 0, 100.0)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> ATTACK_RANGE =
            ATTRIBUTES.register("attack_range",
                    () -> new RangedAttribute("attribute.reignofnether.attack_range", 10.0, 2, 100.0)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> AGGRO_RANGE =
            ATTRIBUTES.register("aggro_range",
                    () -> new RangedAttribute("attribute.reignofnether.aggro_range", 10.0, 0, 50)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> RANGED_DAMAGE_RESIST =
            ATTRIBUTES.register("ranged_damage_resist",
                    () -> new RangedAttribute("attribute.reignofnether.ranged_damage_resist", 0.0, 0, 1.0)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> MAGIC_DAMAGE_RESIST =
            ATTRIBUTES.register("magic_damage_resist",
                    () -> new RangedAttribute("attribute.reignofnether.magic_damage_resist", 0.0, 0, 1.0)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> BASE_MAX_HEALTH =
            ATTRIBUTES.register("base_max_health",
                    () -> new RangedAttribute("attribute.reignofnether.base_max_health", 100, 1, 999999)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> BASE_MAX_MANA =
            ATTRIBUTES.register("base_max_mana",
                    () -> new RangedAttribute("attribute.reignofnether.base_max_mana", 100, 1, 999999)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> MANA_REGEN_PER_SECOND =
            ATTRIBUTES.register("mana_regen_per_second",
                    () -> new RangedAttribute("attribute.reignofnether.mana_regen_per_second", 0.0, 0, 999999)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> MAX_MANA_BONUS_PER_LEVEL =
            ATTRIBUTES.register("max_mana_bonus_per_level",
                    () -> new RangedAttribute("attribute.reignofnether.max_mana_bonus_per_level", 0.0, 0, 999999)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> MAX_HEALTH_BONUS_PER_LEVEL =
            ATTRIBUTES.register("max_health_bonus_per_level",
                    () -> new RangedAttribute("attribute.reignofnether.max_health_bonus_per_level", 0.0, 0, 999999)
                            .setSyncable(true)
            );

    public static final RegistryObject<Attribute> ATTACK_DAMAGE_BONUS_PER_LEVEL =
            ATTRIBUTES.register("attack_damage_bonus_per_level",
                    () -> new RangedAttribute("attribute.reignofnether.attack_damage_bonus_per_level", 0.0, 0, 999999)
                            .setSyncable(true)
            );

    public static void init(FMLJavaModLoadingContext context) {
        ATTRIBUTES.register(context.getModEventBus());
    }
}
