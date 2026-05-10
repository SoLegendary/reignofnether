package com.solegendary.reignofnether.commands.argument;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;

public class BuildingSelector {
	
	public static final BiConsumer<Vec3, List<? extends BuildingPlacement>> ORDER_ARBITRARY = (p_261404_, p_261405_) -> {
	};
	
	private final int maxResults;
	private final Function<Vec3, Vec3> position;
	@Nullable
	private final AABB aabb;
	private final BiConsumer<Vec3, List<? extends BuildingPlacement>> order;
	@Nullable
	private String ownerName;
	@Nullable
	private final String buildingName;
	private final boolean usesSelector;
	private final String rotation;
	
	public BuildingSelector(int pMaxResults, Function<Vec3, Vec3> pPositions, @Nullable AABB pAabb, BiConsumer<Vec3, List<? extends BuildingPlacement>> pOrder, @Nullable String pType, boolean pUsesSelector, String rotation) {
		this.maxResults = pMaxResults;
		this.position = pPositions;
		this.aabb = pAabb;
		this.order = pOrder;
		this.buildingName = pType;
		this.usesSelector = pUsesSelector;
		this.rotation = rotation;
	}
	
	private static boolean intersects(BuildingPlacement placement, AABB aabb) {
		BlockPos bMin = placement.minCorner;
		BlockPos bMax = placement.maxCorner;
		return !(bMax.getX() < aabb.minX || bMin.getX() > aabb.maxX
			|| bMax.getY() < aabb.minY || bMin.getY() > aabb.maxY
			|| bMax.getZ() < aabb.minZ || bMin.getZ() > aabb.maxZ);
	}
	
	private static Rotation parseRotation(String input) throws CommandSyntaxException {
		return switch (input) {
			case "0" -> Rotation.NONE;
			case "90" -> Rotation.CLOCKWISE_90;
			case "180" -> Rotation.CLOCKWISE_180;
			case "270" -> Rotation.COUNTERCLOCKWISE_90;
			default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
				.create("Invalid rotation: " + input);
		};
	}
	
	public int getMaxResults() {
		return this.maxResults;
	}
	
	private void checkPermissions(CommandSourceStack pSource) throws CommandSyntaxException {
		if (this.usesSelector && !ForgeHooks.canUseEntitySelectors(pSource)) {
			throw EntityArgument.ERROR_SELECTORS_NOT_ALLOWED.create();
		}
	}
	
	public BuildingPlacement findSingleBuilding(CommandSourceStack pSource) throws CommandSyntaxException {
		this.checkPermissions(pSource);
		List<? extends BuildingPlacement> list = this.findBuildings(pSource);
		if (list.isEmpty()) {
			throw EntityArgument.NO_ENTITIES_FOUND.create();
		} else if (list.size() > 1) {
			throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
		} else {
			return list.get(0);
		}
	}
	
	public List<? extends BuildingPlacement> findBuildings(CommandSourceStack pSource) throws CommandSyntaxException {
		this.checkPermissions(pSource);
		Vec3 vec3 = this.position.apply(pSource.getPosition());
		{
			List<BuildingPlacement> list = Lists.newArrayList();
			this.addBuildings(this.buildingName, list, vec3);
			
			return this.sortAndLimit(vec3, list);
		}
		
	}
	
	private <T extends BuildingPlacement> List<T> sortAndLimit(Vec3 pPos, List<T> pBuildings) {
		if (pBuildings.size() > 1) {
			this.order.accept(pPos, pBuildings);
		}
		
		return pBuildings.subList(0, Math.min(this.maxResults, pBuildings.size()));
	}
	
	private void addBuildings(String pBuildingName, List<BuildingPlacement> pResult, Vec3 pPos) throws CommandSyntaxException {
		int i = this.getResultLimit();
		if (pResult.size() < i) {
			if (this.aabb != null) {
				getBuildings(pBuildingName, this.aabb.move(pPos), pResult, i);
			} else {
				getBuildings(pBuildingName, pResult, i);
			}
			
		}
	}
	
	private boolean checkOwnerNameAndRotation(BuildingPlacement building) throws CommandSyntaxException {
		return (ownerName == null || Objects.equals(ownerName, building.ownerName)) && ((rotation == null || Objects.equals(parseRotation(rotation), building.rotation)));
	}
	
	public void getBuildings(String pBuildingName, AABB pBounds, List<BuildingPlacement> pOutput, int pMaxResults) throws CommandSyntaxException {
		
		for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
			if (intersects(building, pBounds) && (pBuildingName == null || Objects.equals(building.getBuilding().name, pBuildingName)) && checkOwnerNameAndRotation(building)) {
				pOutput.add(building);
				if (pOutput.size() >= pMaxResults) {
					return;
				}
			}
		}
	}
	
	public void getBuildings(String pBuildingName, List<BuildingPlacement> pOutput, int pMaxResults) throws CommandSyntaxException {
		
		for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
			if ((pBuildingName == null || Objects.equals(building.getBuilding().name, pBuildingName)) && checkOwnerNameAndRotation(building)) {
				pOutput.add(building);
				if (pOutput.size() >= pMaxResults) {
					return;
				}
			}
		}
	}
	
	private int getResultLimit() {
		return this.order == ORDER_ARBITRARY ? this.maxResults : Integer.MAX_VALUE;
	}
	
	public void setOwnerName(@Nullable String ownerName) {
		this.ownerName = ownerName;
	}
}
