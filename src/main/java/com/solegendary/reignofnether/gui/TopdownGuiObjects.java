package com.solegendary.reignofnether.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder("reignofnether")
public class TopdownGuiObjects
{
    @ObjectHolder("topdowngui_container")
    public static MenuType<TopdownGuiContainer> CONTAINER_TYPE = null;
}
