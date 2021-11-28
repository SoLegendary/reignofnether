package com.solegendary.ageofcraft.gui;

import com.solegendary.ageofcraft.gui.TopdownGuiContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder("ageofcraft")
public class TopdownGuiObjects
{
    @ObjectHolder("topdowngui_container")
    public static ContainerType<TopdownGuiContainer> CONTAINER_TYPE = null;
}
