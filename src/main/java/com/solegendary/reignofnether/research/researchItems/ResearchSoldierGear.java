package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.soldier.SoldierServerEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// capitol research for first-person soldiers, the same for every faction: armor or weapons, tiers I to III.
// Each tier needs the one before it and re-equips the commander's living soldiers when done
public class ResearchSoldierGear extends ProductionItem {

    private static final String[] NUMERALS = {"", "I", "II", "III"};

    public final boolean armor;
    public final int tier;
    @Nullable private final ResearchSoldierGear previous;
    private final String itemName;

    public ResearchSoldierGear(boolean armor, int tier, @Nullable ResearchSoldierGear previous) {
        super(ResourceCost.Research(50 + 50 * tier, 50 + 50 * tier, 100 * tier - 50, 30 + 30 * tier), ProdDupeRule.DISALLOW);
        this.armor = armor;
        this.tier = tier;
        this.previous = previous;
        this.itemName = (armor ? "Soldier Armor " : "Soldier Weapons ") + NUMERALS[tier];
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                ResearchServerEvents.addResearch(placement.ownerName, this);
                SoldierServerEvents.reequip(placement.ownerName);
            }
        };
    }

    public String getItemName() {
        return itemName;
    }

    private ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                armor ? "textures/icons/items/chestplate.png" : "textures/icons/items/sword.png");
    }

    private String langKey() {
        return armor ? "research.reignofnether.soldier_armor" : "research.reignofnether.soldier_weapons";
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(itemName,
            icon(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> PlayerRolesClient.soldiersOf(prodBuilding.ownerName).isEmpty()
                || itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(this)
                || (previous != null && !ResearchClient.hasResearch(previous)),
            () -> true,
            List.of(fcs(I18n.get(langKey(), NUMERALS[tier]), true),
                ResourceCosts.getFormattedCost(defaultCost),
                ResourceCosts.getFormattedTime(defaultCost),
                fcs(""),
                fcs(I18n.get(langKey() + ".tooltip1")),
                fcs(I18n.get("research.reignofnether.soldier_gear.tooltip2"))
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(itemName,
            icon(),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
