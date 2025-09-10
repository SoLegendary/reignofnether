package com.solegendary.reignofnether.ability.heroAbilities.piglin;

//Causes other abilities to use your resources to gain improved effects
//Higher levels raise the effects but also cost of the buff
//This can be toggled this on and off

// only uses resources in multiples of 100

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class GreedIsGoodPassive extends HeroAbility {

    public int maxResourcesPerCast = 100;

    public GreedIsGoodPassive(HeroUnit hero) {
        super(hero, 3, 0, UnitAction.NONE, 0, 0, 0, false);
        this.autocastEnableAction = UnitAction.ENABLE_GREED_IS_GOOD_PASSIVE;
        this.autocastDisableAction = UnitAction.DISABLE_GREED_IS_GOOD_PASSIVE;
        this.setAutocast(true);
    }

    public boolean rankUp() {
        if (super.rankUp()) {
            updateStatsForRank();
            return true;
        }
        return false;
    }

    public void updateStatsForRank() {
        if (rank == 1) {
            maxResourcesPerCast = 100;
        } else if (rank == 2) {
            maxResourcesPerCast = 200;
        } else if (rank == 3) {
            maxResourcesPerCast = 300;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton("Greed is Good",
                new ResourceLocation("minecraft", "textures/block/gold_block.png"),
                hotkey,
                this::isAutocasting,
                () -> rank == 0,
                () -> true,
                this::toggleAutocast,
                null,
                getTooltipLines(),
                this
        );
    }

    @Override
    public Button getRankUpButton() {
        return super.getRankUpButtonProtected(
                "Greed is Good",
                new ResourceLocation("minecraft", "textures/block/gold_block.png")
        );
    }

    public List<FormattedCharSequence> getTooltipLines() {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.greed_is_good") + " " + rankString(), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip2", maxResourcesPerCast)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines() {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.greed_is_good"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle()),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip2", maxResourcesPerCast)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.can_be_toggled")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.rank3"), rank == 2)
        );
    }

    // return the amount of 100s of resources spent
    public int spendResourcesAndGet100sSpent(ResourceName resName) {
        int totalSpent = 0;
        String ownerName = hero.getOwnerName();
        boolean isClientSide = ((LivingEntity) hero).level().isClientSide();
        List<Resources> resourcesList = isClientSide ? ResourcesClientEvents.resourcesList : ResourcesServerEvents.resourcesList;
        if (isAutocasting()) {
            for (Resources resources : resourcesList) {
                if (resources.ownerName.equals(ownerName)) {
                    for (int i = 0; i < rank; i++) {
                        Resources resToSpend = new Resources(hero.getOwnerName(), 0, 0, 0);
                        if (resName == ResourceName.FOOD && resources.food >= 100) {
                            resToSpend.food -= 100;
                            totalSpent += 100;
                        } else if (resName == ResourceName.WOOD && resources.wood >= 100) {
                            resToSpend.wood -= 100;
                            totalSpent += 100;
                        } else if (resName == ResourceName.ORE && resources.ore >= 100) {
                            resToSpend.ore -= 100;
                            totalSpent += 100;
                        }
                        if (!isClientSide) {
                            ResourcesServerEvents.addSubtractResources(resToSpend);
                        }
                    }
                }
            }
        }
        return totalSpent / 100;
    }
}
