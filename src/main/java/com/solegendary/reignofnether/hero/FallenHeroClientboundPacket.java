package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.HeroUnitSave;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class FallenHeroClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<FallenHeroClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "fallen_hero_clientbound"));

    @Override
    public CustomPacketPayload.Type<FallenHeroClientboundPacket> type() {
        return TYPE;
    }

    public String uuid;
    public String name;
    public String ownerName;
    public int experience;
    public int skillPoints;
    public int ability1Rank;
    public int ability2Rank;
    public int ability3Rank;
    public int ability4Rank;
    public NonNullList<ItemStack> items;

    public static void addFallenHero(HeroUnitSave heroUnitSave) {
        for (ServerPlayer sp : PlayerServerEvents.players) {
            if (sp.getName().getString().equals(heroUnitSave.ownerName)) {
                PacketHandler.send(PacketHandler.toPlayer(() -> sp),
                        new FallenHeroClientboundPacket(heroUnitSave));
            }
        }
    }

    public static void addFallenHero(ServerPlayer player, HeroUnitSave heroUnitSave) {
        PacketHandler.send(PacketHandler.toPlayer(() -> player),
                new FallenHeroClientboundPacket(heroUnitSave));
    }

    public FallenHeroClientboundPacket(HeroUnitSave heroUnitSave) {
        this.uuid = heroUnitSave.uuid;
        this.name = heroUnitSave.name;
        this.ownerName = heroUnitSave.ownerName;
        this.experience = heroUnitSave.experience;
        this.skillPoints = heroUnitSave.skillPoints;
        this.ability1Rank = heroUnitSave.ability1Rank;
        this.ability2Rank = heroUnitSave.ability2Rank;
        this.ability3Rank = heroUnitSave.ability3Rank;
        this.ability4Rank = heroUnitSave.ability4Rank;
        this.items = heroUnitSave.items;
    }

    public FallenHeroClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.uuid = buffer.readUtf();
        this.name = buffer.readUtf();
        this.ownerName = buffer.readUtf();
        this.experience = buffer.readInt();
        this.skillPoints = buffer.readInt();
        this.ability1Rank = buffer.readInt();
        this.ability2Rank = buffer.readInt();
        this.ability3Rank = buffer.readInt();
        this.ability4Rank = buffer.readInt();
        this.items = NonNullList.withSize(UnitInventory.MAX_INVENTORY_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < this.items.size(); i++)
            // 1.21.1 removed FriendlyByteBuf#readItem; ItemStack's own optional stream codec has the
            // same wire shape (presence boolean, then the nbt + component payload).
            this.items.set(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.uuid);
        buffer.writeUtf(this.name);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.experience);
        buffer.writeInt(this.skillPoints);
        buffer.writeInt(this.ability1Rank);
        buffer.writeInt(this.ability2Rank);
        buffer.writeInt(this.ability3Rank);
        buffer.writeInt(this.ability4Rank);
        for (ItemStack stack : this.items)
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    HeroClientEvents.addFallenHero(new HeroUnitSave(
                        uuid,
                        name,
                        ownerName,
                        experience,
                        skillPoints,
                        0,
                        ability1Rank,
                        ability2Rank,
                        ability3Rank,
                        ability4Rank,
                        items
                    ));
                });
        });
        return;
    }
}
