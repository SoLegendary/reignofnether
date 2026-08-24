package com.solegendary.reignofnether.gamemode;

public enum GameMode {
    CLASSIC, // Standard RTS match, can also appear as King of the Beacon if a neutral capturable beacon exists
    SURVIVAL, // Wave survival - left click changes difficulty
    SANDBOX, // Enables mapmaker tools and neutral building placement
    SCENARIO, // Tailored map design from a sandbox player with custom starting and win conditions
    NONE // used for packets
}
