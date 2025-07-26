package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// used by militia to swap between a sword and a bow

public class WeaponSwapBow extends Ability {

    private final MilitiaUnit militiaUnit;

    public WeaponSwapBow(MilitiaUnit militiaUnit) {
        super(
                UnitAction.MILITIA_USE_BOW,
                militiaUnit.level(),
                0,
                0,
                0,
                false
        );
        this.militiaUnit = militiaUnit;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Weapon Swap",
                new ResourceLocation("minecraft", "textures/item/bow.png"),
                hotkey,
                () -> false,
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_MILITIA_BOWS) || militiaUnit.isUsingBow(),
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(UnitAction.MILITIA_USE_BOW),
                null,
                List.of(
                        fcs(I18n.get("abilities.reignofnether.weapon_swap_bow"), true),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.weapon_swap_bow.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.weapon_swap_bow.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (unitUsing == this.militiaUnit)
            this.militiaUnit.swapWeapons(true);
    }
}
