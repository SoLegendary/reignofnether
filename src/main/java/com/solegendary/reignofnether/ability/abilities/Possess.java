package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.NecromancerUnit;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class Possess extends Ability {

    public static final int RANGE = 6;
    public static final int BONUS_CHANNELING_RANGE = 6;

    public static final int BASE_CHANNEL_TICKS = 40;
    public static final int CHANNEL_TICKS_PER_POP_COST = 30;
    public static final int MAX_CHANNEL_TICKS = 140;
    public static final int POP_PER_WRAITH = 3; // units of pop <= 3 can be possessed by 1 wraith, 4-6 takes 2 wraiths, 7+ takes 3 wraiths
    public static final int PARTIAL_POSSESS_DURATION_SECONDS = 30;

    public Possess() {
        super(
            UnitAction.POSSESS,
            0,
            RANGE,
            0,
            true,
            true
        );
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof WraithUnit wraithUnit) {
            GenericTargetedSpellGoal goal = wraithUnit.getPossessGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                "Possess",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/possess.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.POSSESS || isAutocasting(unit),
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_POSSESSION),
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.POSSESS),
                () -> toggleAutocast(unit),
                List.of(
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.possess"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE005  " + RANGE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.possess.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.possess.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.possess.tooltip3"), Style.EMPTY)
                ),
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity instanceof HeroUnit || !(targetEntity instanceof Unit unit) || unit.getOwnerName().equals(unitUsing.getOwnerName())) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("abilities.reignofnether.possess.error"));
            return;
        }
        if (unitUsing instanceof WraithUnit wraithUnit) {
            wraithUnit.getPossessGoal().setAbility(this);
            wraithUnit.getPossessGoal().setTarget(targetEntity);
        }
    }
}
