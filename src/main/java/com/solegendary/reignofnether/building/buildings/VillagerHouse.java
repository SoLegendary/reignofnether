package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VillagerHouse extends ProductionBuilding {

    public final static String buildingName = "Villager House";
    public final static String structureName = "villager_house";

    public VillagerHouse(LevelAccessor level, BlockPos originPos, Rotation rotation, String ownerName) {
        super();
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.OAK_LOG;

        // TODO: all these onClick triggers needs to be on serverside
        this.productionButtons = Arrays.asList(
            new Button(
                "Zombie",
                14,
                "textures/mobheads/zombie.png",
                Keybinding.keyQ,
                () -> false,
                () -> false,
                () -> {
                    System.out.println("added zombie to queue");
                    this.productionQueue.add(new ZombieUnitProd(this));
                    BuildingServerboundPacket.startProduction(getMinCorner(this.blocks), ZombieUnitProd.itemName);
                }
            ),
            new Button(
                "Skeleton",
                14,
                "textures/mobheads/skeleton.png",
                Keybinding.keyW,
                () -> false,
                () -> false,
                () -> {
                    System.out.println("added skeleton to queue");
                    this.productionQueue.add(new SkeletonUnitProd(this));
                    BuildingServerboundPacket.startProduction(getMinCorner(this.blocks), SkeletonUnitProd.itemName);
                }
            ),
            new Button(
                "Creeper",
                14,
                "textures/mobheads/creeper.png",
                Keybinding.keyE,
                () -> false,
                () -> false,
                () -> {
                    System.out.println("added creeper to queue");
                    this.productionQueue.add(new CreeperUnitProd(this));
                    BuildingServerboundPacket.startProduction(getMinCorner(this.blocks), CreeperUnitProd.itemName);
                }
            )
        );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }
}
