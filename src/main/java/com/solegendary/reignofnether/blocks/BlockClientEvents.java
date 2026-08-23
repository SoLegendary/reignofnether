package com.solegendary.reignofnether.blocks;


import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;
import com.solegendary.reignofnether.building.addon.RangeIndicatorAddon;
import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchSculkAmplifiers;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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

        if (tempMouseCircleOrigin != null && MC.level != null) {
            Set<BlockPos> bps = MiscUtil.CircleUtil.getCircle(tempMouseCircleOrigin.getFirst(), tempMouseCircleOrigin.getSecond());
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
        if (MC.player == null || MC.level == null) return;
        ItemStack heldItem = MC.player.getMainHandItem();
        if (!heldItem.is(BlockRegistrar.SPIDER_FRIENDLY_BARRIER.get().asItem()))
            return;

        BlockPos playerPos = MC.player.blockPosition();
        BlockPos min = playerPos.offset(-10, -10, -10);
        BlockPos max = playerPos.offset(10, 10, 10);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (MC.level.getBlockState(pos).getBlock() instanceof SpiderFriendlyBarrierBlock) {
                MyRenderer.drawBlockOutline(evt.getPoseStack(), pos, 0.6f);
            }
        }
    }

    // maintain a mapping of night sources for easy culling calcs
    private static final int NIGHT_SOURCES_UPDATE_TICKS_MAX = 50;
    private static int nightSourcesUpdateTicks = NIGHT_SOURCES_UPDATE_TICKS_MAX;
    public static ArrayList<Pair<BlockPos, Integer>> nightSourceOrigins = new ArrayList<>();
    // temp circle for stuff like placing a night source building, mousing over a catalyst with sonic boom, etc.
    public static Pair<BlockPos, Integer> tempMouseCircleOrigin = null;
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
            if (nsa0 != null) {
                tempMouseCircleOrigin = new Pair<>(CursorClientEvents.getPreselectedBlockPos(), nsa0.getDefaultNightRange() - VISIBLE_BORDER_ADJ);
            } else if (BuildingClientEvents.getPreselectedBuilding() instanceof SculkCatalystPlacement scp &&
                        CursorClientEvents.getLeftClickAction() == UnitAction.CAST_SONIC_BOOM &&
                        ResearchClient.hasResearch(ProductionItems.RESEARCH_SCULK_AMPLIFIERS) &&
                        MC.player != null && scp.ownerName.equals(MC.player.getName().getString())) {
                tempMouseCircleOrigin = new Pair<>(scp.centrePos, ResearchSculkAmplifiers.SPLIT_BOOM_RANGE);
            } else {
                tempMouseCircleOrigin = null;
            }
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
