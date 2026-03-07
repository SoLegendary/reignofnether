package com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant;

//Causes other abilities to use your resources to gain improved effects
//Higher levels raise the effects but also cost of the buff
//This can be toggled this on and off

// only uses resources in multiples of 100

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class GreedIsGoodPassive extends HeroAbility {

    public int maxResourcesPerCast = 100;

    public GreedIsGoodPassive() {
        super(3, 0, UnitAction.NONE, 0, 0, 0, false);
        this.autocastEnableAction = UnitAction.ENABLE_GREED_IS_GOOD_PASSIVE;
        this.autocastDisableAction = UnitAction.DISABLE_GREED_IS_GOOD_PASSIVE;
        this.setDefaultAutocast(true);
    }

    public boolean rankUp(HeroUnit hero) {
        if (super.rankUp(hero)) {
            updateStatsForRank(hero);
            return true;
        }
        return false;
    }

    @Override
    public void updateStatsForRank(HeroUnit hero) {
        if (getRank(hero) == 1) {
            maxResourcesPerCast = 100;
        } else if (getRank(hero) == 2) {
            maxResourcesPerCast = 200;
        } else if (getRank(hero) == 3) {
            maxResourcesPerCast = 300;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Greed is Good",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/greed_is_good.png"),
                hotkey,
                () -> isAutocasting(hero),
                () -> getRank(hero) == 0,
                () -> true,
                () -> toggleAutocast(hero),
                null,
                getTooltipLines((HeroUnit) hero),
                this,
                hero
        );
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Greed is Good",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/greed_is_good.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.greed_is_good") + " " + rankString(hero), true),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip2", maxResourcesPerCast)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip3"))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.greed_is_good"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.tooltip2", maxResourcesPerCast)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.can_be_toggled")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.greed_is_good.rank3"), getRank(hero) == 2)
        );
    }

    // return the amount of 100s of resources spent
    public int spendResourcesAndGet100sSpent(ResourceName resName, HeroUnit hero) {
        int totalSpent = 0;
        String ownerName = hero.getOwnerName();
        boolean isClientSide = ((LivingEntity) hero).level().isClientSide();
        List<Resources> resourcesList = isClientSide ? ResourcesClientEvents.resourcesList : ResourcesServerEvents.resourcesList;
        if (isAutocasting(hero)) {
            for (Resources resources : resourcesList) {
                if (resources.ownerName.equals(ownerName)) {
                    for (int i = 0; i < getRank(hero); i++) {
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
