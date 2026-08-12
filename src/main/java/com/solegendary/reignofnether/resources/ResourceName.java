package com.solegendary.reignofnether.resources;

// left_click_actions that server can take to clients
public enum ResourceName {
    FOOD,
    WOOD,
    ORE,
    NONE;

    public String langKey() {
        return "resources.reignofnether." + this.toString().toLowerCase();
    }
}
