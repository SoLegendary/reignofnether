package com.solegendary.reignofnether.hud.custombutton;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class CustomButton extends Button {
	
	public static final Codec<CustomButton> CODEC = ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("name").forGetter(CustomButton::name),
		ResourceLocation.CODEC.fieldOf("icon").forGetter(CustomButton::icon),
		Codec.INT.fieldOf("x").forGetter(CustomButton::offsetX),
		Codec.INT.fieldOf("y").forGetter(CustomButton::offsetY),
		CustomButtonActions.getCodec().listOf().optionalFieldOf("left_click_actions", List.of()).forGetter(CustomButton::left_click_actions),
		CustomButtonActions.getCodec().listOf().optionalFieldOf("right_click_actions", List.of()).forGetter(CustomButton::right_click_actions),
		Codec.INT.optionalFieldOf("icon_size", Button.DEFAULT_ICON_SIZE).forGetter(CustomButton::iconSize),
		Codec.BOOL.optionalFieldOf("light_up_on_hover", true).forGetter(CustomButton::lightUpOnHover),
		Codec.BOOL.optionalFieldOf("enable", true).forGetter(CustomButton::isEnabled)
	).apply(instance, CustomButton::new)));
	
	public ResourceLocation id;
	public int OffsetX;
	public int OffsetY;
	public int iconSize;
	public List<CustomButtonActions.CustomButtonAction> leftClickActions = new ArrayList<>();
	public List<CustomButtonActions.CustomButtonAction> rightClickActions = new ArrayList<>();
	public boolean isEnabled;
	Minecraft MC = Minecraft.getInstance();
	public CustomButton(
		String name,
		int iconSize,
		ResourceLocation iconResource,
		@Nullable Keybinding hotkey,
		@Nullable LivingEntity entity,
		@Nullable BuildingPlacement building,
		Supplier<Boolean> isSelected,
		Supplier<Boolean> isHidden,
		Supplier<Boolean> isEnabled,
		@Nullable Runnable onLeftClick,
		@Nullable Runnable onRightClick,
		@Nullable List<FormattedCharSequence> tooltipLines,
		@Nullable ResourceLocation bgIconResource,
		@Nullable ResourceLocation frameResource,
		Supplier<Boolean> isFlashing,
		float greyPercent,
		boolean stretchIconToBorders,
		int tooltipOffsetY,
		boolean lightUpOnHover,
		int OffsetX,
		int OffsetY) {
		super(name, iconSize, iconResource, frameResource != null ? frameResource :
				ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/hud/icon_frame.png"),
			hotkey, isSelected, isHidden, isEnabled, onLeftClick, onRightClick, tooltipLines);
		this.OffsetX = OffsetX;
		this.OffsetY = OffsetY;
		this.entity = entity;
		this.building = building;
		this.iconSize = iconSize;
		this.bgIconResource = bgIconResource;
		this.isFlashing = isFlashing != null ? isFlashing : () -> false;
		this.greyPercent = greyPercent;
		this.stretchIconToBorders = stretchIconToBorders;
		this.tooltipOffsetY = tooltipOffsetY;
		this.imageSize = iconSize;
		this.lightUpOnHover = lightUpOnHover;
	}
	
	
	public CustomButton(String name, ResourceLocation icon, int offsetX, int offsetY, List<CustomButtonActions.CustomButtonAction> leftClickActions, List<CustomButtonActions.CustomButtonAction> rightClickActions, int iconSize, boolean lightUpOnHover, boolean isEnabled) {
		super(
			name,
			Button.DEFAULT_ICON_SIZE,
			icon,
			ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/hud/icon_frame.png"),
			null,
			() -> false,
			() -> false,
			() -> isEnabled,
			() -> {
			},
			() -> {
			},
			null
		);
		this.OffsetX = offsetX;
		this.OffsetY = offsetY;
		this.iconSize = iconSize;
		this.entity = null;
		this.building = null;
		this.bgIconResource = null;
		this.isFlashing = () -> false;
		this.greyPercent = 0.0f;
		this.stretchIconToBorders = false;
		this.tooltipOffsetY = 0;
		this.imageSize = iconSize;
		this.lightUpOnHover = lightUpOnHover;
		this.leftClickActions = leftClickActions;
		this.rightClickActions = rightClickActions;
		this.isEnabled = isEnabled;
	}
	
	public CustomButton(ResourceLocation id, String name, ResourceLocation icon, int offsetX, int offsetY, List<CustomButtonActions.CustomButtonAction> leftClickActions, List<CustomButtonActions.CustomButtonAction> rightClickActions, boolean lightUpOnHover, boolean isEnabled) {
		super(
			name,
			Button.DEFAULT_ICON_SIZE,
			icon,
			ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/hud/icon_frame.png"),
			null,
			() -> false,
			() -> false,
			() -> isEnabled,
			() -> {
			},
			() -> {
			},
			null
		);
		this.id = id;
		this.OffsetX = offsetX;
		this.OffsetY = offsetY;
		this.iconSize = Button.DEFAULT_ICON_SIZE;
		this.entity = null;
		this.building = null;
		this.bgIconResource = null;
		this.isFlashing = () -> false;
		this.greyPercent = 0.0f;
		this.stretchIconToBorders = false;
		this.tooltipOffsetY = 0;
		this.imageSize = Button.DEFAULT_ICON_SIZE;
		this.lightUpOnHover = lightUpOnHover;
		this.leftClickActions = leftClickActions;
		this.rightClickActions = rightClickActions;
		this.isEnabled = isEnabled;
	}
	
	private Boolean isEnabled() {
		return isEnabled;
	}
	
	private Boolean lightUpOnHover() {
		return lightUpOnHover;
	}
	
	public String name() {
		return this.name;
	}
	
	public ResourceLocation icon() {
		return this.iconResource;
	}
	
	public int offsetX() {
		return OffsetX;
	}
	
	public int offsetY() {
		return OffsetY;
	}
	
	public List<CustomButtonActions.CustomButtonAction> left_click_actions() {
		return leftClickActions;
	}
	
	public List<CustomButtonActions.CustomButtonAction> right_click_actions() {
		return rightClickActions;
	}
	
	public int iconSize() {
		return iconSize;
	}
	
	@Override
	public void checkClicked(int mouseX, int mouseY, boolean leftClick) {
		if (!OrthoviewClientEvents.isEnabled() || !isEnabled)
			return;
		
		if (isMouseOver(mouseX, mouseY) && MC.player != null) {
			if (leftClick && this.onLeftClick != null && !this.leftClickActions.isEmpty()) {
				MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
				CustomButtonActionServerboundPacket.runLeftClickCommand(id);
			} else if (!leftClick && this.onRightClick != null && !rightClickActions.isEmpty()) {
				MC.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.2f, 1.0f);
				CustomButtonActionServerboundPacket.runRightClickCommand(id);
			}
		}
	}
}