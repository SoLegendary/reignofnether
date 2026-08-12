package com.solegendary.reignofnether.hud.custombutton;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ExtraCodecs;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomButtonMappingManager implements ResourceManagerReloadListener {
	
	private static final String MAPPING_PATH = "reignofnether/custom_button_mappings.json";
	
	@Override
	public void onResourceManagerReload(@NotNull ResourceManager pResourceManager) {
		registerMappings(pResourceManager);
	}
	
	public static void registerMappings(@NotNull ResourceManager pResourceManager) {
		try {
			Map<ResourceLocation, Resource> resources = pResourceManager.listResources("reignofnether", (resourceLocation) -> resourceLocation.getPath().equals(MAPPING_PATH));
			if (resources.isEmpty()) {
				ReignOfNether.LOGGER.info("No mapping file found: {}", MAPPING_PATH);
			} else {
				Map<ResourceLocation, List<ResourceLocation>> entity_mappings = new HashMap<>();
				Map<ResourceLocation, List<ResourceLocation>> building_mappings = new HashMap<>();
				for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
					try (Reader reader = entry.getValue().openAsReader()) {
						JsonElement jsonElement = JsonParser.parseReader(reader);
						
						DataResult<MappingData> result = MappingData.CODEC.parse(JsonOps.INSTANCE, jsonElement);
						result.resultOrPartial(error ->
							ReignOfNether.LOGGER.error("Load mapping file error: {}", error)
						).ifPresent(data -> {
							entity_mappings.putAll(data.entities);
							building_mappings.putAll(data.buildings);
						});
					}
				}
				CustomButtonServerEvents.registerButtons(new MappingData(entity_mappings, building_mappings));
			}
		} catch (Exception e) {
			ReignOfNether.LOGGER.info("Mapping file not found: {}", MAPPING_PATH);
		}
	}
	
	public record MappingData(
		Map<ResourceLocation, List<ResourceLocation>> entities,
		Map<ResourceLocation, List<ResourceLocation>> buildings
	) {
		public static final Codec<Map<ResourceLocation, List<ResourceLocation>>> MAPPING_CODEC = Codec.unboundedMap(
			ResourceLocation.CODEC,
			ResourceLocation.CODEC.listOf()
		);
		
		public static final Codec<MappingData> CODEC = ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(instance -> instance.group(
			MAPPING_CODEC.fieldOf("entities").forGetter(MappingData::entities),
			MAPPING_CODEC.fieldOf("buildings").forGetter(MappingData::buildings)
		).apply(instance, MappingData::new)));
	}
}
