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

public class EnchantLeatherArmor extends EnchantAbility {

    private static final UnitAction ACTION = UnitAction.ENCHANT_LEATHER_ARMOR;

    public EnchantLeatherArmor() {
        super(ACTION, ResourceCosts.ENCHANT_LEATHER_ARMOR);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof BlacksmithPlacement)) return null;
        BlacksmithPlacement blacksmith = (BlacksmithPlacement) placement;

        return new AbilityButton(
                "Leather Armor",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/leather_chestplate.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == ACTION || blacksmith.autoCastEnchant == this,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(ACTION),
                () -> {
                    EnchantAbilityServerboundPacket.setAutocastEnchant(ACTION, blacksmith.originPos);
                    if (blacksmith.autoCastEnchant == this)
                        blacksmith.autoCastEnchant = null;
                    else
                        blacksmith.autoCastEnchant = this;
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.leather_armor"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.leather_armor.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    @Override
    public boolean hasAnyEnchant(LivingEntity entity) {
        return !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    @Override
    protected boolean hasSameEnchant(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.LEATHER_CHESTPLATE;
    }

    @Override
    protected void doEnchant(LivingEntity entity) {
        entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
    }
}

