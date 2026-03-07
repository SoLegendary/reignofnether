package com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant;

//Throws out a pile of food - friendly units automatically pick up this food and take a few seconds to eat it to instantly heal
//Higher levels raise the quality of food thrown
//Greed is Good raises the amount of food thrown

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class FancyFeast extends HeroAbility {

    public static final int RANGE = 10;

    private static final int CD_MAX_SECONDS = 45 * ResourceCost.TICKS_PER_SECOND;
    public static final int BASE_ITEMS = 6;
    public static final int BONUS_ITEMS_PER_100_RESOURCES = 2;
    public static int MANA_REFUND_PER_100_RESOURCES = 10;

    public static final float HEALTH_PER_BREAD = 12;
    public static final float HEALTH_PER_CHICKEN = 18;
    public static final float HEALTH_PER_BEEF = 24;

    public FancyFeast() {
        super(3, 70, UnitAction.FANCY_FEAST, CD_MAX_SECONDS, RANGE, 0, false);
    }

    private ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/fancy_feast.png");
    }

    public Item getFoodItem(HeroUnit hero) {
        if (getRank(hero) == 3)
            return Items.COOKED_BEEF;
        else if (getRank(hero) == 2)
            return Items.COOKED_CHICKEN;
        else
            return Items.BREAD;
    }

    private float getHealAmount(HeroUnit hero) {
        if (getRank(hero) == 3)
            return HEALTH_PER_BEEF;
        else if (getRank(hero) == 2)
            return HEALTH_PER_CHICKEN;
        else
            return HEALTH_PER_BREAD;
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof PiglinMerchantUnit piglinMerchantUnit) {
            GenericTargetedSpellGoal goal = piglinMerchantUnit.getCastFancyFeastGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Fancy Feast",
                getIcon(),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.FANCY_FEAST,
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.FANCY_FEAST),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Fancy Feast",
                getIcon(),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.fancy_feast") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.fancy_feast.stats", getHealAmount(hero), CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip2", BASE_ITEMS, BONUS_ITEMS_PER_100_RESOURCES, MANA_REFUND_PER_100_RESOURCES))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.fancy_feast"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip2", BASE_ITEMS, BONUS_ITEMS_PER_100_RESOURCES, MANA_REFUND_PER_100_RESOURCES)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.rank1", HEALTH_PER_BREAD), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.rank2", HEALTH_PER_CHICKEN), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.rank3", HEALTH_PER_BEEF), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((PiglinMerchantUnit) unitUsing).getCastFancyFeastGoal().setAbility(this);
        ((PiglinMerchantUnit) unitUsing).getCastFancyFeastGoal().setTarget(targetBp);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((PiglinMerchantUnit) unitUsing).getCastFancyFeastGoal().setAbility(this);
        ((PiglinMerchantUnit) unitUsing).getCastFancyFeastGoal().setTarget(targetEntity.getOnPos());
    }
}
