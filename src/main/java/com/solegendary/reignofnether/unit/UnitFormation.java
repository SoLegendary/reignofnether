package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class UnitFormation {

    // given a list of entities that were issued a move command to moveBp, find the best pairings of entities and
    // positions that allows each entity the best chance to reach said position without bumping into each other
    public static List<Pair<LivingEntity, BlockPos>> getUnitFormation(List<LivingEntity> entities, BlockPos moveBp) {
        List<Pair<LivingEntity, BlockPos>> formation = new ArrayList<>();

        return formation;
    }

}
