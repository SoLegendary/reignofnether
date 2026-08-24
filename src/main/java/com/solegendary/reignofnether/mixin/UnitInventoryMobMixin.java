package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(Mob.class)
public abstract class UnitInventoryMobMixin extends LivingEntity implements UnitInventory {

    @Unique
    private static final String RON$UNIT_ITEMS_KEY = "reignofnether:UnitItems";

    @Unique
    private final NonNullList<ItemStack> unitItems =
            NonNullList.withSize(MAX_INVENTORY_SIZE, ItemStack.EMPTY);

    protected UnitInventoryMobMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getAllItems() {
        return this.unitItems;
    }

    @Override
    public boolean isFull() {
        for (ItemStack itemStack : getAllItems())
            if (itemStack == ItemStack.EMPTY)
                return false;
        return true;
    }

    @Override
    public ItemStack get(int index) {
        return this.unitItems.get(index);
    }

    @Override
    public void setSlot(int index, ItemStack stack) {
        this.unitItems.set(index, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public void swapSlots(int index1, int index2) {
        Objects.checkIndex(index1, MAX_INVENTORY_SIZE);
        Objects.checkIndex(index2, MAX_INVENTORY_SIZE);
        if (index1 == index2) return;
        ItemStack tmp = this.unitItems.get(index1);
        this.unitItems.set(index1, this.unitItems.get(index2));
        this.unitItems.set(index2, tmp);
    }

    @Override
    public void dropSlot(int index, BlockPos bp) {
        ItemStack stack = this.unitItems.get(index);
        if (!stack.isEmpty() && EnchantmentHelper.hasBindingCurse(stack)) {
            return;
        }
        if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
            this.spawnAtLocation(stack);
        }
        this.unitItems.set(index, ItemStack.EMPTY);
    }

    @Override
    public boolean tryAdding(ItemStack newItemStack) {
        if (!ItemUtil.isUnitItem(newItemStack))
            return false;
        for (int i = 0; i < getAllItems().size(); i++) {
            if (getAllItems().get(i).getItem() == Items.AIR) {
                setSlot(i, newItemStack);
                return true;
            }
        }
        return false;
    }

    @Override
    public void giveTo(int index, UnitInventory inv) {
        if (inv.tryAdding(getAllItems().get(index)))
            this.unitItems.set(index, ItemStack.EMPTY);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void ron$saveUnitItems(CompoundTag tag, CallbackInfo ci) {
        ListTag list = new ListTag();
        for (ItemStack stack : this.unitItems) {
            CompoundTag itemTag = new CompoundTag();
            if (!stack.isEmpty()) {
                stack.save(itemTag);
            }
            list.add(itemTag);
        }
        tag.put(RON$UNIT_ITEMS_KEY, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void ron$readUnitItems(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(RON$UNIT_ITEMS_KEY, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(RON$UNIT_ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < this.unitItems.size(); i++) {
            this.unitItems.set(i, i < list.size()
                    ? ItemStack.of(list.getCompound(i))
                    : ItemStack.EMPTY);
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("RETURN"))
    private void ron$dropUnitItemsOnDeath(DamageSource source, int looting, boolean recentlyHit, CallbackInfo ci) {
        if ((Object) this instanceof HeroUnit) return; // heroes keep their gear

        for (int i = 0; i < this.unitItems.size(); i++) {
            ItemStack stack = this.unitItems.get(i);
            if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
                this.spawnAtLocation(stack);
            }
            this.unitItems.set(i, ItemStack.EMPTY);
        }
    }
}