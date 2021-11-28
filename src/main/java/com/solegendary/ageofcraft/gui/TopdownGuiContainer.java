package com.solegendary.ageofcraft.gui;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.block.ChestBlock;

/**
 * Class which overrides the inventory GUI with an RTS-like one where the mouse is always visible and objects on screen
 * can be clicked on for selection
 *
 * @author SoLegendary
 */

public class TopdownGuiContainer extends Container {

    public static final ITextComponent TITLE = new TranslationTextComponent("topdowngui_container");

    public static TopdownGuiContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer extraData) {
        return new TopdownGuiContainer(windowID, playerInventory);
    }

    public static IContainerProvider getServerContainerProvider()
    {
        return (id, playerInventory, serverPlayer) -> new TopdownGuiContainer(id, playerInventory);
    }

    protected TopdownGuiContainer(int id, PlayerInventory playerInventory)
    {
        super(TopdownGuiObjects.CONTAINER_TYPE, id);
        if (TopdownGuiObjects.CONTAINER_TYPE == null)
            throw new IllegalStateException("Must initialise TopdownGuiObjects.CONTAINER_TYPE before constructing a ContainerBasic!");
    }

    @Override
    // equivalent of old isWithinUsableDistance() && canInteractWith()
    public boolean stillValid(PlayerEntity p_75145_1_) {
        return true;
    }
}
