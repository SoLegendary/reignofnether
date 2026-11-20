package com.solegendary.reignofnether.commands.rtsapi;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Locale;

// One id per thing for /rtsapi and map files. No namespace means reignofnether, so "zombie" is "reignofnether:zombie".
// Units go by their production id; their entity id ("zombie_unit") still works. Custom buildings are "custom:<id>" (their file name in ron/buildings).
public class RonIds {

    public static final String CUSTOM = "custom";

    @Nullable
    public static ResourceLocation parse(String input) {
        String s = input.trim().toLowerCase(Locale.ROOT);
        return ResourceLocation.tryParse(s.contains(":") ? s : ReignOfNether.MOD_ID + ":" + s);
    }

    // a ResourceLocationArgument as typed: getId() would turn "zombie" into "minecraft:zombie"
    public static ResourceLocation arg(CommandContext<CommandSourceStack> ctx, String argName) {
        for (ParsedCommandNode<CommandSourceStack> node : ctx.getNodes())
            if (node.getNode().getName().equals(argName)) {
                ResourceLocation id = parse(node.getRange().get(ctx.getInput()));
                if (id != null)
                    return id;
            }
        return ResourceLocationArgument.getId(ctx, argName);
    }

    @Nullable
    public static ResourceLocation of(ProductionItem item) {
        return ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(item);
    }

    public static ResourceLocation of(Building building) {
        ResourceLocation id = ReignOfNetherRegistries.BUILDING.getKey(building);
        if (id == null && building instanceof CustomBuilding custom)
            return ResourceLocation.fromNamespaceAndPath(CUSTOM, custom.id);
        return id != null ? id : ResourceLocation.fromNamespaceAndPath(CUSTOM,
                building.name.toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9_./-]", ""));
    }

    // built-in or custom building; a custom one also matches without the "custom:" prefix
    @Nullable
    public static Building building(String input) {
        ResourceLocation id = parse(input);
        if (id == null)
            return null;
        Building building = ReignOfNetherRegistries.BUILDING.get(id);
        if (building == null)
            for (CustomBuilding custom : CustomBuildingServerEvents.customBuildings)
                if (of(custom).getPath().equals(id.getPath()))
                    return custom;
        return building;
    }

    // a unit's production id -> its entity id ("reignofnether:zombie" -> "reignofnether:zombie_unit"),
    // an id that's no mod unit or entity -> vanilla ("pig" -> "minecraft:pig"), anything else as is
    public static ResourceLocation entityId(ResourceLocation id) {
        ProductionItem prod = ReignOfNetherRegistries.PRODUCTION_ITEM.get(id);
        if (prod == null && id.getNamespace().equals(ReignOfNether.MOD_ID) && !ForgeRegistries.ENTITY_TYPES.containsKey(id))
            return ResourceLocation.fromNamespaceAndPath(ResourceLocation.DEFAULT_NAMESPACE, id.getPath());
        EntityType<?> type = prod == null ? null : EntityRegistrar.getEntityType(prod.getItemName());
        ResourceLocation entityId = type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
        return entityId != null ? entityId : id;
    }

    // the other way round: "reignofnether:zombie_unit" -> "reignofnether:zombie", anything else as is
    public static ResourceLocation prodId(ResourceLocation id) {
        for (ResourceLocation key : ReignOfNetherRegistries.PRODUCTION_ITEM.keySet())
            if (!key.equals(id) && entityId(key).equals(id))
                return key;
        return id;
    }

    public static final String ACTION = "action";

    // an order as a restrictable id: action:attack, action:move, action:build_repair...
    public static ResourceLocation actionId(UnitAction action) {
        return ResourceLocation.fromNamespaceAndPath(ACTION, action.name().toLowerCase(Locale.ROOT));
    }

    // the canonical production (unit/research), building or order (action:<order>) id, or null if it's none
    @Nullable
    public static ResourceLocation restrictable(String input) {
        ResourceLocation id = parse(input);
        if (id == null)
            return null;
        if (id.getNamespace().equals(ACTION)) {
            for (UnitAction a : UnitAction.values())
                if (actionId(a).equals(id))
                    return id;
            return null;
        }
        id = prodId(id);
        if (ReignOfNetherRegistries.PRODUCTION_ITEM.containsKey(id))
            return id;
        Building building = building(input);
        return building == null ? null : of(building);
    }
}
