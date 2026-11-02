package com.solegendary.reignofnether.building.custombuilding;

import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.blocks.RTSStructureBlockEntity;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.ui.EditorRows;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static net.minecraft.util.Mth.sign;

public class CustomBuildingClientEvents {

    // since every custom building has a different structure, we need to maintain a list of them here
    public static final ArrayList<CustomBuilding> customBuildings = new ArrayList<>();

    public static final ArrayList<BlockPos> rtsStructuresToRenderBB = new ArrayList<>();

    private static final Minecraft MC = Minecraft.getInstance();

    private static CustomBuilding customBuildingToEdit = null;
    private static HudFrame frame = null; // the editor drawn last frame, for clicks

    public static boolean showCommandsMenu = false;
    public static boolean deregisterConfirm = false;

    public static void setCustomBuildingToEdit(CustomBuilding customBuilding) {
        if (customBuildingToEdit != customBuilding)
            customBuildingToEdit = customBuilding;
        else
            customBuildingToEdit = null;
        CustomBuildingClientEvents.deregisterConfirm = false;
        if (showCommandsMenu)
            CustomBuildingMenu.forceRefreshCommandEditBoxes();
    }

    public static CustomBuilding getCustomBuildingToEdit() {
        return customBuildingToEdit;
    }

    public static Building getCustomBuilding(String name) {
        for (CustomBuilding customBuilding : customBuildings)
            if (customBuilding.name.equals(name))
                return customBuilding;
        return null;
    }

    public static void registerCustomBuilding(String playerName, String name, Vec3i structureSize, CompoundTag structureNbt, CompoundTag attributesNbt, ListTag commandsNbt) {
        if (MC.player == null || (!playerName.isEmpty() && !MC.player.getName().getString().equals(playerName)))
            return;

        for (CustomBuilding customBuilding : customBuildings) {
            if (customBuilding.name.equals(name)) {
                MC.player.sendSystemMessage(SystemMessage.error(Component.literal("ERROR (client): custom building '" + name + "' already exists")));
                return;
            }
        }
        ArrayList<BuildingBlock> blocks = BuildingBlockData.getBuildingBlocksFromNbt(structureNbt);
        Block portraitBlock = Blocks.COMMAND_BLOCK;
        for (BuildingBlock bb : blocks) {
            BlockState bs = bb.getBlockState();
            if (!bs.isAir() && bs.getFluidState().isEmpty()) {
                portraitBlock = bs.getBlock();
            }
        }
        CustomBuilding building = new CustomBuilding(name, structureSize, portraitBlock, structureNbt, attributesNbt, commandsNbt);

        customBuildings.add(building);

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) throws NoSuchFieldException {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || MC.level == null) {
            return;
        }

        for (BlockPos bp : rtsStructuresToRenderBB) {
            if (MC.level.getBlockEntity(bp) instanceof RTSStructureBlockEntity be) {
                BlockPos pos = be.getStructurePos();
                Vec3i size = be.getStructureSize();
                MyRenderer.drawLineBox(evt.getPoseStack(),
                        new AABB(bp.getX() + pos.getX(),
                                bp.getY() + pos.getY(),
                                bp.getZ() + pos.getZ(),
                                bp.getX() + pos.getX() + size.getX(),
                                bp.getY() + pos.getY() + size.getY(),
                                bp.getZ() + pos.getZ() + size.getZ()),
                        1f, 1f, 1f, 1f);
            }
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        frame = null;
        if (customBuildingToEdit != null && MC.screen instanceof TopdownGui) {
            frame = EditorRows.begin(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
            CustomBuildingMenu.render(frame, customBuildingToEdit);
            EditorRows.end(frame);
        }
    }

    public static boolean isMouseOverHud(int mouseX, int mouseY) {
        return EditorRows.isOver(frame, mouseX, mouseY);
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        EditorRows.click(frame, evt.getMouseX(), evt.getMouseY(), evt.getButton());
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Post evt) {
        if (customBuildingToEdit != null && showCommandsMenu) {
            CustomBuildingMenu.scrollCommands(-sign(evt.getScrollDelta()));
        }
    }
}
