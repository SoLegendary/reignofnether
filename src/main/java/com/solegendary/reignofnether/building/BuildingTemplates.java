package com.solegendary.reignofnether.building;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.Optional;

public class BuildingTemplates {

    public static boolean initedStructures = false;
    private static StructureManager manager;

    public static StructureTemplate VILLAGER_HOUSE;
    public static StructureTemplate VILLAGER_TOWER;

    public static void initStructures(ServerLevel level) {
        manager = level.getStructureManager();

        VILLAGER_HOUSE = initStructure("villager_house");
        VILLAGER_HOUSE = initStructure("villager_tower");

        initedStructures = true;
    }

    private static StructureTemplate initStructure(String structureName) {
        Optional<StructureTemplate> optional;
        StructureTemplate retVal;
        try {
            optional = manager.get(new ResourceLocation(structureName));
            retVal = optional.orElse(null);
        } catch (ResourceLocationException resourcelocationexception) {
            retVal = null;
        }
        if (retVal == null)
            throw new Error("Failed to initialise structure: " + structureName);
        return retVal;
    }
}
