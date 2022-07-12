package com.solegendary.reignofnether.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.buildings.VillagerHouse;
import com.solegendary.reignofnether.building.buildings.VillagerTower;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static Boolean initedStructures = false;

    public static Class<? extends Building> lastBuildingToPlace = null;
    public static Class<? extends Building> buildingToPlace = null;
    private static Vec3i sizeToPlace = new Vec3i(0,0,0);
    private static ArrayList<BuildingBlock> blocksToPlace = new ArrayList<>();
    private static ArrayList<BlockState> paletteToPlace = new ArrayList<>();
    private static ArrayList<Pair<BlockPos, Direction>> facesToHighlight = new ArrayList<>();

    // highlights an area to construct a building by drawing transparent green/red faces around it
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    public static void highlightArea(PoseStack matrix, BlockPos originPos) {
        for (Pair<BlockPos, Direction> face : facesToHighlight) {
            BlockPos pos = new BlockPos(
                    originPos.getX() + face.getFirst().getX(),
                    originPos.getY() + face.getFirst().getY(),
                    originPos.getZ() + face.getFirst().getZ()
            );
            MyRenderer.drawWhiteBlockFace(matrix, face.getSecond(), pos, 0.5f);
        }

    }

    public static void placeBuilding() {

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelLastEvent evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;

        if (buildingToPlace != null && initedStructures) {
            highlightArea(evt.getPoseStack(), CursorClientEvents.getPreselectedBlockPos());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (!initedStructures) {
            BuildingBlockData.initBlockData(MC);
            initedStructures = true;
        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (evt.getKey() == Keybinds.fnums[6].getKey().getValue()) {
                buildingToPlace = VillagerHouse.class;
            }
            else if (evt.getKey() == Keybinds.fnums[7].getKey().getValue()) {
                buildingToPlace = VillagerTower.class;
            }
            else if (evt.getKey() == Keybinds.fnums[8].getKey().getValue()) {
                buildingToPlace = null;
            }

            if (lastBuildingToPlace != buildingToPlace) {
                // load the new buildingToPlace's data
                try {
                    Method getBlockData = buildingToPlace.getMethod("getBlockData");
                    blocksToPlace = (ArrayList<BuildingBlock>) getBlockData.invoke(null);
                    Method getPaletteData = buildingToPlace.getMethod("getPaletteData");
                    paletteToPlace = (ArrayList<BlockState>) getPaletteData.invoke(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sizeToPlace = Building.getBuildingSize(blocksToPlace);
                System.out.println(sizeToPlace.getX() + "|" + sizeToPlace.getY() + "|" + sizeToPlace.getZ());


                // load faces to highlight
                facesToHighlight = new ArrayList<>();
                for (BuildingBlock block : blocksToPlace) {

                    // TODO: only highlight faces on the outside of the building
                    // maybe we can do this by getting a list of BPs for each of the 5 directions
                    // where each list contains only the first block found as each side is scanned inwards

                    BlockPos bp = block.blockPos;
                    BuildingBlock west = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.west());
                    BuildingBlock north = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.north());
                    BuildingBlock east = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.east());
                    BuildingBlock above = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.above());
                    BuildingBlock south = BuildingBlockData.getBuildingBlockByPos(blocksToPlace, bp.south());

                    if (west == null || west.getBlockState(paletteToPlace).isAir())
                        facesToHighlight.add(new Pair(bp, Direction.WEST));
                    if (north == null || north.getBlockState(paletteToPlace).isAir())
                        facesToHighlight.add(new Pair(bp, Direction.NORTH));
                    if (east == null || east.getBlockState(paletteToPlace).isAir())
                        facesToHighlight.add(new Pair(bp, Direction.EAST));
                    if (above == null || above.getBlockState(paletteToPlace).isAir())
                        facesToHighlight.add(new Pair(bp, Direction.UP));
                    if (south == null || south.getBlockState(paletteToPlace).isAir())
                        facesToHighlight.add(new Pair(bp, Direction.SOUTH));
                }

            }
            lastBuildingToPlace = buildingToPlace;
        }
        if (evt.getAction() == GLFW.GLFW_MOUSE_BUTTON_1 && buildingToPlace != null) {

        }
    }
}
