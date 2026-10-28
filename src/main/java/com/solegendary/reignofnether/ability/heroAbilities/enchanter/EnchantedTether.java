package com.solegendary.reignofnether.ability.heroAbilities.enchanter;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class EnchantedTether extends HeroAbility {

    public static final int RANGE = 10;
    public static final int BREAK_RANGE = 15;

    public static final float DAMAGE_RESIST_MULTIPLIER = 0.5f;

    public static final int CHARGES_RANK_1 = 1;
    public static final int CHARGES_RANK_2 = 2;
    public static final int CHARGES_RANK_3 = 3;

    public static final int CD_RANK_1 = 30;
    public static final int CD_RANK_2 = 25;
    public static final int CD_RANK_3 = 20;
    public static final int DURATION_SECONDS = 30;

    public static final int MANA_COST_RANK_1 = 50;
    public static final int MANA_COST_RANK_2 = 40;
    public static final int MANA_COST_RANK_3 = 30;

    public EnchantedTether() {
        super(3, MANA_COST_RANK_1, UnitAction.ENCHANTED_TETHER, CD_RANK_1 * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
        maxCharges = CHARGES_RANK_1;
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
        AbilityButton button = new AbilityButton("Enchanted Tether",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/enchanted_tether.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.ENCHANTED_TETHER || isAutocasting(hero),
                () -> getRank(hero) == 0,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.ENCHANTED_TETHER),
                () -> toggleAutocast(hero),
                getTooltipLines(hero),
                this,
                hero
        );
        button.innerIconSizeModifier = 1;
        return button;
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        Button button = super.getRankUpButtonProtected(
                "Enchanted Tether",
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/enchanted_tether.png"),
                hero
        );
        return button;
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.enchanted_tether") + " " + rankString(hero), true),
                fcsIcons(I18n.get("abilities.reignofnether.enchanted_tether.stats", cooldownMax / 20, manaCost)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.tooltip2", DURATION_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.tooltip3")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.charges", maxCharges))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.enchanted_tether"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement(hero)), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.tooltip2", DURATION_SECONDS)),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.tooltip3")),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.rank1", CHARGES_RANK_1, CD_RANK_1), getRank(hero) == 0),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.rank2", CHARGES_RANK_2, CD_RANK_2), getRank(hero) == 1),
                fcs(I18n.get("abilities.reignofnether.enchanted_tether.rank3", CHARGES_RANK_3, CD_RANK_3), getRank(hero) == 2)
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        MobEffectInstance mei = targetEntity.getEffect(MobEffectRegistrar.ENCHANTED_TETHER.get());
        if (mei != null && mei.getDuration() > 200) {
            HudClientboundPacket.showTempMessageI18n(unitUsing.getOwnerName(), "abilities.reignofnether.enchanted_tether.error1");
            return;
        }
        if (!(targetEntity instanceof Unit unit) || !AlliancesServerEvents.isAlliedOrOwned(unitUsing.getOwnerName(), unit.getOwnerName()) || targetEntity instanceof HeroUnit) {
            HudClientboundPacket.showTempMessageI18n(unitUsing.getOwnerName(), "abilities.reignofnether.enchanted_tether.error2");
            return;
        }
        ((EnchanterUnit) unitUsing).getCastEnchantedTetherGoal().setAbility(this);
        ((EnchanterUnit) unitUsing).getCastEnchantedTetherGoal().setTarget(targetEntity);
    }
}
