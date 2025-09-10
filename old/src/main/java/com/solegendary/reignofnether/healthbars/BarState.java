package com.solegendary.reignofnether.healthbars;

import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class BarState {

  BarStateType barStateType;
  public final int entityId;
  public float amount;
  public float previousAmountDisplay;
  public float previousAmountDelay;
  public float previousAmount;
  public float lastDelay;
  private float animationSpeed = 0;

  public enum BarStateType {
    HEALTH,
    MANA,
    ABSORB
  }

  private static final float AMOUNT_INDICATOR_DELAY = 10;

  public BarState(int entityId, BarStateType barStateType) {
    this.entityId = entityId;
    this.barStateType = barStateType;
  }

  public void tick() {
    ClientLevel level = Minecraft.getInstance().level;
    if (level == null)
      return;

    Entity entity = Minecraft.getInstance().level.getEntity(entityId);
    if (entity instanceof LivingEntity livingEntity) {

      if (entity instanceof HeroUnit heroUnit && barStateType == BarStateType.MANA) {
        amount = Math.min(heroUnit.getMana(), heroUnit.getMaxMana());
      } else if (barStateType == BarStateType.HEALTH) {
        amount = Math.min(livingEntity.getHealth(), livingEntity.getMaxHealth());
      } else if (barStateType == BarStateType.ABSORB) {
        amount = Math.min(livingEntity.getAbsorptionAmount(), MiscUtil.getMaxAbsorptionAmount(livingEntity));
      }
      incrementTimers();

      if (previousAmount < 0.1) {
        reset();

      } else if (previousAmount != amount) {
        handleChange();

      } else if (lastDelay == 0.0F) {
        reset();
      }
      updateAnimations();
    }
  }

  private void reset() {
    previousAmount = amount;
  }

  private void incrementTimers() {
    if (this.lastDelay > 0) {
      this.lastDelay--;
    }
    if (this.previousAmountDelay > 0) {
      this.previousAmountDelay--;
    }
  }

  private void handleChange() {
    lastDelay = AMOUNT_INDICATOR_DELAY * 2;
    previousAmount = amount;
  }

  private void updateAnimations() {
    if (previousAmountDelay > 0) {
      float diff = previousAmountDisplay - amount;
      if (diff > 0) {
        animationSpeed = diff / 10f;
      }
    } else if (previousAmountDelay < 1 && previousAmountDisplay > amount && animationSpeed > 0) {
      previousAmountDisplay -= animationSpeed;
    } else {
      previousAmountDisplay = amount;
      previousAmountDelay = AMOUNT_INDICATOR_DELAY;
    }
  }

}
