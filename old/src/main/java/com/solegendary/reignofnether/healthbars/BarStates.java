package com.solegendary.reignofnether.healthbars;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class BarStates {

  private static final Map<Integer, BarState> HEALTH_STATES = new HashMap<>();
  private static final Map<Integer, BarState> ABSORB_STATES = new HashMap<>();
  private static final Map<Integer, BarState> MANA_STATES = new HashMap<>();
  private static int tickCount = 0;

  public static BarState getState(LivingEntity entity, BarState.BarStateType barStateType) {
    int id = entity.getId();

    BarState state;
    if (barStateType == BarState.BarStateType.HEALTH) {
      state = HEALTH_STATES.get(id);
      if (state == null) {
        state = new BarState(id, barStateType);
        HEALTH_STATES.put(id, state);
      }
    } else if (barStateType == BarState.BarStateType.ABSORB) {
      state = ABSORB_STATES.get(id);
      if (state == null) {
        state = new BarState(id, barStateType);
        ABSORB_STATES.put(id, state);
      }
    } else {
      state = MANA_STATES.get(id);
      if (state == null) {
        state = new BarState(id, barStateType);
        MANA_STATES.put(id, state);
      }
    }
    return state;
  }

  public static void tick() {
    for (BarState state : HEALTH_STATES.values()) {
      state.tick();
    }
    for (BarState state : ABSORB_STATES.values()) {
      state.tick();
    }
    for (BarState state : MANA_STATES.values()) {
      state.tick();
    }

    if (tickCount % 200 == 0) {
      cleanCache();
    }
    tickCount++;
  }

  private static void cleanCache() {
    HEALTH_STATES.entrySet().removeIf(BarStates::stateExpired);
    ABSORB_STATES.entrySet().removeIf(BarStates::stateExpired);
    MANA_STATES.entrySet().removeIf(BarStates::stateExpired);
  }

  private static boolean stateExpired(Map.Entry<Integer, BarState> entry) {
    if (entry.getValue() == null) {
      return true;
    }
    Minecraft MC = Minecraft.getInstance();

    if (MC.level != null) {
      Entity entity = MC.level.getEntity(entry.getKey());
      if (entity == null)
        return true;
      return !entity.isAlive();
    }
    return false;
  }

}
