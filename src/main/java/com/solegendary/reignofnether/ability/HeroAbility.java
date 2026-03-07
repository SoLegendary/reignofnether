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
import com.solegendary.reignofnether.unit.packets.UnitSyncAbilityClientboundPacket;
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

    public final int maxRank;
    public int manaCost = 0;

    public HeroAbility(int maxRank, UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        super(action, cooldownMax, range, radius, canTargetEntities);
        this.maxRank = maxRank;
        this.manaCost = 0;
    }

    public HeroAbility(int maxRank, int manaCost, UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        super(action, cooldownMax, range, radius, canTargetEntities);
        this.maxRank = maxRank;
        this.manaCost = manaCost;
    }

    public int getLevelRequirement(HeroUnit hero) {
        if (maxRank <= 1) {
           return 6;
        } else {
            return (getRank(hero) * 2) + 1;
        }
    }

    public boolean rankUp(HeroUnit hero) {
        if (getRank(hero) < maxRank && hero.getSkillPoints() > 0 && hero.getHeroLevel() >= getLevelRequirement(hero)) {
            setRank(hero, getRank(hero) + 1);
            hero.setSkillPoints(hero.getSkillPoints() - 1);
            if (((LivingEntity) hero).level().isClientSide)
                hero.updateAbilityButtons();
            else
                hero.syncToClients();
            return true;
        }
        return false;
    }
    
    public void updateStatsForRank(HeroUnit heroUnit) { }

    protected String rankString(HeroUnit hero) {
        return getRank(hero) > 0 ? I18n.get("abilities.reignofnether.rank", getRank(hero)) : I18n.get("abilities.reignofnether.unlearnt");
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of();
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of();
    }

    public AbilityButton getButton(Keybinding hotkey, Unit hero) {
        return null;
    }

    // rank up button for this specific ability
    public Button getRankUpButton(HeroUnit hero) {
        return null;
    }

    protected Button getRankUpButtonProtected(String name, ResourceLocation resourceLocation, HeroUnit hero) {
        Button button = new Button(name,
            14,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/corner_plus.png"),
            (Keybinding) null,
            () -> false,
            () -> !hero.isRankUpMenuOpen() || getRank(hero) >= maxRank,
            () -> hero.getSkillPoints() > 0 && hero.getHeroLevel() >= getLevelRequirement(hero),
            () -> {
                if (rankUp(hero)) {
                    AbilityServerboundPacket.rankUpAbility(((Entity) hero).getId(), action);
                    hero.updateAbilityButtons();
                }
                if (hero.getSkillPoints() <= 0)
                    hero.showRankUpMenu(false);
            },
            null,
            getRankUpTooltipLines(hero)
        );
        button.bgIconResource = resourceLocation;
        button.stretchIconToBorders = true;
        return button;
    }

    public static boolean allSkillsLearnt(HeroUnit hero) {
        if (hero.getHeroLevel() >= HeroUnit.MAX_LEVEL && hero.getSkillPoints() <= 0)
            return true;
        int totalSkillRanks = 0;
        for (HeroAbility ability : hero.getHeroAbilities()) {
            totalSkillRanks += ability.getRank(hero);
        }
        return totalSkillRanks >= 10;
    }

    // button that all heroes have to show ability level up options
    public static Button getRankUpMenuButton(HeroUnit hero) {
        Button menuButton = new Button("Rank up abilities",
            14,
            hero.isRankUpMenuOpen() ?
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross2.png") :
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/level_up.png"),
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

    public Style getLevelReqStyle(HeroUnit hero) {
        return Style.EMPTY.withColor(hero.getHeroLevel() >= getLevelRequirement(hero) ? 0x00FF00 : 0xFF0000);
    }

    public int getRank(HeroUnit hero) {
        return hero.getHeroAbilityRank(this);
    }

    public void setRank(HeroUnit hero, int rank) {
        hero.setHeroAbilityRank(this, rank);
    }
}
