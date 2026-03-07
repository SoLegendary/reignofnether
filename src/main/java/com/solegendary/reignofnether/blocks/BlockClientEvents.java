package com.solegendary.reignofnether.blocks;


import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.NightSource;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class BlockClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    public static NightCircleMode nightCircleMode = NightCircleMode.NO_OVERLAPS;

    // deals with block rendering jobs like range indicators
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        ResourceLocation rl = ResourceLocation.parse("forge:textures/white.png");
        var vertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(rl));
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        // draw range indicators for buildings with abilities and monster night sources
        for (BuildingPlacement building : BuildingClientEvents.getBuildings()) {
            if (building instanceof RangeIndicator ri) {
                for (BlockPos bp : ri.getHighlightBps()) {
                    int snowLayers = BlockUtils.getSnowLayers(building.level.getBlockState(bp.above()));
                    float yOffset = snowLayers * 0.125f;
                    if (BuildingClientEvents.getSelectedBuildings().contains(building)) {
                        MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumer, Direction.UP, yOffset, bp, 0f, 0.8f, 0f, 0.3f);
                    } else if (!ri.showOnlyWhenSelected()) {
                        MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumer, Direction.UP, yOffset, bp, 0f, 0f, 0f, 0.6f);
                    }
                }
            }
        }

        for (LivingEntity le : UnitClientEvents.getSelectedUnits()) {
            if (le instanceof RangeIndicator ri) {
                for (BlockPos bp : ri.getHighlightBps()) {
                    int snowLayers = BlockUtils.getSnowLayers(le.level().getBlockState(bp.above()));
                    float yOffset = snowLayers * 0.125f;
                    MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumer, Direction.UP, yOffset, bp, 0f, 0.8f, 0f, 0.3f);
                }
            }
        }
    }

    // maintain a mapping of night sources for easy culling calcs
    private static final int NIGHT_SOURCES_UPDATE_TICKS_MAX = 50;
    private static int nightSourcesUpdateTicks = NIGHT_SOURCES_UPDATE_TICKS_MAX;
    public static ArrayList<Pair<BlockPos, Integer>> nightSourceOrigins = new ArrayList<>();
    public static final int VISIBLE_BORDER_ADJ = 2; // shrink a bit so borderlines themselves are safe to walk on

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        nightSourcesUpdateTicks -= 1;
        if (nightSourcesUpdateTicks <= 0) {
            nightSourcesUpdateTicks = NIGHT_SOURCES_UPDATE_TICKS_MAX;

            nightSourceOrigins.clear();

            // get list of night source centre:range pairs
            for (BuildingPlacement building : BuildingClientEvents.getBuildings()) {
                if (!building.isExploredClientside || !(building instanceof NightSource ns) || ns.getNightRange() <= 0) {
                    continue;
                }
                nightSourceOrigins.add(new Pair<>(building.centrePos, ns.getNightRange() - VISIBLE_BORDER_ADJ));
            }
        }
    }
}
