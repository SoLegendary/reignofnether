package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.HeroUnitSave;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FallenHeroClientboundPacket {

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
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
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

    public FallenHeroClientboundPacket(FriendlyByteBuf buffer) {
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
            this.items.set(i, buffer.readItem());
    }

    public void encode(FriendlyByteBuf buffer) {
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
            buffer.writeItem(stack);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
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
                    success.set(true);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
