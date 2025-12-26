package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class ThrowHealingPotion extends Ability {

    public static final int CD_MAX_SECONDS = 12;

    public final Potion potion = Potions.STRONG_HEALING;

    //TODO Fix potionThrowRange for Witches gathering on a building
    public ThrowHealingPotion(int potionThrowRange) {
        super(UnitAction.THROW_HEALING_POTION,
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
                potionThrowRange,
            0,
            true,
            true
        );
        this.autocastEnableAction = UnitAction.THROW_HEALING_POTION_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.THROW_HEALING_POTION_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof WitchUnit witchUnit))
            return null;
        return new AbilityButton("Healing Potion",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/splash_potion_healing.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.THROW_HEALING_POTION || isAutocasting(unit),
            () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_HEALING_POTIONS),
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.THROW_HEALING_POTION),
            () -> toggleAutocast(unit),
            List.of(
                fcs(I18n.get("abilities.reignofnether.healing_potion"), true),
                fcsIcons(I18n.get("abilities.reignofnether.healing_potion.tooltip1", CD_MAX_SECONDS, witchUnit.getPotionThrowRange())),
                fcs(I18n.get("abilities.reignofnether.healing_potion.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.autocast"))
            ),
            this,
            unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WitchUnit) unitUsing).getThrowPotionGoal().setPotion(potion);
        ((WitchUnit) unitUsing).getThrowPotionGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((WitchUnit) unitUsing).getThrowPotionGoal().setPotion(potion);
        ((WitchUnit) unitUsing).getThrowPotionGoal().setTarget(targetEntity);
    }
}
