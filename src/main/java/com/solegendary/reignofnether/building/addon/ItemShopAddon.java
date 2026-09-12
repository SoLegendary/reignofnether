package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public interface ItemShopAddon extends BuildingAddon {

    DataType<ArrayList<StockedShopItem>> STOCKED_ITEMS = DataType.createRegistered(
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "stocked_items"),
            (nbt, server) -> { // decode
                ArrayList<StockedShopItem> itemsAndStock = new ArrayList<>();
                ListTag ltag = nbt.getList("list", Tag.TAG_COMPOUND);
                for (int i = 0; i < ltag.size(); i++) {
                    CompoundTag tag = ltag.getCompound(i);
                    UnitItem unitItem = ItemUtil.getUnitItem(tag.getUUID("uuid"));
                    if (unitItem != null) {
                        StockedShopItem stock = new StockedShopItem(
                                unitItem,
                                tag.getInt("buyCost"),
                                tag.getInt("maxStock"),
                                tag.getInt("stock"),
                                tag.getInt("maxRestockTicks"),
                                tag.getInt("restockTicks")
                        );
                        itemsAndStock.add(stock);
                    }
                }
                return itemsAndStock;
            },
            itemsAndStock -> { // encode
                CompoundTag tag = new CompoundTag();
                ListTag ltag = new ListTag();
                for (StockedShopItem stockedShopItem : itemsAndStock) {
                    CompoundTag tag2 = new CompoundTag();
                    tag2.putUUID("uuid", stockedShopItem.item.uuid);
                    tag2.putInt("buyCost", stockedShopItem.getBuyCost());
                    tag2.putInt("maxStock", stockedShopItem.maxStock);
                    tag2.putInt("stock", stockedShopItem.stock);
                    tag2.putInt("maxRestockTicks", stockedShopItem.maxRestockTicks);
                    tag2.putInt("restockTicks", stockedShopItem.getTicksToNextRestock());
                    ltag.add(tag2);
                }
                tag.put("list", ltag);
                return tag;
            },
            ArrayList::new
    );

    default void buyItem(Unit unit, ItemShopPlacement bpl, UnitItem item) {
        ArrayList<StockedShopItem> shopStocks = bpl.getDataStorage().getData(ItemShopAddon.STOCKED_ITEMS);
        if (!bpl.isBuilt || shopStocks == null) return;
        if (!bpl.canServeUnit(unit)) return;
        if (!(unit instanceof UnitInventory inv)) return;
        if (((Entity) unit).level().isClientSide()) return;

        for (StockedShopItem shopStock : shopStocks) {
            if (item.uuid == shopStock.item.uuid) {

                if (!bpl.isPosInsideBuilding(((LivingEntity) unit).getOnPos(), ItemShopPlacement.UNIT_SERVE_RANGE)) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.too_far_away");
                    return;
                }
                if (shopStock.stock <= 0) {
                    String outOfStock = Component.translatable("itemshop.reignofnether.error.out_of_stock", shopStock.getTicksToNextRestock() / 20).getString();
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), outOfStock);
                    return;
                }
                if (inv.isFull()) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.full_inventory");
                    return;
                }
                if (!ResourcesServerEvents.canAfford(unit.getOwnerName(), ResourceName.EMERALD, item.buyCost)) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.cant_afford");
                    return;
                }
                if (!AlliancesServerEvents.isAlliedOrOwned(unit.getOwnerName(), bpl.ownerName)) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.hostile_shop");
                    return;
                }

                ItemStack itemStack = item.getNewItemStack();
                if (inv.tryAdding(itemStack)) {
                    ItemEntity itemEntity = ((LivingEntity) inv).spawnAtLocation(itemStack);
                    if (itemEntity != null) {
                        ((LivingEntity) inv).take(itemEntity, itemStack.getCount());
                        itemEntity.discard();
                    }
                    shopStock.stock -= 1;
                    ResourcesServerEvents.addSubtractResources(Resources.emeralds(unit.getOwnerName(), item.buyCost));
                    SoundClientboundPacket.playSoundAtPos(SoundAction.SELL_ITEM, ((LivingEntity) unit).getOnPos());
                    ItemShopClientboundPacket.syncItemShopStock(bpl.originPos, bpl.getStockedItems());
                } else {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.failed_other");
                }
                break;
            }
        }

    }
}
