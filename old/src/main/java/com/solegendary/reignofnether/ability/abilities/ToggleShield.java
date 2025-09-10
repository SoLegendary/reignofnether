package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ToggleShield extends Ability {

    private static final int CD_MAX_SECONDS = 0;

    private final BruteUnit bruteUnit;

    public static final float MOVESPEED_MULTIPLIER = 0.5f;
    public static final float PROJECTILE_DAMAGE_RESIST = 0.75f;

    public ToggleShield(BruteUnit bruteUnit) {
        super(
                UnitAction.NONE,
                bruteUnit.level(),
                CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND,
                0,
                0,
                false
        );
        this.bruteUnit = bruteUnit;
        this.autocastEnableAction = UnitAction.ENABLE_SHIELD_RAISE;
        this.autocastDisableAction = UnitAction.DISABLE_SHIELD_RAISE;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Shield Stance",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/shield.png"),
                hotkey,
                () -> bruteUnit.isHoldingUpShield,
                () -> !ResearchClient.hasResearch(ProductionItems.RESEARCH_BRUTE_SHIELDS) ||
                        bruteUnit.getItemBySlot(EquipmentSlot.OFFHAND).getItem() != Items.SHIELD,
                () -> true,
                this::toggleAutocast,
                null,
                List.of(
                        fcs(I18n.get("abilities.reignofnether.shield_stance"), true),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.shield_stance.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.shield_stance.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.shield_stance.tooltip3"), Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public void setAutocast(boolean value) {
        super.setAutocast(value);
        if ((isAutocasting() && !bruteUnit.isHoldingUpShield) ||
            (!isAutocasting() && bruteUnit.isHoldingUpShield))
            bruteUnit.toggleShield();
        if (level.isClientSide())
            bruteUnit.updateAbilityButtons();
    }

    @Override
    public boolean shouldResetBehaviours() { return false; }
}
