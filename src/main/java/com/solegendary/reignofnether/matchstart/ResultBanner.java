package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.ui.Text;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** VICTORY / DEFEAT across the screen when your match ends, styled like the launcher's post-match title. */
public final class ResultBanner {

    private ResultBanner() { }

    private static final long SHOW_MS = 5000;
    // the launcher's gradients, top to bottom, and drop shadows
    private static final int[] WIN = { 0xFFFEF481, 0xFFFBEB3C, 0xFFF19A0F, 0xFFDD6003 };
    private static final int[] LOSS = { 0xFFE7E9F5, 0xFFB7BCD6, 0xFF8A5A6A, 0xFF6A2A2A };

    private static long shownAt = -SHOW_MS;
    private static boolean win;

    public static void show(boolean victory) {
        win = victory;
        shownAt = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post evt) {
        if (System.currentTimeMillis() - shownAt > SHOW_MS)
            return;
        GuiGraphics g = evt.getGuiGraphics();
        var scale = HudGeometry.scale();
        String s = I18n.get(win ? "titles.reignofnether.victory" : "titles.reignofnether.defeat");
        float size = Text.px(88);
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        int w = Text.titleWidth(s, size);
        Text.title(g, s, (scale.width() - w) / 2, scale.height() / 4, size, win ? WIN : LOSS, win ? 0xFF380000 : 0xFF000000);
        g.pose().popPose();
    }
}
