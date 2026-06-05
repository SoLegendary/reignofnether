package com.solegendary.reignofnether.hud;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.abilities.CallToArmsUnit;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.attackwarnings.AttackWarningClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.config.ConfigClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.buttons.ActionButtons;
import com.solegendary.reignofnether.hud.buttons.HelperButtons;
import com.solegendary.reignofnether.hud.buttons.StartButtons;
import com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxActionButtons;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxMenuType;
import com.solegendary.reignofnether.scenario.ScenarioClientEvents;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.NonUnitClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.piglins.HoglinUnit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.hud.buttons.HelperButtons.*;
import static com.solegendary.reignofnether.tutorial.TutorialClientEvents.helpButton;
import static com.solegendary.reignofnether.unit.UnitClientEvents.*;
import static com.solegendary.reignofnether.util.MiscUtil.capitaliseAndSpace;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents.diplomacyButton;
import static com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents.observerButton;

public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static String tempMsg = "";
    private static int tempMsgTicksLeft = 0;
    private static final int TEMP_MSG_TICKS_FADE = 50; // ticks left when the msg starts to fade
    private static final int TEMP_MSG_TICKS_MAX = 150; // ticks to show the msg for
    private static final int MAX_BUTTONS_PER_ROW = 6;

    public static final ArrayList<ControlGroup> controlGroups = new ArrayList<>(10);
    public static int lastSelCtrlGroupKey = -1;

    private static final ArrayList<Button> buildingButtons = new ArrayList<>();
    private static final ArrayList<Button> unitButtons = new ArrayList<>();
    private static final ArrayList<Button> productionButtons = new ArrayList<>();
    // buttons which are rendered at the moment in RenderEvent
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();

    // unit that is selected in the list of unit icons
    public static LivingEntity hudSelectedEntity = null;
    // building that is selected in the list of unit icons
    public static BuildingPlacement hudSelectedPlacement = null;
    // classes used to render unit or building portrait (mode, frame, healthbar, stats)
    public static PortraitRendererUnit portraitRendererUnit = new PortraitRendererUnit();
    public static PortraitRendererBuilding portraitRendererBuilding = new PortraitRendererBuilding();

    private static RectZone unitPortraitZone = null;
    private static RectZone buildingPortraitZone = null;

    public static int mouseX = 0;
    public static int mouseY = 0;
    private static int mouseLeftDownX = 0;
    private static int mouseLeftDownY = 0;

    private final static int iconBgColour = 0x64000000;
    private final static int frameBgColour = 0xA0000000;

    private static final ArrayList<RectZone> hudZones = new ArrayList<>();

    private static boolean showPreselectedBlockInfo = true;

    public static void setLowestCdHudEntity() {
        if (UnitClientEvents.getSelectedUnits().isEmpty() || hudSelectedEntity == null) {
            return;
        }

        List<Pair<LivingEntity, Float>> pairs = new ArrayList<>();
        for (LivingEntity livingEntity : getSelectedUnits()) {
            float totalCd = 0;
            if (livingEntity instanceof Unit unit) {
                for (Ability ability : unit.getAbilities().get()) {
                    totalCd += ability.getCooldown(unit);
                    if (ability.isCasting(unit))
                        totalCd += 10;
                }
            }
            Pair<LivingEntity, Float> apply = new Pair<>(livingEntity, totalCd);
            String str1 = getModifiedEntityName(apply.getFirst());
            String str2 = getModifiedEntityName(hudSelectedEntity);
            if (str1.equals(str2)) {
                pairs.add(apply);
            }
        }
        pairs.sort(Comparator.comparing(Pair::getSecond));

        if (!pairs.isEmpty())
            setHudSelectedEntity(pairs.get(0).getFirst());
    }

    public static void setHudSelectedEntity(LivingEntity entity) {
        hudSelectedEntity = entity;
    }


    // not to be used for resource paths
    public static String getModifiedEntityName(LivingEntity entity) {
        if (entity == null)
            return "";

        String name = MiscUtil.getSimpleEntityName(entity);

        if (entity.isBaby())
            name = I18n.get("entity.reignofnether.reignofnether.baby") + " " + name;

        if (!(entity instanceof Unit))
            return name.toLowerCase();

        if (entity instanceof MilitiaUnit militiaUnit && militiaUnit.isUsingBow()) {
            name = I18n.get("entity.reignofnether.militia_unit_archer");
        }
        ItemStack itemStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (itemStack.getItem() instanceof BannerItem) {
            name += " " + I18n.get("entity.reignofnether.captain");
        }
        if (entity.getPassengers().size() == 1) {
            Entity passenger = entity.getPassengers().get(0);
            if (entity instanceof RavagerUnit && passenger instanceof PillagerUnit) {
                name = I18n.get("entity.reignofnether.ravager_unit_artillery");
            } else if (entity instanceof PoisonSpiderUnit && (
                    passenger instanceof SkeletonUnit || passenger instanceof StrayUnit
            )) {
                name = I18n.get("entity.reignofnether.poison_spider_unit_jockey");
            } else if (entity instanceof SpiderUnit && (
                passenger instanceof SkeletonUnit || passenger instanceof StrayUnit
            )) {
                name = I18n.get("entity.reignofnether.spider_unit_jockey");
            }else if (entity instanceof HoglinUnit && passenger instanceof HeadhunterUnit) {
                name = I18n.get("entity.reignofnether.hoglin_unit_rider");
            } else {
                String pName = MiscUtil.getSimpleEntityName(entity.getPassengers().get(0)).replace("_", " ");
                String nameCap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                name += " & " + nameCap;
            }
        }
        if (entity instanceof VillagerUnit vUnit) {
            switch (vUnit.getUnitProfession()) {
                case FARMER -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_farmer");
                    else
                        name = I18n.get("units.reignofnether.farmer");
                }
                case LUMBERJACK -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_lumberjack");
                    else
                        name = I18n.get("units.reignofnether.lumberjack");
                }
                case MINER -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_miner");
                    else
                        name = I18n.get("units.reignofnether.miner");
                }
                case MASON -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_mason");
                    else
                        name = I18n.get("units.reignofnether.mason");
                }
                case HUNTER -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_hunter");
                    else
                        name = I18n.get("units.reignofnether.hunter");
                }
                default -> name = I18n.get("entity.reignofnether.villager_unit");
            }
        }
        if (entity instanceof CreeperUnit cUnit && cUnit.isPowered()) {
            name = I18n.get("entity.reignofnether.charged_creeper");
        }
        return name;
    }

    public static void showTemporaryMessage(String msg) {
        showTemporaryMessage(msg, TEMP_MSG_TICKS_MAX);
    }

    public static void showTemporaryMessage(String msg, int ticks) {
        tempMsgTicksLeft = ticks;
        tempMsg = msg;
    }

    public static void removeFromControlGroups(int entityId) {
        for (ControlGroup controlGroup : controlGroups)
            controlGroup.entityIds.removeIf(id -> id == entityId);
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || !(evt.getScreen() instanceof TopdownGui)) {
            return;
        }
        if (MC.level == null) {
            return;
        }

        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();

        // where to start drawing the centre hud (from left to right: portrait, stats, unit icon buttons)
        int hudStartingXPos = Button.DEFAULT_ICON_FRAME_SIZE * 6 + (Button.DEFAULT_ICON_FRAME_SIZE / 2);

        ArrayList<LivingEntity> selUnits = UnitClientEvents.getSortedSelectedUnits();
        ArrayList<BuildingPlacement> selBuildings = BuildingClientEvents.getSelectedBuildings();

        // create all the unit buttons for this frame
        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int iconSize = 14;
        int iconFrameSize = Button.DEFAULT_ICON_FRAME_SIZE;

        // screenWidth ranges roughly between 440-540
        int buttonsPerRow = (int) Math.ceil((float) (screenWidth - 340) / iconFrameSize);
        buttonsPerRow = Math.min(buttonsPerRow, 8);
        buttonsPerRow = Math.max(buttonsPerRow, 4);

        int buildingProdRows = 0;

        buildingButtons.clear();
        unitButtons.clear();
        productionButtons.clear();
        renderedButtons.clear();
        hudZones.clear();
        unitPortraitZone = null;
        buildingPortraitZone = null;

        int blitX = hudStartingXPos;
        int blitY = MC.getWindow().getGuiScaledHeight();
        int blitXStart = blitX;

        // assign hudSelectedBuilding like hudSelectedUnit in onRenderLiving
        if (selBuildings.size() <= 0) {
            hudSelectedPlacement = null;
        } else if (hudSelectedPlacement == null || selBuildings.size() == 1
            || !selBuildings.contains(hudSelectedPlacement)) {
            hudSelectedPlacement = selBuildings.get(0);
        }

        if (hudSelectedPlacement != null) {
            boolean hudSelBuildingOwned =
                BuildingClientEvents.getPlayerToBuildingRelationship(hudSelectedPlacement) == Relationship.OWNED ||
                        SandboxClientEvents.isSandboxPlayer();
                        //AlliancesClient.canControlAlly(hudSelectedPlacement.ownerName) ||

            // -----------------
            // Building portrait
            // -----------------
            blitY -= portraitRendererBuilding.frameHeight;

            buildingPortraitZone = portraitRendererBuilding.render(evt.getGuiGraphics(),
                blitX,
                blitY,
                hudSelectedPlacement
            );
            hudZones.add(buildingPortraitZone);

            blitX += portraitRendererBuilding.frameWidth + 10;

            blitXStart = blitX + 20;


            // ---------------------------
            // Multiple selected buildings
            // ---------------------------
            for (BuildingPlacement building : selBuildings) {
                if (hudSelBuildingOwned && buildingButtons.size() < (buttonsPerRow * 2)) {
                    String name;
                    if (building.getBuilding() instanceof CustomBuilding customBuilding) {
                        name = customBuilding.name;
                    } else {
                        name = ReignOfNetherRegistries.BUILDING.getKey(building.getBuilding()).toString();
                    }

                    buildingButtons.add(new ButtonBuilder(name)
                        .iconSize(iconSize)
                        .iconResource(building.getBuilding().icon)
                        .isSelected(() -> hudSelectedPlacement.getBuilding() == building.getBuilding())
                        .onLeftClick(() -> {
                            // click to select this unit type as a group
                            if (hudSelectedPlacement.getBuilding() == building.getBuilding()) {
                                BuildingClientEvents.clearSelectedBuildings();
                                BuildingClientEvents.addSelectedBuilding(building);
                            } else { // select this one specific unit
                                hudSelectedPlacement = building;
                            }
                        })
                        .build()
                    );
                }
            }

            if (buildingButtons.size() >= 2) {
                blitX += 20;
                blitY += 6;
                // background frame
                hudZones.add(MyRenderer.renderFrameWithBg(evt.getGuiGraphics(),
                    blitX - 5,
                    blitY - 10,
                    iconFrameSize * buttonsPerRow + 10,
                    iconFrameSize * 2 + 20,
                    frameBgColour
                ));

                int buttonsRendered = 0;
                for (Button buildingButton : buildingButtons) {
                    // replace last icon with a +X number of buildings icon and hover tooltip for what those
                    // buildings are
                    if (buttonsRendered >= (buttonsPerRow * 2) - 1 && selBuildings.size() > (buttonsPerRow * 2)) {
                        int numExtraBuildings = selBuildings.size() - (buttonsPerRow * 2) + 1;
                        RectZone plusBuildingsZone = MyRenderer.renderIconFrameWithBg(evt.getGuiGraphics(),
                            buildingButton.frameResource,
                            blitX,
                            blitY,
                            iconFrameSize,
                            iconBgColour
                        );
                        evt.getGuiGraphics().drawCenteredString(
                            MC.font,
                            "+" + numExtraBuildings,
                            blitX + iconFrameSize / 2,
                            blitY + 8,
                            0xFFFFFF
                        );

                        if (plusBuildingsZone.isMouseOver(mouseX, mouseY)) {
                            List<FormattedCharSequence> tooltipLines = new ArrayList<>();
                            int numBuildings = 0;

                            for (int i = selBuildings.size() - numExtraBuildings; i < selBuildings.size(); i++) {
                                BuildingPlacement placement = selBuildings.get(i);
                                BuildingPlacement nextPlacement = null;
                                Building building = placement.getBuilding();

                                Building nextBuilding = null;
                                numBuildings += 1;

                                if (i < selBuildings.size() - 1) {
                                    nextPlacement = selBuildings.get(i + 1);
                                    nextBuilding = nextPlacement.getBuilding();
                                }
                                if (building != nextBuilding) {
                                    tooltipLines.add(FormattedCharSequence.forward("x" + numBuildings + " " + I18n.get(ReignOfNetherRegistries.BUILDING.getKey(nextBuilding).getPath()),
                                        Style.EMPTY
                                    ));
                                    numBuildings = 0;
                                }
                            }
                            MyRenderer.renderTooltip(evt.getGuiGraphics(), tooltipLines, mouseX, mouseY);
                        }
                        break;
                    } else {
                        buildingButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                        renderedButtons.add(buildingButton);
                        buildingButton.renderHealthBar(evt.getGuiGraphics().pose());
                        blitX += iconFrameSize;
                        if (buttonsRendered == buttonsPerRow - 1) {
                            blitX = blitXStart;
                            blitY += iconFrameSize + 6;
                        }
                    }
                    buttonsRendered += 1;
                }
            }

            // ---------------------------------------------------------------
            // Building production queue (show only if 1 building is selected)
            // ---------------------------------------------------------------
            else if ((hudSelBuildingOwned || !PlayerClientEvents.isRTSPlayer()) && hudSelectedPlacement instanceof ProductionPlacement selProdBuilding) {
                blitY = screenHeight - iconFrameSize * 2 - 5;

                for (int i = 0; i < selProdBuilding.productionQueue.size(); i++) {
                    Button button = selProdBuilding.productionQueue.get(i)
                            .item.getCancelButton(selProdBuilding, i == 0);
                    if (!hudSelBuildingOwned) {
                        button.onLeftClick = () -> { };
                        button.onRightClick = () -> { };
                    }
                    productionButtons.add(button);
                }

                if (productionButtons.size() >= 1) {
                    // background frame
                    hudZones.add(MyRenderer.renderFrameWithBg(evt.getGuiGraphics(),
                        blitX - 5,
                        blitY - 10,
                        iconFrameSize * buttonsPerRow + 10,
                        iconFrameSize * 2 + 15,
                        frameBgColour
                    ));

                    // name and progress %
                    ActiveProduction firstProdItem = selProdBuilding.productionQueue.get(0);
                    float percentageDoneInv = firstProdItem.ticksLeft / firstProdItem.item.getCost(true, selProdBuilding.ownerName).ticks;

                    int colour = 0xFFFFFF;
                    if (!firstProdItem.item.isBelowPopulationSupply(selProdBuilding)) {
                        colour = 0xFF0000;
                        if (percentageDoneInv <= 0) {
                            percentageDoneInv = 0.01f;
                        }
                    }
                    evt.getGuiGraphics().drawString(
                        MC.font,
                        Math.round(100 - (percentageDoneInv * 100f)) + "% " + productionButtons.get(0).name,
                        blitX + iconFrameSize + 5,
                        blitY + 2,
                        colour
                    );

                    int buttonsRendered = 0;
                    for (Button prodButton : productionButtons) {
                        // top row for currently-in-progress item
                        if (buttonsRendered == 0) {
                            prodButton.greyPercent = 1 - percentageDoneInv;
                            prodButton.render(evt.getGuiGraphics(), blitX, blitY - 5, mouseX, mouseY);
                            renderedButtons.add(prodButton);
                        }
                        // replace last icon with a +X number of production items left in queue
                        else if (buttonsRendered >= buttonsPerRow && productionButtons.size() > (buttonsPerRow + 1)) {
                            int numExtraItems = productionButtons.size() - buttonsPerRow;
                            MyRenderer.renderIconFrameWithBg(evt.getGuiGraphics(),
                                prodButton.frameResource,
                                blitX,
                                blitY + iconFrameSize,
                                iconFrameSize,
                                iconBgColour
                            );
                            evt.getGuiGraphics().drawCenteredString(
                                MC.font,
                                "+" + numExtraItems,
                                blitX + iconFrameSize / 2,
                                blitY + iconFrameSize + 8,
                                0xFFFFFF
                            );
                            break;
                        }
                        // bottom row for all other queued items
                        else {
                            prodButton.render(evt.getGuiGraphics(), blitX, blitY + iconFrameSize, mouseX, mouseY);
                            renderedButtons.add(prodButton);
                            blitX += iconFrameSize;
                        }
                        buttonsRendered += 1;
                    }
                }
            }


            // ---------------------------
            // Building production buttons
            // ---------------------------
            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            if (hudSelectedPlacement != null && (hudSelBuildingOwned || !PlayerClientEvents.isRTSPlayer())) {
                if (!hudSelectedPlacement.isBuilt) {
                    if (!buildingCancelButton.isHidden.get()) {
                        buildingCancelButton.render(evt.getGuiGraphics(), 0, screenHeight - iconFrameSize, mouseX, mouseY);
                        renderedButtons.add(buildingCancelButton);
                    }
                }
                if (hudSelectedPlacement.isBuilt || hudSelectedPlacement.allowProdWhileBuilding) {

                    if (!hudSelectedPlacement.isBuilt)
                        blitX += Button.DEFAULT_ICON_FRAME_SIZE;

                    List<AbilityButton> buildingAbilities = hudSelectedPlacement.getAbilityButtons()
                            .stream()
                            .filter(b -> !b.isHidden.get())
                            .toList();
                    if (buildingAbilities.size() > 0) {
                        blitY -= Button.DEFAULT_ICON_FRAME_SIZE;
                    }

                    // production buttons on bottom row
                    if (hudSelectedPlacement instanceof ProductionPlacement selProdPlacement) {
                        List<Button> visibleProdButtons = selProdPlacement.productionButtons.stream()
                                .filter(b -> !b.isHidden.get())
                                .toList();
                        if (visibleProdButtons.size() > MAX_BUTTONS_PER_ROW) {
                            blitY -= Button.DEFAULT_ICON_FRAME_SIZE;
                        }
                        buildingProdRows += 1;

                        int rowButtons = 0;
                        for (Button prodButton : visibleProdButtons) {
                            rowButtons += 1;
                            if (rowButtons > MAX_BUTTONS_PER_ROW) {
                                rowButtons = 0;
                                blitX = 0;
                                blitY += Button.DEFAULT_ICON_FRAME_SIZE;
                                buildingProdRows += 1;
                            }
                            prodButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                            productionButtons.add(prodButton);
                            renderedButtons.add(prodButton);
                            blitX += iconFrameSize;
                        }
                    }
                    blitY += Button.DEFAULT_ICON_FRAME_SIZE;
                    blitX = 0;
                    for (AbilityButton abilityButton : buildingAbilities) {
                        if (!abilityButton.isHidden.get()) {
                            abilityButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                            renderedButtons.add(abilityButton);
                            blitX += iconFrameSize;
                        }
                    }
                }
            }
        }

        // --------------------------
        // Unit head portrait + stats
        // --------------------------
        else if (hudSelectedEntity != null && portraitRendererUnit.model != null
            && portraitRendererUnit.renderer != null) {

            blitY -= portraitRendererUnit.frameHeight;

            // write capitalised unit name
            String name = getModifiedEntityName(hudSelectedEntity).replace("_", " ");
            if (hudSelectedEntity.hasCustomName()) {
                name = hudSelectedEntity.getCustomName().getString();
            }

            String nameCap = name.substring(0, 1).toUpperCase() + name.substring(1);

            unitPortraitZone = portraitRendererUnit.render(evt.getGuiGraphics(),
                nameCap,
                blitX,
                blitY,
                mouseX,
                mouseY,
                hudSelectedEntity
            );
            hudZones.add(unitPortraitZone);

            if (hudSelectedEntity instanceof HeroUnit heroUnit) {
                RectZone zone = portraitRendererUnit.renderHeroLevelAndExp(evt.getGuiGraphics(), blitX + 1, blitY - 5, mouseX, mouseY, heroUnit);
                hudZones.add(zone);
                if (zone.isMouseOver(mouseX, mouseY)) {
                    MyRenderer.renderTooltip(evt.getGuiGraphics(),
                        heroUnit.getHeroLevel() >= HeroUnit.MAX_LEVEL ?
                            List.of(fcs(I18n.get("hud.hero.reignofnether.max_level"))) :
                            List.of(
                                    fcs(I18n.get("hud.hero.reignofnether.experience", heroUnit.getExpOnCurrentLevel(), heroUnit.getExpToNextlevel())),
                                    fcs(I18n.get("hud.hero.reignofnether.experience_warning"))
                            ),
                        mouseX, mouseY
                    );
                }
            }
            blitX += portraitRendererUnit.frameWidth;

            if (hudSelectedEntity instanceof Unit unit) {
                hudZones.add(portraitRendererUnit.renderStats(evt.getGuiGraphics(), nameCap, blitX, blitY, mouseX, mouseY, unit));

                blitX += portraitRendererUnit.statsWidth;

                int totalRes = Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue();

                if (hudSelectedEntity instanceof Mob mob && mob.canPickUpLoot() && totalRes > 0) {
                    hudZones.add(portraitRendererUnit.renderResourcesHeld(evt.getGuiGraphics(), blitX, blitY, unit));

                    // return button
                    if (getPlayerToEntityRelationship(hudSelectedEntity) == Relationship.OWNED ||
                        AlliancesClient.canControlAlly(hudSelectedEntity)) {
                        Button returnButton = new Button("Return resources",
                            Button.itemIconSize,
                            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/chest.png"),
                            Keybindings.keyD,
                            () -> unit.getReturnResourcesGoal().getBuildingTarget() != null,
                            () -> false,
                            () -> true,
                            () -> sendUnitCommand(UnitAction.RETURN_RESOURCES_TO_CLOSEST),
                            null,
                            List.of(FormattedCharSequence.forward(I18n.get("hud.reignofnether.drop_off_resources"),
                                Style.EMPTY
                            ))
                        );
                        returnButton.render(evt.getGuiGraphics(), blitX + 10, blitY + 38, mouseX, mouseY);
                        renderedButtons.add(returnButton);
                    }
                }
            } else if (ResourceSources.isHuntableAnimal(hudSelectedEntity)) {
                hudZones.add(portraitRendererUnit.renderResourcesHeld(evt.getGuiGraphics(), blitX, blitY, (Animal) hudSelectedEntity));
                blitX += portraitRendererUnit.statsWidth;
            }

            if (hudSelectedEntity instanceof Unit unit
                && Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() > 0) {
                blitX += portraitRendererUnit.statsWidth + 5;
            } else {
                blitX += 15;
            }
        }

        // ----------------------------------------------
        // Unit icons to select types and show healthbars
        // ----------------------------------------------
        blitXStart = blitX;
        blitY = screenHeight - iconFrameSize * 2 - 10;

        for (LivingEntity unit : selUnits) {
            if ((getPlayerToEntityRelationship(unit) == Relationship.OWNED ||
                    NonUnitClientEvents.canControlAllMobs() ||
                    AlliancesClient.canControlAlly(unit)) &&
                unitButtons.size() < (buttonsPerRow * 2)) {
                // mob head icon
                String unitName = MiscUtil.getSimpleEntityName(unit, true).toLowerCase();
                String buttonImagePath;

                if (unit.isVehicle()) {
                    buttonImagePath = "textures/mobheads/" + unitName + "_half.png";
                } else {
                    buttonImagePath = "textures/mobheads/" + unitName + ".png";
                }

                Button button = new ButtonBuilder(unitName)
                    .iconSize(iconSize)
                    .iconResource(unit instanceof Unit ? ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, buttonImagePath) : null)
                    .entity(unit)
                    .isSelected(() -> hudSelectedEntity == null || getModifiedEntityName(hudSelectedEntity).equals(
                            getModifiedEntityName(unit)))
                    .onLeftClick(() -> {
                        // select this one specific unit
                        if (Keybindings.shiftMod.isDown()) {
                            UnitClientEvents.getSelectedUnits().remove(hudSelectedEntity);
                        } else if (getModifiedEntityName(hudSelectedEntity).equals(getModifiedEntityName(unit))) {
                            UnitClientEvents.clearSelectedUnits();
                            UnitClientEvents.addSelectedUnit(unit);
                        } else { // click to select this unit type as a group
                            HudClientEvents.setHudSelectedEntity(unit);
                        }
                    })
                    .tooltipLines(List.of(fcs(capitaliseAndSpace(getModifiedEntityName(unit)))))
                    .build();

                if (unit.isVehicle() && unit instanceof Unit) {
                    String passengerName = MiscUtil.getSimpleEntityName(unit.getFirstPassenger(), true);
                    button.bgIconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                        "textures/mobheads/" + passengerName + ".png"
                    );
                }
                unitButtons.add(button);
            }
        }

        if (unitButtons.size() >= 2) {
            // background frame
            hudZones.add(MyRenderer.renderFrameWithBg(evt.getGuiGraphics(),
                blitX - 5,
                blitY - 10,
                iconFrameSize * buttonsPerRow + 10,
                iconFrameSize * 2 + 20,
                frameBgColour
            ));

            int buttonsRendered = 0;
            for (Button unitButton : unitButtons) {
                // replace last icon with a +X number of units icon and hover tooltip for what those units are
                if (buttonsRendered >= (buttonsPerRow * 2) - 1 && selUnits.size() > (buttonsPerRow * 2)) {
                    int numExtraUnits = selUnits.size() - (buttonsPerRow * 2) + 1;
                    RectZone plusUnitsZone = MyRenderer.renderIconFrameWithBg(evt.getGuiGraphics(),
                        unitButton.frameResource,
                        blitX,
                        blitY,
                        iconFrameSize,
                        iconBgColour
                    );
                    evt.getGuiGraphics().drawCenteredString(
                        MC.font,
                        "+" + numExtraUnits,
                        blitX + iconFrameSize / 2,
                        blitY + 8,
                        0xFFFFFF
                    );

                    if (plusUnitsZone.isMouseOver(mouseX, mouseY)) {
                        List<FormattedCharSequence> tooltipLines = new ArrayList<>();
                        int numUnits = 0;

                        for (int i = selUnits.size() - numExtraUnits; i < selUnits.size(); i++) {

                            LivingEntity unit = selUnits.get(i);
                            LivingEntity nextUnit;
                            String unitName = HudClientEvents.getModifiedEntityName(unit);
                            String nextUnitName = null;
                            numUnits += 1;

                            if (i < selUnits.size() - 1) {
                                nextUnit = selUnits.get(i + 1);
                                nextUnitName = HudClientEvents.getModifiedEntityName(nextUnit);
                            }
                            if (!unitName.equals(nextUnitName)) {
                                tooltipLines.add(FormattedCharSequence.forward("x" + numUnits + " " + capitaliseAndSpace(unitName),
                                    Style.EMPTY
                                ));
                                numUnits = 0;
                            }
                        }
                        MyRenderer.renderTooltip(evt.getGuiGraphics(), tooltipLines, mouseX, mouseY);
                    }
                    break;
                } else {
                    unitButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                    renderedButtons.add(unitButton);
                    if (unitButton.iconResource == null) {
                        String str = unitButton.name.substring(0,1).toUpperCase();
                        if (unitButton.name.length() > 1) {
                            str += unitButton.name.substring(1, 2);
                        }
                        evt.getGuiGraphics().drawCenteredString(MC.font,
                                fcs(str, true),
                                blitX + (unitButton.iconSize / 2) + 4,
                                blitY + (unitButton.iconSize / 2),
                                0xFFFFFF
                        );
                    }
                    unitButton.renderHealthBar(evt.getGuiGraphics().pose());
                    blitX += iconFrameSize;
                    if (buttonsRendered == buttonsPerRow - 1) {
                        blitX = blitXStart;
                        blitY += iconFrameSize + 6;
                    }
                }
                buttonsRendered += 1;
            }
        }


        // ---------------------------
        // Unit sandbox buttons
        // ---------------------------
        if (SandboxClientEvents.isSandboxPlayer() && (hudSelectedEntity != null || hudSelectedPlacement != null)) {
            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            blitY -= iconFrameSize * buildingProdRows;
            if (hudSelectedEntity != null || !hudSelectedPlacement.getAbilities().isEmpty() || !hudSelectedPlacement.isBuilt) {
                blitY -= iconFrameSize;
            }
            ArrayList<Button> actionButtons = new ArrayList<>();

            actionButtons.add(SandboxActionButtons.getSetRelationshipButton());
            actionButtons.add(SandboxActionButtons.getCycleScenarioRoleButton());
            if (hudSelectedPlacement != null) {
                actionButtons.add(SandboxActionButtons.removeBuildingPlacement);
            }
            if (hudSelectedEntity instanceof Unit) {
                actionButtons.add(SandboxActionButtons.setAnchor);
                actionButtons.add(SandboxActionButtons.resetToAnchor);
                actionButtons.add(SandboxActionButtons.removeAnchor);
            }
            for (Button actionButton : actionButtons) {
                if (!actionButton.isHidden.get()) {
                    actionButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                    renderedButtons.add(actionButton);
                    blitX += iconFrameSize;
                }
            }
        }

        // --------------------------------------------------------
        // Unit action buttons (attack, stop, move, abilities etc.)
        // --------------------------------------------------------
        if (selUnits.size() > 0 &&
                (getPlayerToEntityRelationship(selUnits.get(0)) == Relationship.OWNED ||
                        !PlayerClientEvents.isRTSPlayer() ||
                        NonUnitClientEvents.canControlAllMobs() ||
                        AlliancesClient.canControlAlly(selUnits.get(0))) &&
                hudSelectedEntity instanceof Unit unit) {
            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            ArrayList<Button> actionButtons = new ArrayList<>();

            if (hudSelectedEntity instanceof AttackerUnit) {
                actionButtons.add(ActionButtons.attack);
            }
            if (hudSelectedEntity instanceof WorkerUnit) {
                actionButtons.add(ActionButtons.buildRepair);
                actionButtons.add(ActionButtons.gather);
            }
            if (unit.canGarrison() && GarrisonableBuildingAddon.getGarrison(unit) == null) {
                actionButtons.add(ActionButtons.garrison);
            } else if (GarrisonableBuildingAddon.getGarrison(unit) != null) {
                actionButtons.add(ActionButtons.ungarrison);
            }

            if (!(hudSelectedEntity instanceof WorkerUnit)) {
                actionButtons.add(ActionButtons.hold);
            }
            actionButtons.add(ActionButtons.stop);

            if (hudSelectedEntity instanceof VillagerUnit vUnit)
                for (Ability ability : vUnit.getAbilities().get())
                    if (ability instanceof CallToArmsUnit callToArmsUnit)
                        actionButtons.add(callToArmsUnit.getButton(Keybindings.keyV, vUnit));

            for (Button actionButton : actionButtons) {
                // GATHER button does not have a static icon
                if (actionButton == ActionButtons.gather && hudSelectedEntity instanceof WorkerUnit workerUnit) {
                    switch (workerUnit.getGatherResourceGoal().getTargetResourceName()) {
                        case NONE -> actionButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                                "textures/icons/items/no_gather.png"
                        );
                        case FOOD -> actionButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                                "textures/icons/items/hoe.png"
                        );
                        case WOOD -> actionButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                                "textures/icons/items/axe.png"
                        );
                        case ORE -> actionButton.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                                "textures/icons/items/pickaxe.png"
                        );
                    }
                    String resourceName = UnitClientEvents.getSelectedUnitResourceTarget().toString();
                    String key = String.format("resources.reignofnether.%s", resourceName.toLowerCase(Locale.ENGLISH));
                    actionButton.tooltipLines = List.of(
                            FormattedCharSequence.forward(I18n.get("hud.reignofnether" + ".gather_resources",
                                    I18n.get(key)
                            ), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.reignofnether.change_target_resource"), Style.EMPTY)
                    );
                }
                actionButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                renderedButtons.add(actionButton);
                blitX += iconFrameSize;
            }
            blitX = 0;
            blitY = screenHeight - (iconFrameSize * 2) - 4;

            // includes worker building buttons
            if (TutorialClientEvents.isAtOrPastStage(TutorialStage.BUILD_INTRO) &&
                    (getPlayerToEntityRelationship(selUnits.get(0)) == Relationship.OWNED || !PlayerClientEvents.isRTSPlayer() || ResearchClient.hasCheat("wouldyoukindly")) ||
                    AlliancesClient.canControlAlly(selUnits.get(0))) {
                List<Button> abilityButtons = List.of();
                for (LivingEntity livingEntity : selUnits) {
                    if (livingEntity == hudSelectedEntity) {
                        abilityButtons = unit.getAbilityButtons(); // unit == hudSelectedEntity
                        break;
                    }
                }
                List<Button> unitAbilities = abilityButtons.stream()
                        .filter(b -> !(b instanceof AbilityButton ab) || !(ab.ability instanceof CallToArmsUnit))
                        .toList();

                int rowsUp = (int) Math.floor((float) (unitAbilities.size() - 1) / MAX_BUTTONS_PER_ROW);
                rowsUp = Math.max(0, rowsUp);
                if (SandboxClientEvents.isSandboxPlayer() && (hudSelectedEntity != null || hudSelectedPlacement != null))
                    rowsUp += 1;

                blitY -= iconFrameSize * rowsUp;

                int i = 0;
                for (Button button : unitAbilities) {

                    if (button instanceof AbilityButton abilityButton && abilityButton.ability instanceof HeroAbility heroAbility &&
                            ((HeroUnit)unit).isRankUpMenuOpen() && !HeroAbility.allSkillsLearnt((HeroUnit) unit)) {
                        Button rankUpButton = heroAbility.getRankUpButton((HeroUnit)unit);
                        if (!rankUpButton.isHidden.get()) {
                            i += 1;
                            rankUpButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                            renderedButtons.add(rankUpButton);
                            blitX += iconFrameSize;
                            if (i % MAX_BUTTONS_PER_ROW == 0) {
                                blitX = 0;
                                blitY += iconFrameSize;
                            }
                        }
                    } else if (!button.isHidden.get()) {
                        i += 1;
                        button.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                        renderedButtons.add(button);
                        blitX += iconFrameSize;
                        if (i % MAX_BUTTONS_PER_ROW == 0) {
                            blitX = 0;
                            blitY += iconFrameSize;
                        }
                    }
                }
                if (hudSelectedEntity instanceof HeroUnit hero) {
                    Button rankUpMenuButton = HeroAbility.getRankUpMenuButton(hero);
                    if (!rankUpMenuButton.isHidden.get()) {
                        rankUpMenuButton.render(evt.getGuiGraphics(), 0, blitY - iconFrameSize, mouseX, mouseY);
                        renderedButtons.add(rankUpMenuButton);
                    }
                }
            }
        }
        else if (MC.player != null && SandboxClientEvents.isSandboxPlayer(MC.player.getName().getString()) && selUnits.isEmpty() && selBuildings.isEmpty()) {
            blitX = 0;
            blitY = screenHeight - iconFrameSize;

            ArrayList<Button> actionButtons = new ArrayList<>();
            actionButtons.add(SandboxClientEvents.getCycleBuildingOrUnitsButton());
            actionButtons.add(SandboxClientEvents.getToggleFactionButton());
            actionButtons.add(SandboxClientEvents.getToggleRelationshipButton());

            if (SandboxClientEvents.sandboxMenuType == SandboxMenuType.UNITS) {
                actionButtons.add(SandboxClientEvents.getToggleUnitCheatsButton());
            } else {
                actionButtons.add(SandboxClientEvents.getToggleBuildingCheatsButton());
            }
            actionButtons.add(SandboxClientEvents.getToggleNonUnitControlButton());
            actionButtons.add(SandboxClientEvents.getSortCustomBuildingsButton());

            for (Button actionButton : actionButtons) {
                if (!actionButton.isHidden.get()) {
                    actionButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                    renderedButtons.add(actionButton);
                    blitX += iconFrameSize;
                }
            }
            blitX = 0;
            blitY = screenHeight - (iconFrameSize * 2) - 4;

            List<Button> abilityButtons = switch(SandboxClientEvents.sandboxMenuType) {
                case UNITS -> List.copyOf(SandboxClientEvents.getUnitButtons());
                case BUILDINGS -> List.copyOf(SandboxClientEvents.getBuildingButtons());
                case CUSTOM_BUILDINGS -> List.copyOf(SandboxClientEvents.getCustomBuildingButtons());
            };

            List<Button> shownAbilities = abilityButtons.stream()
                    .filter(b -> !b.isHidden.get() && !(b instanceof AbilityButton ab && ab.ability instanceof CallToArmsUnit))
                    .toList();

            int rowsUp = (int) Math.floor((float) (shownAbilities.size() - 1) / MAX_BUTTONS_PER_ROW);
            rowsUp = Math.max(0, rowsUp);
            if (SandboxClientEvents.isSandboxPlayer() && (hudSelectedEntity != null || hudSelectedPlacement != null))
                rowsUp += 1;

            blitY -= iconFrameSize * rowsUp;

            int i = 0;
            for (Button button : shownAbilities) {
                if (!button.isHidden.get()) {
                    i += 1;
                    button.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                    renderedButtons.add(button);
                    blitX += iconFrameSize;
                    if (i % MAX_BUTTONS_PER_ROW == 0) {
                        blitX = 0;
                        blitY += iconFrameSize;
                    }
                }
            }
        }
        // -----------------
        // Non-unit controls
        // -----------------
        else if (!(hudSelectedEntity instanceof Unit) && !getSelectedUnits().isEmpty() &&
                NonUnitClientEvents.canControlAllMobs()) {
            blitX = 0;
            blitY = screenHeight - iconFrameSize;
            ArrayList<Button> actionButtons = new ArrayList<>();

            if (NonUnitClientEvents.canAttack(getSelectedUnits().get(0)))
                actionButtons.add(ActionButtons.attack);

            actionButtons.add(ActionButtons.stop);

            for (Button actionButton : actionButtons) {
                actionButton.render(evt.getGuiGraphics(), blitX, blitY, mouseX, mouseY);
                renderedButtons.add(actionButton);
                blitX += iconFrameSize;
            }
        }

        // ---------------------------
        // Resources icons and amounts
        // ---------------------------
        Resources resources = null;
        String selPlayerName = null;

        if (!UnitClientEvents.getSelectedUnits().isEmpty()) {
            if (UnitClientEvents.getSelectedUnits().get(0) instanceof Unit unit) {
                selPlayerName = unit.getOwnerName();
            }
        }
        if (!BuildingClientEvents.getSelectedBuildings().isEmpty()) {
            selPlayerName = BuildingClientEvents.getSelectedBuildings().get(0).ownerName;
        }

        boolean alliedWithSelPlayer = MC.player != null && AlliancesClient.isAllied(MC.player.getName().getString(), selPlayerName);
        boolean isSelPlayer = MC.player != null && MC.player.getName().getString().equals(selPlayerName);

        // during a match if nothing is selected, then show your own resources by default
        if (MC.player != null && !isSelPlayer && PlayerClientEvents.isRTSPlayer() && selPlayerName == null) {
            selPlayerName = MC.player.getName().getString();
            isSelPlayer = true;
        }

        if (selPlayerName != null && (isSelPlayer || alliedWithSelPlayer || !PlayerClientEvents.isRTSPlayer() || SandboxClientEvents.isSandboxPlayer())) {
            resources = ResourcesClientEvents.getResources(selPlayerName);
        }

        blitX = 0;
        blitY = 0;

        if ((!PlayerClientEvents.isRTSPlayer() || alliedWithSelPlayer || SandboxClientEvents.isSandboxPlayer()) && !isSelPlayer) {
            if (resources != null) {
                evt.getGuiGraphics().drawString(
                    MC.font,
                    I18n.get("hud.reignofnether.players_resources", selPlayerName),
                    blitX + 5,
                    blitY + 5,
                    0xFFFFFF
                );
            } else if (!PlayerClientEvents.isRTSPlayer() && !TutorialClientEvents.isEnabled()) {
                evt.getGuiGraphics().drawString(
                    MC.font,
                    I18n.get("hud.reignofnether.you_are_spectator"),
                    blitX + 5,
                    blitY + 5,
                    0xFFFFFF
                );
                blitY += 10;
            }
            blitY += 20;
        }

        int resourceBlitYStart = blitY;
        int resourcePanelBottomY = blitY;

        if (resources != null && MC.player != null) {
            for (String resourceName : new String[] { "food", "wood", "ore", "pop" }) {
                ResourceLocation rl;
                String resValueStr = "";
                ResourceName resName;

                List<FormattedCharSequence> tooltip;

                switch (resourceName) {
                    case "food" -> {
                        rl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
                        resValueStr = String.valueOf(resources.food);
                        resName = ResourceName.FOOD;
                    }
                    case "wood" -> {
                        rl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
                        resValueStr = String.valueOf(resources.wood);
                        resName = ResourceName.WOOD;
                    }
                    case "ore" -> {
                        rl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
                        resValueStr = String.valueOf(resources.ore);
                        resName = ResourceName.ORE;
                    }
                    default -> {
                        rl = PlayerColors.getPlayerColorBedIcon(selPlayerName);
                        resValueStr = UnitClientEvents.getCurrentPopulation(selPlayerName) + "/"
                            + BuildingClientEvents.getTotalPopulationSupply(selPlayerName);
                        resName = ResourceName.NONE;
                    }
                }
                hudZones.add(MyRenderer.renderFrameWithBg(evt.getGuiGraphics(),
                    blitX + iconFrameSize - 1,
                    blitY,
                    49,
                    iconFrameSize,
                    frameBgColour
                ));

                hudZones.add(MyRenderer.renderIconFrameWithBg(evt.getGuiGraphics(),
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                    blitX,
                    blitY,
                    iconFrameSize,
                    iconBgColour
                ));

                MyRenderer.renderIcon(evt.getGuiGraphics(),
                    rl,
                    blitX + 4,
                    blitY + 4,
                    iconSize
                );
                evt.getGuiGraphics().drawCenteredString(
                    MC.font,
                    resValueStr,
                    blitX + (iconFrameSize) + 24,
                    blitY + (iconSize / 2) + 1,
                    0xFFFFFF
                );

                // worker count assigned to each resource
                String finalSelPlayerName = selPlayerName;

                int numWorkersHunting = UnitClientEvents.getAllUnits()
                    .stream()
                    .filter(le -> le instanceof WorkerUnit wu && le instanceof Unit u && u.getOwnerName()
                        .equals(finalSelPlayerName) && ResourceSources.isHuntableAnimal(u.getTargetGoal().getTarget()))
                    .toList()
                    .size();

                int numWorkersAssigned = 0;
                // we can only see ReturnResourcesGoal data on server, so we can't use that here
                if (resName == ResourceName.NONE) {
                    numWorkersAssigned = UnitClientEvents.getAllUnits()
                        .stream()
                        .filter(u -> u instanceof WorkerUnit
                                && ((Unit) u).getOwnerName().equals(finalSelPlayerName))
                        .toList()
                        .size();
                } else {
                    for (LivingEntity le : UnitClientEvents.getAllUnits()) {
                        if (le instanceof Unit u && le instanceof WorkerUnit wu && u.getOwnerName()
                            .equals(finalSelPlayerName) && !UnitClientEvents.idleWorkerIds.contains(le.getId())) {

                            boolean alreadyAssigned = false;

                            if (u.getReturnResourcesGoal() != null) {
                                Resources res = Resources.getTotalResourcesFromItems(u.getItems());
                                if (resName == ResourceName.FOOD && res.food > 0
                                    || resName == ResourceName.WOOD && res.wood > 0
                                    || resName == ResourceName.ORE && res.ore > 0) {
                                    numWorkersAssigned += 1;
                                    alreadyAssigned = true;
                                }
                            }
                            if (!alreadyAssigned && wu.getGatherResourceGoal()
                                .getTargetResourceName()
                                .equals(resName)) {
                                numWorkersAssigned += 1;
                            }
                        }
                    }
                }
                if (resName == ResourceName.FOOD) {
                    numWorkersAssigned += numWorkersHunting;
                }

                hudZones.add(MyRenderer.renderIconFrameWithBg(evt.getGuiGraphics(),
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                        blitX + 69,
                        blitY,
                        iconFrameSize,
                        iconBgColour
                ));

                evt.getGuiGraphics().drawCenteredString(
                        MC.font,
                        String.valueOf(numWorkersAssigned),
                        blitX + 69 + (iconFrameSize / 2),
                        blitY + (iconSize / 2) + 1,
                        0xFFFFFF
                );

                blitY += iconFrameSize - 1;
            }
            resourcePanelBottomY = blitY;

            blitY = resourceBlitYStart;
            final String finalSelPlayerName = selPlayerName;
            for (String resourceName : new String[] { "food", "wood", "ore", "population" }) {
                String locName = I18n.get("resources.reignofnether." + resourceName);
                List<FormattedCharSequence> tooltip;
                String key = String.format("resources.reignofnether.%s", resourceName);
                if (resourceName.equals("population")) {
                    tooltip = List.of(FormattedCharSequence.forward(I18n.get("hud.reignofnether.max_resources",
                        I18n.get(key),
                        GameruleClient.maxPopulation
                    ), Style.EMPTY));
                } else {
                    tooltip = List.of(FormattedCharSequence.forward(I18n.get(key), Style.EMPTY));
                }
                if (mouseX >= blitX && mouseY >= blitY && mouseX < blitX + iconFrameSize
                    && mouseY < blitY + iconFrameSize) {
                    MyRenderer.renderTooltip(evt.getGuiGraphics(), tooltip, mouseX + 5, mouseY);
                }
                if (mouseX >= blitX + 69 && mouseY >= blitY && mouseX < blitX + 69 + iconFrameSize
                    && mouseY < blitY + iconFrameSize) {
                    List<FormattedCharSequence> tooltipWorkersAssigned;
                    if (resourceName.equals("population")) {
                        int numWorkers = UnitClientEvents.getAllUnits()
                            .stream()
                            .filter(u -> u instanceof WorkerUnit
                                && ((Unit) u).getOwnerName().equals(finalSelPlayerName))
                            .toList()
                            .size();
                        tooltipWorkersAssigned =
                            List.of(FormattedCharSequence.forward(I18n.get("hud.reignofnether.total_workers",
                            numWorkers
                        ), Style.EMPTY));
                    } else {
                        tooltipWorkersAssigned =
                            List.of(FormattedCharSequence.forward(I18n.get("hud.reignofnether.workers_on_" + resourceName
                        ), Style.EMPTY));
                    }
                    MyRenderer.renderTooltip(evt.getGuiGraphics(), tooltipWorkersAssigned, mouseX + 5, mouseY);


                }
                blitY += iconFrameSize - 1;
            }
        }

        // global production queue - visible to yourself, observers, sandbox players and allies
        int queuePanelStartX = 0;
        int queuePanelStartY = resourcePanelBottomY + 6;
        if (isSelPlayer ||
            !PlayerClientEvents.isRTSPlayer() ||
            SandboxClientEvents.isSandboxPlayer() ||
            AlliancesClient.isAllied(MC.player.getName().getString(), selPlayerName)) {
            Pair<List<RectZone>, List<Button>> renderedElements = GlobalProductionQueueRenderer.renderQueue(evt.getGuiGraphics(),
                    selPlayerName,
                    queuePanelStartX,
                    queuePanelStartY,
                    mouseX,
                    mouseY
            );
            hudZones.addAll(renderedElements.getFirst());
            renderedButtons.addAll(renderedElements.getSecond());
        }

        // --------------------------
        // Temporary warning messages
        // --------------------------
        if (tempMsgTicksLeft > 0 && tempMsg.length() > 0) {
            int ticksUnderFade = Math.min(tempMsgTicksLeft, TEMP_MSG_TICKS_FADE);
            int alpha = (int) (0xFF * ((float) ticksUnderFade / (float) TEMP_MSG_TICKS_FADE));

            evt.getGuiGraphics().drawCenteredString(
                MC.font,
                tempMsg,
                screenWidth / 2,
                screenHeight - iconFrameSize * 2 - 50,
                0xFFFFFF + (alpha << 24)
            );
        }
        if (tempMsgTicksLeft > 0) {
            tempMsgTicksLeft -= 1;
        }

        // ---------------------
        // Control group buttons
        // ---------------------
        blitX = 100;
        // clean up untracked entities/buildings from control groups
        for (ControlGroup controlGroup : controlGroups) {
            controlGroup.clean();

            if (!controlGroup.isEmpty()) {
                Button ctrlGroupButton = controlGroup.getButton();
                ctrlGroupButton.render(evt.getGuiGraphics(), blitX, 0, mouseX, mouseY);
                renderedButtons.add(ctrlGroupButton);
                blitX += iconFrameSize;
            }
        }

        // ---------------------
        // Attack warning button
        // ---------------------
        Button attackWarningButton = AttackWarningClientEvents.getWarningButton();
        if (!attackWarningButton.isHidden.get()) {
            attackWarningButton.render(evt.getGuiGraphics(),
                screenWidth - (MinimapClientEvents.getMapGuiRadius() * 2) - (MinimapClientEvents.CORNER_OFFSET * 2)
                    - 14,
                screenHeight - MinimapClientEvents.getMapGuiRadius() - (MinimapClientEvents.CORNER_OFFSET * 2) - 2,
                mouseX,
                mouseY
            );
            renderedButtons.add(attackWarningButton);
        }

        // ----------------------
        // Map size toggle button
        // ----------------------
        Button toggleMapSizeButton = MinimapClientEvents.getToggleSizeButton();
        if (!toggleMapSizeButton.isHidden.get()) {
            toggleMapSizeButton.render(evt.getGuiGraphics(),
                    screenWidth - (toggleMapSizeButton.iconSize * 2),
                    screenHeight - (toggleMapSizeButton.iconSize * 2),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(toggleMapSizeButton);
        }

        Button markerModeButton = MinimapClientEvents.getMarkerModeButton();
        if (!markerModeButton.isHidden.get()) {
            markerModeButton.render(evt.getGuiGraphics(),
                screenWidth - (markerModeButton.iconSize * (MinimapClientEvents.isLargeMap() ? 12 : 8)),
                screenHeight - (markerModeButton.iconSize * 2),
                mouseX, mouseY
            );
            renderedButtons.add(markerModeButton);
        }

        Button camSensitivityButton = MinimapClientEvents.getCamSensitivityButton();
        if (!camSensitivityButton.isHidden.get()) {
            camSensitivityButton.render(evt.getGuiGraphics(),
                    screenWidth - (camSensitivityButton.iconSize * 4),
                    screenHeight - (camSensitivityButton.iconSize * 2),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(camSensitivityButton);
        }
        Button mapLockButton = MinimapClientEvents.getMapLockButton();
        if (!mapLockButton.isHidden.get()) {
            mapLockButton.render(evt.getGuiGraphics(),
                    screenWidth - (mapLockButton.iconSize * 2),
                    screenHeight - (mapLockButton.iconSize * 4),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(mapLockButton);
        }
        Button highlightAnimalsButton = MinimapClientEvents.getHighlightAnimalsButton();
        if (!highlightAnimalsButton.isHidden.get()) {
            highlightAnimalsButton.render(evt.getGuiGraphics(),
                    screenWidth - (highlightAnimalsButton.iconSize * 2),
                    screenHeight - (highlightAnimalsButton.iconSize * 6),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(highlightAnimalsButton);
        }
        Button nightCirclesButton = MinimapClientEvents.getNightCirclesModeButton();
        if (!nightCirclesButton.isHidden.get()) {
            nightCirclesButton.render(evt.getGuiGraphics(),
                    screenWidth - (nightCirclesButton.iconSize * 4),
                    screenHeight - (nightCirclesButton.iconSize * 4),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(nightCirclesButton);
        }
        Button leavesHidingButton = OrthoviewClientEvents.getLeavesHidingButton();
        if (!leavesHidingButton.isHidden.get()) {
            leavesHidingButton.render(evt.getGuiGraphics(),
                    screenWidth - (leavesHidingButton.iconSize * 6),
                    screenHeight - (leavesHidingButton.iconSize * 2),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(leavesHidingButton);
        }
        Button toggleTeamColorsButton = PlayerColors.getToggleTeamColorsButton();
        if (!toggleTeamColorsButton.isHidden.get()) {
            toggleTeamColorsButton.render(evt.getGuiGraphics(),
                    screenWidth - (toggleTeamColorsButton.iconSize * 6),
                    screenHeight - (toggleTeamColorsButton.iconSize * 4),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(toggleTeamColorsButton);
        }

        Button rotateCW = MinimapClientEvents.getCameraRotateCWButton();
        if (!rotateCW.isHidden.get()) {
            rotateCW.render(evt.getGuiGraphics(),
                    screenWidth - (rotateCW.iconSize * (MinimapClientEvents.isLargeMap() ? 8 : 4)),
                    screenHeight - (rotateCW.iconSize * 2),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(rotateCW);
        }

        Button rotateCCW = MinimapClientEvents.getCameraRotateCCWButton();
        if (!rotateCCW.isHidden.get()) {
            rotateCCW.render(evt.getGuiGraphics(),
                    screenWidth - (rotateCCW.iconSize * (MinimapClientEvents.isLargeMap() ? 10 : 6)),
                    screenHeight - (rotateCCW.iconSize * 2),
                    mouseX,
                    mouseY
            );
            renderedButtons.add(rotateCCW);
        }

        // ------------------------------
        // Start buttons (spectator only)
        // ------------------------------
        if (!PlayerClientEvents.isRTSPlayer() && !PlayerClientEvents.rtsLocked) {

            // scenario
            if (GameruleClient.scenarioMode) {
                Button scenarioStartButton = ScenarioClientEvents.getScenarioStartButton();
                scenarioStartButton.render(evt.getGuiGraphics(),
                        screenWidth - (StartButtons.ICON_SIZE * 2),
                        StartButtons.ICON_SIZE / 2,
                        mouseX,
                        mouseY
                );
                renderedButtons.add(scenarioStartButton);

                Button cycleRoleToPlayButton = ScenarioClientEvents.getCycleRoleToPlayButton();
                if (cycleRoleToPlayButton != null) {
                    cycleRoleToPlayButton.render(evt.getGuiGraphics(),
                            screenWidth - (StartButtons.ICON_SIZE * 4),
                            StartButtons.ICON_SIZE / 2,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(cycleRoleToPlayButton);
                }
            } else { // normal gamemodes
                /*
                Button startPosButton = StartPosClientEvents.getPositionsButton();
                if (!startPosButton.isHidden.get()) {
                    startPosButton.render(evt.getGuiGraphics(),
                            screenWidth - (StartButtons.ICON_SIZE * 6),
                            40,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(startPosButton);
                }
                 */
                Button readyButton = StartPosClientEvents.getReadyButton();
                if (!readyButton.isHidden.get()) {
                    readyButton.render(evt.getGuiGraphics(),
                            screenWidth - (StartButtons.ICON_SIZE * 4),
                            40,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(readyButton);
                }
                Button unreadyButton = StartPosClientEvents.getUnreadyButton();
                if (!unreadyButton.isHidden.get()) {
                    unreadyButton.render(evt.getGuiGraphics(),
                            screenWidth - (StartButtons.ICON_SIZE * 4),
                            40,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(unreadyButton);
                }
                Button diffsButton = ConfigClientEvents.getDiffsButton();
                if (!diffsButton.isHidden.get()) {
                    diffsButton.render(evt.getGuiGraphics(),
                            screenWidth - (StartButtons.ICON_SIZE * 10),
                            StartButtons.ICON_SIZE / 2,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(diffsButton);
                }

                Button gamemodeButton = ClientGameModeHelper.getButton();
                if (gamemodeButton != null && !gamemodeButton.isHidden.get() && !TutorialClientEvents.isEnabled()) {
                    gamemodeButton.render(evt.getGuiGraphics(),
                            screenWidth - (StartButtons.ICON_SIZE * 8),
                            StartButtons.ICON_SIZE / 2,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(gamemodeButton);
                }

                if (ClientGameModeHelper.gameMode != GameMode.SANDBOX) {

                    if (!StartPosClientEvents.isEnabled()) {
                        if (!StartButtons.villagerStartButton.isHidden.get()) {
                            StartButtons.villagerStartButton.render(evt.getGuiGraphics(),
                                    screenWidth - (StartButtons.ICON_SIZE * 6),
                                    StartButtons.ICON_SIZE / 2,
                                    mouseX,
                                    mouseY
                            );
                            renderedButtons.add(StartButtons.villagerStartButton);
                        }
                        if (!StartButtons.monsterStartButton.isHidden.get()) {
                            StartButtons.monsterStartButton.render(evt.getGuiGraphics(),
                                    (int) (screenWidth - (StartButtons.ICON_SIZE * 4f)),
                                    StartButtons.ICON_SIZE / 2,
                                    mouseX,
                                    mouseY
                            );
                            renderedButtons.add(StartButtons.monsterStartButton);
                        }
                        if (!StartButtons.piglinStartButton.isHidden.get()) {
                            StartButtons.piglinStartButton.render(evt.getGuiGraphics(),
                                    screenWidth - (StartButtons.ICON_SIZE * 2),
                                    StartButtons.ICON_SIZE / 2,
                                    mouseX,
                                    mouseY
                            );
                            renderedButtons.add(StartButtons.piglinStartButton);
                        }
                    } else {
                        if (!StartPosClientEvents.villagerReadyButton.isHidden.get()) {
                            StartPosClientEvents.villagerReadyButton.render(evt.getGuiGraphics(),
                                    screenWidth - (StartButtons.ICON_SIZE * 6),
                                    StartButtons.ICON_SIZE / 2,
                                    mouseX,
                                    mouseY
                            );
                            renderedButtons.add(StartPosClientEvents.villagerReadyButton);
                        }
                        if (!StartPosClientEvents.monsterReadyButton.isHidden.get()) {
                            StartPosClientEvents.monsterReadyButton.render(evt.getGuiGraphics(),
                                    (int) (screenWidth - (StartButtons.ICON_SIZE * 4f)),
                                    StartButtons.ICON_SIZE / 2,
                                    mouseX,
                                    mouseY
                            );
                            renderedButtons.add(StartPosClientEvents.monsterReadyButton);
                        }
                        if (!StartPosClientEvents.piglinReadyButton.isHidden.get()) {
                            StartPosClientEvents.piglinReadyButton.render(evt.getGuiGraphics(),
                                    screenWidth - (StartButtons.ICON_SIZE * 2),
                                    StartButtons.ICON_SIZE / 2,
                                    mouseX,
                                    mouseY
                            );
                            renderedButtons.add(StartPosClientEvents.piglinReadyButton);
                        }
                    }
                } else if (!StartButtons.sandboxStartButton.isHidden.get()) {
                    StartButtons.sandboxStartButton.render(evt.getGuiGraphics(),
                            (int) (screenWidth - (StartButtons.ICON_SIZE * 4f)),
                            StartButtons.ICON_SIZE / 2,
                            mouseX,
                            mouseY
                    );
                    renderedButtons.add(StartButtons.sandboxStartButton);
                }
            }
        }
        else if (SurvivalClientEvents.isEnabled) {
            Button nextWaveButton = SurvivalClientEvents.getNextWaveButton();
            if (!nextWaveButton.isHidden.get()) {
                nextWaveButton.tooltipOffsetY = 15;
                nextWaveButton.render(evt.getGuiGraphics(),
                        screenWidth - (StartButtons.ICON_SIZE * 2),
                        StartButtons.ICON_SIZE / 2,
                        mouseX,
                        mouseY
                );
                renderedButtons.add(nextWaveButton);
            }
        }
        else if (SandboxClientEvents.isSandboxPlayer(MC.player.getName().getString())) {
            Button exitButton = SandboxClientEvents.getExitSandboxButton();
            if (!exitButton.isHidden.get()) {
                exitButton.render(evt.getGuiGraphics(),
                        (int) (screenWidth - (StartButtons.ICON_SIZE * 2f)),
                        StartButtons.ICON_SIZE / 2,
                        mouseX,
                        mouseY
                );
                renderedButtons.add(exitButton);
            }
            Button publishScenarioButton = SandboxClientEvents.getPublishScenarioButton();
            if (!publishScenarioButton.isHidden.get()) {
                publishScenarioButton.render(evt.getGuiGraphics(),
                        (int) (screenWidth - (StartButtons.ICON_SIZE * 4f)),
                        StartButtons.ICON_SIZE / 2,
                        mouseX,
                        mouseY
                );
                renderedButtons.add(publishScenarioButton);
            }
            Button configureScenarioButton = SandboxClientEvents.getConfigureScenarioButton();
            if (!configureScenarioButton.isHidden.get()) {
                configureScenarioButton.render(evt.getGuiGraphics(),
                        (int) (screenWidth - (StartButtons.ICON_SIZE * 6f)),
                        StartButtons.ICON_SIZE / 2,
                        mouseX,
                        mouseY
                );
                renderedButtons.add(configureScenarioButton);
            }
        }

        BeaconPlacement beacon = BuildingUtils.getBeacon(true);
        if (beacon != null) {
            Button beaconButton = HelperButtons.getBeaconButton(beacon.ownerName);
            int xi = screenWidth - (StartButtons.ICON_SIZE * 2);
            if (!observerButton.isHidden.get() || !diplomacyButton.isHidden.get()) {
                xi = screenWidth - (StartButtons.ICON_SIZE * 4);
            }
            if (!beaconButton.isHidden.get()) {
                beaconButton.tooltipOffsetY = 15;
                beaconButton.render(evt.getGuiGraphics(),
                        xi,
                        40,
                        mouseX,
                        mouseY
                );
                renderedButtons.add(beaconButton);
            }
        }

        // -------------------------------------------
        // Game rules menu (spectator or sandbox only)
        // -------------------------------------------
        if (SandboxClientEvents.isSandboxPlayer() || !PlayerClientEvents.isRTSPlayer()) {
            Button gamerulesButton = GameruleClient.getGamerulesButton();
            if (MC.player != null && !gamerulesButton.isHidden.get() && !TutorialClientEvents.isEnabled()) {
                int xr = screenWidth - (StartButtons.ICON_SIZE * 8);
                int yr = PlayerClientEvents.isRTSPlayer() ? StartButtons.ICON_SIZE / 2 : 40;
                gamerulesButton.render(evt.getGuiGraphics(), xr, yr, mouseX, mouseY);
                renderedButtons.add(gamerulesButton);
                if (GameruleClient.gamerulesMenuOpen) {
                    List<Button> gameruleButtons = GameruleClient.renderGamerulesGUI(evt.getGuiGraphics(), xr, 40, mouseX, mouseY);
                    renderedButtons.addAll(gameruleButtons);
                }
            }
        }

        // --------------------
        // Tutorial Help button
        // --------------------
        if (!helpButton.isHidden.get()) {
            int xi = screenWidth - (chatButton.iconSize * 2);
            int yi = 40;
            helpButton.render(evt.getGuiGraphics(), xi, yi, mouseX, mouseY);
            renderedButtons.add(helpButton);
        }
        // ---------------------------------
        // Observer/Diplomacy Players Toggle
        // ---------------------------------
        else if (!observerButton.isHidden.get()) {
            int xi = screenWidth - (observerButton.iconSize * 2);
            int yi = 40;
            observerButton.render(evt.getGuiGraphics(), xi, yi, mouseX, mouseY);
            renderedButtons.add(observerButton);
        }
        else if (!diplomacyButton.isHidden.get()) {
            int xi = screenWidth - (diplomacyButton.iconSize * 2);
            int yi = 40;
            diplomacyButton.render(evt.getGuiGraphics(), xi, yi, mouseX, mouseY);
            renderedButtons.add(diplomacyButton);
        }
        else {
            PlayerDisplayClientEvents.resetDisplay();
        }
        // -----------
        // Chat button
        // -----------
        if (!chatButton.isHidden.get()) {
            int xi = screenWidth - (chatButton.iconSize * 2);
            int yi = 70;
            chatButton.render(evt.getGuiGraphics(), xi, yi, mouseX, mouseY);
            renderedButtons.add(chatButton);
        }
        // -------------------------
        // Select all military units
        // -------------------------
        if (!armyButton.isHidden.get()) {
            int xi = screenWidth - (armyButton.iconSize * 2);
            int yi = 100;
            armyButton.render(evt.getGuiGraphics(), xi, yi, mouseX, mouseY);
            renderedButtons.add(armyButton);
        }
        // -------------------
        // Idle workers button
        // -------------------
        if (!idleWorkerButton.isHidden.get()) {
            int xi = screenWidth - (idleWorkerButton.iconSize * 2);
            int yi = 130;
            idleWorkerButton.render(evt.getGuiGraphics(), xi, yi, mouseX, mouseY);
            evt.getGuiGraphics().drawString(
                MC.font,
                String.valueOf(idleWorkerIds.size()),
                xi + 2,
                yi + idleWorkerButton.iconSize - 1,
                0xFFFFFF
            );
            renderedButtons.add(idleWorkerButton);
        }

        // -------------------------
        // Minimap start pos buttons
        // -------------------------
        if (MC.player != null && StartPosClientEvents.isEnabled() &&
                !StartPosClientEvents.isStarting &&
                !PlayerClientEvents.rtsLocked) {

            for (StartPos startPos : StartPosClientEvents.startPoses) {
                int xc = startPos.pos.getX();
                int zc = startPos.pos.getZ();

                // Render the Button overlaid at the map screen position:
                if (MinimapClientEvents.isWorldXZinsideMap(xc, zc)) {
                    Button button = startPos.getButton(MC.player.getName().getString());
                    Vec2 worldPos = new Vec2(xc, zc);
                    Vec2 screenPos = MinimapClientEvents.worldPosToMinimapScreen((int) worldPos.x, (int) worldPos.y);
                    int btnX = (int) screenPos.x - Button.DEFAULT_ICON_FRAME_SIZE / 2;
                    int btnY = (int) screenPos.y - Button.DEFAULT_ICON_FRAME_SIZE / 2;
                    button.render(evt.getGuiGraphics(), btnX, btnY, HudClientEvents.mouseX, HudClientEvents.mouseY);
                    renderedButtons.add(button);
                }
            }
        }

        // ------------------------------------------------------
        // Button tooltips (has to be rendered last to be on top)
        // ------------------------------------------------------
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);

        TutorialClientEvents.checkAndRenderNextAction(evt.getGuiGraphics(), renderedButtons);
    }

    public static boolean isMouseOverAnyButton() {
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        return false;
    }

    public static Button getMousedOverStartPosButton() {
        for (Button button : renderedButtons)
            if (button.name.equals(StartPos.BUTTON_NAME) && button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        return null;
    }

    public static boolean isMouseOverAnyButtonOrHud() {
        for (RectZone hudZone : hudZones)
            if (hudZone.isMouseOver(mouseX, mouseY))
                return true;
        if (MinimapClientEvents.isPointInsideMinimap(mouseX, mouseY))
            return true;
        if (PlayerDisplayClientEvents.isMouseOverHud(mouseX, mouseY))
            return true;
        if (CustomBuildingClientEvents.isMouseOverHud(mouseX, mouseY))
            return true;
        if (ScenarioClientEvents.isMouseOverHud(mouseX, mouseY))
            return true;
        return isMouseOverAnyButton();
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {

        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
            }
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDownX = (int) evt.getMouseX();
            mouseLeftDownY = (int) evt.getMouseY();
        }
    }

    // for some reason some bound vanilla keys like Q and E don't trigger KeyPressed but still trigger keyReleased
    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.KeyReleased.Post evt) {
        if (TextInputClientEvents.isAnyInputFocused())
            return;
        if (MC.screen == null || !MC.screen.getTitle().getString().contains("topdowngui_container"))
            return;
        if (evt.getKeyCode() == GLFW.GLFW_KEY_F3)
            showPreselectedBlockInfo = !showPreselectedBlockInfo;
        for (Button button : renderedButtons)
            button.checkPressed(evt.getKeyCode());
    }



    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) {
            return;
        }
        if (OrthoviewClientEvents.isEnabled()) {
            portraitRendererUnit.tickAnimation();
        }

        // move camera to unit or building when its portrait is clicked/held on
        if (MiscUtil.isLeftClickDown(MC)) {
            if (buildingPortraitZone != null && buildingPortraitZone.isMouseOver(mouseX, mouseY)
                && buildingPortraitZone.isMouseOver(mouseLeftDownX, mouseLeftDownY) && MC.player != null
                && hudSelectedPlacement != null) {
                BlockPos pos = hudSelectedPlacement.centrePos;
                OrthoviewClientEvents.centreCameraOnPos(pos);

            } else if (unitPortraitZone != null && unitPortraitZone.isMouseOver(mouseX, mouseY)
                && unitPortraitZone.isMouseOver(mouseLeftDownX, mouseLeftDownY) && MC.player != null) {
                OrthoviewClientEvents.centreCameraOnPos(hudSelectedEntity.position());
            }
        }
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Post<? extends LivingEntity, ? extends Model> evt) {
        if (hudSelectedEntity != null && hudSelectedEntity.isRemoved())
            hudSelectedEntity = null;

        ArrayList<LivingEntity> units = UnitClientEvents.getSortedSelectedUnits();

        if (units.size() <= 0) {
            HudClientEvents.setHudSelectedEntity(null);
        } else if (hudSelectedEntity == null || units.size() == 1 || !units.contains(hudSelectedEntity)) {
            HudClientEvents.setHudSelectedEntity(units.get(0));
        }

        if (hudSelectedEntity == null) {
            portraitRendererUnit.model = null;
            portraitRendererUnit.renderer = null;
        } else if (evt.getEntity() == hudSelectedEntity) {
            portraitRendererUnit.model = evt.getRenderer().getModel();
            portraitRendererUnit.renderer = evt.getRenderer();
        }
    }

    @SubscribeEvent
    public static void onRenderNamePlate(RenderNameTagEvent evt) {
        //if (OrthoviewClientEvents.isEnabled())
        //    evt.setResult(Event.Result.DENY);
    }

    // MANAGE CONTROL GROUPS
    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.KeyPressed.Pre evt) {
        if (TextInputClientEvents.isAnyInputFocused())
            return;
        if (!(MC.screen instanceof TopdownGui))
            return;

        // Prevent spectator mode options from showing up
        if (OrthoviewClientEvents.isEnabled()) {
            for (Keybinding numKey : Keybindings.nums)
                if (numKey.key == evt.getKeyCode()) {
                    evt.setCanceled(true);
                }
        }

        // Deselect everything
        if (evt.getKeyCode() == Keybindings.deselect.key) {
            UnitClientEvents.clearSelectedUnits();
            BuildingClientEvents.clearSelectedBuildings();
            BuildingClientEvents.setBuildingToPlace(null);
        }

        // Initialize controlGroups with empty arrays if not already initialized
        if (controlGroups.size() < Keybindings.nums.length) {
            controlGroups.clear(); // Clear in case of a previous partial initialization
            for (Keybinding keybinding : Keybindings.nums) {
                controlGroups.add(new ControlGroup());
            }
        }

        // Access and save to controlGroups if index is within bounds
        for (Keybinding keybinding : Keybindings.nums) {
            int index = Integer.parseInt(keybinding.buttonLabel);
            if (index >= 0 && index < controlGroups.size() && evt.getKeyCode() == keybinding.key) {  // Bounds check
                if (Keybindings.ctrlMod.isDown()) {
                    controlGroups.get(index).saveFromSelected(keybinding, true);
                } else if (Keybindings.shiftMod.isDown()) {
                    controlGroups.get(index).saveFromSelected(keybinding, false);
                } else if (Keybindings.altMod.isDown()) {
                    for (ControlGroup controlGroup : controlGroups) {
                        for (LivingEntity le : getSelectedUnits())
                            controlGroup.entityIds.removeIf(id -> id == le.getId());
                        for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings())
                            controlGroup.buildingBps.removeIf(bp -> bp.equals(bpl.originPos));
                    }
                    controlGroups.get(index).saveFromSelected(keybinding, true);
                }
            }
        }

        // Open chat while orthoview is enabled
        if (OrthoviewClientEvents.isEnabled() && evt.getKeyCode() == Keybindings.chat.key) {
            MC.setScreen(new ChatScreen(""));
        }

        // Cycle through selected units
        if (evt.getKeyCode() == Keybindings.tab.key) {
            cycleUnitSubgroups();
            cycleBuildingSubgroups();
        }
    }

    private static void cycleUnitSubgroups() {
        List<LivingEntity> selUnits = UnitClientEvents.getSortedSelectedUnits();
        var unitNameSet = new LinkedHashSet<String>();
        Set<String> uniqueValues = new HashSet<>();
        for (LivingEntity selUnit : selUnits) {
            String modifiedEntityName = getModifiedEntityName(selUnit);
            if (uniqueValues.add(modifiedEntityName)) {
                unitNameSet.add(modifiedEntityName);
            }
        }
        var unitNames = new LinkedList<>(unitNameSet);

        if (unitNames.size() <= 1 || hudSelectedEntity == null)
            return;

        boolean reversed = Keybindings.shiftMod.isDown();
        String selUnitName = getModifiedEntityName(hudSelectedEntity);
        if (reversed)
            Collections.reverse(unitNames);

        // find the next unit name
        String newUnitName = "";
        boolean foundSelected = false;
        for (String uname : unitNames) {
            if (foundSelected) {
                newUnitName = uname;
                break;
            } else if (uname.equals(selUnitName)) {
                foundSelected = true;
            }
        }
        if (newUnitName.isBlank() && !selUnits.isEmpty()) {
            if (reversed) {
                setHudSelectedEntity(selUnits.get(selUnits.size() - 1));
            } else {
                setHudSelectedEntity(selUnits.get(0));
            }
        } else {
            for (LivingEntity le : selUnits) {
                String bplName = getModifiedEntityName(le);
                if (bplName.equals(newUnitName)) {
                    setHudSelectedEntity(le);
                    break;
                }
            }
        }
    }

    private static void cycleBuildingSubgroups() {
        List<BuildingPlacement> selBuildings = BuildingClientEvents.getSelectedBuildings();
        List<String> buildingNames = selBuildings
                .stream().map(b -> {
                    if (b.getBuilding() instanceof CustomBuilding cb) {
                        return cb.name;
                    }
                    return ReignOfNetherRegistries.BUILDING.getKey(b.getBuilding()).toString();
                })
                .distinct().sorted(Comparator.comparing(b -> b))
                .collect(Collectors.toList());

        if (buildingNames.size() <= 1 || hudSelectedPlacement == null)
            return;

        boolean reversed = Keybindings.shiftMod.isDown();
        String selBuildingName = ReignOfNetherRegistries.BUILDING.getKey(hudSelectedPlacement.getBuilding()).toString();

        if (reversed)
            Collections.reverse(buildingNames);

        // find the next building name
        String newBuildingName = "";
        boolean foundSelected = false;
        for (String bname : buildingNames) {
            if (foundSelected) {
                newBuildingName = bname;
                break;
            } else if (bname.equals(selBuildingName)) {
                foundSelected = true;
            }
        }
        if (newBuildingName.isBlank() && !selBuildings.isEmpty()) {
            if (reversed) {
                hudSelectedPlacement = selBuildings.get(selBuildings.size() - 1);
            } else {
                hudSelectedPlacement = selBuildings.get(0);
            }
        } else {
            for (BuildingPlacement bpl : selBuildings) {
                String bplName = ReignOfNetherRegistries.BUILDING.getKey(bpl.getBuilding()).toString();
                if (bplName.equals(newBuildingName)) {
                    hudSelectedPlacement = bpl;
                    break;
                }
            }
        }
    }

    // newUnitIds are replacing oldUnitIds - replace them in every control group while retaining their index
    public static void convertControlGroups(int[] oldUnitIds, int[] newUnitIds) {
        if (MC.level == null) {
            return;
        }
        for (ControlGroup group : controlGroups) {
            for (int i = 0; i < oldUnitIds.length; i++) {
                for (int j = 0; j < group.entityIds.size(); j++) {
                    if (group.entityIds.get(j) == oldUnitIds[i]) {
                        group.entityIds.add(j, newUnitIds[i]);
                        break;
                    }
                }
            }
            for (int i = 0; i < oldUnitIds.length; i++) {
                for (int j = 0; j < oldUnitIds.length; j++) {
                    final int k = j;
                    group.entityIds.removeIf(id -> id == oldUnitIds[k]);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        if (MC.screen != null && MC.level != null && SandboxClientEvents.isSandboxPlayer() && showPreselectedBlockInfo) {
            int y = 5;
            for (ControlGroup controlGroup : controlGroups) {
                if (!controlGroup.buildingBps.isEmpty() || !controlGroup.entityIds.isEmpty()) {
                    y += 20;
                    break;
                }
            }
            BlockPos bp = CursorClientEvents.getPreselectedBlockPos();
            evt.getGuiGraphics().drawString(MC.font, "BlockPos: " + bp.toShortString(), 100, y, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, MC.level.getBlockState(bp).getBlock().toString().replaceFirst("Block", ""), 100, y + 10, 0xFFFFFF);
        }
    }
}
