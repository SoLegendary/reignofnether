package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.AbilityClientboundPacket;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchBlazeFirewall;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.BlazeUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class FirewallShot extends Ability {

    public static final int CD_MAX_SECONDS = 15;
    public static final int RANGE = 20;

    private final BlazeUnit blazeUnit;

    public FirewallShot(BlazeUnit blazeUnit) {
        super(UnitAction.SHOOT_FIREWALL, CD_MAX_SECONDS * ResourceCost.TICKS_PER_SECOND, RANGE, 0, true);
        this.blazeUnit = blazeUnit;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton("Fire Wall Shot",
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/fire.png"),
            hotkey,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.SHOOT_FIREWALL,
            () -> !ResearchClient.hasResearch(ResearchBlazeFirewall.itemName),
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.SHOOT_FIREWALL),
            null,
            List.of(FormattedCharSequence.forward(
                    I18n.get("abilities.reignofnether.fire_wall_shot"),
                    Style.EMPTY.withBold(true)
                ),
                FormattedCharSequence.forward(
                    I18n.get("abilities.reignofnether.fire_wall_shot.tooltip1", CD_MAX_SECONDS) + RANGE,
                    MyRenderer.iconStyle
                ),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.fire_wall_shot.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("abilities.reignofnether.fire_wall_shot.tooltip3"), Style.EMPTY)
            ),
            this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) {
        BlockPos bp = targetEntity.getOnPos();
        use(level, unitUsing, bp);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        this.blazeUnit.shootFirewallShot(targetBp);
        this.setToMaxCooldown();
        if (!level.isClientSide()) {
            AbilityClientboundPacket.sendSetCooldownPacket(this.blazeUnit.getId(), this.action, this.cooldownMax);
        }
    }
}
