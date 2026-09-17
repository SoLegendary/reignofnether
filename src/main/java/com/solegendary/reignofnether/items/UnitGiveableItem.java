package com.solegendary.reignofnether.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class UnitGiveableItem extends Item {

    private final boolean isFoil;

    public UnitGiveableItem(Properties pProperties) {
        super(pProperties);
        this.isFoil = false;
    }

    public UnitGiveableItem(Properties pProperties, boolean isFoil) {
        super(pProperties);
        this.isFoil = isFoil;
    }

    public boolean isFoil(ItemStack pStack) {
        return isFoil;
    }

    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        return InteractionResult.PASS;
        // TODO: use on unit to give the item to that unit (dropped on death for a regular unit)
    }
}
