package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.items.ItemServerEvents;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.StockedShopItem;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Heroes buy the plan's items at our market (the guide: Broadsword, then a medallion) when there's no fight on,
// with the emeralds the army earns. It walks over, buys once in reach, and goes back to the army.
public class BotMarket {

    private final BotBrain bot;

    BotMarket(BotBrain bot) {
        this.bot = bot;
    }

    // true while the hero is busy shopping (nothing else orders it this think)
    boolean shop(LivingEntity hero) {
        if (!ItemServerEvents.ENABLED || !(hero instanceof UnitInventory inv) || bot.defence.isDefending() ||
                bot.army.state != BotArmy.State.RALLY || !bot.memory.enemiesNear(hero.position(), 16).isEmpty())
            return false;
        ItemShopPlacement shop = market();
        UnitItem item = shop == null ? null : nextItem(inv, shop);
        if (item == null)
            return false;
        if (shop.isPosInsideBuilding(hero.getOnPos(), ItemShopPlacement.UNIT_SERVE_RANGE)) {
            if (!bot.spendAction())
                return true;
            ItemServerEvents.buyItem((Unit) hero, item.descId, shop.originPos);
            bot.refreshResources();
            bot.log("market: hero {} buys {}", hero.getId(), item.descId);
            return true;
        }
        if (!shop.centrePos.equals(((Unit) hero).getMoveGoal().getMoveTarget()))
            bot.order(UnitAction.MOVE, List.of(hero), shop.centrePos);
        return true;
    }

    @Nullable
    private ItemShopPlacement market() {
        if (bot.plan.market() == null)
            return null;
        for (BuildingPlacement b : bot.buildings)
            if (b.isBuilt && b.getBuilding() == bot.plan.market() && b instanceof ItemShopPlacement shop)
                return shop;
        return null;
    }

    // the first plan item the hero lacks that's in stock and affordable
    @Nullable
    private UnitItem nextItem(UnitInventory inv, ItemShopPlacement shop) {
        for (String descId : bot.plan.marketItems()) {
            UnitItem item = ItemUtil.getUnitItem(descId);
            if (item == null || inv.isHolding(item))
                continue;
            for (StockedShopItem stock : shop.getStockedItems())
                if (stock.item.descId.equals(descId))
                    return stock.stock > 0 && bot.resources.emerald >= stock.buyCost && !inv.isFull(item) ? item : null;
            return null; // the plan's next item isn't sold here: stop, the order matters
        }
        return null;
    }
}
