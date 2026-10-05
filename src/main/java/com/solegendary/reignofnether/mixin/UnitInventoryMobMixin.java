package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Mixin(Mob.class)
public abstract class UnitInventoryMobMixin extends LivingEntity implements UnitInventory {

    @Shadow public abstract InteractionResult interact(Player pPlayer, InteractionHand pHand);

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
    public boolean isFull(UnitItem item) {
        for (ItemStack itemStack : getAllItems()) {
            if (itemStack.isEmpty() || itemStack == ItemStack.EMPTY)
                return false;
            UnitItem held = ItemUtil.getUnitItem(itemStack);
            if (held != null && held == item && itemStack.getCount() < Math.max(1, held.maxStackSize))
                return false;
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : getAllItems())
            if (itemStack != ItemStack.EMPTY && !itemStack.isEmpty())
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
    @Nullable
    public ItemStack get(UnitItem unitItem) {
        for (ItemStack itemStack : this.unitItems) {
            if (ItemUtil.getUnitItem(itemStack) == unitItem) {
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
    public boolean deleteItem(UUID uuid) {
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
    public boolean deleteItem(UnitItem item) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            if (stack != null && stack.getTag() != null && stack.getItem() == item.getItem()) {
                if (!stack.isEmpty()) {
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
        UnitItem unitItem = ItemUtil.getUnitItem(newItemStack);
        if (unitItem == null || newItemStack.isEmpty())
            return false;

        int startCount = newItemStack.getCount();
        int maxStack = Math.max(1, unitItem.maxStackSize);
        NonNullList<ItemStack> items = getAllItems();

        // top up existing partial stacks
        for (ItemStack existing : items) {
            if (newItemStack.isEmpty()) break;
            if (existing.isEmpty() || ItemUtil.getUnitItem(existing) != unitItem) continue;
            int space = maxStack - existing.getCount();
            if (space <= 0) continue;
            int toMove = Math.min(space, newItemStack.getCount());
            existing.grow(toMove);
            newItemStack.shrink(toMove);
        }
        // spill the rest into empty slots, respecting max stack size
        for (int i = 0; i < items.size() && !newItemStack.isEmpty(); i++) {
            if (!items.get(i).isEmpty()) continue;
            int toMove = Math.min(maxStack, newItemStack.getCount());
            ItemStack copy = newItemStack.copy();
            copy.setCount(toMove);
            if (copy.getTag() != null)
                copy.getTag().remove("uuid"); // set() assigns a fresh one
            newItemStack.shrink(toMove);
            set(i, copy);
        }
        boolean added = newItemStack.getCount() < startCount;
        if (added) {
            syncToClient();
            if (this instanceof Unit unit)
                unit.dropAllResources();
        }
        return added;
    }

    @Override
    public void giveTo(UUID uuid, UnitInventory inv) {
        ItemStack stack = get(uuid);
        if (stack == null || EnchantmentHelper.hasBindingCurse(stack)) return;

        ItemStack toGive = stack.copy();
        if (inv.tryAdding(toGive)) {
            int moved = stack.getCount() - toGive.getCount();
            if (moved >= stack.getCount())
                deleteItem(uuid); // whole stack went across
            else
                stack.shrink(moved); // keep the remainder
            syncToClient();
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
                    CompoundTag tag = itemStack.getTag();
                    if (itemStack.getTag() != null && unitItem.toggleActiveOnUse) {
                        if (!tag.contains("active"))
                            tag.putBoolean("active", true);
                        else
                            tag.putBoolean("active", !tag.getBoolean("active"));
                    }
                    afterUse(unitItem, itemStack, uuid);
                    return true;
                } else if (!this.level().isClientSide() && !unitItem.suppressDefaultError) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.use");
                }
            }
        }
        return false;
    }

    @Override
    public boolean isHolding(UnitItem unitItem) {
        for (ItemStack itemStack : getAllItems()) {
            UnitItem heldUnitItem = ItemUtil.getUnitItem(itemStack);
            if (heldUnitItem != null && heldUnitItem.descId.equals(unitItem.descId))
                return true;
        }
        return false;
    }

    @Override
    public boolean isHoldingActive(UnitItem unitItem) {
        for (ItemStack itemStack : getAllItems()) {
            UnitItem heldUnitItem = ItemUtil.getUnitItem(itemStack);
            if (heldUnitItem != null && heldUnitItem.descId.equals(unitItem.descId))
                if (ItemUtil.isActive(itemStack))
                    return true;
        }
        return false;
    }

    private void afterUse(UnitItem unitItem, ItemStack itemStack, UUID uuid) {
        if (unitItem.consumeOnUse) {
            if (itemStack.getCount() <= 1)
                this.deleteItem(uuid);
            else
                itemStack.shrink(1);
        }
        if (this instanceof HeroUnit heroUnit && unitItem.manaCost > 0)
            heroUnit.setMana(heroUnit.getMana() - unitItem.manaCost);
        if (unitItem.cooldownTicksMax > 0)
            itemStack.getOrCreateTag().putLong(UnitItem.RON$COOLDOWN_KEY, this.level().getGameTime() + unitItem.cooldownTicksMax);
        //if (this instanceof KeyframeAnimated && unitItem.doCastAnimation) {
        //    UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.CAST_SPELL, this);
        //}
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
        if (this.level().isClientSide() || stack.isEmpty() || !(this instanceof HeroUnit)) return;
        UnitItem unitItem = ItemUtil.getUnitItem(stack);
        if (unitItem == null || unitItem.attributes.isEmpty()) return;

        UUID itemUuid = stack.getOrCreateTag().getUUID("uuid");
        int i = 0;
        for (Attribute attr : unitItem.attributes.keySet()) {
            AttributeModifier modifier = unitItem.attributes.get(attr);
            AttributeInstance instance = this.getAttribute(attr);
            if (instance != null) {
                Set<Attribute> NON_STACKABLE_ATTRIBUTES = Set.of(
                        Attributes.MOVEMENT_SPEED,
                        AttributeRegistrar.EVASION_CHANCE.get()
                );
                boolean isNonStackable = NON_STACKABLE_ATTRIBUTES.contains(attr);
                boolean hasExistingMod = false;

                if (isNonStackable) {
                    for (AttributeModifier mod : instance.getModifiers()) {
                        if (mod.getName().startsWith("reignofnether:item:")) {
                            hasExistingMod = true;
                            break;
                        }
                    }
                }
                if (!isNonStackable || !hasExistingMod) {
                    UUID modUuid = ron$deriveModifierUUID(itemUuid, i);
                    if (instance.getModifier(modUuid) == null) {
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
        if (this.level().isClientSide() || stack.isEmpty() || !(this instanceof HeroUnit)) return;
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
        if (this instanceof HeroUnit) return; // heroes keep their gear

        for (int i = 0; i < this.unitItems.size(); i++) {
            ItemStack stack = this.unitItems.get(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
            this.unitItems.set(i, ItemStack.EMPTY);
        }
    }

    @Inject(
        method = "tick",
        at = @At("TAIL")
    )
    public void tick(CallbackInfo ci) {
        if (tickCount % 20 == 0 && isHoldingActive(UnitItems.BELL_OF_ARMS)) {
            addEffect(new MobEffectInstance(MobEffectRegistrar.VILLAGER_INSPIRATION.get(), 30, 0, true, false));
        }
        if (tickCount % 20 == 0) {
            for (ItemStack itemStack : getAllItems()) {
                if (itemStack.getTag() != null && itemStack.getTag().hasUUID("uuid")) {
                    UnitItem unitItem = ItemUtil.getUnitItem(itemStack);
                    if (unitItem != null && unitItem.forceAutocast &&
                        isOffCooldown(unitItem, itemStack) && canAffordManaCost(unitItem)) {
                        UUID itemUUID = itemStack.getTag().getUUID("uuid");
                        use(itemUUID);
                    }
                }
            }
            if (!isEmpty() && !(this instanceof HeroUnit)) {
                ParticleUtil.addParticleExplosion(ParticleRegistrar.LEVEL_UP.get(), 2, level(), position(), 0.05);
            }
        }
    }

    @Override
    public boolean canPickupUnitItems() {
        boolean hasBackpack = false;
        if (this instanceof Unit unit) {
            if (level().isClientSide()) {
                hasBackpack = ResearchClient.hasResearch(ProductionItems.RESEARCH_ITEM_BACKPACKS);
            } else {
                hasBackpack = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_ITEM_BACKPACKS);
            }
        }
        return ((Mob) (Object) this).canPickUpLoot() && (hasBackpack || (this instanceof HeroUnit));
    }

    @Override
    public boolean canUseUnitItems() {
        return this instanceof HeroUnit;
    }
}