package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.abilities.EnchantGust;
import com.solegendary.reignofnether.ability.abilities.EnchantMaiming;
import com.solegendary.reignofnether.ability.abilities.EnchantMultishot;
import com.solegendary.reignofnether.ability.abilities.EnchantQuickCharge;
import com.solegendary.reignofnether.ability.abilities.EnchantSharpness;
import com.solegendary.reignofnether.ability.abilities.EnchantVigor;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Library extends ProductionBuilding implements RangeIndicatorAddon {
    private static final EnchantMaiming ENCHANT_MAIMING = new EnchantMaiming();
    private static final EnchantQuickCharge ENCHANT_QUICK_CHARGE = new EnchantQuickCharge();
    private static final EnchantSharpness ENCHANT_SHARPNESS = new EnchantSharpness();
    private static final EnchantMultishot ENCHANT_MULTISHOT = new EnchantMultishot();
    private static final EnchantVigor ENCHANT_VIGOR = new EnchantVigor();
    private static final EnchantGust ENCHANT_GUST = new EnchantGust();

    // a save id is the enchant's index + 1, so only ever append to this list
    private static final List<EnchantAbility> ENCHANTS = List.of(
            ENCHANT_MAIMING, ENCHANT_QUICK_CHARGE, ENCHANT_SHARPNESS, ENCHANT_MULTISHOT, ENCHANT_VIGOR, ENCHANT_GUST);

    // any number of enchants can be autocast at once
    // (older saves stored a single enchant under "autocast-id" instead of "autocast-ids")
    public static final DataType<Set<EnchantAbility>> AUTO_CAST_ENCHANTS = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "auto_cast_enchant"), (tag, server) -> {
        Set<EnchantAbility> enchants = new LinkedHashSet<>();
        List<Integer> ids = new ArrayList<>();
        for (int id : tag.getIntArray("autocast-ids"))
            ids.add(id);
        if (tag.contains("autocast-id"))
            ids.add(tag.getInt("autocast-id"));
        for (int id : ids)
            if (id >= 1 && id <= ENCHANTS.size())
                enchants.add(ENCHANTS.get(id - 1));
        return enchants;
    }, enchants -> {
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("autocast-ids", enchants.stream().mapToInt(e -> ENCHANTS.indexOf(e) + 1).filter(id -> id > 0).toArray());
        return nbt;
    }, LinkedHashSet::new);

    public static boolean isAutoCasting(BuildingPlacement placement, EnchantAbility enchant) {
        return placement.getDataStorage().getData(AUTO_CAST_ENCHANTS).contains(enchant);
    }

    public static void toggleAutoCast(BuildingPlacement placement, EnchantAbility enchant) {
        Set<EnchantAbility> enchants = placement.getDataStorage().getData(AUTO_CAST_ENCHANTS);
        if (!enchants.remove(enchant))
            enchants.add(enchant);
    }

    public final static String buildingName = "Library";
    public final static String structureName = "library";
    public final static String upgradedStructureName = "library_grand";
    public final static ResourceCost cost = ResourceCosts.LIBRARY;

    public Library() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.ENCHANTING_TABLE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/enchanting_table_top.png");

        this.buildTimeModifier = 1.1f;

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.OAK_LOG);
        this.startingBlockTypes.add(Blocks.SPRUCE_STAIRS);

        this.explodeChance = 0.2f;
        this.maxHealth = 125d;
        this.maxHealthBonusPerUpgradeLevel = 25d;

        this.abilities.add(ENCHANT_MAIMING, Keybindings.abilitySlot1);
        this.abilities.add(ENCHANT_QUICK_CHARGE, Keybindings.abilitySlot2);
        this.abilities.add(ENCHANT_SHARPNESS, Keybindings.abilitySlot3);
        this.abilities.add(ENCHANT_MULTISHOT, Keybindings.abilitySlot4);
        this.abilities.add(ENCHANT_VIGOR, Keybindings.abilitySlot5);
        this.abilities.add(ENCHANT_GUST, Keybindings.abilitySlot6);

        this.productions.add(ProductionItems.RESEARCH_EVOKER_VEXES, Keybindings.abilitySlot7);
        this.productions.add(ProductionItems.RESEARCH_UPGRADED_WINDCALLERS, Keybindings.abilitySlot8);
        this.productions.add(ProductionItems.RESEARCH_GRAND_LIBRARY, Keybindings.abilitySlot9);

        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/enchanting_table_top.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.LIBRARY,
            TutorialClientEvents::isEnabled,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(Component.translatable("buildings.reignofnether.library").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.library.tooltip1").getVisualOrderText(),
                Component.translatable("buildings.reignofnether.library.tooltip2").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.library.tooltip3").getVisualOrderText()
            ),
            this
        );
    }

    // check that the flag is built based on existing placed blocks
    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.GLOWSTONE) {
                return 1;
            }
        return 0;
    }

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }

    @Override
    public String getUpgradedName(BuildingPlacement placement) {
        return Component.translatable("buildings.reignofnether.library.upgraded").getString();
    }

    @Override
    public void onBuilt(BuildingPlacement buildingPlacement) {
        super.onBuilt(buildingPlacement);
        updateHighlightBps(buildingPlacement);
    }

    @Override
    public void tick(Level tickLevel, BuildingPlacement bp) {
        super.tick(tickLevel, bp);

        if (bp.getTickAgeAfterBuilt() > 0 && bp.getTickAgeAfterBuilt() % 15 == 0 && bp.isBuilt) {
            for (EnchantAbility autoCastEnchant : new ArrayList<>(bp.getDataStorage().getData(AUTO_CAST_ENCHANTS))) {
                if (!autoCastEnchant.isOffCooldown(bp))
                    continue;

                List<Mob> mobs = new ArrayList<>();
                for (Mob e : MiscUtil.getEntitiesWithinRange(new Vector3d(
                                bp.centrePos.getX(),
                                bp.centrePos.getY(),
                                bp.centrePos.getZ()
                        ),
                        autoCastEnchant.range - 1,
                        Mob.class,
                        tickLevel
                )) {
                    // skip units that can't be enchanted (other owners, already enchanted) so they don't block eligible ones
                    if (e instanceof Unit unit && unit.getOwnerName().equals(bp.ownerName) &&
                            autoCastEnchant.isCorrectUnitAndEquipment(e) &&
                            !autoCastEnchant.hasSameEnchant(e) &&
                            autoCastEnchant.canAfford(bp)) {
                        mobs.add(e);
                    }
                }
                if (!mobs.isEmpty()) {
                    autoCastEnchant.use(tickLevel, bp, mobs.get(0));
                }
            }
        }
        if (tickLevel.isClientSide && bp.getTickAgeAfterBuilt() > 0 && bp.getTickAgeAfterBuilt() % 100 == 0)
            updateHighlightBps(bp);

    }

    @Override
    public int getRange(BuildingPlacement placement) {
        return EnchantAbility.RANGE;
    }

    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide())
            return;
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).clear();
        placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE).addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                getRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ, placement.level));
    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return true;
    }
}
