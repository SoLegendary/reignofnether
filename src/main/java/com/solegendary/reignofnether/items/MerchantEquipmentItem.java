package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class MerchantEquipmentItem extends UnitItem {

    Predicate<LivingEntity> isCompatibleTarget;

    protected MerchantEquipmentItem(UnitItemBuilder builder, Predicate<LivingEntity> isCompatibleTarget) {
        super(builder
            .type(UnitItemType.UPGRADE)
            .sellValue(50)
            .consumeOnUse()
        );
        this.isCompatibleTarget = isCompatibleTarget;
        this.onUseEntity = this::tryEquip;
    }

    // shared drop-and-equip logic used by all merchant upgrades
    private boolean tryEquip(Unit unit, LivingEntity le) {
        if (!isCompatibleTarget.test(le) || !(le instanceof Unit targetUnit))
            return false;

        Mob mob = (Mob) unit;
        ItemStack itemStack = getNewItemStack();
        ItemEntity itemEntity = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), itemStack);
        mob.level().addFreshEntity(itemEntity);
        itemEntity.tickCount = 100;

        if (Unit.tryPickingUpEquipment(targetUnit, itemEntity))
            return true;

        itemEntity.discard();
        return false;
    }
}