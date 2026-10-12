package com.solegendary.reignofnether.util;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 1.21.1 replaced Forge's {@code RenderGuiOverlayEvent} (which fired once per
 * {@code ElementType}) with {@code RenderGuiLayerEvent}, which fires once per named layer in
 * {@code Gui}'s {@code LayeredDraw}. The mod's overlay handlers all want to draw once, on top of
 * the whole vanilla HUD, which is what filtering on the last layer gives us.
 */
public class GuiLayerCompat {
	private GuiLayerCompat() {}

	/** Last layer {@code Gui} renders, so anything drawn in it sits on top of the vanilla HUD. */
	public static final ResourceLocation TOP_LAYER = VanillaGuiLayers.SAVING_INDICATOR;

    /** True when the event belongs to {@link #TOP_LAYER}. */
    public static boolean isTopLayer(RenderGuiLayerEvent event) {
        return TOP_LAYER.equals(event.getName());
    }
}
