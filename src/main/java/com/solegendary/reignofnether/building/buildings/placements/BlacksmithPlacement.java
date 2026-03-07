package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.ability.EquipAbility;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.RangeIndicator;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlacksmithPlacement extends ProductionPlacement implements RangeIndicator {
    public EquipAbility autoCastEquip = null;
    public BlacksmithPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override
    public String getUpgradedName() {
        return I18n.get("buildings.villagers.reignofnether.blacksmith.superior");
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        updateHighlightBps();
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (tickAgeAfterBuilt > 0 && tickAgeAfterBuilt % 15 == 0 && isBuilt && autoCastEquip != null
                && autoCastEquip.isOffCooldown(this)) {

            List<Mob> mobs = new ArrayList<>();
            for (Mob e : MiscUtil.getEntitiesWithinRange(new Vector3d(
                    this.centrePos.getX(),
                    this.centrePos.getY(),
                    this.centrePos.getZ()
                ),
                    autoCastEquip.range - 1,
                Mob.class,
                tickLevel
            )) {
                if ((autoCastEquip.isCorrectUnit(e) && autoCastEquip.canAfford(this)
                    && !autoCastEquip.hasItemInSlot(e)
                )) {
                    mobs.add(e);
                }
            }
            if (!mobs.isEmpty()) {
                autoCastEquip.use(tickLevel, this, mobs.get(0));
            }
        }
        if (tickLevel.isClientSide && tickAgeAfterBuilt > 0 && tickAgeAfterBuilt % 100 == 0)
            updateHighlightBps();
    }

    private final Set<BlockPos> borderBps = new HashSet<>();

    @Override
    public void updateHighlightBps() {
        if (!level.isClientSide())
            return;
        this.borderBps.clear();
        this.borderBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(centrePos,
                EquipAbility.RANGE - BlockClientEvents.VISIBLE_BORDER_ADJ, level));
    }

    @Override
    public Set<BlockPos> getHighlightBps() {
        return borderBps;
    }

    @Override
    public boolean showOnlyWhenSelected() {
        return true;
    }
}
