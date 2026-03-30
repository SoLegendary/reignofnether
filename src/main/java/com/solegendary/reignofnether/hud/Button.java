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
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Class for creating buttons that consist of an icon inside of a frame which is selectable
 * All functionality that occurs on click/hover/etc. is enforced by HudClientEvents
 */

public class Button {

    public String name;
    public int x; // top left
    public int y;
    int iconSize;
    int iconFrameSize;
    int iconSelectedFrameSize;
    public static int DEFAULT_ICON_SIZE = 14;
    public static int DEFAULT_ICON_FRAME_SIZE = 22;
    public int tooltipOffsetY = 0;
    public static final int itemIconSize = DEFAULT_ICON_SIZE;
    public boolean stretchIconToBorders = false;

    public ResourceLocation iconResource;
    public ResourceLocation bgIconResource = null; // for rendering a background icon (eg. for mounted unit passengers)
    public ResourceLocation frameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");

    public ItemStack iconItem = null;

    public Keybinding hotkey = null; // for action/ability buttons
    public LivingEntity entity = null; // for selected unit buttons
    public BuildingPlacement building = null; // for selected building buttons

    /** https://stackoverflow.com/questions/29945627/java-8-lambda-void-argument
     * Supplier       ()    -> x
     * Consumer       x     -> ()
     * Runnable       ()    -> ()
     * Predicate      x     -> boolean
     */
    public Supplier<Boolean> isSelected; // controls selected frame rendering
    public Supplier<Boolean> isHidden; // special highlighting for an on-state (eg. auto-cast/auto-producing)
    public Supplier<Boolean> isEnabled; // is the button allowed to be used right now? (eg. off cooldown)
    public Runnable onLeftClick;
    public Runnable onRightClick;
    public List<FormattedCharSequence> tooltipLines;

    public Supplier<Boolean> isFlashing = () -> false;

    // used for cooldown indication, productionItem progress, etc.
    // @ 0.0, appears clear and normal
    // @ 0.5, bottom half is greyed out
    // @ 1.0, whole button is greyed out
    public float greyPercent = 0.0f;

    Minecraft MC = Minecraft.getInstance();

    // constructor for ability/action/production buttons
    public Button(String name, int iconSize, ResourceLocation iconRl, @Nullable Keybinding hotkey, Supplier<Boolean> isSelected,
                  Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, @Nullable Runnable onLeftClick,
                  @Nullable Runnable onRightClick, @Nullable List<FormattedCharSequence> tooltipLines) {
        this.name = name;
        this.iconResource = iconRl;
        this.iconSize = iconSize;
        this.iconFrameSize = iconSize + 8;
        this.iconSelectedFrameSize = iconSize + 10;
        this.hotkey = hotkey;
        this.isSelected = isSelected;
        this.isHidden = isHidden;
        this.isEnabled = isEnabled;
        this.onLeftClick = onLeftClick;
        this.onRightClick = onRightClick;
        this.tooltipLines = tooltipLines;
    }

    // constructor for ability/action/production buttons with non-default frame
    public Button(String name, int iconSize, ResourceLocation iconRl, ResourceLocation frameRl, @Nullable Keybinding hotkey, Supplier<Boolean> isSelected,
                  Supplier<Boolean> isHidden, Supplier<Boolean> isEnabled, @Nullable Runnable onLeftClick,
                  @Nullable Runnable onRightClick, @Nullable List<FormattedCharSequence> tooltipLines) {
        this.name = name;
        this.iconResource = iconRl;
        this.frameResource = frameRl;
        this.iconSize = iconSize;
        this.iconFrameSize = iconSize + 8;
        this.iconSelectedFrameSize = iconSize + 10;
        this.hotkey = hotkey;
        this.isSelected = isSelected;
        this.isHidden = isHidden;
        this.isEnabled = isEnabled;
        this.onLeftClick = onLeftClick;
        this.onRightClick = onRightClick;
        this.tooltipLines = tooltipLines;
    }

    public void renderHealthBar(PoseStack poseStack) {
        if (entity != null)
            HealthBarClientEvents.renderForEntity(poseStack, entity,
                    x + ((float) iconFrameSize / 2), y - 5,
                    iconFrameSize - 1,
                    HealthBarClientEvents.RenderMode.GUI_ICON);
        else if (building != null)
            HealthBarClientEvents.renderForBuilding(poseStack, building,
                    x + ((float) iconFrameSize / 2), y - 5,
                    iconFrameSize - 1,
                    HealthBarClientEvents.RenderMode.GUI_ICON);
    }

    protected void renderHotkey(GuiGraphics guiGraphics, int x, int y) {
        // hotkey letter
        if (this.hotkey != null) {
            String hotkeyStr = hotkey.buttonLabel;
            hotkeyStr = hotkeyStr.substring(0,Math.min(3, hotkeyStr.length()));
            guiGraphics.pose().translate(0,0,1);
            guiGraphics.drawCenteredString(MC.font,
                    hotkeyStr,
                    x + iconSize + 8 - (hotkeyStr.length() * 4),
                    y + iconSize - 1,
                    0xFFFFFF);
        }
    }

    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;

        int xyDiff = (DEFAULT_ICON_SIZE - iconSize) / 2;

        if (this.frameResource != null) {
            guiGraphics.pose().translate(0,0,1);
            MyRenderer.renderIconFrameWithBg(guiGraphics, this.frameResource, x + xyDiff, y + xyDiff, iconFrameSize, 0x64000000);
        }

        if (bgIconResource != null) {
            guiGraphics.pose().translate(0,0,1);
            int iconX = frameResource != null ? x+4 + (7 - iconSize/2) : x + (7 - iconSize/2);
            int iconY = frameResource != null ? y+4 + (7 - iconSize/2) : y + (7 - iconSize/2);
            if (stretchIconToBorders) {
                iconX -= 1;
                iconY -= 1;
            }
            MyRenderer.renderIcon(
                    guiGraphics,
                    bgIconResource,
                    iconX,
                    iconY,
                    stretchIconToBorders ? iconSize + 2 : iconSize
            );
        }
        // item/unit icon
        if (iconResource != null) {
            int iconX = x+4 + (7 - xyDiff - iconSize/2);
            int iconY = y+4 + (7 - xyDiff - iconSize/2);
            if (stretchIconToBorders) {
                iconX -= 1;
                iconY -= 1;
            }
            guiGraphics.pose().translate(0,0,1);
            MyRenderer.renderIcon(
                    guiGraphics,
                    iconResource,
                    iconX, iconY,
                    stretchIconToBorders ? DEFAULT_ICON_SIZE + 2 : DEFAULT_ICON_SIZE
            );
        }
        if (iconItem != null) {
            guiGraphics.pose().translate(0,0,1);
            MyRenderer.renderItem(guiGraphics, iconItem, x+4 + (7 - xyDiff - iconSize/2), y+4 + (7 - xyDiff - iconSize/2), 0.75f);
        }

        renderHotkey(guiGraphics, x, y);

        // user is holding click or hotkey down over the button and render frame if so
        if (isEnabled.get() &&
            (isSelected.get() || (hotkey != null && hotkey.isDown()) || (isMouseOver(mouseX, mouseY) &&
                    ((MiscUtil.isLeftClickDown(MC) && onLeftClick != null) ||
                    (MiscUtil.isRightClickDown(MC) && onRightClick != null))
            ))) {

            if (frameResource != null) {
                ResourceLocation iconFrameSelectedResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_selected.png");
                guiGraphics.pose().translate(0,0,1);
                MyRenderer.renderIcon(
                        guiGraphics,
                        iconFrameSelectedResource,
                        x-1 + xyDiff,y-1 + xyDiff,
                        iconSelectedFrameSize
                );
            }
        }
        // light up on hover
        if (isEnabled.get() && isMouseOver(mouseX, mouseY)) {
            guiGraphics.pose().translate(0,0,1);
            guiGraphics.fill( // x1,y1, x2,y2,
                    x + xyDiff, y + xyDiff,
                    x + xyDiff + iconFrameSize,
                    y + xyDiff + iconFrameSize,
                    0x32FFFFFF); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }

        if (greyPercent > 0 || !isEnabled.get()) {
            int greyHeightPx = Math.round(greyPercent * iconFrameSize);
            if (!isEnabled.get())
                greyHeightPx = 0;

            guiGraphics.pose().translate(0,0,1);
            guiGraphics.fill( // x1,y1, x2,y2,
                    x + xyDiff,
                    y + xyDiff + greyHeightPx,
                    x + xyDiff + iconFrameSize,
                    y + xyDiff + iconFrameSize,
                    0x99000000); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }

        if (isFlashing.get()) {
            guiGraphics.fill(x, y,
                x + iconFrameSize,
                y + iconFrameSize,
                (0xFFFFFF | ((int) (0x80 * MiscUtil.getOscillatingFloat(0,1)) << 24))
            ); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255
        }
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int xyDiff = (DEFAULT_ICON_SIZE - iconSize) / 2;
        return (mouseX >= x + xyDiff &&
                mouseY >= y + xyDiff &&
                mouseX < x + xyDiff + iconFrameSize &&
                mouseY < y + xyDiff + iconFrameSize);
    }

    // must be done from mouse press event
    public void checkClicked(int mouseX, int mouseY, boolean leftClick) {
        if (!OrthoviewClientEvents.isEnabled() || !isEnabled.get())
            return;

        if (isMouseOver(mouseX, mouseY) && MC.player != null) {
            if (leftClick && this.onLeftClick != null) {
                MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
                this.onLeftClick.run();
            }
            else if (!leftClick && this.onRightClick != null) {
                MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
                this.onRightClick.run();
            }
        }
    }

    // must be done from key press event
    public void checkPressed(int key) {
        if (!OrthoviewClientEvents.isEnabled() || !isEnabled.get())
            return;

        if (hotkey != null && hotkey.key == key) {
            if (MC.player != null)
                MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
            this.onLeftClick.run();
        }
    }
}
