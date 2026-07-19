package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
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
    public static void clearPreselectedItems() {
        preselectedItems.clear();
    }
    public static ArrayList<ItemEntity> getPreselectedItems() {
        return preselectedItems;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS ||
                HudClientEvents.isMouseOverAnyButtonOrHud())
            return;
        if (MC.level != null && OrthoviewClientEvents.isEnabled()) {
            for (ItemEntity itemEntity : preselectedItems) {
                ResourceSource res = ResourceSources.getFromItem(itemEntity.getItem().getItem());
                boolean isResourceItem = res != null && res.resourceValue > 0;
                if (ItemUtil.isUnitItem(itemEntity) || isResourceItem) {
                    MyRenderer.drawBoxBottom(
                            evt.getPoseStack(),
                            itemEntity.getBoundingBox().inflate(0.25, 0, 0.25),
                            1, 1, 1,
                            CursorClientEvents.isRightClickDown() ? 1.0f : 0.25f
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        if (OrthoviewClientEvents.isEnabled()) {
            for (ItemEntity itemEntity : preselectedItems) {
                UnitItem unitItem = ItemUtil.getUnitItem(itemEntity.getItem().getItem());
                if (unitItem != null) {
                    MyRenderer.renderItemEntityTooltip(evt.getGuiGraphics(), unitItem, evt.getMouseX(), evt.getMouseY());
                    break;
                }
            }
        }
    }
}
