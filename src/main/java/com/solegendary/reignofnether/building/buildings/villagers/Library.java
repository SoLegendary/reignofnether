package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.EnchantAbility;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
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
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Library extends ProductionBuilding implements RangeIndicatorAddon {
    private static final EnchantMaiming ENCHANT_MAIMING = new EnchantMaiming();
    private static final EnchantQuickCharge ENCHANT_QUICK_CHARGE = new EnchantQuickCharge();
    private static final EnchantSharpness ENCHANT_SHARPNESS = new EnchantSharpness();
    private static final EnchantMultishot ENCHANT_MULTISHOT = new EnchantMultishot();
    private static final EnchantVigor ENCHANT_VIGOR = new EnchantVigor();

    public static final DataType<EnchantAbility> AUTO_CAST_ENCHANT = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "auto_cast_enchant"), (tag, server) -> {
        int id = tag.getInt("autocast-id");
        return switch (id) {
            case 1: yield ENCHANT_MAIMING;
            case 2: yield ENCHANT_QUICK_CHARGE;
            case 3: yield ENCHANT_SHARPNESS;
            case 4: yield ENCHANT_MULTISHOT;
            case 5: yield ENCHANT_VIGOR;
            default: yield null;
        };
    }, enchantAbility -> {
        int id = 0;
        if (enchantAbility instanceof EnchantMaiming)
            id = 1;
        if (enchantAbility instanceof EnchantQuickCharge)
            id = 2;
        if (enchantAbility instanceof EnchantSharpness)
            id = 3;
        if (enchantAbility instanceof EnchantMultishot)
            id = 4;
        if (enchantAbility instanceof EnchantVigor)
            id = 5;
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("autocast-id", id);
        return nbt;
    });


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

        this.abilities.add(ENCHANT_MAIMING, Keybindings.keyQ);
        this.abilities.add(ENCHANT_QUICK_CHARGE, Keybindings.keyW);
        this.abilities.add(ENCHANT_SHARPNESS, Keybindings.keyE);
        this.abilities.add(ENCHANT_MULTISHOT, Keybindings.keyR);
        this.abilities.add(ENCHANT_VIGOR, Keybindings.keyT);

        this.productions.add(ProductionItems.RESEARCH_LINGERING_POTIONS, Keybindings.keyY);
        this.productions.add(ProductionItems.RESEARCH_HEALING_POTIONS, Keybindings.keyU);
        this.productions.add(ProductionItems.RESEARCH_WATER_POTIONS, Keybindings.keyI);
        this.productions.add(ProductionItems.RESEARCH_EVOKER_VEXES, Keybindings.keyO);
        this.productions.add(ProductionItems.RESEARCH_GRAND_LIBRARY, Keybindings.keyP);

        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    public Faction getFaction() {
        return Faction.VILLAGERS;
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/enchanting_table_top.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.LIBRARY,
            TutorialClientEvents::isEnabled,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.library"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.library.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.library.tooltip2"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.library.tooltip3"),
                    Style.EMPTY
                )
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
        return I18n.get("buildings.villagers.reignofnether.library.upgraded");
    }

    @Override
    public void onBuilt(BuildingPlacement buildingPlacement) {
        super.onBuilt(buildingPlacement);
        updateHighlightBps(buildingPlacement);
    }

    @Override
    public void tick(Level tickLevel, BuildingPlacement bp) {
        super.tick(tickLevel, bp);

        EnchantAbility autoCastEnchant = bp.getDataStorage().getData(AUTO_CAST_ENCHANT);

        if (bp.getTickAgeAfterBuilt() > 0 && bp.getTickAgeAfterBuilt() % 15 == 0 && bp.isBuilt && autoCastEnchant != null
                && autoCastEnchant.isOffCooldown(bp)) {

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
                if ((
                        autoCastEnchant.isCorrectUnitAndEquipment(e) && autoCastEnchant.canAfford(bp)
                                && autoCastEnchant.getMutuallyExclusiveEnchant(e) == null
                )) {
                    mobs.add(e);
                }
            }
            if (!mobs.isEmpty()) {
                autoCastEnchant.use(tickLevel, bp, mobs.get(0));
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
        return false;
    }
}
