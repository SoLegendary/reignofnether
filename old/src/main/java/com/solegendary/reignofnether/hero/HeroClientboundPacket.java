package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class HeroClientboundPacket {

    HeroAction action;
    int unitId;
    float value;
    int abilityIndex;

    public static void setExperience(int unitId, int value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.SET_EXPERIENCE, unitId, value, 0));
    }

    public static void setSkillPoints(int unitId, int value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.SET_SKILL_POINTS, unitId, value, 0));
    }

    public static void setCharges(int unitId, int value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.SET_CHARGES, unitId, value, 0));
    }

    public static void setAbilityRank(int unitId, int rank, int abilityIndex) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.SET_ABILITY_RANK, unitId, rank, abilityIndex));
    }

    public static void setMana(int unitId, float value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.SET_MANA, unitId, value, 0));
    }

    public static void setMaxMana(int unitId, float value) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.SET_MAX_MANA, unitId, value, 0));
    }

    public static void activateAbilityClientside(int unitId, int abilityIndex) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.ACTIVATE_ABILITY_CLIENTSIDE, unitId, 0, abilityIndex));
    }

    public static void deactivateAbilityClientside(int unitId, int abilityIndex) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new HeroClientboundPacket(HeroAction.DEACTIVATE_ABILITY_CLIENTSIDE, unitId, 0, abilityIndex));
    }

    public HeroClientboundPacket(HeroAction action, int unitId, float value, int abilityIndex) {
        this.action = action;
        this.unitId = unitId;
        this.value = value;
        this.abilityIndex = abilityIndex;
    }

    public HeroClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(HeroAction.class);
        this.unitId = buffer.readInt();
        this.value = buffer.readFloat();
        this.abilityIndex = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeFloat(this.value);
        buffer.writeInt(this.abilityIndex);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    HeroUnit hero = null;
                    for(LivingEntity entity : UnitClientEvents.getAllUnits())
                        if (entity.getId() == unitId && entity instanceof HeroUnit)
                            hero = (HeroUnit) entity;

                    if (hero != null) {
                        switch (action) {
                            case SET_EXPERIENCE -> hero.setExperience((int) value);
                            case SET_SKILL_POINTS -> hero.setSkillPoints((int) value);
                            case SET_CHARGES -> hero.setChargesFromSaveData((int) value);
                            case SET_ABILITY_RANK -> {
                                List<HeroAbility> abls = hero.getHeroAbilities();
                                if (abls.size() > abilityIndex) abls.get(abilityIndex).rank = (int) value;
                                for (HeroAbility abl : abls)
                                    abl.updateStatsForRank();
                                ((Unit) hero).updateAbilityButtons();
                            }
                            case SET_MANA -> hero.setMana(value);
                            case SET_MAX_MANA -> hero.setMaxMana(value);
                            case ACTIVATE_ABILITY_CLIENTSIDE -> hero.activateAbilityClientside(abilityIndex);
                            case DEACTIVATE_ABILITY_CLIENTSIDE -> hero.deactivateAbilityClientside(abilityIndex);
                        }
                    }
                    success.set(true);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
