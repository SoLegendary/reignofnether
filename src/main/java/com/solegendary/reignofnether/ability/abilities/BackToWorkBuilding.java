package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.LanguageUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

public class BackToWorkBuilding extends Ability {

    private static final int RANGE = TownCentre.MILITIA_RANGE + 5;

    public BackToWorkBuilding() {
        super(
                UnitAction.BACK_TO_WORK_BUILDING,
                0,
                RANGE,
                0,
                false,
                false
        );
        this.defaultHotkey = Keybindings.build;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return new AbilityButton(
                "Back to Work (Building)",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_pickaxe.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> sendUnitCommand(UnitAction.BACK_TO_WORK_BUILDING),
                null,
                List.of(
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("abilities.reignofnether.back_to_work_building"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("abilities.reignofnether.back_to_work_building.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(LanguageUtil.getTranslation("abilities.reignofnether.back_to_work_building.tooltip2"), Style.EMPTY)
                ),
                this,
                placement
        );
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) {
        if (!level.isClientSide()) {
            var villagers = MiscUtil.getEntitiesWithinRange(
                    new Vector3d(buildingUsing.centrePos.getX(), buildingUsing.centrePos.getY(), buildingUsing.centrePos.getZ()),
                    range, VillagerUnit.class, buildingUsing.getLevel());
            for (VillagerUnit villager : villagers) {
                if (villager.getOwnerName().equals(buildingUsing.ownerName)) {
                    villager.callToArmsGoal.stop();
                    Unit.resetBehaviours(villager);
                    villager.getGatherResourceGoal().saveData = villager.getGatherResourceGoal().permSaveData;
                    villager.getGatherResourceGoal().loadState();
                }
            }

            var militias = MiscUtil.getEntitiesWithinRange(
                    new Vector3d(buildingUsing.centrePos.getX(), buildingUsing.centrePos.getY(), buildingUsing.centrePos.getZ()),
                    range, MilitiaUnit.class, buildingUsing.getLevel());
            for (MilitiaUnit militia : militias) {
                if (militia.getOwnerName().equals(buildingUsing.ownerName) && !militia.isCaptain) {
                    militia.convertToVillager();
                }
            }
        }
    }
}
