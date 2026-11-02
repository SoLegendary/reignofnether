package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerboundPacket;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnit;
import com.solegendary.reignofnether.unit.units.piglins.GruntUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.ArrayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SandboxClientEvents {

    private static Faction faction = Factions.VILLAGERS;
    public static Relationship relationship = Relationship.OWNED;
    public static SandboxMenuType sandboxMenuType = SandboxMenuType.UNITS;
    public static CustomBuildingSortOption customBuildingSortOption = CustomBuildingSortOption.NAME;

    private static final Minecraft MC = Minecraft.getInstance();

    public static final List<String> UNIT_CHEATS = List.of("operationcwal", "medievalman", "foodforthought", "slipslopslap");
    public static final List<String> BUILDING_CHEATS = List.of("warpten", "modifythephasevariance");
    public static final List<String> CONTROL_CHEATS = List.of("wouldyoukindly");

    public static Faction getFaction() { return faction; }

    public static void setFaction(Faction newFaction) { faction = newFaction; }

    public static String spawnUnitName = "";

    public static boolean isSandboxPlayer(String playerName) {
        return MC.player != null && playerName.equals(MC.player.getName().getString()) &&
                PlayerClientEvents.isRTSPlayer() && ClientGameModeHelper.gameMode == GameMode.SANDBOX;
    }

    public static boolean isSandboxPlayer() {
        return PlayerClientEvents.isRTSPlayer() && ClientGameModeHelper.gameMode == GameMode.SANDBOX;
    }

    public static List<BuildingPlaceButton> getBuildingButtons() {
        return faction.getBuildingButtons();
    }

    public static List<Button> getCustomBuildingButtons() {
        if (CustomBuildingClientEvents.customBuildings.isEmpty()) {
            return List.of(new Button(
                    "Custom building info",
                    Button.itemIconSize,
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/help.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> true,
                    () -> {
                        if (MC.player != null) {
                            MC.player.addItem(new ItemStack(BlockRegistrar.RTS_STRUCTURE_BLOCK.get()));
                        }
                    },
                    null,
                    List.of(
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.tooltip1")),
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.tooltip2")),
                            fcs(""),
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.tooltip3"))
                    )
            ));
        } else {
            List<Button> list = new ArrayList<>();
            for (CustomBuilding cb : CustomBuildingClientEvents.customBuildings) {
                Button buildButton = cb.getBuildButton(null);
                list.add(buildButton);
            }
            return list;
        }
    }

    public static List<UnitSpawnButton> getUnitButtons() {
        return faction.getEntityButtons();
    }

    public static boolean hasCheats(List<String> cheats) {
        return cheats.stream().allMatch(ResearchClient::hasCheat);
    }

    public static void toggleCheats(List<String> cheats) {
        if (MC.player == null)
            return;
        boolean on = hasCheats(cheats);
        String playerName = MC.player.getName().getString();
        for (String cheat : cheats) {
            if (on)
                ResearchServerboundPacket.removeCheat(playerName, cheat);
            else
                ResearchServerboundPacket.addCheat(playerName, cheat);
        }
    }

    public static void cycleCustomBuildingSort(boolean reverse) {
        CustomBuildingSortOption[] options = CustomBuildingSortOption.values();
        customBuildingSortOption = options[Math.floorMod(customBuildingSortOption.ordinal() + (reverse ? -1 : 1), options.length)];
        sortCustomBuildings();
    }

    public static String ownerName(Relationship relationship) {
        return switch (relationship) {
            case NEUTRAL -> "";
            case HOSTILE -> "Enemy";
            default -> MC.player != null ? MC.player.getName().getString() : "";
        };
    }

    public static void sortCustomBuildings() {
        switch (customBuildingSortOption) {
            case NAME -> CustomBuildingClientEvents.customBuildings.sort(Comparator.comparing(b -> b.name));
            case SIZE -> CustomBuildingClientEvents.customBuildings.sort(Comparator.comparing(b -> b.structureSize.getX() * b.structureSize.getY() * b.structureSize.getZ()));
            case FACTION -> CustomBuildingClientEvents.customBuildings.sort(
                    Comparator.comparing((CustomBuilding b) -> b.buildableByVillagers)
                            .thenComparing((CustomBuilding b) -> b.buildableByMonsters)
                            .thenComparing((CustomBuilding b) -> b.buildableByPiglins).reversed());
        }
    }

    public static Button getExitSandboxButton() {
        return new Button(
                "Exit Sandbox Mode",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                PlayerServerboundPacket::resetRTS,
                null,
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.exit1"), true)
                )
        );
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Post evt) {
        if (SandboxPanel.takesWheel())
            SandboxPanel.scroll(evt.getScrollDelta() > 0 ? -1 : 1);
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!OrthoviewClientEvents.isEnabled()) return;

        // prevent clicking behind HUDs
        if (HudClientEvents.isMouseOverAnyButtonOrHud() || MC.player == null) {
            spawnUnitName = "";
            CursorClientEvents.setLeftClickSandboxAction(null);
            return;
        }

        SandboxAction sandboxAction = CursorClientEvents.getLeftClickSandboxAction();
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1 && sandboxAction != null) {

            String ownerName = ownerName(relationship);

            switch (sandboxAction) {
                case SPAWN_UNIT -> SandboxServerboundPacket.spawnUnit(CursorClientEvents.getLeftClickSandboxAction(), ownerName, spawnUnitName, CursorClientEvents.getPreselectedBlockPos());
                case SET_ANCHOR -> SandboxServerboundPacket.setAnchor(CursorClientEvents.getPreselectedBlockPos(), ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits()));
            }

            if (!Keybindings.shiftMod.isDown()) {
                spawnUnitName = "";
                CursorClientEvents.setLeftClickSandboxAction(null);
            }
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            spawnUnitName = "";
            CursorClientEvents.setLeftClickSandboxAction(null);
        }
    }
}
