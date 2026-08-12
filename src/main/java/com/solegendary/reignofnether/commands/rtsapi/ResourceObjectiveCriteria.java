package com.solegendary.reignofnether.commands.rtsapi;

import com.solegendary.reignofnether.mixin.ObjectiveCriteriaAccessor;

import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ResourceObjectiveCriteria {
	
	public static final ObjectiveCriteria FOOD =  ObjectiveCriteriaAccessor.registerCustomCriteria("resources.food", true, ObjectiveCriteria.RenderType.INTEGER);
	public static final ObjectiveCriteria WOOD = ObjectiveCriteriaAccessor.registerCustomCriteria("resources.wood", true, ObjectiveCriteria.RenderType.INTEGER);
	public static final ObjectiveCriteria ORE = ObjectiveCriteriaAccessor.registerCustomCriteria("resources.ore", true, ObjectiveCriteria.RenderType.INTEGER);
	public static final ObjectiveCriteria POPULATION = ObjectiveCriteriaAccessor.registerCustomCriteria("resources.population", true, ObjectiveCriteria.RenderType.INTEGER);
	
	public static void init() {
	}
}
