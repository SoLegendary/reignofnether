package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSource;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;

// data used mainly in GatherResourcesGoal for storing where workers go to gather resources
// also saved to level on server close and loaded on opening
public class TargetResourcesSave {

    public String unitUUID = ""; // only used for saveData to associate this with a worker unit
    public final ArrayList<BlockPos> todoGatherTargets = new ArrayList<>();
    @Nullable public BlockPos gatherTarget = null;
    public ResourceName targetResourceName = ResourceName.NONE;
    @Nullable public ResourceSource targetResourceSource = null;
    @Nullable public BuildingPlacement targetFarm = null;

    public boolean hasData() {
        return this.todoGatherTargets.size() > 0 ||
                this.gatherTarget != null ||
                this.targetResourceName != ResourceName.NONE ||
                this.targetResourceSource != null ||
                this.targetFarm != null;
    }
    public void delete() {
        this.todoGatherTargets.clear();
        this.gatherTarget = null;
        this.targetResourceName = ResourceName.NONE;
        this.targetResourceSource = null;
        this.targetFarm = null;
    }
}
