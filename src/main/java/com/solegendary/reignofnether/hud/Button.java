package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Class for creating buttons that consist of an icon inside of a frame which is selectable
 * All functionality that occurs on click/hover/etc. is enforced by HudClientEvents
 */

public class Button {

    public String name;
    public int x, y;
    int iconSize, iconFrameSize, iconSelectedFrameSize;
    public static int DEFAULT_ICON_SIZE = 14;
    public static int DEFAULT_ICON_FRAME_SIZE = 22;
    public static int DEFAULT_ICON_SELECTED_FRAME_SIZE = 24;
    public int tooltipOffsetY = 0;
    public static final int itemIconSize = DEFAULT_ICON_SIZE;

    public ResourceLocation iconResource;
    public ResourceLocation bgIconResource = null; // for rendering a background icon (eg. for mounted unit passengers)
    public ResourceLocation frameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");

    public Keybinding hotkey = null; // for action/ability buttons
    public LivingEntity entity = null; // for selected unit buttons
    public BuildingPlacement building = null; // for selected building buttons

    public Supplier<Boolean> isSelected, isHidden, isEnabled; // is the button allowed to be used right now? (eg. off cooldown)
    public Runnable onLeftClick, onRightClick;
    public List<FormattedCharSequence> tooltipLines;

    public Supplier<Boolean> isFlashing = () -> false;

    // used for cooldown indication, productionItem progress, etc.
    // @ 0.0, appears clear and normal
    // @ 0.5, bottom half is greyed out
    // @ 1.0, whole button is greyed out
    public float greyPercent = 0.0f;

    Minecraft MC = Minecraft.getInstance();

    public Button(String name, int iconSize, ResourceLocation iconRl, @Nullable Keybinding hotkey, Supplier<Boolean> isSelected,
                  Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, @Nullable Runnable onLeftClick,
                  @Nullable Runnable onRightClick, @Nullable List<FormattedCharSequence> tooltipLines) {
        this(name, iconSize, iconRl, null, hotkey, null, null, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);
    }

    public Button(String name, int iconSize, ResourceLocation iconRl, ResourceLocation frameRl, @Nullable Keybinding hotkey, Supplier<Boolean> isSelected,
                  Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, @Nullable Runnable onLeftClick,
                  @Nullable Runnable onRightClick, @Nullable List<FormattedCharSequence> tooltipLines) {
        this(name, iconSize, iconRl, frameRl, hotkey, null, null, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);
    }

    public Button(String name, int iconSize, ResourceLocation iconRl, LivingEntity entity, Supplier<Boolean> isSelected,
                  Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, @Nullable Runnable onLeftClick,
                  @Nullable Runnable onRightClick, @Nullable List<FormattedCharSequence> tooltipLines) {
        this(name, iconSize, iconRl, null, null, entity, null, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);
    }

    public Button(String name, int iconSize, ResourceLocation iconRl, BuildingPlacement building, Supplier<Boolean> isSelected,
                  Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, @Nullable Runnable onLeftClick,
                  @Nullable Runnable onRightClick, @Nullable List<FormattedCharSequence> tooltipLines) {
        this(name, iconSize, iconRl, null, null, null, building, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);
    }

    private Button(String name, int iconSize, ResourceLocation iconRl, ResourceLocation frameRl, Keybinding hotkey, LivingEntity entity, BuildingPlacement building, Supplier<Boolean> isSelected, Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, Runnable onLeftClick, Runnable onRightClick, List<FormattedCharSequence> tooltipLines) {
        this.name = name;
        this.iconSize = iconSize;
        this.iconResource = iconRl;
        if (frameRl != null) this.frameResource = frameRl;
        this.hotkey = hotkey;
        this.entity = entity;
        this.building = building;
        this.isSelected = isSelected;
        this.isHidden = isHidden;
        this.isEnabled = isEnabled;
        this.onLeftClick = onLeftClick;
        this.onRightClick = onRightClick;
        this.tooltipLines = tooltipLines;
        this.iconFrameSize = iconSize + 8;
        this.iconSelectedFrameSize = iconSize + 10;
    }

    public void renderHealthBar(PoseStack poseStack) {
        float cx = x + (float) iconFrameSize / 2, cy = y - 5;
        if (entity != null) HealthBarClientEvents.renderForEntity(poseStack, entity, cx, cy, iconFrameSize - 1, HealthBarClientEvents.RenderMode.GUI_ICON);
        else if (building != null) HealthBarClientEvents.renderForBuilding(poseStack, building, cx, cy, iconFrameSize - 1, HealthBarClientEvents.RenderMode.GUI_ICON);
    }

    protected void renderHotkey(GuiGraphics guiGraphics, int x, int y) {
        if (this.hotkey == null) return;
        String str = hotkey.buttonLabel;
        str = str.substring(0, Math.min(3, str.length()));
        guiGraphics.pose().translate(0, 0, 1);
        guiGraphics.drawCenteredString(MC.font, str, x + iconSize + 8 - (str.length() * 4), y + iconSize - 1, 0xFFFFFF);
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        int diff = (DEFAULT_ICON_SIZE - iconSize) / 2;

        if (frameResource != null) {
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIconFrameWithBg(guiGraphics, frameResource, x + diff, y + diff, iconFrameSize, 0x64000000);
        }

        if (bgIconResource != null) {
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIcon(guiGraphics, bgIconResource, x + (frameResource != null ? 4 : 0) + (7 - iconSize / 2), y + (frameResource != null ? 4 : 0) + (7 - iconSize / 2), iconSize);
        }
        if (iconResource != null) {
            guiGraphics.pose().translate(0, 0, 1);
            MyRenderer.renderIcon(guiGraphics, iconResource, x + 4 + (7 - diff - iconSize / 2), y + 4 + (7 - diff - iconSize / 2), DEFAULT_ICON_SIZE);
        }

        renderHotkey(guiGraphics, x, y);

        boolean hover = isMouseOver(mouseX, mouseY);
        if (isEnabled.get() && (isSelected.get() || (hotkey != null && hotkey.isDown()) || (hover && ((MiscUtil.isLeftClickDown(MC) && onLeftClick != null) || (MiscUtil.isRightClickDown(MC) && onRightClick != null))))) {
            if (frameResource != null) {
                guiGraphics.pose().translate(0, 0, 1);
                MyRenderer.renderIcon(guiGraphics, ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_selected.png"), x - 1 + diff, y - 1 + diff, iconSelectedFrameSize);
            }
        }

        if (isEnabled.get() && hover) {
            guiGraphics.pose().translate(0, 0, 1);
            guiGraphics.fill(x + diff, y + diff, x + diff + iconFrameSize, y + diff + iconFrameSize, 0x32FFFFFF);
        }

        float gp = isEnabled.get() ? greyPercent : 1f;
        if (gp > 0) {
            int h = Math.round(Math.max(0f, Math.min(1f, gp)) * iconFrameSize);
            if (h > 0) {
                guiGraphics.pose().translate(0, 0, 1);
                guiGraphics.fill(x + diff, y + diff + (iconFrameSize - h), x + diff + iconFrameSize, y + diff + iconFrameSize, 0x99000000);
            }
        }

        if (isFlashing.get()) {
            guiGraphics.fill(x, y, x + iconFrameSize, y + iconFrameSize, (0xFFFFFF | ((int) (0x80 * MiscUtil.getOscillatingFloat(0, 1)) << 24)));
        }
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int diff = (DEFAULT_ICON_SIZE - iconSize) / 2;
        return mouseX >= x + diff && mouseY >= y + diff && mouseX < x + diff + iconFrameSize && mouseY < y + diff + iconFrameSize;
    }

    public void checkClicked(int mouseX, int mouseY, boolean leftClick) {
        if (!OrthoviewClientEvents.isEnabled() || !isEnabled.get() || !isMouseOver(mouseX, mouseY) || MC.player == null) return;
        if (leftClick && onLeftClick != null) {
            MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
            onLeftClick.run();
        } else if (!leftClick && onRightClick != null) {
            MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
            onRightClick.run();
        }
    }

    public void checkPressed(int key) {
        if (!OrthoviewClientEvents.isEnabled() || !isEnabled.get() || hotkey == null || hotkey.key != key || MC.player == null) return;
        MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
        onLeftClick.run();
    }
}