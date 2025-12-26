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

public class ProtectiveEnchantment extends HeroAbility {

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

    public ProtectiveEnchantment() {
        super(3, MANA_COST_RANK_1, UnitAction.PROTECTIVE_ENCHANTMENT, CD_RANK_1 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
        maxCharges = CHARGES_RANK_1;
        this.autocastEnableAction = UnitAction.PROTECTIVE_ENCHANTMENT_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.PROTECTIVE_ENCHANTMENT_AUTOCAST_DISABLE;
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
        AbilityButton button = new AbilityButton("Protective Enchantment",
                null,
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.PROTECTIVE_ENCHANTMENT || isAutocasting(hero),
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.PROTECTIVE_ENCHANTMENT),
                () -> toggleAutocast(hero),
                getTooltipLines(hero),
                this,
                hero
        );
        button.iconItem = new ItemStack(Items.IRON_CHESTPLATE);
        button.iconItem.enchant(Enchantments.MENDING, 1);
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        Button button = super.getRankUpButtonProtected(
                "Protective Enchantment",
                null,
                hero
        );
        button.iconItem = new ItemStack(Items.IRON_SWORD);
        button.iconItem.enchant(Enchantments.MENDING, 1);
        return button;
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.protective_enchantment") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.protective_enchantment.stats", cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip2", DURATION_SECONDS)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.charges", maxCharges))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.protective_enchantment"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip2", DURATION_SECONDS)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.rank1"), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.rank2"), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.rank3"), getRank(hero) == 2)
        );
    }

    private final static List<EntityType<? extends Mob>> ALLOWED_MOB_TYPES = List.of(
            EntityRegistrar.PILLAGER_UNIT.get(),
            EntityRegistrar.VINDICATOR_UNIT.get(),
            EntityRegistrar.EVOKER_UNIT.get()
    );

    public static boolean canEnchantUnit(LivingEntity unit) {
        return ALLOWED_MOB_TYPES.contains(unit.getType()) &&
            !unit.getItemBySlot(EquipmentSlot.CHEST).isEmpty() &&
            !unit.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(Enchantments.MENDING);
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (!ALLOWED_MOB_TYPES.contains(targetEntity.getType())) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error3"));
            return;
        }
        if (targetEntity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error6"));
            return;
        }
        if (targetEntity.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(Enchantments.MENDING)) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error4"));
            return;
        }
        ((EnchanterUnit) unitUsing).getCastEnchantProtectiveGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastEnchantProtectiveGoal().setTarget(targetEntity);
    }
}
