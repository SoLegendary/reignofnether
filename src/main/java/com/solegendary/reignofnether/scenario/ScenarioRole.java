package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.resources.Resources;
import net.minecraft.nbt.CompoundTag;

// Role that a player can take in a scenario map
public class ScenarioRole {
    public final int index;
    public String name;
    public Faction faction = Faction.NEUTRAL;
    public Resources startingResources = new Resources("", 0,0,0);
    public int teamNumber;
    public boolean isNpc = false;

    CompoundTag nbt = new CompoundTag();

    public ScenarioRole(int index) {
        this.index = index;
        this.name = "Player " + (index + 1);
        this.teamNumber = (index + 1);
        packNbt();
    }

    public static ScenarioRole getFromSave(int index, CompoundTag tag) {
        ScenarioRole role = new ScenarioRole(index);
        role.nbt = tag;
        role.unpackNbt();
        return role;
    }

    public void packNbt() {
        nbt.putInt("index", this.index);
        nbt.putString("name", this.name);
        nbt.putString("faction", this.faction.name());
        nbt.putInt("startingFood", this.startingResources.food);
        nbt.putInt("startingWood", this.startingResources.wood);
        nbt.putInt("startingOre", this.startingResources.ore);
        nbt.putInt("teamNumber", this.teamNumber);
        nbt.putBoolean("isNpc", this.isNpc);
    }

    public void unpackNbt() {
        this.name = nbt.getString("name");
        this.faction = Faction.valueOf(nbt.getString("faction"));
        this.startingResources.food = nbt.getInt("startingFood");
        this.startingResources.wood = nbt.getInt("startingWood");
        this.startingResources.ore = nbt.getInt("startingOre");
        this.teamNumber = nbt.getInt("teamNumber");
        this.isNpc = nbt.getBoolean("isNpc");
    }
}
