package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.ConnectPortal;
import com.solegendary.reignofnether.ability.abilities.DisconnectPortal;
import com.solegendary.reignofnether.ability.abilities.GotoPortal;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchPortalForCivilian;
import com.solegendary.reignofnether.research.researchItems.ResearchPortalForMilitary;
import com.solegendary.reignofnether.research.researchItems.ResearchPortalForTransport;
import com.solegendary.reignofnether.research.researchItems.ResearchResourceCapacity;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.piglins.*;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Portal extends ProductionBuilding implements NetherConvertingBuilding {

    public final static int CIVILIIAN_PORTAL_POPULATION_SUPPLY = 15;

    public enum PortalType {
        BASIC,
        CIVILIAN,
        MILITARY,
        TRANSPORT
    }

    public final static String buildingName = "Basic Portal";
    public final static String structureName = "portal_basic";
    public final static ResourceCost cost = ResourceCosts.BASIC_PORTAL;

    private final double NETHER_CONVERT_RANGE_MAX = 20;
    private double netherConvertRange = 3;
    private int netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
    private int convertsAfterMaxRange = 0;
    public PortalType portalType = PortalType.BASIC;

    public BlockPos destination; // for transport portals

    public double getMaxRange() { return NETHER_CONVERT_RANGE_MAX; }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (isBuilt) {
            netherConvertTicksLeft -= 1;
            if (netherConvertTicksLeft <= 0 && convertsAfterMaxRange < MAX_CONVERTS_AFTER_MAX_RANGE) {
                netherConvertTick(this, netherConvertRange, NETHER_CONVERT_RANGE_MAX);
                if (netherConvertRange < NETHER_CONVERT_RANGE_MAX)
                    netherConvertRange += 0.1f;
                else
                    convertsAfterMaxRange += 1;
                netherConvertTicksLeft = NETHER_CONVERT_TICKS_MAX;
            }
        }
    }

    @Override
    public boolean shouldBeDestroyed() {
        boolean shouldBeDestroyed = super.shouldBeDestroyed();
        if (shouldBeDestroyed)
            disconnectPortal();
        return shouldBeDestroyed;
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        disconnectPortal();
        super.destroy(serverLevel);
    }

    public Portal(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.GRAY_GLAZED_TERRACOTTA;
        this.icon = new ResourceLocation("minecraft", "textures/block/gray_glazed_terracotta.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 1.2f;

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);

        Ability connectPortal = new ConnectPortal(this);
        this.abilities.add(connectPortal);
        Ability gotoPortal = new GotoPortal(this);
        this.abilities.add(gotoPortal);
        Ability disconnectPortal = new DisconnectPortal(this);
        this.abilities.add(disconnectPortal);

        if (level.isClientSide()) {
            this.abilityButtons.add(connectPortal.getButton(Keybindings.keyQ));
            this.abilityButtons.add(gotoPortal.getButton(Keybindings.keyW));
            this.abilityButtons.add(disconnectPortal.getButton(Keybindings.keyE));
            this.productionButtons = Arrays.asList(
                    ResearchPortalForCivilian.getStartButton(this, Keybindings.keyQ),
                    ResearchPortalForMilitary.getStartButton(this, Keybindings.keyW),
                    ResearchPortalForTransport.getStartButton(this, Keybindings.keyE)
            );
        }
    }

    public void disconnectPortal() {
        if (destination != null) {
            Building targetBuilding = BuildingUtils.findBuilding(getLevel().isClientSide(), destination);
            if (targetBuilding instanceof Portal targetPortal && portalType == Portal.PortalType.TRANSPORT)
                targetPortal.destination = null;
        }
        destination = null;
    }

    @Override
    protected void buildNextBlock(ServerLevel level) {
        super.buildNextBlock(level);
        if (this.getBlocksPlaced() >= getBlocksTotal())
            this.getLevel().setBlockAndUpdate(this.centrePos, Blocks.FIRE.defaultBlockState());
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp) {
        BlockPos worldBp = relativeBp.offset(this.originPos);
        Block block = this.getLevel().getBlockState(worldBp).getBlock();
        return block != Blocks.OBSIDIAN && block != Blocks.NETHER_PORTAL;
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public void changeStructure(PortalType portalType) {
        String newStructureName = "";
        switch (portalType) {
            case CIVILIAN -> {
                this.name = "Civilian Portal";
                this.portraitBlock = Blocks.CYAN_GLAZED_TERRACOTTA;
                this.icon = new ResourceLocation("minecraft", "textures/block/cyan_glazed_terracotta.png");
                newStructureName = "portal_civilian";
                this.canAcceptResources = true;
                popSupply = CIVILIIAN_PORTAL_POPULATION_SUPPLY;
                if (this.getLevel().isClientSide()) {
                    this.productionButtons = Arrays.asList(
                            ResearchResourceCapacity.getStartButton(this, Keybindings.keyQ)
                    );
                }
            }
            case MILITARY -> {
                this.name = "Military Portal";
                this.portraitBlock = Blocks.RED_GLAZED_TERRACOTTA;
                this.icon = new ResourceLocation("minecraft", "textures/block/red_glazed_terracotta.png");
                newStructureName = "portal_military";
                this.canSetRallyPoint = true;
                if (this.getLevel().isClientSide())
                    this.productionButtons = Arrays.asList(
                            BruteProd.getStartButton(this, Keybindings.keyQ),
                            HeadhunterProd.getStartButton(this, Keybindings.keyW),
                            HoglinProd.getStartButton(this, Keybindings.keyE),
                            BlazeProd.getStartButton(this, Keybindings.keyR),
                            WitherSkeletonProd.getStartButton(this, Keybindings.keyT),
                            GhastProd.getStartButton(this, Keybindings.keyY)
                    );
            }
            case TRANSPORT -> {
                this.name = "Transport Portal";
                this.portraitBlock = Blocks.BLUE_GLAZED_TERRACOTTA;
                this.icon = new ResourceLocation("minecraft", "textures/block/blue_glazed_terracotta.png");
                newStructureName = "portal_transport";
            }
        }
        if (!newStructureName.isEmpty()) {
            ArrayList<BuildingBlock> newBlocks = BuildingBlockData.getBuildingBlocks(newStructureName, this.getLevel());
            this.blocks = getAbsoluteBlockData(newBlocks, this.getLevel(), originPos, rotation);
            super.refreshBlocks();
            this.portalType = portalType;
        }
    }

    public boolean isUpgraded() {
        return portalType != PortalType.BASIC;
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Portal.buildingName,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Portal.class,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(CentralPortal.buildingName) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                () -> BuildingClientEvents.setBuildingToPlace(Portal.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Portal.buildingName, Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("An obsidian portal used to spread nether blocks.", Style.EMPTY),
                        FormattedCharSequence.forward("Can be upgraded for various different functions.", Style.EMPTY)
                ),
                null
        );
    }
}




















