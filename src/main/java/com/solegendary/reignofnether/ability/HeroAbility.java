package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public abstract class HeroAbility extends Ability {

    // can be ranked up when the hero levels up
    // requires a HeroUnit to be passed

    public final HeroUnit hero;
    public int rank = 0; // 0 == not learnt
    public final int maxRank;
    public int manaCost = 0;

    public HeroAbility(HeroUnit hero, int maxRank, UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        super(action, ((Entity) hero).level(), cooldownMax, range, radius, canTargetEntities);
        this.hero = hero;
        this.maxRank = maxRank;
        this.manaCost = 0;
    }

    public HeroAbility(HeroUnit hero, int maxRank, int manaCost, UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        super(action, ((Entity) hero).level(), cooldownMax, range, radius, canTargetEntities);
        this.hero = hero;
        this.maxRank = maxRank;
        this.manaCost = manaCost;
    }

    public int getLevelRequirement() {
        if (maxRank <= 1) {
           return 6;
        } else {
            return (rank * 2) + 1;
        }
    }

    public boolean rankUp() {
        if (rank < maxRank && hero.getSkillPoints() > 0 && hero.getHeroLevel() >= getLevelRequirement()) {
            rank += 1;
            hero.setSkillPoints(hero.getSkillPoints() - 1);
            if (((LivingEntity) hero).level().isClientSide)
                ((Unit) hero).updateAbilityButtons();
            return true;
        }
        return false;
    }

    public void updateStatsForRank() { }

    protected String rankString() {
        return rank > 0 ? I18n.get("abilities.reignofnether.rank", rank) : I18n.get("abilities.reignofnether.unlearnt");
    }

    public List<FormattedCharSequence> getRankUpTooltipLines() {
        return List.of();
    }

    public List<FormattedCharSequence> getTooltipLines() {
        return List.of();
    }

    public AbilityButton getButton(Keybinding hotkey) {
        return null;
    }

    // rank up button for this specific ability
    public Button getRankUpButton() {
        return null;
    }

    protected Button getRankUpButtonProtected(String name, ResourceLocation resourceLocation) {
        Button button = new Button(name,
            14,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/corner_plus.png"),
            (Keybinding) null,
            () -> false,
            () -> !hero.isRankUpMenuOpen() || rank >= maxRank,
            () -> hero.getSkillPoints() > 0 && hero.getHeroLevel() >= getLevelRequirement(),
            () -> {
                if (rankUp()) {
                    AbilityServerboundPacket.rankUpAbility(((Entity) hero).getId(), action);
                    ((Unit) hero).updateAbilityButtons();
                }
                if (hero.getSkillPoints() <= 0)
                    hero.showRankUpMenu(false);
            },
            null,
            getRankUpTooltipLines()
        );
        button.bgIconResource = resourceLocation;
        return button;
    }

    public static boolean allSkillsLearnt(HeroUnit hero) {
        if (hero.getHeroLevel() >= HeroUnit.MAX_LEVEL && hero.getSkillPoints() <= 0)
            return true;
        int totalSkillRanks = 0;
        for (HeroAbility ability : hero.getHeroAbilities()) {
            totalSkillRanks += ability.rank;
        }
        return totalSkillRanks >= 10;
    }

    // button that all heroes have to show ability level up options
    public static Button getRankUpMenuButton(HeroUnit hero) {
        Button menuButton = new Button("Rank up abilities",
            14,
            hero.isRankUpMenuOpen() ?
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/cross.png") :
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
            Keybindings.keyU,
            () -> false,
            () -> allSkillsLearnt(hero) || !PlayerClientEvents.isRTSPlayer(),
            () -> true,
            () -> hero.showRankUpMenu(!hero.isRankUpMenuOpen()),
            null,
            List.of(fcs(I18n.get("abilities.reignofnether.rank_up_menu", hero.getSkillPoints()), true))
        );
        menuButton.isFlashing = () -> !hero.isRankUpMenuOpen() && hero.getSkillPoints() > 0;
        return menuButton;
    }

    public Style getLevelReqStyle() {
        return Style.EMPTY.withColor(hero.getHeroLevel() >= getLevelRequirement() ? 0x00FF00 : 0xFF0000);
    }
}
