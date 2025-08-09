package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.building.BuildingUtils.getMinCorner;

public class ProductionPlacement extends BuildingPlacement {
    private BlockPos rallyPoint;
    private LivingEntity rallyPointEntity;
    public List<Button> productionButtons;
    public final List<ActiveProduction> productionQueue = new ArrayList<>();
    private ActiveProduction active;


    public ProductionPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        if (building instanceof ProductionBuilding productionBuilding) {
            productionButtons = productionBuilding.productions.getButtons(this);
        }
    }

    public BlockPos getRallyPoint() { return this.rallyPoint; }

    public LivingEntity getRallyPointEntity() {
        if (this.rallyPointEntity == null)
            return null;
        if (!this.rallyPointEntity.isAlive()) {
            this.rallyPointEntity = null;
            return null;
        }
        return this.rallyPointEntity;
    }

    public void setRallyPoint(BlockPos rallyPoint) {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        if (!canSetRallyPoint())
            return;
        if (isPosInsideBuilding(rallyPoint))
            this.rallyPoint = null;
        else
            this.rallyPoint = rallyPoint;
        this.rallyPointEntity = null;
    }

    public boolean canSetRallyPoint() {
        return ((ProductionBuilding) getBuilding()).canSetRallyPoint;
    }

    public void setRallyPointEntity(LivingEntity entity) {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        if (!canSetRallyPoint() || entity == null)
            return;
        else if (!(entity instanceof Unit unit) || unit.getOwnerName().equals(this.ownerName))
            this.rallyPointEntity = entity;
        this.rallyPoint = null;
    }

    private boolean isProducing() {
        return this.productionQueue.size() > 0;
    }
    // start with the centre pos then go down and look at adjacent blocks until we reach a non-solid block
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        return building.getIndoorSpawnPoint(level, centrePos);
    }

    // start with the centre pos then go down and look at adjacent blocks until we reach a non-solid block
    public BlockPos getDefaultOutdoorSpawnPoint() {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        return building.getDefaultOutdoorSpawnPoint(getMinCorner(blocks));
    }

    public Entity produceUnit(ServerLevel level, EntityType<? extends Unit> entityType, String ownerName, boolean spawnIndoors) {
        ProductionBuilding building = (ProductionBuilding) getBuilding();
        LivingEntity rallyEntity = getRallyPointEntity();
        BlockPos spawnPoint;
        if (spawnIndoors) {
            spawnPoint = getIndoorSpawnPoint(level);
            if (entityType == EntityRegistrar.GHAST_UNIT.get())
                spawnPoint = spawnPoint.offset(0,5,0);
        }
        else if (rallyPoint != null)
            spawnPoint = getClosestGroundPos(rallyPoint, (int) building.spawnRadiusOffset);
        else if (rallyPointEntity != null)
            spawnPoint = getClosestGroundPos(rallyPointEntity.getOnPos(), (int) building.spawnRadiusOffset);
        else
            spawnPoint = getDefaultOutdoorSpawnPoint();

        Entity entity = entityType.spawn(level, (CompoundTag) null,
                null,
                spawnPoint,
                MobSpawnType.SPAWNER,
                true,
                false
        );
        BlockPos defaultRallyPoint = getDefaultOutdoorSpawnPoint();

        BlockPos rallyPoint = this.rallyPoint == null ? defaultRallyPoint : this.rallyPoint;

        if (entity instanceof Unit unit) {
            unit.setOwnerName(ownerName);
            unit.setupEquipmentAndUpgradesServer();

            if (rallyEntity != null) {
                if (ResourceSources.isHuntableAnimal(rallyEntity)) {
                    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                        UnitServerEvents.addActionItem(
                                this.ownerName,
                                UnitAction.ATTACK,
                                rallyEntity.getId(),
                                new int[] { entity.getId() },
                                rallyPoint,
                                new BlockPos(0,0,0)
                        );
                    });
                } else {
                    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                        UnitServerEvents.addActionItem(
                                this.ownerName,
                                UnitAction.FOLLOW,
                                rallyEntity.getId(),
                                new int[] { entity.getId() },
                                rallyPoint,
                                new BlockPos(0,0,0)
                        );
                    });
                }
            } else {
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                    UnitServerEvents.addActionItem(
                            this.ownerName,
                            UnitAction.MOVE,
                            -1,
                            new int[] { entity.getId() },
                            rallyPoint,
                            new BlockPos(0,0,0)
                    );
                });
            }
        }
        return entity;
    }

    // return true if successful
    public boolean startProductionItem(ProductionItem prodItem, BlockPos pos) {
        boolean success = false;

        /*ProductionItem prodItem = null;
        switch(itemName) {
            case CreeperProd.itemName -> prodItem = new CreeperProd(this);
            case SkeletonProd.itemName -> prodItem = new SkeletonProd(this);
            case ZombieProd.itemName -> prodItem = new ZombieProd(this);
            case StrayProd.itemName -> prodItem = new StrayProd(this);
            case HuskProd.itemName -> prodItem = new HuskProd(this);
            case DrownedProd.itemName -> prodItem = new DrownedProd(this);
            case SpiderProd.itemName -> prodItem = new SpiderProd(this);
            case PoisonSpiderProd.itemName -> prodItem = new PoisonSpiderProd(this);
            case VillagerProd.itemName -> prodItem = new VillagerProd(this);
            case ZombieVillagerProd.itemName -> prodItem = new ZombieVillagerProd(this);
            case VindicatorProd.itemName -> prodItem = new VindicatorProd(this);
            case PillagerProd.itemName -> prodItem = new PillagerProd(this);
            case IronGolemProd.itemName -> prodItem = new IronGolemProd(this);
            case WitchProd.itemName -> prodItem = new WitchProd(this);
            case EvokerProd.itemName -> prodItem = new EvokerProd(this);
            case SlimeProd.itemName -> prodItem = new SlimeProd(this);
            case WardenProd.itemName -> prodItem = new WardenProd(this);
            case RavagerProd.itemName -> prodItem = new RavagerProd(this);

            case GruntProd.itemName -> prodItem = new GruntProd(this);
            case BruteProd.itemName -> prodItem = new BruteProd(this);
            case HeadhunterProd.itemName -> prodItem = new HeadhunterProd(this);
            case HoglinProd.itemName -> prodItem = new HoglinProd(this);
            case BlazeProd.itemName -> prodItem = new BlazeProd(this);
            case WitherSkeletonProd.itemName -> prodItem = new WitherSkeletonProd(this);
            case MagmaCubeProd.itemName -> prodItem = new MagmaCubeProd(this);
            case GhastProd.itemName -> prodItem = new GhastProd(this);

            case ResearchVindicatorAxes.itemName -> prodItem = new ResearchVindicatorAxes(this);
            case ResearchPillagerCrossbows.itemName -> prodItem = new ResearchPillagerCrossbows(this);
            case ResearchLabLightningRod.itemName -> prodItem = new ResearchLabLightningRod(this);
            case ResearchResourceCapacity.itemName -> prodItem = new ResearchResourceCapacity(this);
            case ResearchSpiderJockeys.itemName -> prodItem = new ResearchSpiderJockeys(this);
            case ResearchPoisonSpiders.itemName -> prodItem = new ResearchPoisonSpiders(this);
            case ResearchHusks.itemName -> prodItem = new ResearchHusks(this);
            case ResearchDrowned.itemName -> prodItem = new ResearchDrowned(this);
            case ResearchStrays.itemName -> prodItem = new ResearchStrays(this);
            case ResearchSlimeConversion.itemName -> prodItem = new ResearchSlimeConversion(this);
            case ResearchLingeringPotions.itemName -> prodItem = new ResearchLingeringPotions(this);
            case ResearchEvokerVexes.itemName -> prodItem = new ResearchEvokerVexes(this);
            case ResearchGolemSmithing.itemName -> prodItem = new ResearchGolemSmithing(this);
            case ResearchSilverfish.itemName -> prodItem = new ResearchSilverfish(this);
            case ResearchSculkAmplifiers.itemName -> prodItem = new ResearchSculkAmplifiers(this);
            case ResearchCastleFlag.itemName -> prodItem = new ResearchCastleFlag(this);
            case ResearchRavagerCavalry.itemName -> prodItem = new ResearchRavagerCavalry(this);
            case ResearchBruteShields.itemName -> prodItem = new ResearchBruteShields(this);
            case ResearchHoglinCavalry.itemName -> prodItem = new ResearchHoglinCavalry(this);
            case ResearchHeavyTridents.itemName -> prodItem = new ResearchHeavyTridents(this);
            case ResearchBlazeFirewall.itemName -> prodItem = new ResearchBlazeFirewall(this);
            case ResearchWitherClouds.itemName -> prodItem = new ResearchWitherClouds(this);
            case ResearchAdvancedPortals.itemName -> prodItem = new ResearchAdvancedPortals(this);
            case ResearchFireResistance.itemName -> prodItem = new ResearchFireResistance(this);
            case ResearchGrandLibrary.itemName -> prodItem = new ResearchGrandLibrary(this);
            case ResearchSpiderWebs.itemName -> prodItem = new ResearchSpiderWebs(this);
            case ResearchBloodlust.itemName -> prodItem = new ResearchBloodlust(this);
            case ResearchCubeMagma.itemName -> prodItem = new ResearchCubeMagma(this);
            case ResearchSoulFireballs.itemName -> prodItem = new ResearchSoulFireballs(this);

            case ResearchPortalForCivilian.itemName -> prodItem = new ResearchPortalForCivilian(this);
            case ResearchPortalForMilitary.itemName -> prodItem = new ResearchPortalForMilitary(this);
            case ResearchPortalForTransport.itemName -> prodItem = new ResearchPortalForTransport(this);
        }*/
        if (prodItem != null) {
            // only worry about checking affordability on serverside
            if (getLevel().isClientSide()) {
                ActiveProduction activeProduction = new ActiveProduction(prodItem, true, ownerName);
                productionQueue.add(activeProduction);
                success = true;
            }
            else {
                if (prodItem.canAfford(getLevel(), ownerName)) {
                    ActiveProduction activeProduction = new ActiveProduction(prodItem, false, ownerName);
                    productionQueue.add(activeProduction);
                    ResourcesServerEvents.addSubtractResources(new Resources(
                            ownerName,
                            -prodItem.getCost(level.isClientSide(), ownerName).food,
                            -prodItem.getCost(level.isClientSide(), ownerName).wood,
                            -prodItem.getCost(level.isClientSide(), ownerName).ore
                    ));
                    success = true;
                }
                else {
                    if (!prodItem.isBelowMaxPopulation(level, ownerName))
                        ResourcesClientboundPacket.warnMaxPopulation(ownerName);
                    else if (!prodItem.canAffordPopulation(getLevel(), ownerName))
                        ResourcesClientboundPacket.warnInsufficientPopulation(ownerName);
                    else
                        ResourcesClientboundPacket.warnInsufficientResources(ownerName,
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.FOOD, prodItem.getCost(level.isClientSide(), ownerName).food),
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.WOOD, prodItem.getCost(level.isClientSide(), ownerName).wood),
                                ResourcesServerEvents.canAfford(ownerName, ResourceName.ORE, prodItem.getCost(level.isClientSide(), ownerName).ore)
                        );
                }
            }
        }
        return success;
    }

    public boolean cancelProductionItem(ProductionItem item, BlockPos pos, boolean frontItem) {
        boolean success = false;

        if (!productionQueue.isEmpty()) {
            if (frontItem) {
                ActiveProduction prodItem = productionQueue.get(0);
                productionQueue.remove(0);
                active = null;
                if (!getLevel().isClientSide()) {
                    ResourcesServerEvents.addSubtractResources(new Resources(
                            ownerName,
                            prodItem.item.getCost(level.isClientSide(), ownerName).food,
                            prodItem.item.getCost(level.isClientSide(), ownerName).wood,
                            prodItem.item.getCost(level.isClientSide(), ownerName).ore
                    ));
                }
                success = true;
            }
            else {
                // find first non-started item to remove
                for (int i = 0; i < productionQueue.size(); i++) {
                    ActiveProduction prodItem = productionQueue.get(i);
                    if (prodItem.item.equals(item) &&
                            prodItem.ticksLeft >= prodItem.item.getCost(level.isClientSide(), ownerName).ticks) {
                        productionQueue.remove(prodItem);
                        if (!getLevel().isClientSide()) {
                            ResourcesServerEvents.addSubtractResources(new Resources(
                                    ownerName,
                                    prodItem.item.getCost(level.isClientSide(), ownerName).food,
                                    prodItem.item.getCost(level.isClientSide(), ownerName).wood,
                                    prodItem.item.getCost(level.isClientSide(), ownerName).ore
                            ));
                        }
                        success = true;
                        break;
                    }
                }
            }
        }
        return success;
    }

    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (productionQueue.size() >= 1) {
            ActiveProduction nextItem = productionQueue.get(0);
            if (nextItem.item.tick(this, nextItem)) {
                if (!tickLevel.isClientSide()) {
                    active = null;
                    productionQueue.remove(0);
                    if (productionQueue.isEmpty())
                        BuildingClientboundPacket.clearQueue(this.originPos);
                    else
                        BuildingClientboundPacket.completeProduction(this.originPos);
                }
            }
        }
    }

    @Override
    public void updateButtons() {
        if (level.isClientSide()) {
            super.updateButtons();
            if (getBuilding() instanceof ProductionBuilding productionBuilding) {
                productionButtons = productionBuilding.productions.getButtons(this);
            }
        }
    }
}
