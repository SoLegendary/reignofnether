package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.BuildingAbilityServerboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class EnchantSharpness extends EnchantAbility {

    private static final UnitAction ENCHANT_ACTION = UnitAction.ENCHANT_SHARPNESS;

    public EnchantSharpness() {
        super(ENCHANT_ACTION, ResourceCosts.ENCHANT_SHARPNESS, 2, EquipmentSlot.MAINHAND);
    }

    @Override
    public Enchantment getEnchantment() {
        return Enchantments.SHARPNESS;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement.getBuilding() instanceof Library)) return null;
        return new AbilityButton(
                "Sharpness Enchantment",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_axe.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == ENCHANT_ACTION || placement.getDataStorage().getData(Library.AUTO_CAST_ENCHANT) == this,
                () -> false,
                () -> placement.getUpgradeLevel() > 0,
                () -> CursorClientEvents.setLeftClickAction(ENCHANT_ACTION),
                () -> {
                    BuildingAbilityServerboundPacket.doAbility(ENCHANT_ACTION, placement.originPos);
                    if (placement.getDataStorage().getData(Library.AUTO_CAST_ENCHANT) == this)
                        placement.getDataStorage().setData(Library.AUTO_CAST_ENCHANT, null);
                    else
                        placement.getDataStorage().setData(Library.AUTO_CAST_ENCHANT, this);
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.sharpness"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.sharpness.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.sharpness.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.sharpness.tooltip3"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        return entity instanceof VindicatorUnit &&
                entity.getItemBySlot(equipmentSlot).getItem() instanceof AxeItem;
    }

    @Override
    public Enchantment getMutuallyExclusiveEnchant(LivingEntity entity) {
        for (Enchantment enchantment : entity.getItemBySlot(equipmentSlot).getAllEnchantments().keySet()) {
            if (enchantment == EnchantmentRegistrar.MAIMING.get() || enchantment == getEnchantment())
                return enchantment;
        }
        return null;
    }
}
