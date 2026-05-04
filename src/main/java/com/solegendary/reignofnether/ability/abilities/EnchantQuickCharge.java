package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.BuildingAbilityServerboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class EnchantQuickCharge extends EnchantAbility {

    private static final UnitAction ENCHANT_ACTION = UnitAction.ENCHANT_QUICKCHARGE;

    public EnchantQuickCharge() {
        super(ENCHANT_ACTION, ResourceCosts.ENCHANT_QUICK_CHARGE, 2, EquipmentSlot.MAINHAND);
        this.defaultHotkey = Keybindings.keyW;
    }

    @Override
    public Enchantment getEnchantment() {
        return Enchantments.QUICK_CHARGE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement.getBuilding() instanceof Library)) return null;
        return new AbilityButton(
                "Quick Charge Enchantment",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/crossbow_arrow.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == ENCHANT_ACTION || placement.getDataStorage().getData(Library.AUTO_CAST_ENCHANT) == this,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(ENCHANT_ACTION),
                () -> {
                    BuildingAbilityServerboundPacket.doAbility(ENCHANT_ACTION, placement.originPos, this.oneClickOneUse);
                    if (placement.getDataStorage().getData(Library.AUTO_CAST_ENCHANT) == this)
                        placement.getDataStorage().setData(Library.AUTO_CAST_ENCHANT, null);
                    else
                        placement.getDataStorage().setData(Library.AUTO_CAST_ENCHANT, this);
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.quickshot"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.quickshot.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.quickshot.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("ability.reignofnether.enchant.quickshot.tooltip3"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        return entity instanceof PillagerUnit &&
                entity.getItemBySlot(equipmentSlot).getItem() instanceof CrossbowItem;
    }

    @Override
    public Enchantment getMutuallyExclusiveEnchant(LivingEntity entity) {
        for (Enchantment enchantment : entity.getItemBySlot(equipmentSlot).getAllEnchantments().keySet()) {
            if (enchantment == Enchantments.MULTISHOT || enchantment == getEnchantment())
                return enchantment;
        }
        return null;
    }
}
