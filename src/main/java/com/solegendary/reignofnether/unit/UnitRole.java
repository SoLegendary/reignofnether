package com.solegendary.reignofnether.unit;

// What a unit is for, hand-written in every unit class (Unit.getUnitRoles) so the AI knows how to use and
// counter a new unit the moment it exists. Damage numbers come from the unit's own stats; roles say what
// those numbers can't (who it can reach, what it's good against, how it's countered).
public enum UnitRole {
    FRONTLINE, // melee or tank that holds the line
    RANGED,    // fights from range with projectiles (ranged damage resist applies)
    CASTER,    // its damage or value comes from spells and abilities
    HEALER,    // heals allies
    HARMING,   // its damage is harming potions: heals undead instead
    FLYER,     // flies: melee can't hit it
    ANTI_AIR,  // can hit flyers
    SIEGE,     // good against buildings
    AOE,       // splash or area damage: strong against clumped armies
    FAST,      // clearly faster than most units: raider, chaser
    SCOUT,
    WORKER,
    HERO,
    UNDEAD,    // healed by harming, immune to harming potions
    SUMMON     // created by an ability, never trained
}
