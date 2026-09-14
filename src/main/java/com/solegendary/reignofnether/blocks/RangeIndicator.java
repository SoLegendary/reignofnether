package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.FirewallShot;
import com.solegendary.reignofnether.ability.heroAbilities.wildfire.MoltenBomb;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public interface RangeIndicator {

    public default void updateHighlightBps(Level level) {
        if (level == null || !level.isClientSide()) return;

        float range = 0;
        float radius = 0;
        BlockPos bp = null;
        boolean showRangeLine = false;
        boolean showRadiusCircle = false;
        boolean showRangeCircle = false;

        if (this instanceof Unit unit) {
            for (Ability ability : unit.getAbilities().get()) {
                if (CursorClientEvents.getLeftClickAction() == ability.action) {
                    range = ability.range;
                    radius = ability.radius;
                    bp = ((LivingEntity) unit).getOnPos();
                    showRangeLine = ability.showRangeLine;
                    showRadiusCircle = ability.showRadiusCircle;
                    showRangeCircle = ability.showRangeCircle;
                }
            }
        } else if (this instanceof BuildingPlacement bpl) {
            for (Ability ability : bpl.getAbilities()) {
                if (CursorClientEvents.getLeftClickAction() == ability.action) {
                    range = ability.range;
                    radius = ability.radius;
                    bp = bpl.centrePos;
                    showRangeLine = ability.showRangeLine;
                    showRadiusCircle = ability.showRadiusCircle;
                    showRangeCircle = ability.showRangeCircle;
                }
            }
        } else if (this instanceof UnitItem unitItem) {
            if (ItemClientEvents.actionableUnitItem == unitItem && HudClientEvents.hudSelectedEntity != null) {
                range = unitItem.range;
                radius = unitItem.radius;
                bp = HudClientEvents.hudSelectedEntity.getOnPos();
                showRangeLine = unitItem.showRangeLine;
                showRadiusCircle = unitItem.showRadiusCircle;
                showRangeCircle = unitItem.showRangeCircle;
            }
        }
        Set<BlockPos> highlightBps = new HashSet<>();
        if (bp != null) {
            if (showRangeLine) {
                BlockPos limitedBp = MyMath.getXZRangeLimitedBlockPos(bp, CursorClientEvents.getPreselectedBlockPos(), range + 1);
                for (BlockPos pos : MiscUtil.getLine2D(bp, limitedBp))
                    highlightBps.add(MiscUtil.getHighestGroundBlock(level, pos).above());
            }
            if (showRadiusCircle) {
                BlockPos limitedBp = MyMath.getXZRangeLimitedBlockPos(bp, CursorClientEvents.getPreselectedBlockPos(), range + 1);
                highlightBps.addAll(MiscUtil.getRangeIndicatorFilledCircleBlocks(limitedBp, (int) radius - 1, level));
            }
            if (showRangeCircle) {
                highlightBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(bp, (int) (range - 1), level));
            }
        }
        setHighlightBps(highlightBps);
    }
    public default void setHighlightBps(Set<BlockPos> bps) { }
    public Set<BlockPos> getHighlightBps();
    public default boolean showOnlyWhenSelected() {
        return true;
    }
}