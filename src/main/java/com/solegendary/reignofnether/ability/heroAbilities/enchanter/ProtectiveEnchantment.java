package com.solegendary.reignofnether.ability.heroAbilities.enchanter;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
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

public class ProtectiveEnchantment extends AbstractEnchantment {

    public static final int RANGE = 10;

    public static final float MAX_ABSORB_HP = 20;

    public static final int CHARGES_RANK_1 = 2;
    public static final int CHARGES_RANK_2 = 3;
    public static final int CHARGES_RANK_3 = 4;

    public static final int CD_RANK_1 = 25;
    public static final int CD_RANK_2 = 20;
    public static final int CD_RANK_3 = 15;

    public static final int MANA_COST_RANK_1 = 40;
    public static final int MANA_COST_RANK_2 = 30;
    public static final int MANA_COST_RANK_3 = 20;

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
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/protective_enchantment.png"),
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
        button.stretchIconToBorders = true;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        Button button = super.getRankUpButtonProtected(
                "Protective Enchantment",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/protective_enchantment.png"),
                hero
        );
        return button;
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.protective_enchantment") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.protective_enchantment.stats", cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip2", MAX_ABSORB_HP)),
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
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.tooltip2", MAX_ABSORB_HP)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.rank1", CHARGES_RANK_1, CD_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.rank2", CHARGES_RANK_2, CD_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.protective_enchantment.rank3", CHARGES_RANK_3, CD_RANK_3), getRank(hero) == 2)
        );
    }


    @Override
    public boolean canEnchant(LivingEntity le) {
        return le instanceof Unit &&
                !le.getItemBySlot(EquipmentSlot.CHEST).isEmpty() &&
                !le.getItemBySlot(EquipmentSlot.CHEST).getAllEnchantments().containsKey(EnchantmentRegistrar.FORTYIFYING.get());
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (targetEntity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error6"));
            return;
        }
        if (targetEntity.getItemBySlot(EquipmentSlot.CHEST).getAllEnchantments().containsKey(EnchantmentRegistrar.FORTYIFYING.get())) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error4"));
            return;
        }
        ((EnchanterUnit) unitUsing).getCastEnchantProtectiveGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastEnchantProtectiveGoal().setTarget(targetEntity);
    }
}
