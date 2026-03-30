package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class GameruleClient {

    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean doLogFalling = true; // only for GUI
    public static boolean neutralAggro = false;
    public static int maxPopulation = ResourceCosts.DEFAULT_MAX_POPULATION;
    public static boolean doUnitGriefing = false; // only for GUI
    public static boolean doPlayerGriefing = true; // only for GUI
    public static boolean improvedPathfinding = true; // only for GUI
    public static double groundYLevel = -320;
    public static double flyingMaxYLevel = 320;
    public static boolean allowBeacons = true;
    public static boolean pvpModesOnly = false;
    public static double beaconWinMinutes = 10;
    public static boolean slantedBuilding = true;
    public static int allowedHeroes = 2;
    public static boolean lockAlliances = false;
    public static boolean scenarioMode = false;

    public static boolean gamerulesMenuOpen = false;

    public static Button getGamerulesButton() {
        return new Button(
                "Game Rules Menu",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/repeating_command_block_back.png"),
                (Keybinding) null,
                () -> gamerulesMenuOpen,
                () -> scenarioMode,
                () -> !StartPosClientEvents.isStarting,
                () -> gamerulesMenuOpen = !gamerulesMenuOpen,
                null,
                MC.player != null && MC.player.hasPermissions(4) ?
                        List.of(fcs(I18n.get("hud.gamerule.reignofnether.menu"))) :
                        List.of(
                                fcs(I18n.get("hud.gamerule.reignofnether.menu")),
                                fcs(I18n.get("hud.gamerule.reignofnether.warn_not_opped"))
                        )
        );
    }

    private static class GameruleBooleanButton extends Button {
        private final String label;
        public GameruleBooleanButton(String label, boolean enabled, Runnable onLeftClick, String tooltip) {
            super(
                    "Boolean Game Rule",
                    10,
                    enabled ? ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png") :
                            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> MC.player != null && MC.player.hasPermissions(4) && !StartPosClientEvents.isStarting,
                    onLeftClick,
                    null,
                    List.of(fcs(tooltip))
            );
            this.label = label;
            this.frameResource = null;
        }
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
            super.render(guiGraphics, x, y, mouseX, mouseY);
            guiGraphics.drawString(MC.font, label,x + 23, y + 7, 0xFFFFFF);
        }
        @Override
        public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY - 10);
        }
    }

    private static class GameruleIntegerButton extends Button {
        private final String label;
        public GameruleIntegerButton(String label, Runnable onLeftClick, Runnable onRightClick, List<FormattedCharSequence> tooltipLines) {
            super(
                "Integer Game Rule",
                10,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> MC.player != null && MC.player.hasPermissions(4) && !StartPosClientEvents.isStarting,
                onLeftClick,
                onRightClick,
                tooltipLines
            );
            this.label = label;
        }
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
            super.render(guiGraphics, x, y, mouseX, mouseY);
            guiGraphics.drawString(MC.font, label,
                    x + 23, y + 7, 0xFFFFFF);
        }
        @Override
        public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY - 10);
        }
    }

    // returns list of rendered buttons
    public static List<Button> renderGamerulesGUI(GuiGraphics guiGraphics, int xTR, int yTR, int mouseX, int mouseY) {
        ArrayList<Button> buttons = new ArrayList<>();
        int width = 145;
        int x = xTR - width - 10;
        int y = yTR - 30;

        buttons.add(new GameruleBooleanButton("doLogFalling", doLogFalling,
            () -> GameruleServerboundPacket.setLogFalling(!doLogFalling),
            I18n.get("commands.reignofnether.gamerule.do_log_falling")
        ));
        buttons.add(new GameruleBooleanButton("neutralAggro", neutralAggro,
            () -> GameruleServerboundPacket.setNeutralAggro(!neutralAggro),
            I18n.get("commands.reignofnether.gamerule.neutral_aggro")
        ));
        buttons.add(new GameruleBooleanButton("doUnitGriefing", doUnitGriefing,
            () -> GameruleServerboundPacket.setUnitGriefing(!doUnitGriefing),
            I18n.get("commands.reignofnether.gamerule.unit_griefing")
        ));
        buttons.add(new GameruleBooleanButton("doPlayerGriefing", doPlayerGriefing,
            () -> GameruleServerboundPacket.setPlayerGriefing(!doPlayerGriefing),
            I18n.get("commands.reignofnether.gamerule.player_griefing")
        ));
        buttons.add(new GameruleBooleanButton("improvedPathfinding", improvedPathfinding,
            () -> GameruleServerboundPacket.setImprovedPathfinding(!improvedPathfinding),
            I18n.get("commands.reignofnether.gamerule.improved_pathfinding")
        ));
        buttons.add(new GameruleBooleanButton("allowBeacons", allowBeacons,
            () -> GameruleServerboundPacket.setAllowBeacons(!allowBeacons),
            I18n.get("commands.reignofnether.gamerule.allow_beacons")
        ));
        buttons.add(new GameruleBooleanButton("pvpModesOnly", pvpModesOnly,
            () -> GameruleServerboundPacket.setPvpModesOnly(!pvpModesOnly),
            I18n.get("commands.reignofnether.gamerule.pvp_modes_only")
        ));
        buttons.add(new GameruleBooleanButton("slantedBuilding", slantedBuilding,
                () -> GameruleServerboundPacket.setSlantedBuilding(!slantedBuilding),
                I18n.get("commands.reignofnether.gamerule.slanted_buildings")
        ));
        buttons.add(new GameruleBooleanButton("lockAlliances", lockAlliances,
                () -> GameruleServerboundPacket.setLockAlliances(!lockAlliances),
                I18n.get("commands.reignofnether.gamerule.lock_alliances")
        ));
        buttons.add(new GameruleIntegerButton("allowedHeroes: " + Math.round(allowedHeroes),
            () -> {
                int value = Math.min(2, allowedHeroes + 1);
                GameruleServerboundPacket.setAllowedHeroes(value);
            },
            () -> {
                int value = Math.max(0, allowedHeroes - 1);
                GameruleServerboundPacket.setAllowedHeroes(value);
            },
            List.of(
                    fcs(I18n.get("commands.reignofnether.gamerule.allowed_heroes")),
                    fcs(I18n.get("hud.gamerule.reignofnether.click"))
            )
        ));
        buttons.add(new GameruleIntegerButton("maxPopulation: " + Math.round(maxPopulation),
            () -> {
                int value = Math.min(10000, maxPopulation + (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setMaxPopulation(value);
            },
            () -> {
                int value = Math.max(0, maxPopulation - (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setMaxPopulation(value);
            },
            List.of(
                fcs(I18n.get("commands.reignofnether.gamerule.max_population")),
                fcs(I18n.get("hud.gamerule.reignofnether.click")),
                fcs(I18n.get("hud.gamerule.reignofnether.shift_click"))
            )
        ));
        buttons.add(new GameruleIntegerButton("groundYLevel: " + Math.round(groundYLevel),
            () -> {
                int value = (int) Math.min(320, groundYLevel + (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setGroundYLevel(value);
            },
            () -> {
                int value = (int) Math.max(-320, groundYLevel - (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setGroundYLevel(value);
            },
            List.of(
                fcs(I18n.get("commands.reignofnether.gamerule.ground_y_level")),
                fcs(I18n.get("hud.gamerule.reignofnether.click")),
                fcs(I18n.get("hud.gamerule.reignofnether.shift_click"))
            )
        ));
        buttons.add(new GameruleIntegerButton("flyingMaxYLevel: " + Math.round(flyingMaxYLevel),
            () -> {
                int value = (int) Math.min(320, flyingMaxYLevel + (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setFlyingMaxYLevel(value);
            },
            () -> {
                int value = (int) Math.max(-320, flyingMaxYLevel - (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setFlyingMaxYLevel(value);
            },
            List.of(
                fcs(I18n.get("commands.reignofnether.gamerule.flying_max_y_level")),
                fcs(I18n.get("hud.gamerule.reignofnether.click")),
                fcs(I18n.get("hud.gamerule.reignofnether.shift_click"))
            )
        ));
        buttons.add(new GameruleIntegerButton("beaconWinMinutes: " + Math.round(beaconWinMinutes),
            () -> {
                int value = (int) Math.min(1000, beaconWinMinutes + (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setBeaconWinMinutes(value);
            },
            () -> {
                int value = (int) Math.max(1, beaconWinMinutes - (Keybindings.shiftMod.isDown() ? 10 : 1));
                GameruleServerboundPacket.setBeaconWinMinutes(value);
            },
            List.of(
                fcs(I18n.get("commands.reignofnether.gamerule.beacon_win_minutes")),
                fcs(I18n.get("hud.gamerule.reignofnether.click")),
                fcs(I18n.get("hud.gamerule.reignofnether.shift_click"))
            )
        ));

        int height = (buttons.size() * 20) - 5;
        MyRenderer.renderFrameWithBg(guiGraphics, x, y, width, height, 0xA0000000);

        int i = 0;
        for (Button button : buttons) {
            button.render(guiGraphics, x + 5, y + 5, mouseX, mouseY);
            y += 18;
        }
        return buttons;
    }
}












