package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.AbstractTotem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

import static com.solegendary.reignofnether.player.PlayerServerEvents.serverLevel;

public class TotemItem extends UnitItem {

    private final EntityType<? extends AbstractTotem> totemType;

    public TotemItem(UnitItemBuilder builder, EntityType<? extends AbstractTotem> totemType) {
        super(builder
            .type(UnitItemType.CONSUMABLE)
            .buyCost(300)
            .sellValue(150)
            .consumeOnUse()
        );
        this.onUseGround = this::spawnTotem;
        this.totemType = totemType;
    }

    // shared drop-and-equip logic used by all merchant upgrades
    private boolean spawnTotem(Unit unit, BlockPos pos) {
        LivingEntity le = (LivingEntity) unit;
        if (!le.level().isClientSide()) {
            Entity entity = UnitServerEvents.spawnMob(totemType, serverLevel, pos, unit.getOwnerName());
            if (entity != null) {
                SoundClientboundPacket.playSoundAtPos(SoundAction.TOTEM_PLACE, pos);
                return true;
            }
            return false;
        }
        return true;
    }
}