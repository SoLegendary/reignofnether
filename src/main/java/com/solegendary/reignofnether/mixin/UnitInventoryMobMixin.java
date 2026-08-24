package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.UnitItem;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    @Nullable
    public ItemStack get(UUID uuid) {
        for (ItemStack itemStack : this.unitItems) {
            if (itemStack.getTag() != null &&
                itemStack.getTag().hasUUID("uuid") &&
                itemStack.getTag().getUUID("uuid").equals(uuid)) {
                return itemStack;
            }
        }
        return null;
    }

    @Override
    public void set(int index, ItemStack stack, UUID uuid) {
        CompoundTag uuidTag = new CompoundTag();
        uuidTag.putUUID("uuid", uuid);
        if (stack != null)
            stack.setTag(uuidTag);
        this.unitItems.set(index, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public void set(int index, ItemStack stack) {
        CompoundTag uuidTag = new CompoundTag();
        uuidTag.putUUID("uuid", UUID.randomUUID());
        if (stack != null)
            stack.setTag(uuidTag);
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
    public boolean dropSlot(int index, BlockPos bp) {
        ItemStack stack = this.unitItems.get(index);
        if (!stack.isEmpty() && EnchantmentHelper.hasBindingCurse(stack)) {
            return false;
        }
        if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) {
            this.spawnAtLocation(stack);
        }
        this.unitItems.set(index, ItemStack.EMPTY);
        return true;
    }

    @Override
    public boolean dropUUID(UUID uuid, BlockPos bp) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            if (stack != null && stack.getTag() != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = stack.getTag().getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (EnchantmentHelper.hasBindingCurse(stack)) {
                        return false;
                    }
                    if (!EnchantmentHelper.hasVanishingCurse(stack)) {
                        this.spawnAtLocation(stack);
                    }
                    this.unitItems.set(i, ItemStack.EMPTY);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean deleteUUID(UUID uuid) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            if (stack != null && stack.getTag() != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = stack.getTag().getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (EnchantmentHelper.hasBindingCurse(stack)) {
                        return false;
                    }
                    this.unitItems.set(i, ItemStack.EMPTY);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean tryAdding(ItemStack newItemStack) {
        if (!ItemUtil.isUnitItem(newItemStack))
            return false;
        for (int i = 0; i < getAllItems().size(); i++) {
            if (getAllItems().get(i).getItem() == Items.AIR) {
                set(i, newItemStack);
                return true;
            }
        }
        return false;
    }

    @Override
    public void giveTo(UUID uuid, UnitInventory inv) {
        if (inv.tryAdding(get(uuid)))
            this.deleteUUID(uuid);
    }

    @Override
    public void useOnGround(UUID uuid, BlockPos blockPos) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack.getItem());
            if (unitItem != null) {
                unitItem.onUseGround.accept(blockPos);
                this.deleteUUID(uuid);
            }
        }
    }

    @Override
    public void useOnEntity(UUID uuid, LivingEntity entity) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack.getItem());
            if (unitItem != null && entity.isAlive()) {
                unitItem.onUseEntity.accept(entity);
                this.deleteUUID(uuid);
            }
        }
    }

    @Override
    public void useOnBuilding(UUID uuid, BuildingPlacement building) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack.getItem());
            if (unitItem != null && !building.shouldBeDestroyed()) {
                unitItem.onUseBuilding.accept(building);
                this.deleteUUID(uuid);
            }
        }
    }

    @Override
    public void use(UUID uuid) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack.getItem());
            if (unitItem != null) {
                unitItem.onUse.run();
                this.deleteUUID(uuid);
            }
        }
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