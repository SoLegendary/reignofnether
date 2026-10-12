package com.solegendary.reignofnether.hero;

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.function.Supplier;

public class HeroClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<HeroClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "hero_clientbound"));

    @Override
    public CustomPacketPayload.Type<HeroClientboundPacket> type() {
        return TYPE;
    }

    HeroAction action;
    int unitId;
    float value;
    int abilityIndex;

    public static void setExperience(int unitId, int value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.SET_EXPERIENCE, unitId, value, 0));
    }

    public static void setSkillPoints(int unitId, int value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.SET_SKILL_POINTS, unitId, value, 0));
    }

    public static void setCharges(int unitId, int value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.SET_CHARGES, unitId, value, 0));
    }

    public static void setAbilityRank(int unitId, int rank, int abilityIndex) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.SET_ABILITY_RANK, unitId, rank, abilityIndex));
    }

    public static void setMana(int unitId, float value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.SET_MANA, unitId, value, 0));
    }

    public static void setMaxMana(int unitId, float value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.SET_MAX_MANA, unitId, value, 0));
    }

    public static void activateAbilityClientside(int unitId, int abilityIndex) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.ACTIVATE_ABILITY_CLIENTSIDE, unitId, 0, abilityIndex));
    }

    public static void deactivateAbilityClientside(int unitId, int abilityIndex) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new HeroClientboundPacket(HeroAction.DEACTIVATE_ABILITY_CLIENTSIDE, unitId, 0, abilityIndex));
    }

    public HeroClientboundPacket(HeroAction action, int unitId, float value, int abilityIndex) {
        this.action = action;
        this.unitId = unitId;
        this.value = value;
        this.abilityIndex = abilityIndex;
    }

    public HeroClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(HeroAction.class);
        this.unitId = buffer.readInt();
        this.value = buffer.readFloat();
        this.abilityIndex = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeFloat(this.value);
        buffer.writeInt(this.abilityIndex);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
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
                                if (abls.size() > abilityIndex) abls.get(abilityIndex).setRank(hero, (int)value);
                                for (HeroAbility abl : abls)
                                    abl.updateStatsForRank(hero);
                                hero.updateAbilityButtons();
                            }
                            case SET_MANA -> hero.setMana(value);
                            case SET_MAX_MANA -> hero.setMaxMana(value);
                            case ACTIVATE_ABILITY_CLIENTSIDE -> hero.activateAbilityClientside(abilityIndex);
                            case DEACTIVATE_ABILITY_CLIENTSIDE -> hero.deactivateAbilityClientside(abilityIndex);
                        }
                    }
                });
        });
        return;
    }
}
