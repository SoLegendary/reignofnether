package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.MountGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.StrayUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.piglins.HoglinUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class MountHoglin extends Ability {
    public MountHoglin() {
        super(UnitAction.MOUNT_HOGLIN, 0, 0, 0, true);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton("Mount Hoglin",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/hoglin.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.MOUNT_HOGLIN,
            () -> ((Entity)unit).isPassenger() || !ResearchClient.hasResearch(ProductionItems.RESEARCH_HOGLIN_CAVALRY),
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.MOUNT_HOGLIN),
            () -> UnitClientEvents.sendUnitCommand(UnitAction.MOUNT_HOGLIN),
            List.of(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.mount_hoglin"), Style.EMPTY)),
            this,
            unit
        );
    }

    private MountGoal getMountGoal(Entity entity) {
        if (entity instanceof PillagerUnit pillagerUnit) {
            return pillagerUnit.getMountGoal();
        }
        if (entity instanceof StrayUnit strayUnit) {
            return strayUnit.getMountGoal();
        }
        if (entity instanceof SkeletonUnit skeletonUnit) {
            return skeletonUnit.getMountGoal();
        }
        if (entity instanceof HeadhunterUnit headhunterUnit) {
            return headhunterUnit.getMountGoal();
        }
        return null;
    }


    // right click
    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        MountGoal mountGoal = getMountGoal((Entity) unitUsing);
        if (mountGoal != null)
            mountGoal.autofind = true;
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        MountGoal mountGoal = getMountGoal((Entity) unitUsing);
        if (mountGoal != null && targetEntity instanceof HoglinUnit) {
            getMountGoal((Entity) unitUsing).setTarget(targetEntity);
        } else if (level.isClientSide()) {
            HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.mount_hoglin.error1"));
        }
    }
}
