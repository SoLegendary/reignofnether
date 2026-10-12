package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.util.ItemTagCompat;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.util.ParticleUtil;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.items.unititems.MerchantEquipmentItem;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import com.solegendary.reignofnether.util.EnchantmentUtil;
import com.solegendary.reignofnether.util.AttributeHelpers;
import com.solegendary.reignofnether.items.UnitItems;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.mixin.UnitInventoryMobMixin;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.ItemClientboundPacket;

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

    /**
     * 1.5.0 adds this to {@link UnitInventory}: it answers "is this unit holding that unit item,
     * and is it switched on", which is how the bell of arms and similar actives are gated. The
     * merge had the interface method but not its implementation, so every unit threw
     * {@code AbstractMethodError} on the first tick.
     */
    @Override
    public boolean isHoldingActive(UnitItem unitItem) {
        for (ItemStack itemStack : getAllItems()) {
            UnitItem heldUnitItem = ItemUtil.getUnitItem(itemStack);
            if (heldUnitItem != null && heldUnitItem.descId.equals(unitItem.descId)
                    && ItemUtil.isActive(itemStack))
                return true;
        }
        return false;
    }

    /**
     * 1.5.0's {@link UnitInventory#isFull(UnitItem)}: true when there is no room left for more
     * of that item. 1.21.1 dropped {@code Container#isFull}, so this is the mod's own check.
     */
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
            if (ItemTagCompat.tag(itemStack) != null &&
                ItemTagCompat.tag(itemStack).hasUUID("uuid") &&
                ItemTagCompat.tag(itemStack).getUUID("uuid").equals(uuid)) {
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
            CompoundTag tag = ItemTagCompat.getOrCreateTag(stack);
            if (!tag.hasUUID("uuid"))
                tag.putUUID("uuid", UUID.randomUUID());
        }
        ItemStack newStack = stack == null ? ItemStack.EMPTY : stack;
        this.unitItems.set(index, newStack);
        if (!newStack.isEmpty()) ron$applyItemAttributes(newStack);
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
            if (stack != null && ItemTagCompat.tag(stack) != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = ItemTagCompat.tag(stack).getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (!stack.isEmpty() && EnchantmentUtil.hasBindingCurse(stack)) {
                        return false;
                    }
                    if (!stack.isEmpty()) {
                        BehaviorUtils.throwItem(this, stack, bp.getCenter(), new Vec3(0.25f,0.25f,0.25f), 0.3F);
                    }
                    ron$removeItemAttributes(stack);
                    this.unitItems.set(i, ItemStack.EMPTY);
                    syncToClient();
                    return true;
                }
            }
        }
        return false;
    }

    
    /**
     * 1.5.0 renamed this from {@code deleteUUID} to {@code deleteItem} and added the overloads below
     * to match {@link UnitInventory}. The interface methods had arrived with the merge but not their
     * implementations, and because this mixin is abstract javac did not complain - every unit threw
     * {@code AbstractMethodError} on its first tick instead.
     */
    @Override
    public boolean deleteItem(UUID uuid) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            if (stack != null && ItemTagCompat.tag(stack) != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = ItemTagCompat.tag(stack).getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (EnchantmentUtil.hasBindingCurse(stack)) {
                        return false;
                    }
                    ron$removeItemAttributes(stack);
                    this.unitItems.set(i, ItemStack.EMPTY);
                    if (this instanceof com.solegendary.reignofnether.unit.interfaces.HeroUnit heroUnit)
                        heroUnit.setStatsForLevel();
                    syncToClient();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean deleteItem(UnitItem unitItem) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            if (stack != null && ItemTagCompat.tag(stack) != null && stack.getItem() == unitItem.getItem()) {
                if (!stack.isEmpty()) {
                    if (EnchantmentUtil.hasBindingCurse(stack)) {
                        return false;
                    }
                    ron$removeItemAttributes(stack);
                    this.unitItems.set(i, ItemStack.EMPTY);
                    if (this instanceof com.solegendary.reignofnether.unit.interfaces.HeroUnit heroUnit)
                        heroUnit.setStatsForLevel();
                    syncToClient();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 1.5.0: only units that can pick loot up (and only with the item backpacks research, or
     * heroes) may take unit items off the ground.
     */
    @Override
    public boolean canPickupUnitItems() {
        boolean hasBackpack = false;
        if (this instanceof Unit unit) {
            if (level().isClientSide()) {
                hasBackpack = ResearchClient.hasResearch(ProductionItems.RESEARCH_ITEM_BACKPACKS);
            } else {
                hasBackpack = ResearchServerEvents.playerHasResearch(unit.getOwnerName(),
                        ProductionItems.RESEARCH_ITEM_BACKPACKS);
            }
        }
        return ((Mob) (Object) this).canPickUpLoot() && (hasBackpack || (this instanceof HeroUnit));
    }

    /** Only heroes can activate unit items. */
    @Override
    public boolean canUseUnitItems() {
        return this instanceof HeroUnit;
    }

    /** Heroes can activate anything; flagged items (and merchant gear the holder can wear) are usable by regular units too. */
    @Override
    public boolean canUseItem(UnitItem unitItem) {
        if (unitItem instanceof MerchantEquipmentItem mei && mei.isCompatibleTarget.test(this))
            return true;
        return this instanceof HeroUnit || (unitItem != null && unitItem.nonHeroUseable);
    }

    /** True when this inventory holds that unit item at all, switched on or not. */
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
        if (itemStack != null && !EnchantmentUtil.hasBindingCurse(itemStack)) {
            if (inv.tryAdding(get(uuid))) {
                this.deleteItem(uuid);
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
            if (unitItem != null && canUseItem(unitItem) && unitItem.onUse != null && checkManaCostAndCooldown(unitItem, itemStack)) {
                if (unitItem.onUse.test(unit)) {
                    afterUse(unitItem, itemStack, uuid);
                    return true;
                } else if (!this.level().isClientSide() && !unitItem.suppressDefaultError) {
                    HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.use");
                }
            } else if (unitItem != null && !canUseItem(unitItem)) {
                HudClientboundPacket.showTempMessageI18n(unit.getOwnerName(), "item.reignofnether.error.cant_use_item");
            }
        }
        return false;
    }

    private void afterUse(UnitItem unitItem, ItemStack itemStack, UUID uuid) {
        if (unitItem.consumeOnUse) {
            itemStack.setCount(itemStack.getCount() - 1);
            if (itemStack.isEmpty())
                this.deleteItem(uuid);
        }
        if (this instanceof HeroUnit heroUnit && unitItem.manaCost > 0)
            heroUnit.setMana(heroUnit.getMana() - unitItem.manaCost);
        if (unitItem.cooldownTicksMax > 0)
            ItemTagCompat.getOrCreateTag(itemStack).putLong(UnitItem.RON$COOLDOWN_KEY, this.level().getGameTime() + unitItem.cooldownTicksMax);
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

        CompoundTag itemTag = ItemTagCompat.tag(stack);
        if (itemTag == null || !itemTag.hasUUID("uuid")) return;
        UUID itemUuid = itemTag.getUUID("uuid");
        int i = 0;
        for (Attribute attr : unitItem.attributes.keySet()) {
            AttributeModifier modifier = unitItem.attributes.get(attr);
            AttributeInstance instance = this.getAttribute(AttributeHelpers.holder(attr));
            if (instance != null) {
                boolean hasMovespeedMod = false;
                for (AttributeModifier mod : instance.getModifiers())
                    if (mod.id().getNamespace().equals(ReignOfNether.MOD_ID))
                        hasMovespeedMod = true;

                if (attr != Attributes.MOVEMENT_SPEED || !hasMovespeedMod) {
                    ResourceLocation modId = ron$deriveModifierId(itemUuid, i);
                    if (instance.getModifier(modId) == null) { // idempotency guard
                        instance.addTransientModifier(new AttributeModifier(
                                modId, modifier.amount(), modifier.operation()));
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
        CompoundTag tag = ItemTagCompat.tag(stack);
        if (unitItem == null || tag == null || !tag.hasUUID("uuid")) return;

        UUID itemUuid = tag.getUUID("uuid");
        int i = 0;
        for (Attribute attribute : unitItem.attributes.keySet()) {
            AttributeInstance instance = this.getAttribute(AttributeHelpers.holder(attribute));
            if (instance != null)
                instance.removeModifier(ron$deriveModifierId(itemUuid, i));
            i++;
        }
    }

    @Unique
    // 1.21.1 identifies attribute modifiers by ResourceLocation rather than UUID; the id still
    // has to be derived from the item so that re-equipping the same item does not stack them.
    private static ResourceLocation ron$deriveModifierId(UUID itemUuid, int modifierIndex) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "item_" + modifierIndex + "_" + itemUuid);
    }

    private boolean canAffordManaCost(UnitItem unitItem) {
        if (unitItem.manaCost <= 0)
            return true;
        return this instanceof HeroUnit heroUnit && heroUnit.getMana() >= unitItem.manaCost;
    }

    private boolean isOffCooldown(UnitItem unitItem, ItemStack itemStack) {
        if (unitItem.cooldownTicksMax <= 0)
            return true;
        CompoundTag tag = ItemTagCompat.tag(itemStack);
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
                stack.save(this.level().registryAccess(), itemTag);
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
            ItemStack stack = i < list.size() ? ItemStack.parseOptional(this.level().registryAccess(), list.getCompound(i)) : ItemStack.EMPTY;
            this.unitItems.set(i, stack);
            if (!stack.isEmpty()) ron$applyItemAttributes(stack);
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("RETURN"))
    // 1.21.1 passes the ServerLevel instead of the looting level.
    private void ron$dropUnitItemsOnDeath(ServerLevel pLevel, DamageSource source, boolean recentlyHit, CallbackInfo ci) {
        if ((Object) this instanceof HeroUnit) return; // heroes keep their gear

        for (int i = 0; i < this.unitItems.size(); i++) {
            ItemStack stack = this.unitItems.get(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
            this.unitItems.set(i, ItemStack.EMPTY);
        }
    }

    /**
     * 1.5.0's periodic unit-item pass: the bell of arms keeps handing out villager inspiration
     * while it is held, items flagged {@code forceAutocast} fire themselves once off cooldown,
     * and a non-hero unit that is carrying anything gets the levelling particle.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void ron$tickUnitItems(CallbackInfo ci) {
        if (tickCount % 20 != 0)
            return;

        if (isHoldingActive(UnitItems.BELL_OF_ARMS)) {
            addEffect(new MobEffectInstance(
                    MobEffectHelpers.holder(MobEffectRegistrar.VILLAGER_INSPIRATION.get()), 30, 0, true, false));
        }
        if (this instanceof Unit auraOwner) {
            boolean hasHorrorArmour = isHolding(UnitItems.HUNGRY_HORROR_ARMOUR);
            boolean hasPiglinArmour = isHolding(UnitItems.GOLDEN_PIGLIN_ARMOUR);
            if (hasHorrorArmour || hasPiglinArmour) {
                for (Mob mob : MiscUtil.getEntitiesWithinRange(position(), UnitItems.ARMOUR_AURA_RANGE, Mob.class, level())) {
                    if (mob instanceof Unit unit2 && AlliancesServerEvents.isAlliedOrOwned(auraOwner.getOwnerName(), unit2.getOwnerName())
                            && !(mob instanceof AbstractTotem)) {
                        if (hasHorrorArmour && NightUtils.isInRangeOfNightSource(mob.position(), mob.level().isClientSide()))
                            mob.addEffect(new MobEffectInstance(MobEffectHelpers.holder(MobEffectRegistrar.LIFESTEAL.get()), 30, 0, true, false));
                        if (hasPiglinArmour && MiscUtil.isOnNetherTerrain(mob))
                            mob.addEffect(new MobEffectInstance(MobEffectHelpers.holder(MobEffectRegistrar.PIGLIN_FERVOR.get()), 30, 0, true, false));
                    }
                }
            }
        }
        for (ItemStack itemStack : getAllItems()) {
            CompoundTag tag = ItemTagCompat.tag(itemStack);
            if (tag != null && tag.hasUUID("uuid")) {
                UnitItem unitItem = ItemUtil.getUnitItem(itemStack);
                if (unitItem != null && unitItem.forceAutocast &&
                    isOffCooldown(unitItem, itemStack) && canAffordManaCost(unitItem)) {
                    use(tag.getUUID("uuid"));
                }
            }
        }
        if (!isEmpty() && !(this instanceof HeroUnit)) {
            ParticleUtil.addParticleExplosion(ParticleRegistrar.LEVEL_UP.get(), 2, level(), position(), 0.05);
        }
    }
}