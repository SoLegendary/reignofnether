package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.abilities.CallToArmsUnit;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.ActionButtons;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.buttons.HelperButtons;
import com.solegendary.reignofnether.hud.layout.CommandGrid;
import com.solegendary.reignofnether.hud.layout.CommandGrid.Command;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.sandbox.SandboxActionButtons;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.NonUnitClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.solegendary.reignofnether.hud.layout.CommandGrid.*;

/**
 * The 5x3 command card at the bottom-left. Keys come from the slot (QWERT / ASDFG / ZXCVB), so the
 * same position always has the same key whatever is selected.
 */
public final class CommandCard {

    private CommandCard() { }

    private static int page = 0;
    private static Object pageOwner = null;

    // buildings a worker's advanced tab shows; the rest are on the basic tab
    private static final java.util.Set<Building> ADVANCED_BUILDINGS = java.util.Set.of(
            Buildings.DUNGEON, Buildings.SPIDER_LAIR, Buildings.SLIME_PIT, Buildings.LABORATORY, Buildings.STRONGHOLD,
            Buildings.BASTION, Buildings.HOGLIN_STABLES, Buildings.FLAME_SANCTUARY, Buildings.WITHER_SHRINE,
            Buildings.BASALT_SPRINGS, Buildings.FORTRESS,
            Buildings.BLACKSMITH, Buildings.WITCH_HUT, Buildings.ARCANE_TOWER, Buildings.LIBRARY, Buildings.CASTLE,
            Buildings.IRON_GOLEM_BUILDING, Buildings.BEACON);

    // a worker's open build tab: 0 = its commands, 1 = basic buildings, 2 = advanced buildings
    private static int tab = 0;

    private record Contents(String title, List<Command<Button>> commands, List<Button> abilities) {
        boolean isEmpty() {
            return commands.isEmpty() && abilities.isEmpty();
        }

        static Contents empty(String title) {
            return new Contents(title, List.of(), List.of());
        }
    }

    public static void reset() {
        page = 0;
        tab = 0;
        pageOwner = null;
        gatherKey = null;
    }

    public static void render(HudFrame f) {
        if (SpectatorHud.isActive())
            return; // spectators can't command anything
        Rect card = f.layout.commandCard();
        Contents c = contents();
        // a floating card, only there when there is something to command
        if (c.isEmpty())
            return;
        int pad = HudLayout.CONSOLE_PAD;
        Rect panel = new Rect(card.x() - pad, card.y() - pad, card.w() + pad * 2, card.h() + pad * 2);
        Theme.panel(f.g, panel.x(), panel.y(), panel.w(), panel.h(), Theme.FLOATING);
        f.zone(panel);

        Object owner = HudClientEvents.hudSelectedPlacement != null ? HudClientEvents.hudSelectedPlacement : HudClientEvents.hudSelectedEntity;
        if (owner != pageOwner) {
            pageOwner = owner;
            page = 0;
            tab = 0;
        }
        CommandGrid.Layout<Button> layout = CommandGrid.assign(c.commands(), c.abilities(), page);

        Text.small(f.g, Text.fitSmall(c.title().toUpperCase(Locale.ROOT), card.w() - 70), card.x(), card.y(), Theme.LABEL);
        renderSkillPointChip(f, card);

        for (int slot = 0; slot < CommandGrid.SLOTS; slot++) {
            int x = f.layout.slotX(slot);
            int y = f.layout.slotY(slot);
            Button b = layout.slots().get(slot);
            if (slot == layout.pageSlot())
                renderPageCell(f, x, y, slot, layout.pageCount());
            else if (b == null)
                ButtonCell.renderEmpty(f.g, x, y, HudLayout.CELL, Keybindings.grid[slot].getCurrentLabel());
            else
                renderCell(f, b, x, y, slot);
        }
    }

    private static void renderCell(HudFrame f, Button b, int x, int y, int slot) {
        Runnable rankUp = rankUpAction(b);
        f.cell(isUnlearnt(b) ? locked(b) : b, x, y, HudLayout.CELL, Keybindings.grid[slot], rankUp);
        if (rankUp == null)
            return;
        // pulsing "+" in the corner, clickable on its own
        int s = 9;
        int px = x + HudLayout.CELL - s;
        boolean on = (System.currentTimeMillis() / 500) % 2 == 0;
        Theme.outline(f.g, px, y, s, s, 1, Theme.ON_ORANGE);
        f.g.fill(px, y, px + s, y + s, on ? Theme.ORANGE : Theme.ORANGE_DARK);
        Icon.PLUS.drawIn(f.g, px - 1, y - 1, s + 2, s + 2, Theme.ON_ORANGE);
        Button plus = new ButtonBuilder("rank up").onLeftClick(rankUp).build();
        f.register(plus, new Rect(px, y, s, s), null, () -> Tooltip.of(I18n.get("hud.reignofnether.rank_up"))
                .status(I18n.get("hud.reignofnether.rank_up_hint", Keybindings.grid[slot].getCurrentLabel()), Theme.GOLD));
    }

    // unlearnt hero abilities stay on the grid, locked, so their "+" can be clicked and their key never moves
    private static boolean isUnlearnt(Button b) {
        return b instanceof AbilityButton ab && ab.ability instanceof HeroAbility ha && ab.getUnit() instanceof HeroUnit hero
                && ha.getRank(hero) == 0;
    }

    private static Button locked(Button b) {
        return new ButtonBuilder(b.name).iconResource(b.iconResource).innerIconSizeModifier(b.innerIconSizeModifier)
                .isEnabled(() -> false).tooltipLines(b.tooltipLines).build();
    }

    private static Runnable rankUpAction(Button b) {
        if (!(b instanceof AbilityButton ab) || !(ab.ability instanceof HeroAbility ha) || !(ab.getUnit() instanceof HeroUnit hero))
            return null;
        if (ha.getRank(hero) >= ha.maxRank)
            return null;
        Button rankUp = ha.getRankUpButton(hero);
        if (rankUp == null || !rankUp.isEnabled.get())
            return null;
        return rankUp.onLeftClick;
    }

    private static void renderSkillPointChip(HudFrame f, Rect card) {
        if (!(HudClientEvents.hudSelectedEntity instanceof HeroUnit hero) || hero.getSkillPoints() <= 0
                || UnitClientEvents.getPlayerToEntityRelationship(HudClientEvents.hudSelectedEntity) != Relationship.OWNED)
            return;
        String s = I18n.get("hud.reignofnether.skill_points", hero.getSkillPoints());
        int w = Text.smallWidth(s) + 6;
        int x = card.right() - w;
        Theme.orangeChip(f.g, x, card.y(), w, 8);
        Text.small(f.g, s, x + 3, card.y(), Theme.ON_ORANGE);
    }

    private static void renderPageCell(HudFrame f, int x, int y, int slot, int pageCount) {
        Button next = new ButtonBuilder("next page").onLeftClick(() -> page = (page + 1) % pageCount).build();
        Rect r = new Rect(x, y, HudLayout.CELL, HudLayout.CELL);
        Theme.cell(f.g, x, y, r.w(), r.h(), Theme.CellState.READY, f.hovered(r));
        String label = (Math.floorMod(page, pageCount) + 1) + "/" + pageCount;
        Text.bigCentered(f.g, label, r.centerX(), r.centerY() - 3, Theme.GOLD, Text.px(16));
        Theme.keyCap(f.g, Keybindings.grid[slot].getCurrentLabel(), x + 1, y + 1, Theme.MUTED);
        f.register(next, r, Keybindings.grid[slot], () -> Tooltip.of(I18n.get("hud.reignofnether.next_page"))
                .status(I18n.get("hud.reignofnether.page", Math.floorMod(page, pageCount) + 1, pageCount), Theme.MUTED)
                .key(Keybindings.grid[slot].getCurrentLabel()));
    }

    // ------------------------------------------------------------------ what goes on the card

    private static Contents contents() {
        BuildingPlacement building = HudClientEvents.hudSelectedPlacement;
        LivingEntity entity = HudClientEvents.hudSelectedEntity;
        List<LivingEntity> selUnits = UnitClientEvents.getSortedSelectedUnits();

        if (building != null)
            return buildingContents(building);
        if (!selUnits.isEmpty() && entity instanceof Unit unit && canCommand(selUnits.get(0)))
            return unitContents(entity, unit, selUnits.get(0));
        if (!(entity instanceof Unit) && !UnitClientEvents.getSelectedUnits().isEmpty() && NonUnitClientEvents.canControlAllMobs())
            return mobContents();
        if (SandboxClientEvents.isSandboxPlayer() && selUnits.isEmpty() && BuildingClientEvents.getSelectedBuildings().isEmpty())
            return sandboxContents();
        return Contents.empty(I18n.get("hud.reignofnether.card.no_selection"));
    }

    private static boolean canCommand(LivingEntity first) {
        return UnitClientEvents.getPlayerToEntityRelationship(first) == Relationship.OWNED
                || !PlayerClientEvents.isRTSPlayer()
                || NonUnitClientEvents.canControlAllMobs()
                || AlliancesClient.canControlAlly(first);
    }

    private static Contents unitContents(LivingEntity entity, Unit unit, LivingEntity first) {
        List<Command<Button>> commands = new ArrayList<>();
        boolean worker = entity instanceof WorkerUnit;

        if (entity instanceof AttackerUnit)
            commands.add(new Command<>(ActionButtons.attack, SLOT_A));
        commands.add(new Command<>(ActionButtons.stop, SLOT_S));
        if (!worker)
            commands.add(new Command<>(ActionButtons.hold, SLOT_D));
        else if (canReturnResources(entity, unit))
            commands.add(new Command<>(returnResourcesButton(unit), SLOT_D));
        if (worker) {
            commands.add(new Command<>(ActionButtons.buildRepair, SLOT_F));
            updateGatherButton((WorkerUnit) entity);
            commands.add(new Command<>(ActionButtons.gather, SLOT_G));
        }
        Button garrison = null;
        if (unit.canGarrison() && GarrisonableBuildingAddon.getGarrison(unit) == null)
            garrison = ActionButtons.garrison;
        else if (GarrisonableBuildingAddon.getGarrison(unit) != null)
            garrison = ActionButtons.ungarrison;
        if (!worker && garrison != null)
            commands.add(new Command<>(garrison, SLOT_F));

        // workers: Q-T abilities, A-G commands, Z/X open the basic/advanced building tabs, which fill Q-G
        // in a fixed order, so no building changes key when garrison or return resources appears
        List<Button> abilities = new ArrayList<>();
        List<Button> basic = new ArrayList<>();
        List<Button> advanced = new ArrayList<>();
        if (worker) {
            abilities.add(garrison); // null keeps Q, so the rest stay put
            if (entity instanceof VillagerUnit vUnit)
                for (Ability ability : vUnit.getAbilities().get())
                    if (ability instanceof CallToArmsUnit callToArms)
                        abilities.add(callToArms.getButton(Keybindings.hotkey1, vUnit));
            if (entity instanceof VillagerUnit vUnit)
                abilities.add(professionPassive(vUnit));
        }
        boolean showAbilities = TutorialClientEvents.isAtOrPastStage(TutorialStage.BUILD_INTRO) &&
                (UnitClientEvents.getPlayerToEntityRelationship(first) == Relationship.OWNED
                        || !PlayerClientEvents.isRTSPlayer() || ResearchClient.hasCheat("wouldyoukindly"))
                || AlliancesClient.canControlAlly(first);
        if (showAbilities)
            for (Button b : unit.getAbilityButtons())
                if ((!b.isHidden.get() || isUnlearnt(b)) && !(b instanceof AbilityButton ab && ab.ability instanceof CallToArmsUnit)) {
                    if (b instanceof BuildingPlaceButton bp)
                        (ADVANCED_BUILDINGS.contains(bp.building) ? advanced : basic).add(b);
                    else
                        abilities.add(b);
                }
        addSandboxUnitActions(abilities, true);

        String name = SelectionPanel.displayName(entity);
        String mode = I18n.get(worker ? "hud.reignofnether.card.build" : entity instanceof HeroUnit ? "hud.reignofnether.card.hero" : "hud.reignofnether.card.army");
        if (basic.isEmpty() && advanced.isEmpty())
            return new Contents(mode + " · " + name, commands, abilities);

        List<Command<Button>> tabs = new ArrayList<>();
        tabs.add(new Command<>(tabButton(1, "hud.reignofnether.card.basic", basic), SLOT_Z));
        if (!advanced.isEmpty())
            tabs.add(new Command<>(tabButton(2, "hud.reignofnether.card.advanced", advanced), SLOT_Z + 1));
        if (tab == 0) {
            commands.addAll(tabs);
            return new Contents(mode + " · " + name, commands, abilities);
        }
        List<Button> buildings = tab == 1 ? basic : advanced;
        List<Button> overflow = new ArrayList<>();
        for (int i = 0; i < buildings.size(); i++) {
            if (i < SLOT_Z)
                tabs.add(new Command<>(buildings.get(i), i)); // Q-T then A-G, row by row
            else
                overflow.add(buildings.get(i));
        }
        return new Contents(I18n.get(tab == 1 ? "hud.reignofnether.card.basic" : "hud.reignofnether.card.advanced") + " · " + name, tabs, overflow);
    }

    // the profession's bonus as a passive, with its tool: iron, diamond once veteran (what the villager holds)
    private static Button professionPassive(VillagerUnit v) {
        String tool = switch (SelectionPanel.professionShown(v)) {
            case FARMER -> "hoe";
            case LUMBERJACK -> "axe";
            case MINER -> "pickaxe";
            case MASON -> "shovel";
            case HUNTER -> "sword";
            default -> null;
        };
        if (tool == null)
            return null;
        boolean hunter = tool.equals("sword");
        String tier = v.isVeteran() ? (hunter ? "stone_" : "diamond_") : (hunter ? "wooden_" : "iron_");
        ResourceLocation icon = ResourceLocation.withDefaultNamespace("textures/item/" + tier + tool + ".png");
        return new AbilityButton(I18n.get("units.reignofnether." + SelectionPanel.professionShown(v).name().toLowerCase(Locale.ROOT)),
                icon, null, () -> false, () -> false, () -> v.getUnitProfession() != VillagerUnitProfession.NONE,
                null, null, List.of(), null, v) {
            @Override
            public Tooltip hudTooltip() {
                return SelectionPanel.professionTooltip(v);
            }
        };
    }

    // opens its tab, or goes back to the worker's commands when it's already open
    private static Button tabButton(int n, String key, List<Button> buildings) {
        return new ButtonBuilder(I18n.get(key))
                .iconResource(buildings.isEmpty() ? null : buildings.get(0).iconResource)
                .isSelected(() -> tab == n)
                .onLeftClick(() -> {
                    tab = tab == n ? 0 : n;
                    page = 0;
                })
                .tooltipLines(List.of(MiscUtil.fcs(I18n.get(key))))
                .build();
    }

    private static boolean canReturnResources(LivingEntity entity, Unit unit) {
        return entity instanceof Mob mob && mob.canPickUpLoot()
                && Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() > 0
                && (UnitClientEvents.getPlayerToEntityRelationship(entity) == Relationship.OWNED || AlliancesClient.canControlAlly(entity));
    }

    private static Button returnResourcesButton(Unit unit) {
        return new ButtonBuilder(I18n.get("hud.reignofnether.drop_off_resources"))
                .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/chest.png"))
                .isSelected(() -> unit.getReturnResourcesGoal().getBuildingTarget() != null)
                .onLeftClick(() -> UnitClientEvents.sendUnitCommand(UnitAction.RETURN_RESOURCES_TO_CLOSEST))
                .tooltipLines(List.of(MiscUtil.fcs(I18n.get("hud.reignofnether.drop_off_resources"))))
                .build();
    }

    private static String gatherKey = null; // icon + resource last applied, so render doesn't rebuild them every frame

    private static void updateGatherButton(WorkerUnit worker) {
        String icon = switch (worker.getGatherResourceGoal().getTargetResourceName()) {
            case FOOD -> "hoe";
            case WOOD -> "axe";
            case ORE -> "pickaxe";
            default -> "no_gather";
        };
        String resource = UnitClientEvents.getSelectedUnitResourceTarget().toString().toLowerCase(Locale.ENGLISH);
        if ((icon + resource).equals(gatherKey))
            return;
        gatherKey = icon + resource;
        ActionButtons.gather.iconResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/" + icon + ".png");
        ActionButtons.gather.tooltipLines = List.of(
                MiscUtil.fcs(I18n.get("hud.reignofnether.gather_resources", I18n.get("resources.reignofnether." + resource))),
                MiscUtil.fcs(I18n.get("hud.reignofnether.change_target_resource")));
    }

    private static Contents mobContents() {
        List<Command<Button>> commands = new ArrayList<>();
        if (NonUnitClientEvents.canAttack(UnitClientEvents.getSelectedUnits().get(0)))
            commands.add(new Command<>(ActionButtons.attack, SLOT_A));
        commands.add(new Command<>(ActionButtons.stop, SLOT_S));
        return new Contents(SelectionPanel.displayName(UnitClientEvents.getSelectedUnits().get(0)), commands, List.of());
    }

    private static Contents buildingContents(BuildingPlacement placement) {
        boolean owned = BuildingClientEvents.getPlayerToBuildingRelationship(placement) == Relationship.OWNED
                || SandboxClientEvents.isSandboxPlayer();
        String name = SelectionPanel.buildingName(placement);
        if (!owned && PlayerClientEvents.isRTSPlayer())
            return Contents.empty(name);

        List<Command<Button>> commands = new ArrayList<>();
        List<Button> abilities = new ArrayList<>();

        if (!placement.isBuilt && !HelperButtons.buildingCancelButton.isHidden.get())
            commands.add(new Command<>(HelperButtons.buildingCancelButton, SLOT_B));

        if (placement.isBuilt || placement.allowProdWhileBuilding) {
            if (ItemClientEvents.ENABLED && placement instanceof ItemShopPlacement shop && !shop.getStockedItems().isEmpty())
                commands.add(new Command<>(shopToggleButton(shop), NO_PREFERENCE));
            if (placement instanceof ProductionPlacement prod)
                for (Button b : prod.productionButtons)
                    if (!b.isHidden.get())
                        abilities.add(b);
            if (placement.isBuilt)
                for (AbilityButton b : placement.getAbilityButtons())
                    if (b != null && !b.isHidden.get())
                        commands.add(new Command<>(b, NO_PREFERENCE));
        }
        addSandboxUnitActions(abilities, false);
        String mode = placement instanceof ProductionPlacement ? I18n.get("hud.reignofnether.card.train") : I18n.get("hud.reignofnether.card.building");
        return new Contents(name + " · " + mode, commands, abilities);
    }

    private static Button shopToggleButton(ItemShopPlacement shop) {
        return new ButtonBuilder("Shop Menu")
                .iconResource(ButtonCell.ICON_EMERALD)
                .tooltipLines(List.of(MiscUtil.fcs(I18n.get("itemshop.reignofnether.toggle_menu"))))
                .isSelected(() -> ItemClientEvents.openItemShop == shop)
                .onLeftClick(() -> ItemClientEvents.openItemShop = ItemClientEvents.openItemShop == shop ? null : shop)
                .build();
    }

    private static void addSandboxUnitActions(List<Button> list, boolean unit) {
        if (!SandboxClientEvents.isSandboxPlayer())
            return;
        list.add(SandboxActionButtons.getSetRelationshipButton());
        list.add(SandboxActionButtons.getCycleScenarioRoleButton());
        if (!unit)
            list.add(SandboxActionButtons.removeBuildingPlacement);
        if (unit && HudClientEvents.hudSelectedEntity instanceof Unit) {
            list.add(SandboxActionButtons.setAnchor);
            list.add(SandboxActionButtons.resetToAnchor);
            list.add(SandboxActionButtons.removeAnchor);
        }
        list.removeIf(b -> b != null && b.isHidden.get()); // null: a worker's kept-empty slot
    }

    private static Contents sandboxContents() {
        List<Command<Button>> commands = new ArrayList<>();
        for (Button b : List.of(
                SandboxClientEvents.getCycleBuildingOrUnitsButton(),
                SandboxClientEvents.getToggleFactionButton(),
                SandboxClientEvents.getToggleRelationshipButton(),
                SandboxClientEvents.sandboxMenuType == com.solegendary.reignofnether.sandbox.SandboxMenuType.UNITS
                        ? SandboxClientEvents.getToggleUnitCheatsButton()
                        : SandboxClientEvents.getToggleBuildingCheatsButton(),
                SandboxClientEvents.getToggleNonUnitControlButton(),
                SandboxClientEvents.getSortCustomBuildingsButton()))
            if (!b.isHidden.get())
                commands.add(new Command<>(b, NO_PREFERENCE));

        List<Button> spawns = new ArrayList<>(switch (SandboxClientEvents.sandboxMenuType) {
            case UNITS -> SandboxClientEvents.getUnitButtons();
            case BUILDINGS -> SandboxClientEvents.getBuildingButtons();
            case CUSTOM_BUILDINGS -> SandboxClientEvents.getCustomBuildingButtons();
        });
        spawns.removeIf(b -> b.isHidden.get());
        return new Contents(I18n.get("hud.reignofnether.card.sandbox"), commands, spawns);
    }
}
