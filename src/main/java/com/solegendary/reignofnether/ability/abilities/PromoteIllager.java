package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.*;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class PromoteIllager extends Ability {

    private static final int CD_MAX = 120 * ResourceCost.TICKS_PER_SECOND;
    private static final int RANGE = 20;
    private static final int BUFF_RANGE = 10;

    public PromoteIllager() {
        super(UnitAction.PROMOTE_ILLAGER, CD_MAX, RANGE, 0, true, true);
        this.defaultHotkey = Keybindings.abilitySlot2;
    }

    // checks that the unit has a banner and applies the speed buff to nearby friendly units if it is
    public static void checkAndApplyBuff(LivingEntity entity) {
        if (!entity.level().isClientSide() && entity instanceof Unit captainUnit
            && entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem) {
            List<Mob> nearbyMobs = MiscUtil.getEntitiesWithinRange(new Vector3d(entity.position().x,
                    entity.position().y,
                    entity.position().z
                ),
                BUFF_RANGE,
                Mob.class,
                entity.level()
            );

            for (Mob mob : nearbyMobs)
                if (mob instanceof Unit unit && unit.getOwnerName().equals(captainUnit.getOwnerName()) &&
                    !(mob instanceof RavagerUnit) &&
                    !(mob instanceof WindcallerUnit windcallerUnit && windcallerUnit.isFlying()))
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 0));
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return new AbilityButton("Promote Illager",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/ominous_banner.png"),
            hotkey,
            () -> false,
            () -> {
                if (placement.getBuilding() instanceof Castle) {
                    return placement.getUpgradeLevel() == 0;
                }
                return true;
            },
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.PROMOTE_ILLAGER),
            null,
            List.of(
                fcs(I18n.get("abilities.reignofnether.promote_illager"), true),
                fcsIcons(I18n.get("abilities.reignofnether.promote_illager.tooltip1", CD_MAX / 20) + RANGE),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.promote_illager.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.promote_illager.tooltip3", BUFF_RANGE)),
                fcs(I18n.get("abilities.reignofnether.promote_illager.tooltip_no_effect")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.promote_illager.tooltip4"))
            ),
            this,
            placement
        );
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) {
        if (!(buildingUsing.getBuilding() instanceof Castle))
            return;

        Vec3 pos = targetEntity.getEyePosition();
        if (buildingUsing.centrePos.distToCenterSqr(pos.x, pos.y, pos.z) > RANGE * RANGE) {
            if (level.isClientSide()) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.promote_illager.error1"));
            }
        } else if (targetEntity instanceof VindicatorUnit || targetEntity instanceof PillagerUnit ||
                   targetEntity instanceof EvokerUnit || targetEntity instanceof MilitiaUnit) {

            Unit unit = (Unit) targetEntity;

            if (targetEntity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem) {
                if (level.isClientSide()) {
                    HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.promote_illager.error2"));
                }
                return;
            }
            if (!unit.getOwnerName().equals(buildingUsing.ownerName)) {
                if (level.isClientSide()) {
                    HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.promote_illager.error3"));
                }
                return;
            }
            // only once promotedIllager allowed at a time
            LivingEntity promotedIllager = buildingUsing.getDataStorage().getData(Castle.PROMOTED_ILLAGER);
            if (promotedIllager != null && promotedIllager.isAlive() &&
                promotedIllager.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof BannerItem) {
                promotedIllager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.AIR));
            }
            buildingUsing.getDataStorage().setData(Castle.PROMOTED_ILLAGER, targetEntity);
            targetEntity.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());

            // spawn a firework
            if (!level.isClientSide()) {
                MiscUtil.shootFirework(level, targetEntity.getEyePosition());
            }
            this.setToMaxCooldown(buildingUsing);
        } else {
            if (level.isClientSide()) {
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.promote_illager.error4"));
            }
        }
    }
}
