package com.solegendary.reignofnether.building.production;

public enum ProdDupeRule {
    DISALLOW, // no duplicates allowed anywhere, eg. one-off global research
    DISALLOW_FOR_BUILDING, // no duplicates for one building, eg. building upgrades
    ALLOW // duplicates always allowed, eg. unit training
}
