package com.solegendary.reignofnether.hud.custombutton;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber
public class CustomButtonServerEvents {
	
	
	public static final ResourceKey<Registry<CustomButton>> CUSTOM_BUTTON_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "rts_buttons"));
	
	public static final Map<ResourceLocation, CustomButton> customButtons = new HashMap<>();
	public static final Map<EntityType<?>, List<ResourceLocation>> entityMappings = new HashMap<>();
	public static final Map<Building, List<ResourceLocation>> buildingMappings = new HashMap<>();
	public static final ArrayList<ResourceLocation> alwaysRenderButtons = new ArrayList<>();
//	public static final Map<ResourceLocation, CustomButton> customFrozenButtons = new HashMap<>();
	
	public static CustomButton getButton(ResourceLocation id) {
		return customButtons.get(id);
	}
	
	public static void registerButtons(CustomButtonMappingManager.MappingData data) {
		entityMappings.clear();
		buildingMappings.clear();
		alwaysRenderButtons.clear();
		
		Set<ResourceLocation> allocated = new HashSet<>(customButtons.size());
		Map<ResourceLocation, List<ResourceLocation>> entityMappings = new HashMap<>();
		Map<ResourceLocation, List<ResourceLocation>> buildingMappings = new HashMap<>();
		
		for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : data.entities().entrySet()) {
			EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entry.getKey());
			if (entityType != null) {
				ArrayList<ResourceLocation> list = new ArrayList<>(entry.getValue());
				list.retainAll(customButtons.keySet());
				if (!list.isEmpty()) {
					CustomButtonServerEvents.entityMappings.put(entityType, list);
					allocated.addAll(list);
					entityMappings.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : data.buildings().entrySet()) {
			Building building = ReignOfNetherRegistries.BUILDING.get(entry.getKey());
			if (building != null) {
				ArrayList<ResourceLocation> list = new ArrayList<>(entry.getValue());
				list.retainAll(customButtons.keySet());
				if (!list.isEmpty()) {
					CustomButtonServerEvents.buildingMappings.put(building, list);
					allocated.addAll(list);
					buildingMappings.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		for (ResourceLocation button : customButtons.keySet()) {
			if (!allocated.contains(button)) {
				alwaysRenderButtons.add(button);
			}
		}
		syncCustomButtons(entityMappings, buildingMappings);
	}
	
	@SubscribeEvent
	public static void registerButtonMappings(ServerStartedEvent evt) {
		customButtons.clear();
		
		Registry<CustomButton> registry = evt.getServer().registryAccess().registryOrThrow(CustomButtonServerEvents.CUSTOM_BUTTON_REGISTRY_KEY);
		for (CustomButton button : registry) {
			button.id = registry.getKey(button);
			customButtons.put(button.id, button);
		}
	}

	@SubscribeEvent
	public static void onServerAboutToStart(PlayerEvent.PlayerLoggedInEvent evt) {
		MinecraftServer server = evt.getEntity().level().getServer();
		if (server != null) {
			CustomButtonMappingManager.registerMappings(server.getResourceManager());
		}
	}
	
	private static void syncCustomButtons(Map<ResourceLocation, List<ResourceLocation>> entityMappings, Map<ResourceLocation, List<ResourceLocation>> buildingMappings) {
		PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomButtonClientboundPacket(
			(byte) 0,
			null,
			null,
			null,
			0,
			0,
			0,
			null,
			false,
			false,
			false,
			false
		));
		for (CustomButton button : customButtons.values()) {
			PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomButtonClientboundPacket(
				(byte) 1,
				button.id,
				button.name,
				button.iconResource,
				button.OffsetX,
				button.OffsetY,
				button.iconSize,
				null,
				!button.leftClickActions.isEmpty(),
				!button.rightClickActions.isEmpty(),
				button.lightUpOnHover,
				button.isEnabled
			));
		}
		PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomButtonClientboundPacket(
			(byte) 2,
			null,
			null,
			null,
			0,
			0,
			0,
			entityMappings,
			false,
			false,
			false,
			false
		));
		PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomButtonClientboundPacket(
			(byte) 3,
			null,
			null,
			null,
			0,
			0,
			0,
			buildingMappings,
			false,
			false,
			false,
			false
		));
		PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new CustomButtonClientboundPacket(
			(byte) 4,
			null,
			null,
			null,
			0,
			0,
			0,
			Map.of(CustomButtonClientboundPacket.ALWAYS_KEY, alwaysRenderButtons),
			false,
			false,
			false,
			false
		));
	}
}
