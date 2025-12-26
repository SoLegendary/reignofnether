package com.solegendary.reignofnether.ability.heroAbilities.enchanter;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class CivilEnchantment extends HeroAbility {

    public static final int RANGE = 10;

    public static final int CHARGES_RANK_1 = 2;
    public static final int CHARGES_RANK_2 = 3;
    public static final int CHARGES_RANK_3 = 4;

    public static final int CD_RANK_1 = 30;
    public static final int CD_RANK_2 = 25;
    public static final int CD_RANK_3 = 20;

    public static final int MANA_COST_RANK_1 = 50;
    public static final int MANA_COST_RANK_2 = 40;
    public static final int MANA_COST_RANK_3 = 30;

    public static final int DURATION_SECONDS = 30;

    public CivilEnchantment() {
        super(3, MANA_COST_RANK_1, UnitAction.CIVIL_ENCHANTMENT, CD_RANK_1 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
        maxCharges = CHARGES_RANK_1;
        this.autocastEnableAction = UnitAction.CIVIL_ENCHANTMENT_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.CIVIL_ENCHANTMENT_AUTOCAST_DISABLE;
    }

    @Override
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
            maxCharges = CHARGES_RANK_1;
            cooldownMax = CD_RANK_1 * ResourceCost.TICKS_PER_SECOND;
            manaCost = MANA_COST_RANK_1;
        } else if (getRank(hero) == 2) {
            maxCharges = CHARGES_RANK_2;
            cooldownMax = CD_RANK_2 * ResourceCost.TICKS_PER_SECOND;
            manaCost = MANA_COST_RANK_2;
        } else if (getRank(hero) == 3) {
            maxCharges = CHARGES_RANK_3;
            cooldownMax = CD_RANK_3 * ResourceCost.TICKS_PER_SECOND;
            manaCost = MANA_COST_RANK_3;
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        if (!(unit instanceof HeroUnit hero)) return null;
        AbilityButton button = new AbilityButton("Civil Enchantment",
                null,
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.CIVIL_ENCHANTMENT || isAutocasting(hero),
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.CIVIL_ENCHANTMENT),
                () -> toggleAutocast(hero),
                getTooltipLines(hero),
                this,
                hero
        );
        button.iconItem = new ItemStack(Items.IRON_PICKAXE);
        button.iconItem.enchant(Enchantments.BLOCK_EFFICIENCY, 1);
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        Button button = super.getRankUpButtonProtected(
                "Civil Enchantment",
                null,
                hero
        );
        button.iconItem = new ItemStack(Items.IRON_PICKAXE);
        button.iconItem.enchant(Enchantments.BLOCK_EFFICIENCY, 1);
        return button;
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.civil_enchantment") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.civil_enchantment.stats", cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.tooltip2", DURATION_SECONDS)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.charges", maxCharges))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.civil_enchantment"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.tooltip2", DURATION_SECONDS)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.civil_enchantment.rank3"), getRank(hero) == 2)
        );
    }

    public static boolean canEnchantUnit(LivingEntity unit) {
        return ALLOWED_MOB_TYPES.contains(unit.getType()) &&
                !unit.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(Enchantments.BLOCK_EFFICIENCY);
    }

    private final static List<EntityType<? extends Mob>> ALLOWED_MOB_TYPES = List.of(
            EntityRegistrar.VILLAGER_UNIT.get()
    );

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (!ALLOWED_MOB_TYPES.contains(targetEntity.getType())) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error3"));
            return;
        }
        if (targetEntity.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(Enchantments.BLOCK_EFFICIENCY)) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error4"));
            return;
        }
        ((EnchanterUnit) unitUsing).getCastEnchantCivilGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastEnchantCivilGoal().setTarget(targetEntity);
    }
}
