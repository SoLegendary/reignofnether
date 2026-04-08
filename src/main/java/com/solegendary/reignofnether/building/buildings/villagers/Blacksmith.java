package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.EquipAbility;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
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

public class Blacksmith extends ProductionBuilding implements RangeIndicatorAddon {
    private static final EquipLeatherChestplate EQUIP_LEATHER_CHESTPLATE_INSTANCE = new EquipLeatherChestplate();
    private static final EquipChainmailChestplate EQUIP_CHAINMAIL_CHESTPLATE = new EquipChainmailChestplate();

    public static final DataType<EquipAbility> AUTO_CAST_EQUIP = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "auto_cast_equip"), (tag, server) -> {
        int id = tag.getInt("autocast-id");
        return switch (id) {
            case 1: yield EQUIP_LEATHER_CHESTPLATE_INSTANCE;
            case 2: yield EQUIP_CHAINMAIL_CHESTPLATE;
            default: yield null;
        };
    }, equipAbility -> {
        int id = 0;
        if (equipAbility instanceof EquipLeatherChestplate)
            id = 1;
        if (equipAbility instanceof EquipChainmailChestplate)
            id = 2;
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("autocast-id", id);
        return nbt;
    });


    public final static String buildingName = "Blacksmith";
    public final static String structureName = "blacksmith";
    public final static String upgradedStructureName = "blacksmith_superior";
    public final static ResourceCost cost = ResourceCosts.BLACKSMITH;

    public Blacksmith() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.SMITHING_TABLE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/smithing_table_front.png");

        this.buildTimeModifier = 0.85f;

        this.startingBlockTypes.add(Blocks.OAK_PLANKS);
        this.startingBlockTypes.add(Blocks.COBBLESTONE);

        this.abilities.add(EQUIP_LEATHER_CHESTPLATE_INSTANCE, Keybindings.keyT);
        this.abilities.add(EQUIP_CHAINMAIL_CHESTPLATE, Keybindings.keyY);

        this.productions.add(ProductionItems.IRON_GOLEM, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_GOLEM_SMITHING, Keybindings.keyW);
        this.productions.add(ProductionItems.RESEARCH_MILITIA_BOWS, Keybindings.keyE);
        this.productions.add(ProductionItems.RESEARCH_SUPERIOR_BLACKSMITH, Keybindings.keyR);

        setActiveAddon(RangeIndicatorAddon.class, this, true);
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/smithing_table_front.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.BLACKSMITH,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.ATTACK_ENEMY_BASE),
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.blacksmith"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.blacksmith.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.blacksmith.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks())
            if (block.getBlockState().getBlock() == Blocks.BLAST_FURNACE) {
                return 1;
            }
        return 0;
    }

    @Override
    public String getUpgradedStructureName(int upgradeLevel) {
        return upgradeLevel > 0 ? upgradedStructureName : structureName;
    }

    @Override
    public int getRange(BuildingPlacement placement) {
        return EquipAbility.RANGE;
    }

    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide())
            return;
        getHighlightBps(placement).clear();
        getHighlightBps(placement).addAll(MiscUtil.getRangeIndicatorCircleBlocks(placement.centrePos,
                getRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ, placement.level));
    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return true;
    }

    @Override
    public void onBuilt(BuildingPlacement buildingPlacement) {
        super.onBuilt(buildingPlacement);
        updateHighlightBps(buildingPlacement);
    }

    @Override
    public String getUpgradedName(BuildingPlacement placement) {
        return I18n.get("buildings.villagers.reignofnether.blacksmith.superior");
    }

    @Override
    public void tick(Level tickLevel, BuildingPlacement placement) {
        super.tick(tickLevel, placement);

        EquipAbility autoCastEquip = placement.getDataStorage().getData(AUTO_CAST_EQUIP);
        if (placement.getTickAgeAfterBuilt() > 0 && placement.getTickAgeAfterBuilt() % 15 == 0 && placement.isBuilt && autoCastEquip != null
                && autoCastEquip.isOffCooldown(placement)) {

            List<Mob> mobs = new ArrayList<>();
            for (Mob e : MiscUtil.getEntitiesWithinRange(new Vector3d(
                            placement.centrePos.getX(),
                            placement.centrePos.getY(),
                            placement.centrePos.getZ()
                    ),
                    autoCastEquip.range - 1,
                    Mob.class,
                    tickLevel
            )) {
                if ((autoCastEquip.isCorrectUnit(e) && autoCastEquip.canAfford(placement)
                        && !autoCastEquip.hasItemInSlot(e)
                )) {
                    mobs.add(e);
                }
            }
            if (!mobs.isEmpty()) {
                autoCastEquip.use(tickLevel, placement, mobs.get(0));
            }
        }
        if (tickLevel.isClientSide && placement.getTickAgeAfterBuilt() > 0 && placement.getTickAgeAfterBuilt() % 100 == 0)
            updateHighlightBps(placement);
    }
}
