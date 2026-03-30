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
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class MartialEnchantment extends AbstractEnchantment {

    public static final int RANGE = 10;

    public static final int CHARGES_RANK_1 = 2;
    public static final int CHARGES_RANK_2 = 3;
    public static final int CHARGES_RANK_3 = 4;

    public static final int CD_RANK_1 = 25;
    public static final int CD_RANK_2 = 20;
    public static final int CD_RANK_3 = 15;

    public static final int MANA_COST_RANK_1 = 40;
    public static final int MANA_COST_RANK_2 = 30;
    public static final int MANA_COST_RANK_3 = 20;

    public MartialEnchantment() {
        super(3, MANA_COST_RANK_1, UnitAction.MARTIAL_ENCHANTMENT, CD_RANK_1 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
        maxCharges = CHARGES_RANK_1;
        this.autocastEnableAction = UnitAction.MARTIAL_ENCHANTMENT_AUTOCAST_ENABLE;
        this.autocastDisableAction = UnitAction.MARTIAL_ENCHANTMENT_AUTOCAST_DISABLE;
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
        AbilityButton button = new AbilityButton("Martial Enchantment",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/martial_enchantment.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.MARTIAL_ENCHANTMENT || isAutocasting(hero),
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.MARTIAL_ENCHANTMENT),
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
                "Martial Enchantment",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/martial_enchantment.png"),
                hero
        );
        return button;
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.martial_enchantment") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.martial_enchantment.stats", cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.charges", maxCharges))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.martial_enchantment"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.tooltip2")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.rank1", CHARGES_RANK_1, CD_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.rank2", CHARGES_RANK_2, CD_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.martial_enchantment.rank3", CHARGES_RANK_3, CD_RANK_3), getRank(hero) == 2)
        );
    }

    @Nullable
    public static Enchantment getEnchantmentForUnit(LivingEntity unit) {
        if (unit instanceof MilitiaUnit militiaUnit)
            return militiaUnit.isUsingBow() ? Enchantments.POWER_ARROWS : Enchantments.SHARPNESS;
        if (unit instanceof VindicatorUnit)
            return EnchantmentRegistrar.BREACHING.get();
        if (unit instanceof PillagerUnit)
            return Enchantments.PIERCING;
        if (unit instanceof EvokerUnit)
            return EnchantmentRegistrar.ZEAL.get();
        if (unit instanceof SkeletonUnit || unit instanceof HeadhunterUnit)
            return Enchantments.POWER_ARROWS;
        if (unit instanceof BruteUnit)
            return Enchantments.SHARPNESS;
        return null;
    }

    @Override
    public List<EntityType<? extends Mob>> getAllowedMobTypes() {
        return List.of(
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.EVOKER_UNIT.get(),
                EntityRegistrar.SKELETON_UNIT.get(),
                EntityRegistrar.HEADHUNTER_UNIT.get(),
                EntityRegistrar.BRUTE_UNIT.get()
        );
    }

    @Override
    public boolean canEnchant(LivingEntity le) {
        return getAllowedMobTypes().contains(le.getType()) &&
                le instanceof Unit &&
                !le.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() &&
                !le.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(getEnchantmentForUnit(le));
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        if (!getAllowedMobTypes().contains(targetEntity.getType())) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error3"));
            return;
        }
        if (targetEntity.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error7"));
            return;
        }
        Enchantment enchantment = getEnchantmentForUnit(targetEntity);
        if (enchantment != null && targetEntity.getItemBySlot(EquipmentSlot.MAINHAND).getAllEnchantments().containsKey(enchantment)) {
            if (level.isClientSide())
                HudClientEvents.showTemporaryMessage(I18n.get("ability.reignofnether.enchant.error4"));
            return;
        }
        ((EnchanterUnit) unitUsing).getCastEnchantMartialGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastEnchantMartialGoal().setTarget(targetEntity);
    }
}
