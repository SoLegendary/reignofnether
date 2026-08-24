package com.solegendary.reignofnether.commands.rtsapi.data_accessor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingCommand;
import com.solegendary.reignofnether.commands.rtsapi.argument.BuildingArgument;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

public class BuildingDataAccessor implements DataAccessor {
	
	static final SimpleCommandExceptionType ERROR_NO_BUILDINGS = new SimpleCommandExceptionType(Component.translatable("commands.data.block.invalid"));
	
	public static final Function<String, DataCommands.DataProvider> PROVIDER = (p_139305_) -> new DataCommands.DataProvider() {
		public @NotNull DataAccessor access(@NotNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
			String ownerName;
			try {
				ownerName = StringArgumentType.getString(ctx, "ownerName");
			}
			catch (IllegalArgumentException e) {
				try {
					ownerName = getPlayerName(EntityArgument.getPlayer(ctx, "ownerSelector"));
				} catch (IllegalArgumentException e1) {
					ownerName = null;
				}
			}
			BuildingPlacement building = BuildingArgument.getBuilding(ctx, "targets", ownerName);
			if (building == null) {
				throw BuildingDataAccessor.ERROR_NO_BUILDINGS.create();
			} else {
				return new BuildingDataAccessor(building);
			}
		}
		
		public @NotNull ArgumentBuilder<CommandSourceStack, ?> wrap(@NotNull ArgumentBuilder<CommandSourceStack, ?> p_139316_, @NotNull Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> p_139317_) {
			return p_139316_
				.then(Commands.literal("building")
					.then(Commands.argument("targets", BuildingArgument.building())
						.then(p_139317_.apply(Commands.argument("ownerName", StringArgumentType.string())))
						.then(p_139317_.apply(Commands.argument("ownerSelector", EntityArgument.player())))
						.then(p_139317_.apply(Commands.literal("*")))
					)
				);
		}
	};
	private final BuildingPlacement building;
	
	public BuildingDataAccessor(BuildingPlacement pBuilding) {
		this.building = pBuilding;
	}
	
	private static String getPlayerName(ServerPlayer player) {
		return player.getName().getString();
	}
	
	private static CompoundTag getData(BuildingPlacement building) {
		CompoundTag pCompound = getCompoundTag(building);
		CompoundTag cooldownsCompound = new CompoundTag();
		Object2ObjectArrayMap<Ability, Float> buildingCooldowns = building.cooldowns;
		for (Ability key : buildingCooldowns.keySet()) {
			cooldownsCompound.putFloat(key.action.name(), buildingCooldowns.get(key));
		}
		pCompound.put("cooldowns", cooldownsCompound);
		CompoundTag chargesCompound = new CompoundTag();
		Object2ObjectArrayMap<Ability, Integer> buildingCharges = building.charges;
		for (Ability key : buildingCharges.keySet()) {
			chargesCompound.putInt(key.action.name(), buildingCharges.get(key));
		}
		pCompound.put("charges", chargesCompound);
		return pCompound;
	}
	
	@NotNull
	private static CompoundTag getCompoundTag(BuildingPlacement building) {
		CompoundTag pCompound = getTag(building);
		if (!building.tags.isEmpty()) {
			ListTag listtag = new ListTag();
			
			for(String s : building.tags) {
				listtag.add(StringTag.valueOf(s));
			}
			
			pCompound.put("Tags", listtag);
		}
		
		ListTag listtag = new ListTag();
		{
			if (!building.commands.isEmpty()) {
				for (BuildingCommand command : building.commands) {
					CompoundTag ctag = new CompoundTag();
					ctag.putInt("tickCooldown", command.tickCooldown);
					ctag.putInt("tickCooldownMax", command.tickCooldownMax);
					ctag.putString("commandStr", command.commandStr);
					ctag.putString("condition", command.condition.toString());
					ctag.putInt("index", building.commands.indexOf(command));
					listtag.add(ctag);
				}
			} else {
				CompoundTag ctag = new CompoundTag();
				ctag.putInt("tickCooldown", 0);
				ctag.putInt("tickCooldownMax", 0);
				ctag.putString("commandStr", "demo");
				ctag.putString("condition", "NONE");
				listtag.add(ctag);
			}
			pCompound.put("Commands", listtag);
		}
		return pCompound;
	}
	
	@NotNull
	private static CompoundTag getTag(BuildingPlacement building) {
		CompoundTag pCompound = new CompoundTag();
		pCompound.putBoolean("isBuilt", building.isBuilt);
		pCompound.putInt("baseMsPerBuild", building.baseMsPerBuild);
		pCompound.putFloat("minBlocksPercent", building.minBlocksPercent);
		pCompound.putString("ownerName", building.ownerName);
		pCompound.putInt("scenarioRoleIndex", building.scenarioRoleIndex);
		pCompound.putLong("ticksToExtinguishMax", building.ticksToExtinguishMax);
		pCompound.putLong("ticksToSpawnAnimalsMax", building.ticksToSpawnAnimalsMax);
		pCompound.putInt("maxAnimals", building.maxAnimals);
		pCompound.putInt("animalSpawnBlockRange", building.animalSpawnBlockRange);
		pCompound.putInt("animalSpawnRangeMin", building.animalSpawnRangeMin);
		pCompound.putInt("health", building.getHealth());
		pCompound.putInt("maxHealth", building.getMaxHealth());
		return pCompound;
	}
	
	private static void setData(BuildingPlacement building, CompoundTag pCompound) {
		building.isBuilt = pCompound.getBoolean("isBuilt");
		building.baseMsPerBuild = pCompound.getInt("baseMsPerBuild");
		building.minBlocksPercent = pCompound.getFloat("minBlocksPercent");
		building.ownerName = pCompound.getString("ownerName");
		building.scenarioRoleIndex = pCompound.getInt("scenarioRoleIndex");
		building.ticksToExtinguishMax = pCompound.getLong("ticksToExtinguishMax");
		building.ticksToSpawnAnimalsMax = pCompound.getLong("ticksToSpawnAnimalsMax");
		building.maxAnimals = pCompound.getInt("maxAnimals");
		building.animalSpawnBlockRange = pCompound.getInt("animalSpawnBlockRange");
		building.animalSpawnRangeMin = pCompound.getInt("animalSpawnRangeMin");
		CompoundTag cooldowns = pCompound.getCompound("cooldowns");
		List<Ability> buildingAbilities = building.getAbilities();
		for (Ability key : buildingAbilities) {
			if (cooldowns.contains(key.action.name())) {
				key.cooldownMax = cooldowns.getFloat(key.action.name());
				building.setCooldown(key, cooldowns.getFloat(key.action.name()));
			}
		}
		if (pCompound.contains("Tags", Tag.TAG_LIST)) {
			building.tags.clear();
			ListTag tag = pCompound.getList("Tags", Tag.TAG_STRING);
			int i = Math.min(tag.size(), 1024);
			
			for(int j = 0; j < i; ++j) {
				building.tags.add(tag.getString(j));
			}
		}
		{
			building.commands.clear();
			for (Tag tag : pCompound.getList("Commands", Tag.TAG_COMPOUND))
				building.commands.add(BuildingCommand.getFromNbt((CompoundTag) tag));
		}
	}
	
	@Override
	public @NotNull CompoundTag getData() {
		return getData(building);
	}
	
	@Override
	public void setData(@NotNull CompoundTag pOther) {
		setData(building, pOther);
	}
	
	@Override
	public @NotNull Component getModifiedSuccess() {
		return Component.translatable("commands.data.entity.modified", building.getBuilding().name);
	}
	
	@Override
	public @NotNull Component getPrintSuccess(@NotNull Tag pNbt) {
		return Component.translatable("commands.data.entity.query", building.getBuilding().name, NbtUtils.toPrettyComponent(pNbt));
	}
	
	@Override
	public @NotNull Component getPrintSuccess(NbtPathArgument.@NotNull NbtPath pPath, double pScale, int pValue) {
		return Component.translatable("commands.data.entity.get", pPath, building.getBuilding().name, String.format(Locale.ROOT, "%.2f", pScale), pValue);
	}
}
