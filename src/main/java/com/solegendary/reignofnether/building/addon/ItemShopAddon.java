package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public interface ItemShopAddon extends BuildingAddon {

    DataType<HashMap<UnitItem, Integer>> ITEMS_AND_STOCK = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "items_and_stock"),
            (nbt, server) -> { // decode
                HashMap<UnitItem, Integer> itemsAndStock = new HashMap<>();
                ListTag ltag = nbt.getList("list", Tag.TAG_COMPOUND);
                for (int i = 0; i < ltag.size(); i++) {
                    CompoundTag tag = ltag.getCompound(i);
                    UnitItem unitItem = ItemUtil.getUnitItem(tag.getUUID("uuid"));
                    if (unitItem != null)
                        itemsAndStock.put(unitItem, tag.getInt("stock"));
                }
                return itemsAndStock;
            },
            itemsAndStock -> { // encode
                CompoundTag tag = new CompoundTag();
                ListTag ltag = new ListTag();
                for (UnitItem unitItem : itemsAndStock.keySet()) {
                    int stock = itemsAndStock.get(unitItem);
                    CompoundTag tag2 = new CompoundTag();
                    tag2.putUUID("uuid", unitItem.uuid);
                    tag2.putInt("stock", stock);
                }
                tag.put("list", ltag);
                return tag;
            },
            HashMap::new
    );

    default void buyItem(BuildingPlacement bpl, UnitItem item, Unit unit) {
        HashMap<UnitItem, Integer> itemsAndStock = bpl.getDataStorage().getData(ItemShopAddon.ITEMS_AND_STOCK);
        if (!(unit instanceof UnitInventory inv)) return;
        if (itemsAndStock == null || !itemsAndStock.containsKey(item)) return;
        if (((Entity) unit).level().isClientSide()) return;

        int stock = itemsAndStock.get(item);

        if (!bpl.isPosInsideBuilding(((LivingEntity) unit).getOnPos(), 2)) {
            HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.too_far_away");
            return;
        }
        if (stock <= 0) {
            HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.out_of_stock");
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
            itemsAndStock.put(item, stock - 1);
            bpl.getDataStorage().setData(ItemShopAddon.ITEMS_AND_STOCK, itemsAndStock);
            ResourcesServerEvents.addSubtractResources(Resources.emeralds(unit.getOwnerName(), item.buyCost));
            SoundClientboundPacket.playSoundAtPos(SoundAction.SELL_ITEM, ((LivingEntity) unit).getOnPos());
        } else {
            HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "itemshop.reignofnether.error.failed_other");
        }
    }
}
