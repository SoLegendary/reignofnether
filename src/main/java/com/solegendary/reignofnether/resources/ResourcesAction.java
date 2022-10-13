package com.solegendary.reignofnether.resources;

// actions that server can take to clients
public enum ResourcesAction {
    SYNC, // replace Resources objects if existing or append to list if not
    ADD_SUBTRACT // add/subtract the given resources
}
