package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.time.TimeClientEvents;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// "Buttons" that are just to show off mob effects
public class MobEffectIcon extends Button {

    public static final int ICON_SIZE = 8;
    public final MobEffect effect;
    private final String descId;
    private final Long startTime = TimeClientEvents.getClientTime();
    public int duration = 0;

    public MobEffectIcon(MobEffect effect, ResourceLocation iconRl, String descId) {
        super("Passive Icon", ICON_SIZE, iconRl, null, () -> false, () -> true, () -> true, null, null, List.of());
        this.frameResource = getFrameRl(effect);
        this.effect = effect;
        this.tooltipLines = List.of(
                fcs(I18n.get("effect.reignofnether." + descId), true),
                fcs(I18n.get("effect.reignofnether."  + descId + ".desc"))
        );
        this.descId = descId;
        if (duration > 0) {
            this.getGreyPercent = () -> {
                if (this.duration <= 0)
                    return 0f;
                long elapsed = TimeClientEvents.getClientTime() - startTime;
                float percent = (float) elapsed / (float) this.duration;
                return Math.max(0f, Math.min(1f, percent));
            };
        }
    }

    private ResourceLocation getFrameRl(MobEffect effect) {
        return switch (effect.getCategory()) {
            case BENEFICIAL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_diamond.png");
            case HARMFUL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_red.png");
            case NEUTRAL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");
        };
    }

    public MobEffectIcon copy(MobEffectInstance instance) {
        MobEffectIcon icon = new MobEffectIcon(
                this.effect,
                this.iconResource,
                this.descId
        );
        icon.duration = instance.getDuration();
        icon.bottomLeftText = () -> {
            int amp = instance.getAmplifier();
            return amp > 0 ? String.valueOf(amp + 1) : "";
        };
        return icon;
    }
}
