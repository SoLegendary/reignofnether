package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import com.solegendary.reignofnether.hud.effecticons.MobEffectIcon;

// "Buttons" that are just to show off mob effects
public class MobEffectIcon extends Button {

    public static final int ICON_SIZE = 8;
    public final Holder<MobEffect> effect;
    private final String descId;
    private final Long startTime = TimeClientEvents.getClientTime();
    public int duration = 0;
    private static final int MIN_DURATION_SHOWN = 40; // don't show really short effects as most of them are just auras

    public MobEffectIcon(Holder<MobEffect> effect, ResourceLocation iconRl, String descId) {
        super("Passive Icon", ICON_SIZE, iconRl, null, () -> false, () -> true, () -> true, null, null, List.of());
        this.frameResource = getFrameRl(effect);
        this.effect = effect;
        this.tooltipLines = List.of(
                fcs(I18n.get("effect.reignofnether." + descId), true),
                fcs(I18n.get("effect.reignofnether."  + descId + ".desc"))
        );
        this.descId = descId;
        this.greyInverted = true;
        this.innerIconSizeModifier = -1;
    }

    private ResourceLocation getFrameRl(Holder<MobEffect> effect) {
        return switch (effect.value().getCategory()) {
            case BENEFICIAL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_diamond.png");
            case HARMFUL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_red.png");
            case NEUTRAL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");
        };
    }

    public MobEffectIcon copyWithInstance(MobEffectInstance instance) {
        MobEffectIcon icon = new MobEffectIcon(
                this.effect,
                this.iconResource,
                this.descId
        );
        icon.duration = instance.getDuration();
        if (icon.duration >= MIN_DURATION_SHOWN) {
            icon.getGreyPercent = () -> {
                if (icon.duration <= 0)
                    return 0f;
                long elapsed = TimeClientEvents.getClientTime() - icon.startTime;
                float percent = (float) elapsed / (float) icon.duration;
                return Math.max(0f, Math.min(1f, percent));
            };
        }
        icon.bottomLeftText = () -> {
            int amp = instance.getAmplifier();
            return amp > 0 ? String.valueOf(amp + 1) : "";
        };
        return icon;
    }

    private int getSecondsRemaining() {
        long elapsed = TimeClientEvents.getClientTime() - startTime;
        long remainingTicks = Math.max(0, duration - elapsed);
        return (int) Math.ceil(remainingTicks / 20d);
    }

    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = I18n.get("effect.reignofnether." + descId);
        String desc = I18n.get("effect.reignofnether." + descId + ".desc");

        if (duration >= MIN_DURATION_SHOWN) {
            name += " " + I18n.get("effect.reignofnether.duration", getSecondsRemaining());
        }
        this.tooltipLines = List.of(
                fcs(name, true),
                fcs(desc)
        );
        MyRenderer.renderTooltip(guiGraphics, this.tooltipLines, mouseX, mouseY);
    }
}
