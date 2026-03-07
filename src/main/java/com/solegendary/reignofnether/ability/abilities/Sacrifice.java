package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Sacrifice extends Ability {

    private static final int CD_MAX = 0;
    public static final int RANGE = 8;

    public Sacrifice() {
        super(UnitAction.SACRIFICE, CD_MAX, RANGE, 0, true, true);
        this.autocastEnableAction = UnitAction.SACRIFICE_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.SACRIFICE_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return new AbilityButton("Sacrifice",
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_hoe.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.SACRIFICE || isAutocasting(placement),
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.SACRIFICE),
            () -> toggleAutocast(placement),
            List.of(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice"),
                            Style.EMPTY.withBold(true)
                    ),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice.tooltip1", RANGE),
                            MyRenderer.iconStyle
                    ),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice.tooltip2"), Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("abilities.reignofnether.autocast"), Style.EMPTY),
                    getAutoSacrificeTooltip(placement)
            ),
            this,
            placement
        );
    }

    private FormattedCharSequence getAutoSacrificeTooltip(BuildingPlacement placement) {
        String unitType = "";
        if (placement instanceof SculkCatalystPlacement sculkCat)
            unitType = sculkCat.autoSacrificeUnitType;

        return unitType != null && !unitType.isBlank() && isAutocasting(placement) ?
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice.tooltip4", unitType), Style.EMPTY) :
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice.tooltip3"), Style.EMPTY);
    }

    public boolean isValidTarget(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) {
        return targetEntity instanceof Unit unit && unit.getOwnerName().equals(buildingUsing.ownerName) &&
                !level.getBlockState(targetEntity.getOnPos()).isAir() &&
                !BuildingUtils.isWithinRangeOfMaxedCatalyst(targetEntity) &&
                !BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), targetEntity.getOnPos().above()) &&
                targetEntity.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) < RANGE * RANGE &&
                level.getBlockState(targetEntity.getOnPos()).getBlock() != Blocks.SCULK;
    }

    public String getGenericName(LivingEntity le) {
        String name = MiscUtil.getSimpleEntityName(le).toLowerCase();
        if (name.equals("husk") || name.equals("drowned"))
            return "zombie";
        if (name.equals("stray"))
            return "skeleton";
        if (name.equals("poison spider"))
            return "spider";
        return name;
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) {
        if (!(buildingUsing instanceof SculkCatalystPlacement sculkCat))
            return;

        if (targetEntity instanceof Unit && isAutocasting(buildingUsing)) {
            sculkCat.autoSacrificeUnitType = getGenericName(targetEntity);
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.set_autocast_target", sculkCat.autoSacrificeUnitType));
            return;
        }
        if (!level.isClientSide() && isValidTarget(level, buildingUsing, targetEntity)) {
            targetEntity.kill();
        } else if (level.isClientSide()) {
            if (!(
                targetEntity instanceof Unit unit && unit.getOwnerName().equals(buildingUsing.ownerName)
            )) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.only_own"));
            } else if (targetEntity.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) >= RANGE * RANGE) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.out_of_range"));
            } else if (level.getBlockState(targetEntity.getOnPos()).isAir()) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.in_air"));
            } else if (BuildingUtils.isWithinRangeOfMaxedCatalyst(targetEntity)) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.max_spread"));
            } else if (BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), targetEntity.getOnPos().above()) ||
                        level.getBlockState(targetEntity.getOnPos()).getBlock() == Blocks.SCULK) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.not_spreadable"));
            }
        }
    }

    public void autoSacrifice(BuildingPlacement buildingUsing) {
        if (!(buildingUsing instanceof SculkCatalystPlacement))
            return;

        List<LivingEntity> entities = MiscUtil.getEntitiesWithinRange(Vec3.atCenterOf(buildingUsing.centrePos), range, LivingEntity.class, buildingUsing.level);
        for (LivingEntity le : entities) {
            if (le instanceof Unit && getGenericName(le).equals(((SculkCatalystPlacement) buildingUsing).autoSacrificeUnitType) &&
                isValidTarget(buildingUsing.level, buildingUsing, le)) {
                le.kill();
                return;
            }
        }
    }
}
