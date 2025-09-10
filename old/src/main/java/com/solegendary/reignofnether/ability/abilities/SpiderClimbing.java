package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SpiderClimbing extends Ability {

    private final SpiderUnit spiderUnit;

    public SpiderClimbing(SpiderUnit unit) {
        super(
            UnitAction.NONE,
            unit.level(),
            0,
            0,
            0,
            false,
            false
        );
        this.spiderUnit = unit;
        this.autocastEnableAction = UnitAction.ENABLE_SPIDER_CLIMBING;
        this.autocastDisableAction = UnitAction.DISABLE_SPIDER_CLIMBING;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        ResourceLocation rlLadder = new ResourceLocation("minecraft", "textures/block/ladder.png");
        ResourceLocation rlBarrier = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png");

        AbilityButton ab = new AbilityButton(
                "Toggle Wall Climbing",
                spiderUnit.isWallClimbing() ? rlLadder : rlBarrier,
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                this::toggleAutocast,
                null,
                List.of(
                    spiderUnit.isWallClimbing() ?
                        fcs(I18n.get("abilities.reignofnether.spider_climbing_on")) :
                        fcs(I18n.get("abilities.reignofnether.spider_climbing_off"))
                ),
                this
        );
        if (!spiderUnit.isWallClimbing())
            ab.bgIconResource = rlLadder;

        return ab;
    }

    @Override
    public void setAutocast(boolean value) {
        super.setAutocast(value);
        if ((isAutocasting() && !spiderUnit.isWallClimbing()) ||
            (!isAutocasting() && spiderUnit.isWallClimbing()))
            spiderUnit.toggleWallClimbing();
        if (level.isClientSide())
            spiderUnit.updateAbilityButtons();
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
