package com.solegendary.reignofnether.ability.heroAbilities.piglin;

//Throws out a huge amount of random weapons and armour that your units automatically equip upon pickup
//All equipment has limited durability
//Greed is Good raises the amount of equipment thrown

import com.solegendary.reignofnether.ability.HeroAbility;
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

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class LootExplosion extends HeroAbility {

    private static final int CD_MAX_SECONDS = 240 * ResourceCost.TICKS_PER_SECOND;
    private static final int NUM_ITEMS = 10;
    private static final int NUM_ITEMS_PER_100_RESOURCES = 3;

    public LootExplosion() {
        super(1, UnitAction.LOOT_EXPLOSION, CD_MAX_SECONDS, 0, 0, false);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit hero) {
        return new AbilityButton("Loot Explosion",
                new ResourceLocation("minecraft", "textures/item/iron_chestplate.png"),
                hotkey,
                () -> false,
                () -> rank == 0,
                () -> true,
                () -> sendUnitCommand(UnitAction.LOOT_EXPLOSION),
                null,
                getTooltipLines((HeroUnit) hero),
                this,
                hero
        );
    }

    @Override
    public Button getRankUpButton(HeroUnit hero) {
        return super.getRankUpButtonProtected(
                "Loot Explosion",
                new ResourceLocation("minecraft", "textures/item/iron_chestplate.png"),
                hero
        );
    }

    public List<FormattedCharSequence> getTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.loot_explosion") + " " + rankString(), true),
                fcsIcons(I18n.get("abilities.reignofnether.loot_explosion.stats", CD_MAX_SECONDS / 20)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip3", NUM_ITEMS, NUM_ITEMS_PER_100_RESOURCES))
        );
    }

    public List<FormattedCharSequence> getRankUpTooltipLines(HeroUnit hero) {
        return List.of(
                fcs(I18n.get("abilities.reignofnether.loot_explosion"), true),
                fcs(I18n.get("abilities.reignofnether.level_req", getLevelRequirement()), getLevelReqStyle(hero)),
                fcs(""),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip1")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip2")),
                fcs(I18n.get("abilities.reignofnether.loot_explosion.tooltip3", NUM_ITEMS, NUM_ITEMS_PER_100_RESOURCES))
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
