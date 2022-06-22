package com.solegendary.reignofnether.fogofwar;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FogOfWarClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post evt) {
        LivingEntity selEntity = HudClientEvents.hudSelectedEntity;

        // get world position of corners of the screen
        Vector3d a = MiscUtil.screenPosToWorldPos(MC, 0,0);
        Vector3d b = MiscUtil.screenPosToWorldPos(MC, 0, MC.getWindow().getGuiScaledHeight());
        Vector3d c = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight());
        Vector3d d = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), 0);

        double maxX = Collections.max(Arrays.asList(a.x, b.x, c.x, d.x));
        double minX = Collections.min(Arrays.asList(a.x, b.x, c.x, d.x));
        double maxZ = Collections.max(Arrays.asList(a.z, b.z, c.z, d.z));
        double minZ = Collections.min(Arrays.asList(a.z, b.z, c.z, d.z));

        for (int z = (int) minZ; z < maxZ; z++)
        {
            for (int x = (int) minX; x < maxX; x++) {

            }
        }
    }
}
