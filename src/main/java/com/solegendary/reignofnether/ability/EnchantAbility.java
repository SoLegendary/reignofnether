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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class EnchantAbility extends Ability {

    public static final int CD_MAX = 1;
    public static final int RANGE = 12;
    public final ResourceCost cost;

    public EnchantAbility(UnitAction action, ResourceCost cost) {
        super(
                action,
                CD_MAX,
                RANGE,
                0,
                true,
                true
        );
        this.cost = cost;
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

    public boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        return false;
    }

    public boolean hasAnyEnchant(LivingEntity entity) { return false; }

    protected boolean hasSameEnchant(LivingEntity entity) {
        return false;
    }

    protected void doEnchant(LivingEntity entity) { }

    private void playSound(Level level, LivingEntity te) {
        level.playLocalSound(te.getX(), te.getY(), te.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, te.getSoundSource(), 1.0F + te.getRandom().nextFloat(),
                te.getRandom().nextFloat() * 0.7F + 0.3F, false);
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity te) {

        if (!level.isClientSide() &&
            te instanceof Unit unit &&
            unit.getOwnerName().equals(buildingUsing.ownerName) &&
            isCorrectUnitAndEquipment(te) &&
            !hasSameEnchant(te) &&
            canAfford(buildingUsing) &&
            te.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) < RANGE * RANGE) {

            doEnchant(te);
            ResourcesServerEvents.addSubtractResources(new Resources(buildingUsing.ownerName, -cost.food, -cost.wood, -cost.ore));
            setToMaxCooldown(buildingUsing);
            playSound(level, te);

        } else if (level.isClientSide()) {
            if (!(te instanceof Unit unit &&
                    unit.getOwnerName().equals(buildingUsing.ownerName))) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error1"));
            } else if (te.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) >= RANGE * RANGE) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error2"));
            } else if (!isCorrectUnitAndEquipment(te)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error3"));
            } else if (hasSameEnchant(te)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error4"));
            } else if (!canAfford(buildingUsing)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error5"));
            } else {
                setToMaxCooldown(buildingUsing);
                playSound(level, te);
            }
        }
    }
}
