package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.solegendary.reignofnether.building.BuildingClientEvents.getPlayerToBuildingRelationship;
import static com.solegendary.reignofnether.building.BuildingClientEvents.getSelectedBuildings;
import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedEntity;
import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedPlacement;
import static com.solegendary.reignofnether.unit.UnitClientEvents.*;

// classic RTS control groups that can contain either buildings or units
// ctrl + number to create a group, number to select that group
// groups appear on the HUD as buttons which can be right clicked to be removed
// only owned units/buildings can be placed in a control group

public class ControlGroup {

    // double click/press to centre camera on the first unit/building
    private static final long DOUBLE_CLICK_TIME_MS = 500;
    public long lastClickTime = 0;

    public final ArrayList<Integer> entityIds = new ArrayList<>();
    public final ArrayList<BlockPos> buildingBps = new ArrayList<>(); // origin pos
    private Keybinding keybinding = null;
    private ResourceLocation iconRl = null;

    public ControlGroup() { }

    public int getKey() {
        return keybinding != null ? keybinding.key : -1;
    }

    public void clearAll() {
        this.entityIds.clear();
        this.buildingBps.clear();
    }

    public boolean isEmpty() {
        return entityIds.size() == 0 && buildingBps.size() == 0;
    }

    // removes any entities/buildings that are no longer being tracked (likely dead/left world)
    public void clean() {
        var ids = new ArrayList<>();
        for (LivingEntity livingEntity : getAllUnits()) {
            Integer id = livingEntity.getId();
            ids.add(id);
        }
        this.entityIds.removeIf(e -> !ids.contains(e));
        var poses = new ArrayList<>();
        for (BuildingPlacement b2 : BuildingClientEvents.getBuildings()) {
            BlockPos originPos = b2.originPos;
            poses.add(originPos);
        }
        this.buildingBps.removeIf(b -> !poses.contains(b));
    }

    // assigns selected entities/buildings to this control group
    public void saveFromSelected(Keybinding keybinding, boolean newGroup) {
        if (newGroup) {
            this.clearAll();
            this.keybinding = keybinding;
        }
        List<Integer> saveableUnits = new ArrayList<>();
        for (LivingEntity e : getSelectedUnits()) {
            if (((getPlayerToEntityRelationship(e) == Relationship.OWNED || AlliancesClient.canControlAlly(e)) &&
                 (!entityIds.contains(e.getId())))) {
                Integer id = e.getId();
                saveableUnits.add(id);
            }
        }

        List<BlockPos> saveableBuildings = new ArrayList<>();
        for (BuildingPlacement b : getSelectedBuildings()) {
            if (getPlayerToBuildingRelationship(b) == Relationship.OWNED &&
                !buildingBps.contains(b.originPos)) {
                BlockPos originPos = b.originPos;
                saveableBuildings.add(originPos);
            }
        }

        if (!saveableUnits.isEmpty() && (!this.entityIds.isEmpty() || newGroup)) {
            this.entityIds.addAll(saveableUnits);
        }
        else if (!saveableBuildings.isEmpty() && (!this.buildingBps.isEmpty() || newGroup)) {
            this.buildingBps.addAll(saveableBuildings);
        }
        // assign the icon resource (won't update if the front entity/building dies)
        if (newGroup) {
            if (hudSelectedEntity != null) {
                String unitName = MiscUtil.getSimpleEntityName(hudSelectedEntity);
                iconRl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/" + unitName + ".png");
            } else if (hudSelectedPlacement != null) {
                iconRl = hudSelectedPlacement.getBuilding().icon;
            }
        }
    }

    // selects the control group's assigned entities/buildings
    // if it's already selected then just cycle the hudSelectedEntity
    public void loadToSelected() {
        Minecraft MC = Minecraft.getInstance();
        Player player = MC.player;
        if (MC.level == null)
            return;

        boolean doubleClicked = (System.currentTimeMillis() - lastClickTime) < DOUBLE_CLICK_TIME_MS && player != null;

        if (!entityIds.isEmpty()) {
            BuildingClientEvents.clearSelectedBuildings();
            UnitClientEvents.clearSelectedUnits();
            for (int id : entityIds) {
                List<LivingEntity> entities = new ArrayList<>();
                for (LivingEntity e : getAllUnits()) {
                    if (entityIds.contains(e.getId()) && e instanceof Unit) {
                        entities.add(e);
                    }
                }
                if (!entities.isEmpty()) {
                    UnitClientEvents.clearSelectedUnits();
                    for (LivingEntity entity : entities)
                        UnitClientEvents.addSelectedUnit(entity);
                    HudClientEvents.setLowestCdHudEntity();
                    if (doubleClicked)
                        OrthoviewClientEvents.centreCameraOnPos(entities.get(0).position());
                }
            }
        }
        else if (!buildingBps.isEmpty()) {
            UnitClientEvents.clearSelectedUnits();

            BuildingClientEvents.clearSelectedBuildings();
            for (BlockPos bp : buildingBps)
                for (BuildingPlacement building : BuildingClientEvents.getBuildings())
                    if (building.originPos.equals(bp))
                        BuildingClientEvents.addSelectedBuilding(building);

            if (doubleClicked) {
                BlockPos pos = buildingBps.get(0);
                for (BuildingPlacement building : BuildingClientEvents.getBuildings())
                    if (building.originPos.equals(pos))
                        OrthoviewClientEvents.centreCameraOnPos(building.centrePos);
            }
        }

        lastClickTime = System.currentTimeMillis();
    }

    public Button getButton() {
        return new Button(
            "Control Group " + getKey(),
            Button.itemIconSize,
            iconRl == null ? ResourceLocation.parse("") : iconRl,
            this.keybinding,
            () -> false,
            () -> false,
            () -> true,
            () -> {
                if (!Keybindings.shiftMod.isDown())
                    loadToSelected();
            },
            () -> {
                if (Keybindings.shiftMod.isDown())
                    clearAll();
            },
            List.of(FormattedCharSequence.forward(
                I18n.get("hud.control_group.reignofnether.control_group", keybinding.buttonLabel),
                Style.EMPTY
            ))
        );
    }
}
