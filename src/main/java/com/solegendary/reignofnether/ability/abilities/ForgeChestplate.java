package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Blacksmith;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ForgeChestplate extends Ability {

    public static final UnitAction ACTION = UnitAction.BLACKSMITH_FORGE_CHESTPLATE;
    public static final int RANGE = 12;
    public static final ResourceCost cost = ResourceCosts.BLACKSMITH_FORGE_CHESTPLATE;

    public ForgeChestplate() {
        super(ACTION, 1, RANGE, 0, true, true);
    }

    private boolean canAfford(BuildingPlacement buildingUsing) {
        Resources res = null;
        if (buildingUsing.getLevel().isClientSide()) {
            res = ResourcesClientEvents.getOwnResources();
        } else {
            for (Resources resources : ResourcesServerEvents.resourcesList) {
                if (resources.ownerName.equals(buildingUsing.ownerName)) {
                    res = resources;
                    break;
                }
            }
        }
        if (res != null) {
            return res.food >= cost.food && res.wood >= cost.wood && res.ore >= cost.ore;
        }
        return false;
    }

    private boolean isCorrectUnitAndEquipment(LivingEntity entity) {
        if (!(entity instanceof Unit unit)) {
            return false;
        }
        // Villager military only
        if (!(unit instanceof AttackerUnit)) {
            return false;
        }
        // Must be able to equip
        return entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
    }

    private static ItemStack getChestplateForTier(int tier) {
        if (tier >= 2) {
            return new ItemStack(Items.IRON_CHESTPLATE);
        }
        return new ItemStack(Items.LEATHER_CHESTPLATE);
    }

    private void playSound(Level level, LivingEntity target) {
        level.playLocalSound(
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.ANVIL_USE, target.getSoundSource(),
                0.8F + target.getRandom().nextFloat() * 0.4F,
                target.getRandom().nextFloat() * 0.2F + 0.9F,
                false
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        if (!(placement.getBuilding() instanceof Blacksmith)) {
            return null;
        }

        // Use iron icon once upgraded to tier 2, otherwise leather.
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                "minecraft",
                placement.getUpgradeLevel() >= 2 ? "textures/item/iron_chestplate.png" : "textures/item/leather_chestplate.png"
        );

        return new AbilityButton(
                "Forge Chestplate",
                icon,
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == ACTION,
                () -> placement.getUpgradeLevel() <= 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(ACTION),
                () -> {},
                List.of(
                        net.minecraft.util.FormattedCharSequence.forward(I18n.get("ability.reignofnether.blacksmith_forge_chestplate"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        net.minecraft.util.FormattedCharSequence.forward("", Style.EMPTY),
                        net.minecraft.util.FormattedCharSequence.forward(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.tooltip1"), Style.EMPTY),
                        net.minecraft.util.FormattedCharSequence.forward(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.tooltip2"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) {
        int tier = buildingUsing.getUpgradeLevel();

        if (!level.isClientSide() &&
                targetEntity instanceof Unit unit &&
                unit.getOwnerName().equals(buildingUsing.ownerName) &&
                isCorrectUnitAndEquipment(targetEntity) &&
                tier > 0 &&
                canAfford(buildingUsing) &&
                targetEntity.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) < RANGE * RANGE) {

            targetEntity.setItemSlot(EquipmentSlot.CHEST, getChestplateForTier(tier));
            ResourcesServerEvents.addSubtractResources(new Resources(buildingUsing.ownerName, -cost.food, -cost.wood, -cost.ore));
            setToMaxCooldown(buildingUsing);
            playSound(level, targetEntity);

        } else if (level.isClientSide()) {
            if (!(targetEntity instanceof Unit unit && unit.getOwnerName().equals(buildingUsing.ownerName))) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.error1"));
            } else if (tier <= 0) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.error2"));
            } else if (targetEntity.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) >= RANGE * RANGE) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.error3"));
            } else if (!isCorrectUnitAndEquipment(targetEntity)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.error4"));
            } else if (!canAfford(buildingUsing)) {
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.blacksmith_forge_chestplate.error5"));
            } else {
                setToMaxCooldown(buildingUsing);
                playSound(level, targetEntity);
            }
        }
    }
}


