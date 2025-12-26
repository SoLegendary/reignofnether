//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.solegendary.reignofnether.unit.modelling.models;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerHeadModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection.Hat;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class VillagerUnitProfessionLayer<T extends LivingEntity & VillagerDataHolder, M extends EntityModel<T> & VillagerHeadModel> extends RenderLayer<T, M> {
    private static final Int2ObjectMap<ResourceLocation> LEVEL_LOCATIONS = Util.make(new Int2ObjectOpenHashMap<>(), (p_117657_) -> {
        p_117657_.put(1, ResourceLocation.parse("stone"));
        p_117657_.put(2, ResourceLocation.parse("iron"));
        p_117657_.put(3, ResourceLocation.parse("gold"));
        p_117657_.put(4, ResourceLocation.parse("emerald"));
        p_117657_.put(5, ResourceLocation.parse("diamond"));
    });
    private final Object2ObjectMap<VillagerType, VillagerMetaDataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<VillagerProfession, VillagerMetaDataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap<>();
    private final ResourceManager resourceManager;
    private final String path;

    public VillagerUnitProfessionLayer(RenderLayerParent<T, M> pRenderer, ResourceManager pResourceManager, String pPath) {
        super(pRenderer);
        this.resourceManager = pResourceManager;
        this.path = pPath;
    }

    public void render(@NotNull PoseStack pMatrixStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        if (!pLivingEntity.isInvisible()) {
            VillagerData vData = pLivingEntity.getVillagerData();
            VillagerType biomeType = vData.getType();
            VillagerProfession profession = vData.getProfession();
            VillagerMetaDataSection.Hat typeHatData = this.getHatData(this.typeHatCache, "type", BuiltInRegistries.VILLAGER_TYPE, biomeType);
            VillagerMetaDataSection.Hat profHatData = this.getHatData(this.professionHatCache, "profession", BuiltInRegistries.VILLAGER_PROFESSION, profession);
            M vHeadModel = this.getParentModel();
            ((VillagerUnitModel<?>) vHeadModel).getHatRim().visible = (profession == VillagerProfession.FARMER);
            ResourceLocation biomeTypeRL = this.getResourceLocation("type", BuiltInRegistries.VILLAGER_TYPE.getKey(biomeType));
            renderColoredCutoutModel(vHeadModel, biomeTypeRL, pMatrixStack, pBuffer, pPackedLight, pLivingEntity, 1.0F, 1.0F, 1.0F);
            if (profession != VillagerProfession.NONE && !pLivingEntity.isBaby()) {
                ResourceLocation profRL = this.getResourceLocation("profession", BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession));
                renderColoredCutoutModel(vHeadModel, profRL, pMatrixStack, pBuffer, pPackedLight, pLivingEntity, 1.0F, 1.0F, 1.0F);
                if (profession != VillagerProfession.NITWIT) {
                    ResourceLocation profLevelRL = this.getResourceLocation("profession_level", LEVEL_LOCATIONS.get(Mth.clamp(vData.getLevel(), 1, LEVEL_LOCATIONS.size())));
                    renderColoredCutoutModel(vHeadModel, profLevelRL, pMatrixStack, pBuffer, pPackedLight, pLivingEntity, 1.0F, 1.0F, 1.0F);
                }
            }
            ((VillagerUnitModel<?>) vHeadModel).getHatRim().visible = false;
        }
    }

    private ResourceLocation getResourceLocation(String p_117669_, ResourceLocation p_117670_) {
        return ResourceLocation.fromNamespaceAndPath(p_117670_.getNamespace(), "textures/entity/" + this.path + "/" + p_117669_ + "/" + p_117670_.getPath() + ".png");
    }

    public <K> VillagerMetaDataSection.Hat getHatData(Object2ObjectMap<K, VillagerMetaDataSection.Hat> p_117659_, String p_117660_, DefaultedRegistry<K> p_117661_, K p_117662_) {
        return p_117659_.computeIfAbsent(p_117662_, (p_234880_) -> {
            return this.resourceManager.getResource(this.getResourceLocation(p_117660_, p_117661_.getKey(p_117662_))).flatMap((p_234875_) -> {
                try {
                    return p_234875_.metadata().getSection(VillagerMetaDataSection.SERIALIZER).map(VillagerMetaDataSection::getHat);
                } catch (IOException var2) {
                    return Optional.empty();
                }
            }).orElse(Hat.NONE);
        });
    }
}
