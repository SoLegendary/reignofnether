package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.KeyframeAnimated;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
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
    public NonNullList<ItemStack> getAllItems() {
        return this.unitItems;
    }

    @Override
    public boolean isFull() {
        for (ItemStack itemStack : getAllItems())
            if (itemStack == ItemStack.EMPTY || itemStack.isEmpty())
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
    public void set(int index, ItemStack stack) {
        ItemStack old = this.unitItems.get(index);
        if (!old.isEmpty()) ron$removeItemAttributes(old);
        if (stack != null) {
            CompoundTag tag = stack.getOrCreateTag();
            if (!tag.hasUUID("uuid"))
                tag.putUUID("uuid", UUID.randomUUID());
        }
        ItemStack newStack = stack == null ? ItemStack.EMPTY : stack;
        this.unitItems.set(index, newStack);
        if (!newStack.isEmpty()) ron$applyItemAttributes(newStack);
        if (this instanceof HeroUnit heroUnit) heroUnit.setStatsForLevel();
        syncToClient();
    }

    @Override
    public void swapSlots(int index1, int index2) {
        Objects.checkIndex(index1, MAX_INVENTORY_SIZE);
        Objects.checkIndex(index2, MAX_INVENTORY_SIZE);
        if (index1 == index2) return;
        ItemStack tmp = this.unitItems.get(index1);
        this.unitItems.set(index1, this.unitItems.get(index2));
        this.unitItems.set(index2, tmp);
        syncToClient();
    }

    @Override
    public boolean dropUUID(UUID uuid, BlockPos bp) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            if (stack != null && stack.getTag() != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = stack.getTag().getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (!stack.isEmpty() && EnchantmentHelper.hasBindingCurse(stack)) {
                        return false;
                    }
                    if (!stack.isEmpty()) {
                        BehaviorUtils.throwItem(this, stack, bp.getCenter(), new Vec3(0.25f,0.25f,0.25f), 0.3F);
                    }
                    ron$removeItemAttributes(stack);
                    this.unitItems.set(i, ItemStack.EMPTY);
                    if (this instanceof HeroUnit heroUnit) heroUnit.setStatsForLevel();
                    syncToClient();
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
                    ron$removeItemAttributes(stack);
                    this.unitItems.set(i, ItemStack.EMPTY);
                    if (this instanceof HeroUnit heroUnit) heroUnit.setStatsForLevel();
                    syncToClient();
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
                syncToClient();
                return true;
            }
        }
        return false;
    }

    @Override
    public void giveTo(UUID uuid, UnitInventory inv) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null && !EnchantmentHelper.hasBindingCurse(itemStack)) {
            if (inv.tryAdding(get(uuid))) {
                this.deleteUUID(uuid);
                ItemEntity itemEntity = this.spawnAtLocation(itemStack);
                if (itemEntity != null) {
                    ((LivingEntity) inv).take(itemEntity, itemStack.getCount());
                    itemEntity.discard();
                }
            }
        }
    }

    @Override
    public boolean useOnGround(UUID uuid, BlockPos blockPos) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null && this instanceof Unit unit) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack);
            if (unitItem != null && unitItem.onUseGround != null && checkManaCostAndCooldown(unitItem, itemStack)) {
                if (unitItem.onUseGround.test(unit, blockPos)) {
                    afterUse(unitItem, itemStack, uuid);
                    return true;
                } else if (!this.level().isClientSide() && !unitItem.suppressDefaultError) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.use_on_ground");
                }
            }
        }
        return false;
    }

    @Override
    public boolean useOnEntity(UUID uuid, LivingEntity entity) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null && this instanceof Unit unit) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack);
            if (unitItem != null && entity.isAlive() && unitItem.onUseEntity != null && checkManaCostAndCooldown(unitItem, itemStack)) {
                if (unitItem.onUseEntity.test(unit, entity)) {
                    afterUse(unitItem, itemStack, uuid);
                    return true;
                } else if (!this.level().isClientSide() && !unitItem.suppressDefaultError) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.use_on_entity");
                }
            }
        }
        return false;
    }

    @Override
    public boolean useOnBuilding(UUID uuid, BuildingPlacement building) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null && this instanceof Unit unit) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack);
            if (unitItem != null && !building.shouldBeDestroyed() && unitItem.onUseBuilding != null && checkManaCostAndCooldown(unitItem, itemStack)) {
                if (unitItem.onUseBuilding.test(unit, building)) {
                    afterUse(unitItem, itemStack, uuid);
                    return true;
                } else if (!this.level().isClientSide() && !unitItem.suppressDefaultError) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.use_on_building");
                }
            }
        }
        return false;
    }

    @Override
    public boolean use(UUID uuid) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null && this instanceof Unit unit) {
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack);
            if (unitItem != null && unitItem.onUse != null && checkManaCostAndCooldown(unitItem, itemStack)) {
                if (unitItem.onUse.test(unit)) {
                    afterUse(unitItem, itemStack, uuid);
                    return true;
                } else if (!this.level().isClientSide() && !unitItem.suppressDefaultError) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.use");
                }
            }
        }
        return false;
    }

    private void afterUse(UnitItem unitItem, ItemStack itemStack, UUID uuid) {
        if (unitItem.consumeOnUse) {
            itemStack.setCount(itemStack.getCount() - 1);
            if (itemStack.isEmpty())
                this.deleteUUID(uuid);
        }
        if (this instanceof HeroUnit heroUnit && unitItem.manaCost > 0)
            heroUnit.setMana(heroUnit.getMana() - unitItem.manaCost);
        if (unitItem.cooldownTicksMax > 0)
            itemStack.getOrCreateTag().putLong(UnitItem.RON$COOLDOWN_KEY, this.level().getGameTime() + unitItem.cooldownTicksMax);
        if (this instanceof KeyframeAnimated && unitItem.doCastAnimation) {
            UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.CAST_SPELL, this);
        }
        syncToClient();
    }

    @Override
    public boolean checkManaCostAndCooldown(UnitItem unitItem, ItemStack itemStack) {
        if (!canAffordManaCost(unitItem)) {
            if (!level().isClientSide()) {
                HudClientboundPacket.showTempMessageI18n(((Unit) this).getOwnerName(), "item.reignofnether.error.not_enough_mana");
            } else {
                HudClientEvents.showTempMessageI18n("item.reignofnether.error.not_enough_mana");
            }
            return false;
        }
        if (!isOffCooldown(unitItem, itemStack)) {
            long cooldownSecondsLeft = ItemUtil.getCooldownTicksLeft(itemStack, level()) / 20;
            String str = Component.translatable("item.reignofnether.error.on_cooldown", cooldownSecondsLeft).getString();
            if (!level().isClientSide()) {
                HudClientboundPacket.showTempMessageI18n(((Unit) this).getOwnerName(), str);
            } else {
                HudClientEvents.showTemporaryMessage(str);
            }
            return false;
        }
        return true;
    }

    @Unique
    private void ron$applyItemAttributes(ItemStack stack) {
        if (this.level().isClientSide() || stack.isEmpty()) return;
        UnitItem unitItem = ItemUtil.getUnitItem(stack);
        if (unitItem == null || unitItem.attributes.isEmpty()) return;

        UUID itemUuid = stack.getOrCreateTag().getUUID("uuid");
        int i = 0;
        for (Attribute attr : unitItem.attributes.keySet()) {
            AttributeModifier modifier = unitItem.attributes.get(attr);
            AttributeInstance instance = this.getAttribute(attr);
            if (instance != null) {
                boolean hasMovespeedMod = false;
                for (AttributeModifier mod : instance.getModifiers())
                    if (mod.getName().startsWith("reignofnether:item:"))
                        hasMovespeedMod = true;

                if (attr != Attributes.MOVEMENT_SPEED || !hasMovespeedMod) {
                    UUID modUuid = ron$deriveModifierUUID(itemUuid, i);
                    if (instance.getModifier(modUuid) == null) { // idempotency guard
                        instance.addTransientModifier(new AttributeModifier(
                                modUuid, "reignofnether:item:" + i,
                                modifier.getAmount(), modifier.getOperation()));
                    }
                }
            }
            i++;
        }
    }

    @Unique
    private void ron$removeItemAttributes(ItemStack stack) {
        if (this.level().isClientSide() || stack.isEmpty()) return;
        UnitItem unitItem = ItemUtil.getUnitItem(stack);
        CompoundTag tag = stack.getTag();
        if (unitItem == null || tag == null || !tag.hasUUID("uuid")) return;

        UUID itemUuid = tag.getUUID("uuid");
        int i = 0;
        for (Attribute attribute : unitItem.attributes.keySet()) {
            AttributeInstance instance = this.getAttribute(attribute);
            if (instance != null)
                instance.removeModifier(ron$deriveModifierUUID(itemUuid, i));
            i++;
        }
    }

    @Unique
    private static UUID ron$deriveModifierUUID(UUID itemUuid, int modifierIndex) {
        return UUID.nameUUIDFromBytes((itemUuid + "#" + modifierIndex).getBytes(StandardCharsets.UTF_8));
    }

    private boolean canAffordManaCost(UnitItem unitItem) {
        if (unitItem.manaCost <= 0)
            return true;
        return this instanceof HeroUnit heroUnit && heroUnit.getMana() >= unitItem.manaCost;
    }

    private boolean isOffCooldown(UnitItem unitItem, ItemStack itemStack) {
        if (unitItem.cooldownTicksMax <= 0)
            return true;
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.contains(UnitItem.RON$COOLDOWN_KEY))
            return true;
        long gameTime = this.level().isClientSide() ? TimeClientEvents.serverGameTime : this.level().getGameTime();
        return gameTime >= tag.getLong(UnitItem.RON$COOLDOWN_KEY);
    }

    private void syncToClient() {
        if (!this.level().isClientSide())
            ItemClientboundPacket.syncInventory(this.getId(), getAllItems());
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
            ItemStack stack = i < list.size() ? ItemStack.of(list.getCompound(i)) : ItemStack.EMPTY;
            this.unitItems.set(i, stack);
            if (!stack.isEmpty()) ron$applyItemAttributes(stack);
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("RETURN"))
    private void ron$dropUnitItemsOnDeath(DamageSource source, int looting, boolean recentlyHit, CallbackInfo ci) {
        if ((Object) this instanceof HeroUnit) return; // heroes keep their gear

        for (int i = 0; i < this.unitItems.size(); i++) {
            ItemStack stack = this.unitItems.get(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
            this.unitItems.set(i, ItemStack.EMPTY);
        }
    }
}