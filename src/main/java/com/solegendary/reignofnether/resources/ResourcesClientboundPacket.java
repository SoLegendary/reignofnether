package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ResourcesClientboundPacket {

    // pos is used to identify the building object serverside
    ResourcesAction action;
    public String ownerName;
    public int food;
    public int wood;
    public int ore;

    public static void syncClientResources(ArrayList<Resources> resourcesList) {
        for (Resources resources : resourcesList)
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new ResourcesClientboundPacket(ResourcesAction.SYNC, resources.ownerName, resources.food, resources.wood, resources.ore));
    }

    public static void addSubtractClientResources(Resources resources) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new ResourcesClientboundPacket(ResourcesAction.ADD_SUBTRACT, resources.ownerName, resources.food, resources.wood, resources.ore));
    }

    public static void warnInsufficientResources(String ownerName, boolean food, boolean wood, boolean ore) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new ResourcesClientboundPacket(ResourcesAction.WARN_INSUFFICIENT_RESOURCES, ownerName, food ? 1 : 0, wood ? 1 : 0, ore ? 1 : 0));
    }

    public ResourcesClientboundPacket(ResourcesAction action, String ownerName, int food, int wood, int ore) {
        this.action = action;
        this.ownerName = ownerName;
        this.food = food;
        this.wood = wood;
        this.ore = ore;
    }

    public ResourcesClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(ResourcesAction.class);
        this.ownerName = buffer.readUtf();
        this.food = buffer.readInt();
        this.wood = buffer.readInt();
        this.ore = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.food);
        buffer.writeInt(this.wood);
        buffer.writeInt(this.ore);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (this.action) {
                            case SYNC -> ResourcesClientEvents.syncResources(new Resources(
                                    this.ownerName,
                                    this.food,
                                    this.wood,
                                    this.ore
                            ));
                            case ADD_SUBTRACT -> ResourcesClientEvents.addSubtractResources(new Resources(
                                    this.ownerName,
                                    this.food,
                                    this.wood,
                                    this.ore
                            ));
                            case WARN_INSUFFICIENT_RESOURCES -> {
                                Player player = Minecraft.getInstance().player;
                                if (player != null && player.getName().getString().equals(ownerName)) {
                                    HudClientEvents.showInsufficientResourcesWarning(
                                        this.food > 0,
                                        this.wood > 0,
                                        this.ore > 0
                                    );
                                }
                            }
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
