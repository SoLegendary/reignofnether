package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

public class CallToArmsUnit extends Ability {

    public CallToArmsUnit() {
        super(
                UnitAction.CALL_TO_ARMS_UNIT,
                0,
                0,
                0,
                false,
                false
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                "Call To Arms (Building)",
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/bell.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> sendUnitCommand(UnitAction.CALL_TO_ARMS_UNIT),
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.call_to_arms_unit"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.call_to_arms_unit.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.call_to_arms_unit.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("abilities.reignofnether.call_to_arms_unit.tooltip3", TownCentre.MILITIA_RANGE), Style.EMPTY)
                ),
                this,
                unit
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (unitUsing instanceof VillagerUnit vUnit)
            vUnit.callToArmsGoal.setNearestTownCentreAsTarget();

        if (!level.isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.BELL, ((Mob) unitUsing).getOnPos(), 0.5f);
            CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(() -> {
                SoundClientboundPacket.playSoundAtPos(SoundAction.BELL, ((Mob) unitUsing).getOnPos(), 0.5f);
            });
        }
    }
}
