package com.solegendary.reignofnether.blocks;


import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.resources.BlockUtils;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
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
import java.util.Set;

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
            RangeIndicatorAddon ria;
            if ((ria = building.getBuilding().getActiveAddon(RangeIndicatorAddon.class)) != null) {
                for (BlockPos bp : ria.getHighlightBps(building)) {
                    int snowLayers = BlockUtils.getSnowLayers(building.level.getBlockState(bp.above()));
                    float yOffset = snowLayers * 0.125f;
                    if (BuildingClientEvents.getSelectedBuildings().contains(building)) {
                        MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumer, Direction.UP, yOffset, bp, 0f, 0.8f, 0f, 0.3f);
                    } else if (!ria.showOnlyWhenSelected(building)) {
                        MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumer, Direction.UP, yOffset, bp, 0f, 0f, 0f, 0.6f);
                    }
                }
            }
        }

        if (nightSourceOriginToPlace != null && MC.level != null) {
            Set<BlockPos> bps =  MiscUtil.CircleUtil.getCircle(nightSourceOriginToPlace.getFirst(), nightSourceOriginToPlace.getSecond());
            for (BlockPos bp : bps) {
                int snowLayers = BlockUtils.getSnowLayers(MC.level.getBlockState(bp.above()));
                float yOffset = snowLayers * 0.125f;
                MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumer, Direction.UP, yOffset, bp, 0f, 0.8f, 0f, 0.3f);
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
    public static Pair<BlockPos, Integer> nightSourceOriginToPlace = null;
    public static final int VISIBLE_BORDER_ADJ = 2; // shrink a bit so borderlines themselves are safe to walk on

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        nightSourcesUpdateTicks -= 1;

        NightSourceAddon nsa0 = null;
        Building building = BuildingClientEvents.getBuildingToPlace();
        if (building != null) {
            nsa0 = building.getActiveAddon(NightSourceAddon.class);
        }
        if (nightSourcesUpdateTicks % 2 == 0) {
            if (nsa0 != null)
                nightSourceOriginToPlace = new Pair<>(CursorClientEvents.getPreselectedBlockPos(), nsa0.getDefaultNightRange() - VISIBLE_BORDER_ADJ);
            else
                nightSourceOriginToPlace = null;
        }

        if (nightSourcesUpdateTicks <= 0) {
            nightSourcesUpdateTicks = NIGHT_SOURCES_UPDATE_TICKS_MAX;

            nightSourceOrigins.clear();

            // get list of night source centre:range pairs
            for (BuildingPlacement bpl : BuildingClientEvents.getBuildings()) {
                NightSourceAddon nsa;
                if (!bpl.isExploredClientside || (nsa = bpl.getBuilding().getActiveAddon(NightSourceAddon.class)) == null || nsa.getNightRange(bpl) <= 0) {
                    continue;
                }
                nightSourceOrigins.add(new Pair<>(bpl.centrePos, nsa.getNightRange(bpl) - VISIBLE_BORDER_ADJ));
            }
        }
    }
}
