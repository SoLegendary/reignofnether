package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.resources.Resources;

public class UnitSave {

    public String name;
    public String ownerName;
    public String uuid; // regular integer ID isn't consistent between server restarts
    public Resources resources;

    public UnitSave(String name, String ownerName, String uuid, Resources resources) {
        this.name = name;
        this.ownerName = ownerName;
        this.uuid = uuid;
        this.resources = resources;
    }
}
