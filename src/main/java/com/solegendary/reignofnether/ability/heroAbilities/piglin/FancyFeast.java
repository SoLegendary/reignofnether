package com.solegendary.reignofnether.ability.heroAbilities.piglin;

//Throws out a pile of food - friendly units automatically pick up this food and take a few seconds to eat it to instantly heal
//Higher levels raise the quality of food thrown
//Greed is Good raises the amount of food thrown

// TODO: make piglin units stop to eat vanilla food items if they are damaged

import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.PiglinMerchantUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class FancyFeast extends HeroAbility {

    public static final int RANGE = 10;

    private static final int CD_MAX_SECONDS = 30 * ResourceCost.TICKS_PER_SECOND;
    private static final float BASE_ITEMS = 6;
    private static final float BONUS_ITEMS_PER_RESOURCES = 2;

    private static final float HEALTH_PER_BREAD = 10;
    private static final float HEALTH_PER_CHICKEN = 15;
    private static final float HEALTH_PER_BEEF = 20;


    public FancyFeast() {
        super(3, UnitAction.FANCY_FEAST, CD_MAX_SECONDS, RANGE, 0, false);
    }

    private ResourceLocation getIcon(int plusRank) {
        if (rank + plusRank == 3)
            return new ResourceLocation("minecraft", "textures/item/cooked_beef.png");
        else if (rank + plusRank == 2)
            return new ResourceLocation("minecraft", "textures/item/cooked_chicken.png");
        else
            return new ResourceLocation("minecraft", "textures/item/bread.png");
    }

    private float getHealAmount() {
        if (rank == 3)
            return HEALTH_PER_BEEF;
        else if (rank == 2)
            return HEALTH_PER_CHICKEN;
        else
            return HEALTH_PER_BREAD;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit hero) {
        return new AbilityButton("Fancy Feast",
                getIcon(0),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.FANCY_FEAST,
                () -> rank == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.FANCY_FEAST),
                null,
                getTooltipLines((HeroUnit) hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Fancy Feast",
                getIcon(1),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.fancy_feast") + " " + rankString(), true),
                fcsIcons(I18n.get("abilities.reignofnether.fancy_feast.stats", getHealAmount(), CD_MAX_SECONDS / 20)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip2", BASE_ITEMS, BONUS_ITEMS_PER_RESOURCES))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.fancy_feast"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.tooltip2", BASE_ITEMS, BONUS_ITEMS_PER_RESOURCES)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.rank1"), rank == 0),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.rank2"), rank == 1),
                fcs(I18n.get("abilities.reignofnether.fancy_feast.rank3"), rank == 2)
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
