package com.solegendary.reignofnether;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.entities.models.NecromancerProjectileModel;
import com.solegendary.reignofnether.entities.renderers.ThrowableTntRenderer;
import com.solegendary.reignofnether.entities.renderers.NecromancerProjectileRenderer;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.registrars.*;
import com.solegendary.reignofnether.unit.modelling.models.*;
import com.solegendary.reignofnether.unit.modelling.renderers.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.neutral.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID, bus = Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onBlockColourEvent(RegisterColorHandlersEvent.Block evt) {
        evt.register((bs, blockAndTintGetter, bp, tintIndex) -> {
            int tint = 0xFFFFFF;
            if (bp != null) {
                BuildingPlacement building = BuildingUtils.findBuilding(true, bp);
                if (building instanceof PortalPlacement portal) {
                    switch (portal.getPortalType()) {
                        case CIVILIAN -> tint = 0x00FF00;
                        case MILITARY -> tint = 0xFF0000;
                        case TRANSPORT -> tint = 0x0000FF;
                    }
                }
            }
            return tint;
        }, Blocks.NETHER_PORTAL);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::init);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerEntityRenderer(EntityRegistrar.ZOMBIE_UNIT.get(), ZombieRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ZOMBIE_PIGLIN_UNIT.get(), PiglinUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ZOGLIN_UNIT.get(), ZoglinRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.SKELETON_UNIT.get(), SkeletonRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.HUSK_UNIT.get(), HuskRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.DROWNED_UNIT.get(), DrownedRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.STRAY_UNIT.get(), StrayRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.CREEPER_UNIT.get(), CreeperRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.SPIDER_UNIT.get(), SpiderRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.POISON_SPIDER_UNIT.get(), PoisonSpiderUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.VILLAGER_UNIT.get(), VillagerUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.MILITIA_UNIT.get(), VillagerUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(), ZombieVillagerUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.PILLAGER_UNIT.get(), PillagerUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.VINDICATOR_UNIT.get(), VindicatorUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.IRON_GOLEM_UNIT.get(), IronGolemRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.WITCH_UNIT.get(), WitchRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.EVOKER_UNIT.get(), EvokerUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ENDERMAN_UNIT.get(), EndermanRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.WARDEN_UNIT.get(), WardenRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.RAVAGER_UNIT.get(), RavagerRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.SILVERFISH_UNIT.get(), SilverfishRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.GRUNT_UNIT.get(), PiglinUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.BRUTE_UNIT.get(), PiglinUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.HEADHUNTER_UNIT.get(), PiglinUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.HOGLIN_UNIT.get(), HoglinRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ARMOURED_HOGLIN_UNIT.get(), ArmouredHoglinUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.BLAZE_UNIT.get(), BlazeRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.WITHER_SKELETON_UNIT.get(), WitherSkeletonRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.GHAST_UNIT.get(), GhastUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.MAGMA_CUBE_UNIT.get(), MagmaCubeUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.SLIME_UNIT.get(), SlimeRenderer::new);

        evt.registerEntityRenderer(EntityRegistrar.ROYAL_GUARD_UNIT.get(), RoyalGuardRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ENCHANTER_UNIT.get(), EnchanterRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.NECROMANCER_UNIT.get(), NecromancerRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.WRETCHED_WRAITH_UNIT.get(), WretchedWraithRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.PIGLIN_MERCHANT_UNIT.get(), PiglinMerchantRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.WILDFIRE_UNIT.get(), WildfireRenderer::new);

        evt.registerEntityRenderer(EntityRegistrar.POLAR_BEAR_UNIT.get(), PolarBearRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.GRIZZLY_BEAR_UNIT.get(), GrizzlyBearRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.PANDA_UNIT.get(), PandaRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.WOLF_UNIT.get(), WolfRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.LLAMA_UNIT.get(), LlamaUnitRenderer::new);

        evt.registerEntityRenderer(EntityRegistrar.PHANTOM_SUMMON.get(), PhantomRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.KILLER_RABBIT_UNIT.get(), RabbitRenderer::new);

        evt.registerEntityRenderer(EntityRegistrar.ADJUSTABLE_PRIMED_TNT.get(), TntRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.THROWABLE_TNT_PROJECTILE.get(), ThrowableTntRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get(), ThrownItemRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.NECROMANCER_PROJECTILE.get(), NecromancerProjectileRenderer::new);
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
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> MenuScreens.register(ContainerRegistrar.TOPDOWNGUI_CONTAINER.get(), TopdownGui::new));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VillagerUnitModel.LAYER_LOCATION, VillagerUnitModel::createBodyLayer);
        event.registerLayerDefinition(RoyalGuardModel.LAYER_LOCATION, RoyalGuardModel::createBodyLayer);
        event.registerLayerDefinition(NecromancerModel.LAYER_LOCATION, NecromancerModel::createBodyLayer);
        event.registerLayerDefinition(PiglinMerchantModel.LAYER_LOCATION, PiglinMerchantModel::createBodyLayer);
        event.registerLayerDefinition(ArmouredHoglinUnitModel.LAYER_LOCATION, ArmouredHoglinUnitModel::createBodyLayer);
        event.registerLayerDefinition(NecromancerProjectileModel.LAYER_LOCATION, NecromancerProjectileModel::createBodyLayer);
        event.registerLayerDefinition(EnchanterModel.LAYER_LOCATION, EnchanterModel::createBodyLayer);
        event.registerLayerDefinition(WretchedWraithModel.LAYER_LOCATION, WretchedWraithModel::createBodyLayer);
        event.registerLayerDefinition(WildfireModel.LAYER_LOCATION, WildfireModel::createBodyLayer);
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

