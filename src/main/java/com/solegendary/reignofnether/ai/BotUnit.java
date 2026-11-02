package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Everything the bot remembers about one of its own units, in one place. BotBrain keeps one per living unit
// and drops it when the unit dies, so no manager has to clean up after a dead unit.
final class BotUnit {

    enum Squad { NONE, MAIN, HOME, RAID }

    LivingEntity entity;
    long lastOrdered = -1000; // tick of our last order to it
    UnitAction lastAction;    // and what that order was, to drop exact repeats (BotBrain.order)
    long lastTarget;          // position (BlockPos.asLong) or entity id it was aimed at

    Squad squad = Squad.NONE;
    Squad lentFrom;           // a home or raid unit gone with the main army's wave: back to this squad after it (BotArmy)

    // workers
    boolean hunter;
    boolean builder;          // one of the base's builders (BotEconomy.builders)
    int preyId = -1;          // the animal a hunter was sent after
    long preySince;
    int fightingId = -1;      // a worker hitting back at this attacker (BotDefence.protectWorkers)
    BlockPos fightFrom;       // where that fight started: it doesn't chase far from there
    boolean called;           // called to arms, on its way to the Town Centre (BotDefence)
    Vec3 stillAt;             // watchdog: where it stood still, and since when
    long stillSince;

    // army
    int chasingId = -1;       // an intruder this unit was sent to hunt down (BotDefence.respond)
    boolean autocastsOn;      // researched autocasts switched on (BotMicro)
    Boolean witchHeals;       // a witch's potion: healing or harming (BotMicro), null until set

    // scouts
    BlockPos scoutTarget;
    long scoutTargetTick;

    // heroes
    boolean fleeing;
    float fleeBestHp;
    long fleeHealedTick;
    boolean fightsOn;         // came back without healing: no more running until it heals (BotHeroes.flee)

    BotUnit(LivingEntity entity) {
        this.entity = entity;
    }

    long sinceOrdered(long tick) {
        return tick - lastOrdered;
    }
}
