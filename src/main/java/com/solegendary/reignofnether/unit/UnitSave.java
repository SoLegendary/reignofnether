package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.resources.Resources;

public class UnitSave {

    public String name; // mob name, only used for debug logs
    public String ownerName;
    public int id;
    public Resources resources;

    public UnitSave(String name, String ownerName, int id, Resources resources) {
        this.name = name;
        this.ownerName = ownerName;
        this.id = id;
        this.resources = resources;
    }
}
