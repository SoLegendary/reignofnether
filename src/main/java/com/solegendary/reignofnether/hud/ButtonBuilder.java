package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Builder class for constructing {@link Button} instances.
 *
 * <p>Required fields: {@code name}, {@code iconResource}, {@code isSelected},
 * {@code isHidden}, and {@code isEnabled}.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * Button button = new ButtonBuilder("My Button", myIconRl)
 *     .iconSize(16)
 *     .hotkey(myKeybinding)
 *     .isSelected(() -> false)
 *     .isHidden(() -> false)
 *     .isEnabled(() -> true)
 *     .onLeftClick(() -> System.out.println("clicked!"))
 *     .tooltipLines(myTooltipLines)
 *     .build();
 * }</pre>
 * </p>
 */
public class ButtonBuilder {

    // Required
    private final String name;
    private ResourceLocation iconResource;

    // Optional with defaults
    private int iconSize = Button.DEFAULT_ICON_SIZE;
    private ResourceLocation frameResource = null;
    private ResourceLocation bgIconResource = null;
    private Keybinding hotkey = null;
    private LivingEntity entity = null;
    private BuildingPlacement building = null;
    private Supplier<Boolean> isSelected = () -> false;
    private Supplier<Boolean> isHidden = () -> false;
    private Supplier<Boolean> isEnabled = () -> true;
    private Runnable onLeftClick = null;
    private Runnable onRightClick = null;
    private List<FormattedCharSequence> tooltipLines = null;
    private Supplier<Boolean> isFlashing = () -> false;
    private float greyPercent = 0.0f;
    private boolean stretchIconToBorders = false;
    private int tooltipOffsetY = 0;

    /**
     * Creates a new builder with the two required fields.
     *
     * @param name         the display name of the button
     */
    public ButtonBuilder(String name) {
        this.name = name;
    }

    public ButtonBuilder iconResource(ResourceLocation iconResource) {
        this.iconResource = iconResource;
        return this;
    }

    public ButtonBuilder iconSize(int iconSize) {
        this.iconSize = iconSize;
        return this;
    }

    public ButtonBuilder frameResource(ResourceLocation frameResource) {
        this.frameResource = frameResource;
        return this;
    }

    /** Sets a background icon rendered behind the main icon (e.g. for mounted unit passengers). */
    public ButtonBuilder bgIconResource(ResourceLocation bgIconResource) {
        this.bgIconResource = bgIconResource;
        return this;
    }

    public ButtonBuilder stretchIconToBorders() {
        this.stretchIconToBorders = true;
        return this;
    }

    public ButtonBuilder hotkey(@Nullable Keybinding hotkey) {
        this.hotkey = hotkey;
        return this;
    }

    /** Associates a {@link LivingEntity} with this button (used by unit selection buttons). */
    public ButtonBuilder entity(@Nullable LivingEntity entity) {
        this.entity = entity;
        return this;
    }

    /** Associates a {@link BuildingPlacement} with this button (used by building selection buttons). */
    public ButtonBuilder building(@Nullable BuildingPlacement building) {
        this.building = building;
        return this;
    }

    /** Controls whether the selected frame is rendered around this button. */
    public ButtonBuilder isSelected(Supplier<Boolean> isSelected) {
        this.isSelected = isSelected;
        return this;
    }

    /** controls whether the parent renderer will actually render this button */
    public ButtonBuilder isHidden(Supplier<Boolean> isHidden) {
        this.isHidden = isHidden;
        return this;
    }

    /** Controls whether the button is currently usable (e.g. off cooldown). */
    public ButtonBuilder isEnabled(Supplier<Boolean> isEnabled) {
        this.isEnabled = isEnabled;
        return this;
    }

    /** Causes the button to slowly oscillating flash  */
    public ButtonBuilder isFlashing(Supplier<Boolean> isFlashing) {
        this.isFlashing = isFlashing;
        return this;
    }

    public ButtonBuilder onLeftClick(@Nullable Runnable onLeftClick) {
        this.onLeftClick = onLeftClick;
        return this;
    }

    public ButtonBuilder onRightClick(@Nullable Runnable onRightClick) {
        this.onRightClick = onRightClick;
        return this;
    }

    /**
     * Sets the grey-out percentage used for cooldown indication or production progress.
     * @param greyPercent 0.0 = clear; 1.0 = fully greyed out (bottom-to-top)
     */
    public ButtonBuilder greyPercent(float greyPercent) {
        this.greyPercent = greyPercent;
        return this;
    }

    public ButtonBuilder tooltipLines(@Nullable List<FormattedCharSequence> tooltipLines) {
        this.tooltipLines = tooltipLines;
        return this;
    }

    /** Shifts the tooltip vertically by the given number of pixels. */
    public ButtonBuilder tooltipOffsetY(int tooltipOffsetY) {
        this.tooltipOffsetY = tooltipOffsetY;
        return this;
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    /**
     * Constructs and returns the configured {@link Button}.
     *
     * <p>The appropriate {@link Button} constructor is selected automatically:
     * <ul>
     *   <li>If an {@code entity} is set    → unit-selection constructor</li>
     *   <li>If a {@code building} is set   → building-selection constructor</li>
     *   <li>If a custom {@code frameResource} is set → custom-frame constructor</li>
     *   <li>Otherwise                      → standard action/ability constructor</li>
     * </ul>
     * </p>
     *
     * @throws IllegalStateException if both {@code entity} and {@code building} are set simultaneously
     */
    public Button build() {
        if (entity != null && building != null) {
            throw new IllegalStateException(
                    "ButtonBuilder: cannot set both 'entity' and 'building' on the same button.");
        }

        Button button;
        if (frameResource != null) {
            button = new Button(name, iconSize, iconResource, frameResource, hotkey,
                    isSelected, isHidden, isEnabled,
                    onLeftClick, onRightClick, tooltipLines);
        } else {
            button = new Button(name, iconSize, iconResource, hotkey,
                    isSelected, isHidden, isEnabled,
                    onLeftClick, onRightClick, tooltipLines);
        }
        button.entity = entity;
        button.building = building;
        button.bgIconResource = bgIconResource;
        button.isFlashing = isFlashing;
        button.greyPercent = greyPercent;
        button.stretchIconToBorders = stretchIconToBorders;
        button.tooltipOffsetY = tooltipOffsetY;

        return button;
    }
}