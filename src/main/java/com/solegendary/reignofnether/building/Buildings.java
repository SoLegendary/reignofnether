package com.solegendary.reignofnether.building;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Buildings {

    static Building VILLAGER_HOUSE = new Building("villager_house");
    static Building VILLAGER_TOWER = new Building("villager_tower");

}
