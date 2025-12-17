package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.EnchantAbilityServerboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BlacksmithPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class EnchantIronArmor extends EnchantAbility {

    private static final UnitAction ACTION = UnitAction.ENCHANT_IRON_ARMOR;

    public EnchantIronArmor() {
        super(ACTION, ResourceCosts.ENCHANT_IRON_ARMOR);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof BlacksmithPlacement)) return null;
        BlacksmithPlacement blacksmith = (BlacksmithPlacement) placement;

        return new AbilityButton(
                "Iron Armor",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_chestplate.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == ACTION || blacksmith.autoCastEnchant == this,
                () -> false,
                () -> blacksmith.getUpgradeLevel() > 0,
                () -> CursorClientEvents.setLeftClickAction(ACTION),
                () -> {
                    EnchantAbilityServerboundPacket.setAutocastEnchant(ACTION, blacksmith.originPos);
                    if (blacksmith.autoCastEnchant == this)
                        blacksmith.autoCastEnchant = null;
                    else
                        blacksmith.autoCastEnchant = this;
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.iron_armor"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.iron_armor.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.isEmpty() || chest.getItem() == Items.LEATHER_CHESTPLATE;
    }

    @Override
    public boolean hasAnyEnchant(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() == Items.IRON_CHESTPLATE || 
               chest.getItem() == Items.DIAMOND_CHESTPLATE || 
               chest.getItem() == Items.NETHERITE_CHESTPLATE;
    }

    @Override
    protected boolean hasSameEnchant(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.IRON_CHESTPLATE;
    }

    @Override
    protected void doEnchant(LivingEntity entity) {
        entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
    }
}

