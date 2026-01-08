package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.tps.TPSClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class GraveyardPlacement extends ProductionPlacement {

    public final List<ActiveProduction> stockpile = new ArrayList<>();
    public static final int STOCKPILE_CAP = 10;

    private int overflowUpgradeLevel = 0;
    
    // Server-side tracking of display entities
    private final List<ArmorStand> visualHeads = new ArrayList<>();

    public GraveyardPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override
    public boolean startProductionItem(ProductionItem prodItem) {
        boolean success = false;

        if (getBuilding() instanceof ProductionBuilding pb && !pb.productions.get().contains(prodItem)) {
            return false;
        }

        if (prodItem != null) {
            // Client-side: just add to queue to update UI, server validates real logic
            if (getLevel().isClientSide()) {
                ActiveProduction activeProduction = new ActiveProduction(prodItem, true, ownerName);
                productionQueue.add(activeProduction);
                success = true;
            } else {
                boolean allow = switch (prodItem.dupeRule) {
                    case DISALLOW -> !prodItem.itemIsBeingProduced(false, ownerName);
                    case DISALLOW_FOR_BUILDING -> !prodItem.itemIsBeingProducedAt(false, this);
                    case ALLOW -> true;
                };

                ResourceCost normalCost = prodItem.getCost(false, ownerName);
                boolean canAffordPopulation = prodItem.canAffordPopulation(getLevel(), ownerName);
                boolean canAffordNormalResources = false;
                for (Resources resources : ResourcesServerEvents.resourcesList) {
                    if (resources.ownerName.equals(ownerName)) {
                        canAffordNormalResources = (resources.food >= normalCost.food &&
                                resources.wood >= normalCost.wood &&
                                resources.ore >= normalCost.ore);
                        break;
                    }
                }
                
                // Overflow logic checks
                boolean isUpgraded = getUpgradeLevel() > 0;
                boolean popFull = !canAffordPopulation;
                boolean stockpileAvailable = stockpile.size() < STOCKPILE_CAP;

                if (allow) {
                    if (canAffordNormalResources && !popFull) {
                        // Standard production
                        ActiveProduction activeProduction = new ActiveProduction(prodItem, false, ownerName);
                        productionQueue.add(activeProduction);
                        ResourcesServerEvents.addSubtractResources(new Resources(
                                ownerName,
                                -normalCost.food,
                                -normalCost.wood,
                                -normalCost.ore
                        ));
                        success = true;
                    } else if (isUpgraded && popFull && stockpileAvailable) {
                        // Stockpile production (25% surcharge)
                        int extraFood = (int) Math.ceil(normalCost.food * 1.25);
                        int extraWood = (int) Math.ceil(normalCost.wood * 1.25);
                        int extraOre = (int) Math.ceil(normalCost.ore * 1.25);

                        boolean canAffordExtra = false;
                        for (Resources resources : ResourcesServerEvents.resourcesList)
                            if (resources.ownerName.equals(ownerName))
                                canAffordExtra = (resources.food >= extraFood && resources.wood >= extraWood && resources.ore >= extraOre);

                        if (canAffordExtra) {
                            ActiveProduction activeProduction = new ActiveProduction(prodItem, false, ownerName, true);
                            productionQueue.add(activeProduction);
                            ResourcesServerEvents.addSubtractResources(new Resources(
                                    ownerName,
                                    -extraFood,
                                    -extraWood,
                                    -extraOre
                            ));
                            success = true;
                        } else {
                            // Warn resources
                            ResourcesClientboundPacket.warnInsufficientResources(ownerName,
                                    ResourcesServerEvents.canAfford(ownerName, ResourceName.FOOD, extraFood),
                                    ResourcesServerEvents.canAfford(ownerName, ResourceName.WOOD, extraWood),
                                    ResourcesServerEvents.canAfford(ownerName, ResourceName.ORE, extraOre)
                            );
                        }
                    } else {
                        // Fail
                        if (!prodItem.isBelowMaxPopulation(level, ownerName))
                            ResourcesClientboundPacket.warnMaxPopulation(ownerName);
                        else if (!canAffordPopulation) {
                            ResourcesClientboundPacket.warnInsufficientPopulation(ownerName);
                        } else {
                            ResourcesClientboundPacket.warnInsufficientResources(ownerName,
                                    ResourcesServerEvents.canAfford(ownerName, ResourceName.FOOD, normalCost.food),
                                    ResourcesServerEvents.canAfford(ownerName, ResourceName.WOOD, normalCost.wood),
                                    ResourcesServerEvents.canAfford(ownerName, ResourceName.ORE, normalCost.ore)
                            );
                        }
                    }
                }
            }
        }
        return success;
    }

    @Override
    protected void tickProductionQueue(Level tickLevel) {
        // First: release from stockpile if possible (server-side only).
        if (!tickLevel.isClientSide()) {
            boolean releasedAny = false;
            while (!stockpile.isEmpty()) {
                ActiveProduction head = stockpile.get(0);
                if (!head.item.canAffordPopulation(tickLevel, ownerName)) {
                    break;
                }
                head.item.onComplete.accept(tickLevel, this);
                stockpile.remove(0);
                releasedAny = true;
            }
            if (releasedAny) {
                updateVisuals(tickLevel);
            }
        }

        // Then: tick production queue.
        if (productionQueue.isEmpty()) {
            return;
        }

        ActiveProduction nextItem = productionQueue.get(0);

        // Normal behavior if not an overflow-stockpile entry.
        if (!nextItem.overflowStockpile) {
            if (nextItem.item.tick(this, nextItem)) {
                if (!tickLevel.isClientSide()) {
                    productionQueue.remove(0);
                    if (productionQueue.isEmpty())
                        BuildingClientboundPacket.clearQueue(this.originPos);
                    else
                        BuildingClientboundPacket.completeProduction(this.originPos);
                }
            }
            return;
        }

        // Overflow-stockpile behavior: allow ticking while population is full, and store instead of spawning.
        // (Only meaningful when upgraded and there is stockpile space; if not, fall back to normal gating.)
        boolean upgradeOK = getUpgradeLevel() > 0 && stockpile.size() < STOCKPILE_CAP;
        if (!upgradeOK || !isBuilt) {
            return;
        }

        if (nextItem.ticksLeft > 0) {
            float decrement;
            if (tickLevel.isClientSide()) {
                decrement = (float) (TPSClientEvents.getCappedTPS() / 20D);
                if (ResearchClient.hasCheat("warpten"))
                    decrement *= 10f;
            } else {
                decrement = ResearchServerEvents.playerHasCheat(ownerName, "warpten") ? 10f : 1f;
            }

            nextItem.ticksLeft -= decrement;
            if (nextItem.ticksLeft < 0)
                nextItem.ticksLeft = 0;
        }

        if (nextItem.ticksLeft <= 0 && !tickLevel.isClientSide()) {
            // Complete into stockpile (do NOT spawn yet).
            nextItem.item.recordScore(this);
            stockpile.add(nextItem);
            updateVisuals(tickLevel);

            productionQueue.remove(0);
            if (productionQueue.isEmpty())
                BuildingClientboundPacket.clearQueue(this.originPos);
            else
                BuildingClientboundPacket.completeProduction(this.originPos);
        }
    }
    
    private void updateVisuals(Level level) {
        // Clear existing
        for (ArmorStand as : visualHeads) {
            if (as.isAlive()) as.discard(); // or kill()
        }
        visualHeads.clear();
        
        // Add new
        for (int i = 0; i < stockpile.size(); i++) {
            ActiveProduction ap = stockpile.get(i);
            
            // Position logic: circle around center?
            // Graveyard is approx 9x9? Center is originPos + relative offset.
            // But we have centrePos field.
            double radius = 3.0;
            double angle = (2 * Math.PI / STOCKPILE_CAP) * i;
            double x = centrePos.getX() + radius * Math.cos(angle);
            double z = centrePos.getZ() + radius * Math.sin(angle);
            double y = centrePos.getY() + 3; // Approx height
            
            ArmorStand as = new ArmorStand(EntityType.ARMOR_STAND, level);
            as.setPos(x + 0.5, y, z + 0.5);
            as.setNoGravity(true);
            as.setInvisible(true);
            as.setCustomName(Component.literal("Stockpiled Unit"));
            as.setCustomNameVisible(false);
            
            ItemStack headItem = getHeadItem(ap.item);
            as.setItemSlot(EquipmentSlot.HEAD, headItem);
            
            level.addFreshEntity(as);
            visualHeads.add(as);
        }
    }
    
    private ItemStack getHeadItem(ProductionItem item) {
        // Basic mapping (vanilla item heads are limited; use closest equivalent)
        if (item == ProductionItems.ZOMBIE) return new ItemStack(Items.ZOMBIE_HEAD);
        if (item == ProductionItems.HUSK) return new ItemStack(Items.ZOMBIE_HEAD);
        if (item == ProductionItems.DROWNED) return new ItemStack(Items.ZOMBIE_HEAD);

        if (item == ProductionItems.SKELETON) return new ItemStack(Items.SKELETON_SKULL);
        if (item == ProductionItems.STRAY) return new ItemStack(Items.SKELETON_SKULL);

        return new ItemStack(Items.SKELETON_SKULL); // Default
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        for (ArmorStand as : visualHeads) {
            if (as.isAlive()) as.discard();
        }
        visualHeads.clear();
        stockpile.clear();
    }

    public int getOverflowUpgradeLevel() {
        return overflowUpgradeLevel;
    }

    public void setOverflowUpgradeLevel(int overflowUpgradeLevel) {
        this.overflowUpgradeLevel = overflowUpgradeLevel;
    }
}

