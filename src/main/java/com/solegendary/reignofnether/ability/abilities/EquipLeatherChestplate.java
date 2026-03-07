package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.EnchantEquipAbilityServerboundPacket;
import com.solegendary.reignofnether.ability.EquipAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.BlacksmithPlacement;
import com.solegendary.reignofnether.building.buildings.placements.LibraryPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class EquipLeatherChestplate extends EquipAbility {

    private static final UnitAction EQUIP_ACTION = UnitAction.EQUIP_LEATHER_ARMOR;

    public EquipLeatherChestplate() {
        super(EQUIP_ACTION, ResourceCosts.EQUIP_LEATHER_ARMOR, Items.LEATHER_CHESTPLATE, EquipmentSlot.CHEST);
        this.defaultHotkey = Keybindings.keyT;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement instanceof BlacksmithPlacement blacksmith)) return null;
        return new AbilityButton(
                "Leather Chestplate",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/leather_chestplate.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == EQUIP_ACTION || placement.autocast == this,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(EQUIP_ACTION),
                () -> {
                    EnchantEquipAbilityServerboundPacket.setAutocastEnchantOrEquip(EQUIP_ACTION, blacksmith.originPos);
                    if (blacksmith.autoCastEquip == this)
                        blacksmith.autoCastEquip = null;
                    else
                        blacksmith.autoCastEquip = this;
                },
                List.of(
                        fcs(I18n.get("ability.reignofnether.equip.leather_chestplate"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        fcs("", Style.EMPTY),
                        fcs(I18n.get("ability.reignofnether.equip.leather_chestplate.tooltip1"), Style.EMPTY),
                        fcs("", Style.EMPTY),
                        fcs(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public boolean isCorrectUnit(LivingEntity entity) {
        return List.of(
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.EVOKER_UNIT.get()
        ).contains(entity.getType());
    }
}