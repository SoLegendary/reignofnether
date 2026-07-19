package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ItemClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // items moused over
    private static final ArrayList<ItemEntity> preselectedItems = new ArrayList<>();

    public static void addPreselectedItem(ItemEntity itemEntity) {
        if (!FogOfWarClientEvents.isInBrightChunk(itemEntity))
            return;
        preselectedItems.add(itemEntity);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS ||
                HudClientEvents.isMouseOverAnyButtonOrHud())
            return;
        if (MC.level != null && OrthoviewClientEvents.isEnabled()) {
            for (ItemEntity itemEntity : preselectedItems) {
                MyRenderer.drawBoxBottom(evt.getPoseStack(), itemEntity.getBoundingBox(), 1, 1, 1, 1.0f);
            }
        }
    }
}
