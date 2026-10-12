package com.solegendary.reignofnether.hud.custombutton;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.solegendary.reignofnether.ReignOfNether;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.util.LootUtils;

import net.minecraft.commands.CacheableFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.fml.ModContainer;

import java.util.List;

public class CustomButtonActions {

	/**
	 * 1.21.1 deleted {@code IForgeRegistry}/{@code RegistryBuilder}, and with them the mod's own
	 * codec registry. Nothing outside this file needed it: the only consumer of
	 * {@link #getCodec()} is CustomButton's own serialisation, so the four action codecs are
	 * dispatched by hand off {@link CustomButtonAction#type()} instead of being looked up in a
	 * registry.
	 *
	 * <p>The on-disk format is unchanged: the dispatch key is still written as the {@code type}
	 * field, and each variant keeps its own field shape.
	 */
	/**
	 * Anchor for the dispatch below.
	 *
	 * <p>{@code Codec#dispatch} is an instance method, so it needs a receiver - but that receiver
	 * is never asked to read or write an action. Dispatch only reads/writes the {@code type}
	 * discriminator and hands the rest of the map to the per-variant {@link MapCodec}, so the
	 * cheapest codec in the library works as the anchor.
	 */
	private static final Codec<String> DISPATCH_ANCHOR = Codec.STRING;

	public static final Codec<CustomButtonAction> CODEC = DISPATCH_ANCHOR.dispatch(
			"type",
			CustomButtonAction::type,
			CustomButtonActions::codecFor
	);

	@SuppressWarnings("unchecked")
	private static MapCodec<? extends CustomButtonAction> codecFor(String type) {
		return switch (type) {
			case "run_command" -> RunCommandAction.MAP_CODEC;
			case "run_function" -> RunFunctionAction.MAP_CODEC;
			case "experience"   -> ExperienceAction.MAP_CODEC;
			case "loot"         -> LootAction.MAP_CODEC;
			default -> throw new IllegalArgumentException("Unknown custom button action type: " + type);
		};
	}

	public static void init(ModContainer context) { }

	public static Codec<CustomButtonAction> getCodec() {
		return CODEC;
	}
	
	
	public interface CustomButtonAction {

		/** Registry name this action is written as in the saved JSON. */
		String type();

		void execute(Entity entity);

		void execute(BuildingPlacement building);

	}
	
	public record RunCommandAction(  String command) implements CustomButtonAction {
		public static final MapCodec<RunCommandAction> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.STRING.fieldOf("command").forGetter(RunCommandAction::command)
			).apply(instance, RunCommandAction::new)
		);
		
		@Override
		public String type() {
			return "run_command";
		}
		
		@Override
		public void execute(Entity entity) {
			if (entity.getServer() != null)
				entity.getServer().getCommands().performPrefixedCommand(
					entity.createCommandSourceStack(), command
				);
		}
		
		@Override
		public void execute(BuildingPlacement building) {
			if (building.level instanceof ServerLevel level) {
				ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(building.ownerName);
				
				CommandSourceStack source;
				if (player != null) {
					source = player
						.createCommandSourceStack()
						.withPosition(building.minCorner.offset(-1, 0, -1).getCenter())
						.withLevel(level)
						.withSuppressedOutput()
						.withPermission(2)
						.withSource(player);
				} else {
					source = level.getServer()
						.createCommandSourceStack()
						.withPosition(building.minCorner.offset(-1, 0, -1).getCenter())
						.withLevel(level)
						.withPermission(2)
						.withSuppressedOutput();
				}
				level.getServer().getCommands().performPrefixedCommand(source, command);
			}
		}
	}
	
	public record RunFunctionAction(ResourceLocation namespace) implements CustomButtonAction {
		public static final MapCodec<RunFunctionAction> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("function").forGetter(RunFunctionAction::namespace)
			).apply(instance, RunFunctionAction::new)
		);
		
		@Override
		public String type() {
			return "run_function";
		}
		
		@Override
		public void execute(Entity entity) {
			CacheableFunction function = new CacheableFunction(namespace);
			MinecraftServer minecraftserver = entity.getServer();
			if (minecraftserver != null)
				function.get(minecraftserver.getFunctions()).ifPresent((p_289236_) -> minecraftserver.getFunctions().execute(p_289236_, entity.createCommandSourceStack().withSuppressedOutput().withPermission(2)));
		}
		
		@Override
		public void execute(BuildingPlacement building) {
			CacheableFunction function = new CacheableFunction(namespace);
			
			if (building.level instanceof ServerLevel level) {
				
				MinecraftServer minecraftserver = building.level.getServer();
				function.get(minecraftserver.getFunctions()).ifPresent(
					(p_289236_) -> {
						ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(building.ownerName);
						
						CommandSourceStack source;
						if (player != null) {
							source = player
								.createCommandSourceStack()
								.withPosition(building.minCorner.offset(-1, 0, -1).getCenter())
								.withLevel(level)
								.withSuppressedOutput()
								.withPermission(2)
								.withSource(player);
						} else {
							source = level.getServer()
								.createCommandSourceStack()
								.withPosition(building.minCorner.offset(-1, 0, -1).getCenter())
								.withLevel(level)
								.withPermission(2)
								.withSuppressedOutput();
						}
						minecraftserver.getFunctions().execute(
							p_289236_,
							source
						);
					}
				);
			}
		}
	}
	
	public record ExperienceAction(int points, int level) implements CustomButtonAction {
		public static final MapCodec<ExperienceAction> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				Codec.INT.optionalFieldOf("points", 0).forGetter(ExperienceAction::points),
				Codec.INT.optionalFieldOf("levels", 0).forGetter(ExperienceAction::level)
			).apply(instance, ExperienceAction::new)
		);
		
		@Override
		public String type() {
			return "experience";
		}
		
		@Override
		public void execute(Entity entity) {
			if (entity instanceof ServerPlayer player) {
				player.giveExperiencePoints(points);
				player.giveExperienceLevels(level);
			} else {
				int points = this.points;
				while (points > 0) {
					int del = entity.level().random.nextInt(5) + 1;
					ExperienceOrb orb = new ExperienceOrb(entity.level(), entity.getX(), entity.getY(), entity.getZ(), del);
					entity.level().addFreshEntity(orb);
					points -= del;
				}
			}
		}
		
		@Override
		public void execute(BuildingPlacement building) {
			int points = this.points;
			while (points > 0) {
				int del = building.level.random.nextInt(5) + 1;
				ExperienceOrb orb = new ExperienceOrb(building.level, building.centrePos.getX(), building.centrePos.getY(), building.centrePos.getZ(), del);
				building.level.addFreshEntity(orb);
				points -= del;
			}
		}
	}
	
	public record LootAction(List<ResourceLocation> loots) implements CustomButtonAction {
		public static final MapCodec<LootAction> MAP_CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
				ResourceLocation.CODEC.listOf().optionalFieldOf("loots", List.of()).forGetter(LootAction::loots)
			).apply(instance, LootAction::new)
		);
		
		@Override
		public String type() {
			return "loot";
		}
		
		@Override
		public void execute(Entity entity) {
			if (entity.level() instanceof ServerLevel level) {
				LootParams lootparams = (new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, entity).withLuck((entity instanceof ServerPlayer player) ? player.getLuck() : 0.0f).withParameter(LootContextParams.ORIGIN, entity.position()).create(LootContextParamSets.CHEST));
				for (ResourceLocation resourcelocation : loots) {
					for (ItemStack itemstack : LootUtils.getLootTable(level.getServer(), resourcelocation).getRandomItems(lootparams)) {
						if (entity instanceof ServerPlayer player && player.addItem(itemstack.copy())) {
							ReignOfNether.LOGGER.info("give loots {}", itemstack);
							player.inventoryMenu.broadcastChanges();
							player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
						} else {
							ItemEntity itementity = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), itemstack.copy());
							itementity.setNoPickUpDelay();
							level.addFreshEntity(itementity);
						}
					}
				}
			}
		}
		
		@Override
		public void execute(BuildingPlacement building) {
			if (building.level instanceof ServerLevel level) {
				LootParams lootparams = (new LootParams.Builder(level)).withParameter(LootContextParams.ORIGIN, building.centrePos.getCenter()).withParameter(LootContextParams.ORIGIN, building.minCorner.getCenter()).create(LootContextParamSets.CHEST);
				for (ResourceLocation resourcelocation : loots) {
					for (ItemStack itemstack : LootUtils.getLootTable(level.getServer(), resourcelocation).getRandomItems(lootparams)) {
						ItemEntity itementity = new ItemEntity(level, building.centrePos.getX(), building.centrePos.getY(), building.centrePos.getZ(), itemstack);
						itementity.setNoPickUpDelay();
						level.addFreshEntity(itementity);
					}
				}
			}
		}
	}

//	public record ExplodeAction(List<ResourceLocation> loots) implements CustomButtonAction {
//		public static final ResourceLocation TYPE = new ResourceLocation(ReignOfNether.MOD_ID, "explode");
//		public static final MapCodec<ExplodeAction> CODEC = RecordCodecBuilder.mapCodec(
//			instance -> instance.group(
//				ResourceLocation.CODEC.listOf().optionalFieldOf("loots", List.of()).forGetter(ExplodeAction::loots)
//			).apply(instance, ExplodeAction::new)
//		);
//		
//		@Override
//		public MapCodec<? extends CustomButtonAction> codec() {
//			return CODEC;
//		}
//		
//		@Override
//		public void execute(ServerPlayer player) {
//			LootParams lootparams = (new LootParams.Builder(player.serverLevel())).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.ORIGIN, player.position()).withLuck(player.getLuck()).create(LootContextParamSets.SELECTOR);
//			for (ResourceLocation resourcelocation : loots) {
//				for (ItemStack itemstack : player.server.getLootData().getLootTable(resourcelocation).getRandomItems(lootparams)) {
//					if (player.addItem(itemstack)) {
//						player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
//					} else {
//						ItemEntity itementity = player.drop(itemstack, false);
//						if (itementity != null) {
//							itementity.setNoPickUpDelay();
//							itementity.setTarget(player.getUUID());
//						}
//					}
//				}
//			}
//		}
//	}
}
