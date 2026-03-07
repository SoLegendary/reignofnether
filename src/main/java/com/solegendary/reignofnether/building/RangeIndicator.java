package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public interface RangeIndicator {
    public default void updateHighlightBps() {
        if (this instanceof Unit unit) {
            LivingEntity le = (LivingEntity) unit;
            if (le.level().isClientSide()) {
                setHighlightBps(new HashSet<>());
                for (Ability ability : unit.getAbilities().get()) {
                    if (CursorClientEvents.getLeftClickAction() == ability.action) {
                        setHighlightBps(MiscUtil.getRangeIndicatorCircleBlocks(le.blockPosition(),
                                (int) (ability.range - 1),
                                le.level()
                        ));
                    }
                }
            }
        } else if (this instanceof BuildingPlacement bpl) {
            if (bpl.level.isClientSide()) {
                setHighlightBps(new HashSet<>());
                for (Ability ability : bpl.getAbilities()) {
                    if (CursorClientEvents.getLeftClickAction() == ability.action) {
                        setHighlightBps(MiscUtil.getRangeIndicatorCircleBlocks(bpl.centrePos,
                                (int) (ability.range - 1),
                                bpl.level
                        ));
                    }
                }
            }
        }
    }
    public default void setHighlightBps(Set<BlockPos> bps) { }
    public Set<BlockPos> getHighlightBps();
    public default boolean showOnlyWhenSelected() {
        return true;
    }
}