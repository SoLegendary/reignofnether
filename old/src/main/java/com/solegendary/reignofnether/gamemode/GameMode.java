package com.solegendary.reignofnether.gamemode;

public enum GameMode {
    CLASSIC, // Standard RTS match, can also appear as King of the Beacon if a neutral capturable beacon exists
    SURVIVAL, // Wave survival - left click changes difficulty
    SANDBOX, // Enables mapmaker tools and neutral building placement
    PERSISTENT, // Persistent world RTS mode
    NONE // used for packets
}
