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
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Sacrifice extends Ability {

    private static final int CD_MAX = 0;
    private static final int RANGE = 8;

    public Sacrifice(Level level) {
        super(UnitAction.SACRIFICE, level, CD_MAX, RANGE, 0, true, true);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton("Sacrifice",
            new ResourceLocation("minecraft", "textures/item/iron_hoe.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.SACRIFICE,
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.SACRIFICE),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice"),
                    Style.EMPTY.withBold(true)
                ),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice.tooltip1", RANGE),
                    MyRenderer.iconStyle
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.sacrifice.tooltip2"), Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) {

        if (!level.isClientSide() && buildingUsing instanceof SculkCatalystPlacement && targetEntity instanceof Unit unit
            && unit.getOwnerName().equals(buildingUsing.ownerName) && !level.getBlockState(targetEntity.getOnPos())
            .isAir() && !BuildingUtils.isWithinRangeOfMaxedCatalyst(targetEntity)
            && !BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), targetEntity.getOnPos().above())) {

            if (targetEntity.distanceToSqr(Vec3.atCenterOf(buildingUsing.centrePos)) < RANGE * RANGE) {
                targetEntity.kill();
            }
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
            } else if (BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), targetEntity.getOnPos().above())) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.sacrifice.not_spreadable"));
            }
        }
    }
}
