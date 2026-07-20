package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ToggleShield extends Ability {

    private static final int CD_MAX_SECONDS = 0;

    public static final float MOVESPEED_MULTIPLIER = 0.5f;
    public static final float PROJECTILE_DAMAGE_RESIST = 0.75f;

    public ToggleShield() {
        super(
                UnitAction.TOGGLE_SHIELD_RAISE,
                CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
                0,
                0,
                false
        );
    }

    private final static Predicate<LivingEntity> TOGGLE_CHECK = le ->
            HudClientEvents.hudSelectedEntity instanceof BruteUnit hudUnit &&
            le instanceof BruteUnit unit &&
            hudUnit.isHoldingUpShield() == unit.isHoldingUpShield();

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof BruteUnit bruteUnit))
            return null;
        return new AbilityButton(
                "Shield Stance",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/shield.png"),
                hotkey,
                bruteUnit::isHoldingUpShield,
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_BRUTE_SHIELDS) ||
                        bruteUnit.getItemBySlot(EquipmentSlot.OFFHAND).getItem() != Items.SHIELD,
                () -> true,
                () -> sendUnitCommand(UnitAction.TOGGLE_SHIELD_RAISE, TOGGLE_CHECK),
                null,
                List.of(
                        fcs(I18n.get("abilities.reignofnether.shield_stance"), true),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.shield_stance.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.shield_stance.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.shield_stance.tooltip3"), Style.EMPTY)
                ),
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(unitUsing instanceof BruteUnit bruteUnit))
            return;
        bruteUnit.toggleShield();
        bruteUnit.updateAbilityButtons();
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
