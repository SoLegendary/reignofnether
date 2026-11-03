package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class SpinWebs extends Ability {

    public static final int CD_MAX_SECONDS = 30;
    public static final int RANGE = 8;
    public static final int DURATION_SECONDS = 6;

    public SpinWebs() {
        super(
            UnitAction.SPIN_WEBS,
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
            RANGE,
            0,
            true,
            true
        );
        this.autocastEnableAction = UnitAction.SPIN_WEBS_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.SPIN_WEBS_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                "Spin Webs",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobweb.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.SPIN_WEBS || isAutocasting(unit),
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_SPIDER_WEBS),
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.SPIN_WEBS),
                () -> toggleAutocast(unit),
                List.of(
                        fcs(I18n.get("abilities.reignofnether.spin_webs"), true),
                        fcsIcons("\uE004  " + CD_MAX_SECONDS + "s  \uE005  " + RANGE),
                        fcs(I18n.get("abilities.reignofnether.spin_webs.tooltip1")),
                        fcs(I18n.get("abilities.reignofnether.spin_webs.tooltip2", DURATION_SECONDS)),
                        fcs(""),
                        fcs(I18n.get("abilities.reignofnether.autocast")),
                        fcs(I18n.get("abilities.reignofnether.spin_webs.tooltip3"))
                ),
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (!isOffCooldown(unitUsing))
            return;
        if (unitUsing instanceof SpiderUnit spiderUnit) {
            spiderUnit.getWebGoal().setAbility(this);
            spiderUnit.getWebGoal().setTarget(targetEntity);
        }
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!isOffCooldown(unitUsing))
            return;
        if (unitUsing instanceof SpiderUnit spiderUnit) {
            spiderUnit.getWebGoal().setAbility(this);
            spiderUnit.getWebGoal().setTarget(targetBp);
        }
    }
}
