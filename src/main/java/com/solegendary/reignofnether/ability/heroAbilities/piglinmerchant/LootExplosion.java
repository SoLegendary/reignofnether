package com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant;

//Throws out a huge amount of random weapons and armour that your units automatically equip upon pickup
//All equipment has limited durability
//Greed is Good raises the amount of equipment thrown

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
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

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class LootExplosion extends HeroAbility {

    private static final int CD_MAX_SECONDS = 300 * ResourceCost.TICKS_PER_SECOND;
    public static final int BASE_ITEMS = 12;
    public static final int BONUS_ITEMS_PER_100_RESOURCES = 3;
    public static int MANA_REFUND_PER_100_RESOURCES = 15;

    public LootExplosion() {
        super(1, 125, UnitAction.LOOT_EXPLOSION, CD_MAX_SECONDS, 0, 0, false);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Loot Explosion",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/loot_explosion.png"),
                hotkey,
                () -> false,
                () -> getRank(hero) == 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.LOOT_EXPLOSION),
                null,
                getTooltipLines(hero),
                this,
                hero
        );
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public boolean isCasting(Unit unit) {
        if (unit instanceof PiglinMerchantUnit piglinMerchantUnit) {
            GenericUntargetedSpellGoal goal = piglinMerchantUnit.getCastLootExplosionGoal();
            if (goal != null)
                return goal.isCasting();
        }
        return false;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Loot Explosion",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/loot_explosion.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.loot_explosion") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.loot_explosion.stats", CD_MAX_SECONDS / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip3", BASE_ITEMS, BONUS_ITEMS_PER_100_RESOURCES, MANA_REFUND_PER_100_RESOURCES))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.loot_explosion"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip3", BASE_ITEMS, BONUS_ITEMS_PER_100_RESOURCES, MANA_REFUND_PER_100_RESOURCES))
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((PiglinMerchantUnit) unitUsing).getCastLootExplosionGoal().setAbility(this);
        ((PiglinMerchantUnit) unitUsing).getCastLootExplosionGoal().startCasting();
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        ((PiglinMerchantUnit) unitUsing).getCastLootExplosionGoal().setAbility(this);
        ((PiglinMerchantUnit) unitUsing).getCastLootExplosionGoal().startCasting();
    }
}
