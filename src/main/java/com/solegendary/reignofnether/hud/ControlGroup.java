package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.getPlayerToEntityRelationship;

// classic RTS control groups that can contain either buildings or units
// ctrl + number to create a group, number to select that group
// groups appear on the HUD as buttons which can be right clicked to be removed
// only owned units/buildings can be placed in a control group

public class ControlGroup {

    // double click/press to centre camera on the first unit/building
    private static final long DOUBLE_CLICK_TIME_MS = 500;
    public long lastClickTime = 0;

    private final ArrayList<LivingEntity> entities = new ArrayList<>();
    private final ArrayList<Building> buildings = new ArrayList<>();
    private Keybinding keybinding = null;

    public ControlGroup() { }

    public int getKey() {
        return this.keybinding.key;
    }

    public void clearAll() {
        this.entities.clear();
        this.buildings.clear();
    }

    public boolean isEmpty() {
        return entities.size() == 0 && buildings.size() == 0;
    }

    // removes any entities/buildings that are no longer being tracked (likely dead/left world)
    public void clean() {
        this.entities.removeIf(e ->
                !UnitClientEvents.getAllUnits().stream().
                map(Entity::getId).toList().contains(e.getId()));
        this.buildings.removeIf(b ->
                !BuildingClientEvents.getBuildings().stream().
                map(b2 -> b2.originPos).toList().contains(b.originPos));
    }

    // assigns selected entities/buildings to this control group
    public void saveFromSelected(Keybinding keybinding) {
        this.clearAll();
        this.keybinding = keybinding;

        ArrayList<LivingEntity> selUnits = UnitClientEvents.getSelectedUnits();
        ArrayList<Building> selBuildings = BuildingClientEvents.getSelectedBuildings();

        if (selUnits.size() > 0 && getPlayerToEntityRelationship(selUnits.get(0)) == Relationship.OWNED) {
            this.entities.addAll(selUnits);
        }
        else if (selBuildings.size() > 0 && BuildingClientEvents.getPlayerToBuildingRelationship(selBuildings.get(0)) == Relationship.OWNED) {
            this.buildings.addAll(selBuildings);
        }
    }

    // selects the control group's assigned entities/buildings
    public void loadToSelected() {
        Player player = Minecraft.getInstance().player;
        boolean doubleClicked = (System.currentTimeMillis() - lastClickTime) < DOUBLE_CLICK_TIME_MS && player != null;

        if (entities.size() > 0) {
            BuildingClientEvents.setSelectedBuildings(new ArrayList<>());
            UnitClientEvents.setSelectedUnits(entities);
            if (doubleClicked)
                OrthoviewClientEvents.centreCameraOnPos(entities.get(0).getX(), entities.get(0).getZ());
        }
        else if (buildings.size() > 0) {
            UnitClientEvents.setSelectedUnits(new ArrayList<>());
            BuildingClientEvents.setSelectedBuildings(buildings);
            if (doubleClicked) {
                BlockPos pos = BuildingUtils.getCentrePos(buildings.get(0).getBlocks());
                OrthoviewClientEvents.centreCameraOnPos(pos.getX(), pos.getZ());
            }
        }
        lastClickTime = System.currentTimeMillis();
    }

    public Button getButton() {

        ResourceLocation icon = null;
        if (this.entities.size() > 0) {
            String unitName = HudClientEvents.getSimpleEntityName(this.entities.get(0));
            icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/" + unitName + ".png");
        }
        else if (this.buildings.size() > 0)
            icon = this.buildings.get(0).icon;

        return new Button(
            "Control Group " + getKey(),
            Button.itemIconSize,
            icon,
            this.keybinding,
            () -> false,
            () -> false,
            () -> true,
            this::loadToSelected,
            this::clearAll,
            List.of(FormattedCharSequence.forward("Control Group " + keybinding.buttonLabel + " (Right click to remove)", Style.EMPTY))
        );
    }
}
