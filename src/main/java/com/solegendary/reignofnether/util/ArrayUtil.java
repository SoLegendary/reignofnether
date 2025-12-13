package com.solegendary.reignofnether.util;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class ArrayUtil {
    static public int[] livingEntityListToIdArray(List<LivingEntity> entities) {
        var ids = new int[entities.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = entities.get(i).getId();
        }
        return ids;
    }

    static public int[] intListToArray(List<Integer> list) {
        var ids = new int[list.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = list.get(i);
        }
        return ids;
    }
}
