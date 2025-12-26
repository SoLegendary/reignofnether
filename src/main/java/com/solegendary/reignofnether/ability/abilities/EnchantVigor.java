package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.EnchantAbilityServerboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.LibraryPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.List;

public class EnchantVigor extends EnchantAbility {

    private static final UnitAction ENCHANT_ACTION = UnitAction.ENCHANT_VIGOR;
    public static final Enchantment actualEnchantment = Enchantments.BINDING_CURSE;
    public static final int enchantLevel = 1;
    public static final float cooldownMultiplier = 0.70f;

    public EnchantVigor() {
        super(ENCHANT_ACTION, ResourceCosts.ENCHANT_VIGOR);
        this.defaultHotkey = Keybindings.keyT;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof LibraryPlacement)) return null;
        LibraryPlacement library = (LibraryPlacement) placement;
        return new AbilityButton(
                "Vigor Enchantment",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/stick.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == ENCHANT_ACTION || library.autoCastEnchant == this,
                () -> false,
                () -> library.getUpgradeLevel() > 0,
                () -> CursorClientEvents.setLeftClickAction(ENCHANT_ACTION),
                () -> {
                    EnchantAbilityServerboundPacket.setAutocastEnchant(ENCHANT_ACTION, library.originPos);
                    if (library.autoCastEnchant == this)
                        library.autoCastEnchant = null;
                    else
                        library.autoCastEnchant = this;
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.vigor"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.vigor.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.vigor.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.vigor.tooltip3"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        return entity instanceof EvokerUnit && entity.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof BowItem;
    }

    @Override
    public boolean hasAnyEnchant(LivingEntity entity) {
        return !entity.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().isEmpty();
    }

    @Override
    protected boolean hasSameEnchant(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(actualEnchantment);
    }

    @Override
    protected void doEnchant(LivingEntity entity) {
        ItemStack item = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (item != ItemStack.EMPTY) {
            EnchantmentHelper.setEnchantments(new HashMap<>(), item);
            item.enchant(actualEnchantment, enchantLevel);
        }
    }
}
