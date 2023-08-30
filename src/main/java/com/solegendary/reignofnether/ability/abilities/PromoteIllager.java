package com.solegendary.reignofnether.ability.abilities;

import com.mojang.math.Vector3d;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.StrayUnit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PromoteIllager extends Ability {

    private static final int CD_MAX = 120 * ResourceCost.TICKS_PER_SECOND;
    private static final int RANGE = 20;
    private static final int BUFF_RANGE = 10;

    LivingEntity promotedIllager = null;
    Castle castle;

    public PromoteIllager(Castle castle) {
        super(
            UnitAction.PROMOTE_ILLAGER,
            CD_MAX,
            RANGE,
            0,
            true
        );
        this.castle = castle;
    }

    // checks that the unit has a banner and applies the speed buff to nearby friendly units if it is
    public static void checkAndApplyBuff(LivingEntity entity) {
        if (!entity.level.isClientSide() && entity instanceof Unit captainUnit &&
                entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem) {
            List<PathfinderMob> nearbyMobs = MiscUtil.getEntitiesWithinRange(
                    new Vector3d(entity.position().x, entity.position().y, entity.position().z),
                    BUFF_RANGE,
                    PathfinderMob.class,
                    entity.level);

            for (PathfinderMob mob : nearbyMobs)
                if (mob instanceof Unit unit && unit.getOwnerName().equals(captainUnit.getOwnerName()))
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0));
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
            "Promote Illager",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/ominous_banner.png"),
            hotkey,
            () -> false,
            () -> !castle.isUpgraded(),
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.PROMOTE_ILLAGER),
            null,
            List.of(
                    FormattedCharSequence.forward("Promote Illager", Style.EMPTY.withBold(true)),
                    FormattedCharSequence.forward("\uE004  " + CD_MAX/20 + "s  \uE005  " + RANGE, MyRenderer.iconStyle),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("Promote an illager to a captain, giving it a banner that gives", Style.EMPTY),
                    FormattedCharSequence.forward("a speed buff to all friendly units in a " + BUFF_RANGE + " block radius.", Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("You may only have one captain at a time per upgraded castle.", Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, Building buildingUsing, LivingEntity targetEntity) {
        Vec3 pos = targetEntity.getEyePosition();
        if (buildingUsing.centrePos.distToCenterSqr(pos.x, pos.y, pos.z) > RANGE * RANGE) {
            HudClientEvents.showTemporaryMessage("Unit is too far away!");
        }
        else if (targetEntity instanceof VindicatorUnit ||
            targetEntity instanceof PillagerUnit ||
            targetEntity instanceof EvokerUnit) {

            if (targetEntity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem) {
                HudClientEvents.showTemporaryMessage("That unit is already a captain!");
                return;
            }
            // only once promotedIllager allowed at a time
            if (promotedIllager != null && promotedIllager.isAlive() &&
                promotedIllager.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem) {
                promotedIllager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.AIR));
            }
            promotedIllager = targetEntity;
            promotedIllager.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
            this.setToMaxCooldown();
        }
        else {
            HudClientEvents.showTemporaryMessage("Only Vindicators, Pillagers and Evokers may be promoted");
        }
    }
}
