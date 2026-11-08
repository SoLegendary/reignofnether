package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.TradeResources;
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

    private static final int TRADE_EVERY = 5 * 20, KEEP_EMERALDS = 200, SHORT = 300, PILE = 600;

    private final BotBrain bot;
    private long lastTrade = 0;

    BotMarket(BotBrain bot) {
        this.bot = bot;
    }

    // kills pay in emeralds and only a hero's items use them: past that they buy the wood or ore holding the bot back.
    // Short of emeralds, a pile of food or wood is sold for them first (900 wood sat there while the Castle waited on ore)
    void trade() {
        ItemShopPlacement shop = market();
        if (shop == null || shop.getUpgradeLevel() <= 0 || bot.tick - lastTrade < TRADE_EVERY ||
                Math.min(bot.resources.wood, bot.resources.ore) >= SHORT)
            return; // emeralds from a sale are no use before the upgrade lets us buy with them
        UnitAction buy = bot.resources.wood <= bot.resources.ore ? UnitAction.BUY_WOOD : UnitAction.BUY_ORE;
        if (bot.resources.emerald < KEEP_EMERALDS + TradeResources.TRADE_AMOUNT) {
            if (bot.resources.wood >= PILE && buy == UnitAction.BUY_ORE)
                buy = UnitAction.SELL_WOOD;
            else if (bot.resources.food >= PILE)
                buy = UnitAction.SELL_FOOD;
            else
                return;
        }
        for (Ability a : shop.getAbilities())
            if (a.action == buy && bot.spendAction()) {
                a.use(bot.level, shop, shop.centrePos);
                bot.refreshResources();
                lastTrade = bot.tick;
                bot.log("market: {}, emeralds left {}", buy, bot.resources.emerald);
                return;
            }
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
