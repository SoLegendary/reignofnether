package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public abstract class EquipAbility extends Ability {

    public static final int CD_MAX = 1;
    public static final int RANGE = 12;
    public final ResourceCost cost;
    public final Item item;
    public final EquipmentSlot equipmentSlot;

    public EquipAbility(UnitAction action, ResourceCost cost, Item item, EquipmentSlot equipmentSlot) {
        super(
                action,
                CD_MAX,
                RANGE,
                0,
                true,
                true
        );
        this.cost = cost;
        this.item = item;
        this.equipmentSlot = equipmentSlot;
    }

    public boolean canAfford(BuildingPlacement buildingUsing) {
        Resources res = null;
        if (buildingUsing.getLevel().isClientSide()) {
            res = ResourcesClientEvents.getOwnResources();
        } else {
            for (Resources resources : ResourcesServerEvents.resourcesList)
                if (resources.ownerName.equals(buildingUsing.ownerName))
                    res = resources;
        }
        if (res != null)
            return (res.food >= cost.food &&
                    res.wood >= cost.wood &&
                    res.ore >= cost.ore);
        return false;
    }

    public boolean isCorrectUnit(LivingEntity entity) {
        return false;
    }

    public boolean hasSameItem(LivingEntity entity) {
        return entity.getItemBySlot(equipmentSlot).getItem() == item;
    }

    public boolean hasItemInSlot(LivingEntity entity) {
        return entity.hasItemInSlot(equipmentSlot);
    }

    protected void doEquip(LivingEntity entity) {
        entity.setItemSlot(equipmentSlot, new ItemStack(item));
    }

    private void playSound(Level level, LivingEntity te) {
        level.playLocalSound(te.getX(), te.getY(), te.getZ(),
                SoundEvents.SMITHING_TABLE_USE, te.getSoundSource(), 1.0F + te.getRandom().nextFloat(),
                te.getRandom().nextFloat() * 0.7F + 0.3F, false);
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity te) {
        BlockPos centreBottom = new BlockPos(buildingUsing.centrePos.getX(), buildingUsing.minCorner.getY(), buildingUsing.centrePos.getZ());

        if (!level.isClientSide() &&
            te instanceof Unit unit &&
            unit.getOwnerName().equals(buildingUsing.ownerName) &&
            isCorrectUnit(te) &&
            !hasSameItem(te) &&
            canAfford(buildingUsing) &&
            te.distanceToSqr(Vec3.atCenterOf(centreBottom)) < RANGE * RANGE) {

            doEquip(te);
            ResourcesServerEvents.addSubtractResources(new Resources(buildingUsing.ownerName, -cost.food, -cost.wood, -cost.ore));
            setToMaxCooldown(buildingUsing);
            playSound(level, te);

        } else if (level.isClientSide()) {
            if (!(te instanceof Unit unit && unit.getOwnerName().equals(buildingUsing.ownerName))) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.equip.error1"));
            } else if (te.distanceToSqr(Vec3.atCenterOf(centreBottom)) >= RANGE * RANGE) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.equip.error2"));
            } else if (!isCorrectUnit(te)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.equip.error3"));
            } else if (hasSameItem(te)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.equip.error4"));
            } else if (!canAfford(buildingUsing)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.equip.error5"));
            } else {
                setToMaxCooldown(buildingUsing);
                playSound(level, te);
            }
        }
    }
}
