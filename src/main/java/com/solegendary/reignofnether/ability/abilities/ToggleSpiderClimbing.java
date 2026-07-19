package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ToggleSpiderClimbing extends Ability {

    public ToggleSpiderClimbing() {
        super(
            UnitAction.TOGGLE_SPIDER_CLIMBING,
            0,
            0,
            0,
            false,
            false
        );
    }

    private final static Predicate<LivingEntity> TOGGLE_CHECK = le ->
            HudClientEvents.hudSelectedEntity instanceof SpiderUnit hudUnit &&
            le instanceof SpiderUnit unit &&
            hudUnit.isWallClimbing() == unit.isWallClimbing();

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof SpiderUnit spiderUnit))
            return null;
        ResourceLocation rlLadder = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ladder.png");
        ResourceLocation rlBarrier = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png");

        AbilityButton ab = new AbilityButton(
                "Toggle Wall Climbing",
                spiderUnit.isWallClimbing() ? rlLadder : rlBarrier,
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> sendUnitCommand(UnitAction.TOGGLE_SPIDER_CLIMBING, TOGGLE_CHECK),
                null,
                List.of(
                        spiderUnit.isWallClimbing() ?
                        fcs(I18n.get("abilities.reignofnether.spider_climbing_on")) :
                        fcs(I18n.get("abilities.reignofnether.spider_climbing_off"))
                ),
                this,
                unit
        );
        if (!spiderUnit.isWallClimbing())
            ab.bgIconResource = rlLadder;

        return ab;
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(unitUsing instanceof SpiderUnit spiderUnit))
            return;
        spiderUnit.toggleWallClimbing();
        spiderUnit.updateAbilityButtons();
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
