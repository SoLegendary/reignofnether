package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.packets.UnitConvertClientboundPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TemporaryMilitiaUnit extends MilitiaUnit {
    public TemporaryMilitiaUnit(EntityType<? extends Vindicator> entityType, Level level) {
        super(entityType, level);
    }

    // convert back to villager when out of range of a TC
    @Override
    public void tick() {
        super.tick();
        if (!shouldDiscard() && !isDeadOrDying()) {
            if (!this.isCaptain && this.tickCount > 100 && this.tickCount % 10 == 0 && !converted &&
                    !level().isClientSide()) {
                if (!inRangeOfTownCentre() && !inRangeOfBellHolder())
                    convertToVillager();
            }
        }
    }

    private boolean inRangeOfTownCentre() {
        BuildingPlacement building = BuildingUtils.findClosestBuilding(level().isClientSide(), this.getEyePosition(),
                (b) -> b.isBuilt && b.ownerName.equals(getOwnerName()) && b.getBuilding() instanceof TownCentre);

        int range = TownCentre.MILITIA_RANGE;

        if (building == null) {
            return false;
        } else {
            Vec3 tcCentre = new Vec3( // ignore vertical distance
                    building.centrePos.getCenter().x(),
                    this.getEyeY(),
                    building.centrePos.getCenter().z()
            );
            return this.getEyePosition().distanceToSqr(tcCentre) <= range * range;
        }
    }

    private boolean inRangeOfBellHolder() {
        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (le instanceof UnitInventory inv && inv.isHoldingActive(UnitItems.BELL_OF_ARMS)) {
                int range = UnitItems.BELL_OF_ARMS_RANGE;
                if (this.getEyePosition().distanceToSqr(le.position()) <= range * range)
                    return true;
            }
        }
        return false;
    }

    @Override
    public void convertToVillager() {
        if (!converted) {
            LivingEntity newEntity = this.convertToUnit(EntityRegistrar.VILLAGER_UNIT.get());
            if (newEntity instanceof VillagerUnit vUnit) {
                if (resourcesSaveData != null) {
                    vUnit.getGatherResourceGoal().saveData = resourcesSaveData;
                    vUnit.getGatherResourceGoal().loadState();
                }
                vUnit.setProfession(this.profession);
                vUnit.isVeteran = this.isVeteran;
                vUnit.farmerExp = this.farmerExp;
                vUnit.lumberjackExp = this.lumberjackExp;
                vUnit.minerExp = this.minerExp;
                vUnit.masonExp = this.masonExp;
                vUnit.hunterExp = this.hunterExp;
                vUnit.chestplate = this.getItemBySlot(EquipmentSlot.CHEST).getItem();
                vUnit.chestplateEnchanted = this.getItemBySlot(EquipmentSlot.CHEST).isEnchanted();
                vUnit.swordEnchanted = this.swordEnchanted;
                vUnit.bowEnchanted = this.bowEnchanted;

                UnitConvertClientboundPacket.syncConvertedUnits(getOwnerName(), List.of(getId()), List.of(newEntity.getId()));
                converted = true;
            }
        }
    }
}
