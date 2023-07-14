package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchSpiderJockeys;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.MountGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class MountSpider extends Ability {

    private final LivingEntity entity;

    public MountSpider(LivingEntity entity) {
        super(
            UnitAction.MOUNT_SPIDER,
            0,
            0,
            0,
            true
        );
        this.entity = entity;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
            "Mount Spider",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/spider.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.MOUNT_SPIDER,
            () -> entity.getVehicle() != null || !ResearchClient.hasResearch(ResearchSpiderJockeys.itemName),
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.MOUNT_SPIDER),
            null,
            List.of(
                FormattedCharSequence.forward("Mount Spider", Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity instanceof SpiderUnit)
            ((SkeletonUnit) unitUsing).getMountGoal().setTarget(targetEntity);
        else if (level.isClientSide())
            HudClientEvents.showTemporaryMessage("Invalid target!");
    }
}
