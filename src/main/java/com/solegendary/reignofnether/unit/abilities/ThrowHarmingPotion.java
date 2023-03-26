package com.solegendary.reignofnether.unit.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.Ability;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class ThrowHarmingPotion extends Ability {

    public static final int CD_MAX_SECONDS = 10;
    public static final int RANGE = 8;

    private final WitchUnit witchUnit;

    public ThrowHarmingPotion(WitchUnit witchUnit) {
        super(
                UnitAction.THROW_HARMING_POTION,
                CD_MAX_SECONDS * ResourceCosts.TICKS_PER_SECOND,
                RANGE,
                0
        );
        this.witchUnit = witchUnit;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey) {
        return new AbilityButton(
                "Harming Potion",
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/splash_potion_harming.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == UnitAction.THROW_HARMING_POTION,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(UnitAction.THROW_HARMING_POTION),
                null,
                List.of(
                        FormattedCharSequence.forward("Harming Potion", Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE006  3  " + "\uE004  " + CD_MAX_SECONDS + "s  \uE005  " + RANGE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("Throw a harming potion, dealing instant damage to any unit", Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        ((WitchUnit) unitUsing).throwPotion(new Vec3(targetBp.getX(), targetBp.getY(), targetBp.getZ()), Potions.HARMING);
        this.setToMaxCooldown();
    }
}
