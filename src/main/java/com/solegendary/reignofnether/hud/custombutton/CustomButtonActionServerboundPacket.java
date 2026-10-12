package com.solegendary.reignofnether.hud.custombutton;

import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class CustomButtonActionServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<CustomButtonActionServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "custom_button_action_serverbound"));

    @Override
    public CustomPacketPayload.Type<CustomButtonActionServerboundPacket> type() {
        return TYPE;
    }
	
	public ResourceLocation button;
	public boolean isLeft;
	
	public CustomButtonActionServerboundPacket(ResourceLocation button, boolean isLeft) {
		this.button = button;
		this.isLeft = isLeft;
	}
	
	public static CustomButtonActionServerboundPacket decode(RegistryFriendlyByteBuf buf) {
		return new CustomButtonActionServerboundPacket(buf.readResourceLocation(), buf.readBoolean());
	}
	
	public static void runLeftClickCommand(ResourceLocation button) {
		PacketHandler.sendToServer(new CustomButtonActionServerboundPacket(button, true));
	}
	
	public static void runRightClickCommand(ResourceLocation button) {
		PacketHandler.sendToServer(new CustomButtonActionServerboundPacket(button, false));
	}
	
	public void encode(RegistryFriendlyByteBuf buf) {
		buf.writeResourceLocation(button);
		buf.writeBoolean(isLeft);
	}
	
	public void handle(IPayloadContext ctx) {
		ctx.enqueueWork(() -> {
			if (!(ctx.player() instanceof ServerPlayer player))
				return;
			
			CustomButton button = CustomButtonServerEvents.customButtons.get(this.button);
			if (button == null)
				return;
			List<CustomButtonActions.CustomButtonAction> actions = button.leftClickActions;
			if (actions == null || actions.isEmpty())
				return;
			for (CustomButtonActions.CustomButtonAction action : actions) {
				action.execute(player);
			}
			
		});
	}
}