package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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
    public int iconSize;
    public int iconFrameSize;
    public int iconSelectedFrameSize;
    public static int DEFAULT_ICON_SIZE = 14;
    public static int DEFAULT_ICON_FRAME_SIZE = 22;
    public int tooltipOffsetY = 0;
    // size the HUD last drew this button at (for tutorial highlights)
    public int drawnSize = DEFAULT_ICON_FRAME_SIZE;
    public int imageSize = DEFAULT_ICON_SIZE;
    public static final int itemIconSize = DEFAULT_ICON_SIZE;
    public int innerIconSizeModifier = 0;
    public String playerNameForHeadIcon = "";
    public int bgColour = 0x64000000;
    public Supplier<String> bottomLeftText = null;
    public int bottomLeftTextColor = 0xFFFFFF;

    public ResourceLocation iconResource;
    public ResourceLocation bgIconResource = null; // for rendering a background icon (eg. for mounted unit passengers)
    public ResourceLocation frameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png");

    public ItemStack iconItem = null;
    public float iconItemScale = 1.0f;

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
    public Runnable onLeftClickRelease = null;
    public List<FormattedCharSequence> tooltipLines;
    public boolean lightUpOnHover = true;

    public Supplier<Boolean> isFlashing = () -> false;

    // what this button costs to use (production, buildings), shown on the command card
    @Nullable
    public Supplier<ResourceCost> cost = null;

    // used for cooldown indication, productionItem progress, etc.
    // @ 0.0, appears clear and normal
    // @ 0.5, bottom half is greyed out
    // @ 1.0, whole button is greyed out
    public float greyPercent = 0.0f;
    public long pressedMs = 0; // last click or hotkey, so the HUD cell can show it pressed in

    public Supplier<Float> getGreyPercent = null;
    public boolean greyWhenDisabled = true;
    public boolean greyInverted = false;
    public boolean showSelectedFrameWhenDisabled = false;

    protected Minecraft MC = Minecraft.getInstance();

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

    /** Updates state that changes every frame (cooldown fill, counts). Called before drawing. */
    public void refresh() { }

    /** The HUD's tooltip for this button: its legacy lines restyled, or null when it has none. */
    @Nullable
    public com.solegendary.reignofnether.ui.Tooltip hudTooltip() {
        return tooltipLines == null || tooltipLines.isEmpty() ? null : com.solegendary.reignofnether.ui.Tooltip.fromLines(tooltipLines);
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY);
    }

    /** What pressing this button's hotkey does: a full click (press then release). */
    public void pressHotkey() {
        if (this.onLeftClick != null)
            this.onLeftClick.run();
        if (this.onLeftClickRelease != null)
            this.onLeftClickRelease.run();
    }
}
