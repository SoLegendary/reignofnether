package com.solegendary.reignofnether.fogofwar;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FogOfWarClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onBlockActivated(BlockEvent.EntityPlaceEvent evt) {
        //System.out.println(evt.getPos());

        //BlockState block = MC.level.getBlockState(evt.getPos());
    }
}
