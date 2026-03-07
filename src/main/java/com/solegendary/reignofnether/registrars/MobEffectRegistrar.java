package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.stringtemplate.v4.ST;

import java.util.List;


public class MobEffectRegistrar {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ReignOfNether.MOD_ID);

    // Prevents any actions or movement from happening
    public static final RegistryObject<MobEffect> STUN = MOB_EFFECTS.register("stun",  () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0xFFFFFF)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "3fd2b186-9aab-4018-88a9-c150d2f6862c", -1.0f, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "126163a9-2ae8-4aff-96f2-2b15c9c0fb55", -1.0f, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // Similar to STUN but also prevents the mob from being knocked back or pushed
    public static final RegistryObject<MobEffect> FREEZE = MOB_EFFECTS.register("freeze",  () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0x000000)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "aa19485b-837c-4cd5-91f3-440e05d60ba0", -1.0f, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "de7be626-0f4e-4954-9c11-ec5539d40dd7", -1.0f, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // Prevents players from issuing any new commands
    // usually used in conjunction with a force-attack command for a taunt effect, or a move command for a fear effect
    public static final RegistryObject<MobEffect> UNCONTROLLABLE = MOB_EFFECTS.register("uncontrollable", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0xFF0000));

    // Causes a mob to turn into a zombie villager, drowned, zombie piglin or zoglin upon death depending on the unit type
    public static final RegistryObject<MobEffect> ZOMBIE_INFECTED = MOB_EFFECTS.register("zombie_infected", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0x000000));

    public static final RegistryObject<MobEffect> MINOR_MOVEMENT_SPEED = MOB_EFFECTS.register("minor_speed", () -> new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 3402751)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "e6b9720b-131d-4c17-b029-ab8161e8da97", 0.05, AttributeModifier.Operation.MULTIPLY_BASE));

    public static final RegistryObject<MobEffect> MINOR_MOVEMENT_SLOWDOWN = MOB_EFFECTS.register("minor_slowdown", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 3402751)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "eb256076-43e6-470e-a907-434a389da860", -0.05, AttributeModifier.Operation.MULTIPLY_BASE));

    // Increases damage taken by units, and can cause negative armour (does not affect players)
    // The LUCK modifier is just a placeholder
    public static final RegistryObject<MobEffect> DAMAGE_TAKEN_INCREASE = MOB_EFFECTS.register("damage_taken_increase", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 3402751)
            .addAttributeModifier(Attributes.LUCK, "e0772108-0408-4fa3-ad55-f90f5595d610", -0.05, AttributeModifier.Operation.ADDITION));

    // Causes a unit to take 2x fire and magma damage, and spreads it to another friendly mob if it dies
    // Higher amplifiers increase the duration of the passed effect
    public static final RegistryObject<MobEffect> SCORCHING_FIRE = MOB_EFFECTS.register("scorching_fire", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0xFC6203));
    //.addAttributeModifier(Attributes.MOVEMENT_SPEED, "06d218a6-4328-4df1-8263-5dfc23f0c65c", -0.10, AttributeModifier.Operation.MULTIPLY_BASE));

    // ticks fire damage faster
    public static final RegistryObject<MobEffect> INTENSE_HEAT = MOB_EFFECTS.register("intense_heat", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0xFC6203));

    // causes fire tick damage to be doubled and renders fire on entities as blue
    // also causes wildfires and blazes to render as soulfire variants
    public static final RegistryObject<MobEffect> SOULS_AFLAME = MOB_EFFECTS.register("souls_aflame", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 0x4287f5));

    public static final RegistryObject<MobEffect> ATTACK_SLOWDOWN = MOB_EFFECTS.register("attack_slowdown", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 3402751)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "95086ec9-c6cc-41b4-a2ce-9b5cf28011e4", -0.05, AttributeModifier.Operation.MULTIPLY_BASE));

    // Used to give workers temporary efficiency effects (faster gathering and build speed)
    public static final RegistryObject<MobEffect> TEMPORARY_EFFICIENCY = MOB_EFFECTS.register("temporary_efficiency", () -> new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 0x68FF52)
            .addAttributeModifier(Attributes.LUCK, "a417cf34-dc4e-4047-8e14-89eece60c2f8", 0.05, AttributeModifier.Operation.MULTIPLY_BASE));

    public static final RegistryObject<MobEffect> BLOODLUST = MOB_EFFECTS.register("bloodlust", () -> new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 0xFF0000)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "b9da3d7f-da19-4860-9daa-328be5911517", 0.20, AttributeModifier.Operation.MULTIPLY_BASE)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "52f887cb-3048-44fc-b176-98314b5467bd", 0.60, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // Causes a unit to take 1dmg/s per layer of Wraith snow they're standing on
    public static final RegistryObject<MobEffect> FROST_DAMAGE = MOB_EFFECTS.register("frost_damage", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 3402751));

    // Doubles the effect of all unit enchantments (does not affect players)
    public static final RegistryObject<MobEffect> ENCHANTMENT_AMPLIFIER = MOB_EFFECTS.register("enchantment_amplifier", () -> new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 0x000000));

    public static final RegistryObject<MobEffect> DISARM = MOB_EFFECTS.register("disarm", () -> new InstantenousMobEffect(MobEffectCategory.HARMFUL, 3402751)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "a5faf34d-0155-49cf-9c6e-73f16ad41a42", -1.0f, AttributeModifier.Operation.MULTIPLY_TOTAL));


    public static boolean isInterrupt(MobEffect mobEffect) {
        return mobEffect == STUN.get() ||
                mobEffect == UNCONTROLLABLE.get();
    }

    public static void init(FMLJavaModLoadingContext context) {
        MOB_EFFECTS.register(context.getModEventBus());
    }
}