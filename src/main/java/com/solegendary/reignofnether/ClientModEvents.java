package com.solegendary.reignofnether;

import com.solegendary.reignofnether.blocks.GarrisonBlockRenderer;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.entities.models.NecromancerProjectileModel;
import com.solegendary.reignofnether.entities.renderers.ThrowableTntRenderer;
import com.solegendary.reignofnether.entities.renderers.NecromancerProjectileRenderer;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.particles.BigEnchantParticle;
import com.solegendary.reignofnether.particles.BigSoulFlameParticle;
import com.solegendary.reignofnether.registrars.*;
import com.solegendary.reignofnether.unit.modelling.models.*;
import com.solegendary.reignofnether.unit.modelling.renderers.*;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.neutral.*;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

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

        evt.register(
                (state, level, pos, tintIndex) -> 0xE0E0E0,
                BlockRegistrar.WRAITH_SNOW_LAYER.get()
        );
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
        evt.registerEntityRenderer(EntityRegistrar.BOGGED_UNIT.get(), BoggedUnitRenderer::new);
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
        evt.registerEntityRenderer(EntityRegistrar.MARAUDER_UNIT.get(), MarauderRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.HOGLIN_UNIT.get(), HoglinRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.ARMOURED_HOGLIN_UNIT.get(), ArmouredHoglinUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.BLAZE_UNIT.get(), BlazeUnitRenderer::new);
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
        evt.registerEntityRenderer(EntityRegistrar.WRAITH_SNOWBALL.get(), ThrownItemRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.MOLTEN_BOMB_PROJECTILE.get(), (ctx) -> new ThrownItemRenderer<>(ctx, 3.0F, true));
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> {
            MenuScreens.register(ContainerRegistrar.TOPDOWNGUI_CONTAINER.get(), TopdownGui::new);
            ItemBlockRenderTypes.setRenderLayer(
                    BlockRegistrar.UNEXTINGUISHABLE_SOUL_FIRE.get(),
                    RenderType.cutout()
            );
        });
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerBlockEntityRenderer(BlockEntityRegistrar.GARRISON_BLOCK_ENTITY.get(), GarrisonBlockRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VillagerUnitModel.LAYER_LOCATION, VillagerUnitModel::createBodyLayer);
        event.registerLayerDefinition(RoyalGuardModel.LAYER_LOCATION, RoyalGuardModel::createBodyLayer);
        event.registerLayerDefinition(NecromancerModel.LAYER_LOCATION, NecromancerModel::createBodyLayer);
        event.registerLayerDefinition(PiglinMerchantModel.LAYER_LOCATION, PiglinMerchantModel::createBodyLayer);
        event.registerLayerDefinition(MarauderModel.LAYER_LOCATION, MarauderModel::createBodyLayer);
        event.registerLayerDefinition(ArmouredHoglinUnitModel.LAYER_LOCATION, ArmouredHoglinUnitModel::createBodyLayer);
        event.registerLayerDefinition(NecromancerProjectileModel.LAYER_LOCATION, NecromancerProjectileModel::createBodyLayer);
        event.registerLayerDefinition(EnchanterModel.LAYER_LOCATION, EnchanterModel::createBodyLayer);
        event.registerLayerDefinition(WretchedWraithModel.LAYER_LOCATION, WretchedWraithModel::createBodyLayer);
        event.registerLayerDefinition(WildfireModel.LAYER_LOCATION, WildfireModel::createBodyLayer);
        event.registerLayerDefinition(AbstractVillagerUnitRenderer.VILLAGER_ARMOR_OUTER_LAYER, IllagerArmorModel::createOuterArmorLayer);
        event.registerLayerDefinition(AbstractVillagerUnitRenderer.VILLAGER_ARMOR_INNER_LAYER, IllagerArmorModel::createInnerArmorLayer);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerParticles(RegisterParticleProvidersEvent evt) {
        evt.registerSpriteSet(
                ParticleRegistrar.BIG_ENCHANT.get(),
                BigEnchantParticle.Provider::new
        );
        evt.registerSpriteSet(
                ParticleRegistrar.BIG_SOUL_FLAME.get(),
                BigSoulFlameParticle.Provider::new
        );
    }

}

