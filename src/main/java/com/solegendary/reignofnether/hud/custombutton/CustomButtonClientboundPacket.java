package com.solegendary.reignofnether.hud.custombutton;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CustomButtonClientboundPacket {
	
	public static final ResourceLocation ALWAYS_KEY =
		ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "always");
	
	public byte type; //0 clear 1 common button 2 entity button 3 building button
	public ResourceLocation id;
	public String name;
	public ResourceLocation iconResource;
	public int OffsetX;
	public int OffsetY;
	public int iconSize;
	public Map<ResourceLocation, List<ResourceLocation>> mappings;
	public boolean hasLeftClickAction;
	public boolean hasRightClickAction;
	public boolean lightUpOnHover;
	public boolean isEnabled;
	
	public CustomButtonClientboundPacket(
		byte type,
		ResourceLocation id,
		String name,
		ResourceLocation iconResource,
		int OffsetX,
		int OffsetY,
		int iconSize,
		Map<ResourceLocation, List<ResourceLocation>> mappings,
		boolean hasLeftClickAction,
		boolean hasRightClickAction,
		boolean lightUpOnHover,
		boolean isEnabled
	) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.iconResource = iconResource;
		this.OffsetX = OffsetX;
		this.OffsetY = OffsetY;
		this.iconSize = iconSize;
		this.mappings = mappings;
		this.hasLeftClickAction = hasLeftClickAction;
		this.hasRightClickAction = hasRightClickAction;
		this.lightUpOnHover = lightUpOnHover;
		this.isEnabled = isEnabled;
	}
	
	public static CustomButtonClientboundPacket decode(FriendlyByteBuf buf) {
		byte type = buf.readByte();
		return switch (type) {
			case 0 -> new CustomButtonClientboundPacket(type, null, null, null, 0, 0, 0, null, false, false, false, false);
			case 1 -> new CustomButtonClientboundPacket(
				type,
				buf.readResourceLocation(),
				buf.readUtf(),
				buf.readResourceLocation(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt(),
				null,
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readBoolean(),
				buf.readBoolean()
			);
			case 2, 3, 4 -> new CustomButtonClientboundPacket(
				type,
				null,
				null,
				null,
				0,
				0,
				0,
				buf.readMap(
					FriendlyByteBuf::readResourceLocation,
					b -> b.readList(FriendlyByteBuf::readResourceLocation)
				),
				false,
				false,
				false,
				false
			);
			default -> null;
		};
	}
	
	public void encode(FriendlyByteBuf buf) {
		buf.writeByte(this.type);
		switch (this.type) {
			case 0 -> {
			}
			case 1 -> {
				buf.writeResourceLocation(this.id);
				buf.writeUtf(this.name);
				buf.writeResourceLocation(this.iconResource);
				buf.writeInt(this.OffsetX);
				buf.writeInt(this.OffsetY);
				buf.writeInt(this.iconSize);
				buf.writeBoolean(this.hasLeftClickAction);
				buf.writeBoolean(this.hasRightClickAction);
				buf.writeBoolean(this.lightUpOnHover);
				buf.writeBoolean(this.isEnabled);
			}
			case 2, 3, 4 -> buf.writeMap(
				this.mappings,
				FriendlyByteBuf::writeResourceLocation,
				(b, list) -> b.writeCollection(list, FriendlyByteBuf::writeResourceLocation)
			);
		}
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handlePacket));
		ctx.get().setPacketHandled(true);
	}
	
	public void handlePacket() {
		switch (this.type) {
			case 0 -> CustomButtonClientEvents.clear();
			case 1 -> CustomButtonClientEvents.registerButtons(
				this.id,
				new CustomButton(
					this.id,
					this.name,
					this.iconResource,
					this.OffsetX,
					this.OffsetY,
					this.hasLeftClickAction ? List.of(new CustomButtonActions.ExperienceAction(1, 2)) : List.of(),
					this.hasRightClickAction ? List.of(new CustomButtonActions.ExperienceAction(1, 2)) : List.of(),
					this.lightUpOnHover,
					this.isEnabled
				)
			);
			case 2 -> CustomButtonClientEvents.registerEntityMappings(this.mappings);
			case 3 -> CustomButtonClientEvents.registerBuildingMappings(this.mappings);
			case 4 -> CustomButtonClientEvents.registerAlwaysRenderButtons(this.mappings.get(ALWAYS_KEY));
		}
	}
}
	
