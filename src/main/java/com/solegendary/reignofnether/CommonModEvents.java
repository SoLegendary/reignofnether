package com.solegendary.reignofnether;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.neutral.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID, bus = Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::init);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent evt) {
        evt.put(EntityRegistrar.POLAR_BEAR_UNIT.get(), PolarBearUnit.createAttributes().build());
        evt.put(EntityRegistrar.GRIZZLY_BEAR_UNIT.get(), GrizzlyBearUnit.createAttributes().build());
        evt.put(EntityRegistrar.PANDA_UNIT.get(), PandaUnit.createAttributes().build());
        evt.put(EntityRegistrar.WOLF_UNIT.get(), WolfUnit.createAttributes().build());
        evt.put(EntityRegistrar.LLAMA_UNIT.get(), WolfUnit.createAttributes().build());
        evt.put(EntityRegistrar.PHANTOM_SUMMON.get(), PhantomSummon.createAttributes().build());
        evt.put(EntityRegistrar.ZOMBIE_UNIT.get(), ZombieUnit.createAttributes().build());
        evt.put(EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get(), ZombiePiglinUnit.createAttributes().build());
        evt.put(EntityRegistrar.ZOGLIN_UNIT.get(), ZoglinUnit.createAttributes().build());
        evt.put(EntityRegistrar.SKELETON_UNIT.get(), SkeletonUnit.createAttributes().build());
        evt.put(EntityRegistrar.HUSK_UNIT.get(), HuskUnit.createAttributes().build());
        evt.put(EntityRegistrar.DROWNED_UNIT.get(), DrownedUnit.createAttributes().build());
        evt.put(EntityRegistrar.STRAY_UNIT.get(), StrayUnit.createAttributes().build());
        evt.put(EntityRegistrar.BOGGED_UNIT.get(), BoggedUnit.createAttributes().build());
        evt.put(EntityRegistrar.CREEPER_UNIT.get(), CreeperUnit.createAttributes().build());
        evt.put(EntityRegistrar.SPIDER_UNIT.get(), SpiderUnit.createAttributes().build());
        evt.put(EntityRegistrar.POISON_SPIDER_UNIT.get(), SpiderUnit.createAttributes().build());
        evt.put(EntityRegistrar.VILLAGER_UNIT.get(), VillagerUnit.createAttributes().build());
        evt.put(EntityRegistrar.MILITIA_UNIT.get(), MilitiaUnit.createAttributes().build());
        evt.put(EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(), ZombieVillagerUnit.createAttributes().build());
        evt.put(EntityRegistrar.PILLAGER_UNIT.get(), PillagerUnit.createAttributes().build());
        evt.put(EntityRegistrar.VINDICATOR_UNIT.get(), VindicatorUnit.createAttributes().build());
        evt.put(EntityRegistrar.IRON_GOLEM_UNIT.get(), IronGolemUnit.createAttributes().build());
        evt.put(EntityRegistrar.WITCH_UNIT.get(), WitchUnit.createAttributes().build());
        evt.put(EntityRegistrar.EVOKER_UNIT.get(), EvokerUnit.createAttributes().build());
        evt.put(EntityRegistrar.ENDERMAN_UNIT.get(), EndermanUnit.createAttributes().build());
        evt.put(EntityRegistrar.WARDEN_UNIT.get(), WardenUnit.createAttributes().build());
        evt.put(EntityRegistrar.RAVAGER_UNIT.get(), RavagerUnit.createAttributes().build());
        evt.put(EntityRegistrar.SILVERFISH_UNIT.get(), SilverfishUnit.createAttributes().build());
        evt.put(EntityRegistrar.GRUNT_UNIT.get(), GruntUnit.createAttributes().build());
        evt.put(EntityRegistrar.HEADHUNTER_UNIT.get(), HeadhunterUnit.createAttributes().build());
        evt.put(EntityRegistrar.MARAUDER_UNIT.get(), MarauderUnit.createAttributes().build());
        evt.put(EntityRegistrar.BRUTE_UNIT.get(), BruteUnit.createAttributes().build());
        evt.put(EntityRegistrar.HOGLIN_UNIT.get(), HoglinUnit.createAttributes().build());
        evt.put(EntityRegistrar.ARMOURED_HOGLIN_UNIT.get(), ArmouredHoglinUnit.createAttributes().build());
        evt.put(EntityRegistrar.BLAZE_UNIT.get(), BlazeUnit.createAttributes().build());
        evt.put(EntityRegistrar.WITHER_SKELETON_UNIT.get(), WitherSkeletonUnit.createAttributes().build());
        evt.put(EntityRegistrar.GHAST_UNIT.get(), GhastUnit.createAttributes().build());
        evt.put(EntityRegistrar.MAGMA_CUBE_UNIT.get(), SlimeUnit.createAttributes().build());
        evt.put(EntityRegistrar.SLIME_UNIT.get(), SlimeUnit.createAttributes().build());
        evt.put(EntityRegistrar.ROYAL_GUARD_UNIT.get(), RoyalGuardUnit.createAttributes().build());
        evt.put(EntityRegistrar.ENCHANTER_UNIT.get(), EnchanterUnit.createAttributes().build());
        evt.put(EntityRegistrar.NECROMANCER_UNIT.get(), NecromancerUnit.createAttributes().build());
        evt.put(EntityRegistrar.WRETCHED_WRAITH_UNIT.get(), WretchedWraithUnit.createAttributes().build());
        evt.put(EntityRegistrar.PIGLIN_MERCHANT_UNIT.get(), PiglinMerchantUnit.createAttributes().build());
        evt.put(EntityRegistrar.WILDFIRE_UNIT.get(), WildfireUnit.createAttributes().build());
        evt.put(EntityRegistrar.KILLER_RABBIT_UNIT.get(), KillerRabbitUnit.createAttributes().build());
    }

    @SubscribeEvent
    public static void creativeTabSetup(BuildCreativeModeTabContentsEvent event) {
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.BUILDING_BLOCKS.location()){
            for(Item item : BlockRegistrar.blockItems.get(CreativeModeTabs.BUILDING_BLOCKS)){
                event.accept(item);
            }
        }
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.FUNCTIONAL_BLOCKS.location()){
            for(Item item : BlockRegistrar.blockItems.get(CreativeModeTabs.FUNCTIONAL_BLOCKS)){
                event.accept(item);
            }
        }
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.SPAWN_EGGS.location()){
            event.accept(ItemRegistrar.ZOMBIE_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.HUSK_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.DROWNED_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.ZOMBIE_PIGLIN_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.ZOGLIN_UNIT);
            event.accept(ItemRegistrar.SKELETON_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.STRAY_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.BOGGED_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.CREEPER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.SPIDER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.POISON_SPIDER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.VILLAGER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.MILITIA_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.ZOMBIE_VILLAGER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.VINDICATOR_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.PILLAGER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.IRON_GOLEM_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.WITCH_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.EVOKER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.ENDERMAN_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.WARDEN_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.RAVAGER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.SILVERFISH_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.GRUNT_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.BRUTE_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.HEADHUNTER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.MARAUDER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.HOGLIN_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.BLAZE_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.WITHER_SKELETON_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.GHAST_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.MAGMA_CUBE_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.SLIME_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.ROYAL_GUARD_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.NECROMANCER_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.PIGLIN_MERCHANT_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.WOLF_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.LLAMA_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.PANDA_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.GRIZZLY_BEAR_UNIT_SPAWN_EGG);
            event.accept(ItemRegistrar.POLAR_BEAR_UNIT_SPAWN_EGG);
        }
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.TOOLS_AND_UTILITIES.location()){
            event.accept(ItemRegistrar.THROWABLE_TNT);
            event.accept(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE);
        }
    }
}

