package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
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

public class ThrowLingeringRegenPotion extends Ability {

    public static final int CD_MAX_SECONDS = 10;

    private final WitchUnit witchUnit;

    public final Potion potion = Potions.STRONG_REGENERATION;

    public ThrowLingeringRegenPotion(WitchUnit witchUnit) {
        super(
            UnitAction.THROW_LINGERING_REGEN_POTION,
            witchUnit.level(),
            CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
            witchUnit.getPotionThrowRange(),
            0,
            true,
            true
        );
        this.witchUnit = witchUnit;
        this.autocastEnableAction = UnitAction.THROW_LINGERING_REGEN_POTION_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.THROW_LINGERING_REGEN_POTION_AUTOCAST_DISABLE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
            "Lingering Regen Potion",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/lingering_potion_regeneration.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.THROW_LINGERING_REGEN_POTION || isAutocasting(),
            () -> false, //!ResearchClient.hasResearch(ResearchLingeringPotions.itemName),
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.THROW_LINGERING_REGEN_POTION),
            this::toggleAutocast,
            List.of(
                fcs(I18n.get("abilities.reignofnether.lingering_regen_potion"), true),
                fcsIcons(I18n.get("abilities.reignofnether.lingering_regen_potion.tooltip1", CD_MAX_SECONDS, witchUnit.getPotionThrowRange())),
                fcs(I18n.get("abilities.reignofnether.lingering_regen_potion.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.autocast"))
            ),
            this
        );
    }

    // lingering vs splash is set in WitchUnit.throwPotion
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
