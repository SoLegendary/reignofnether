package com.solegendary.reignofnether.alliance;

// what an owner lets another player use. Resources, research and population come with ownership itself
// (PlayerRoles.ownerOf), so co-commanders share those without a grant
public enum Share {
    VISION,  // see what the owner's units and buildings see
    CONTROL  // command the owner's units and buildings
}
