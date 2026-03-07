package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.neutral.Beacon;
import com.solegendary.reignofnether.building.buildings.neutral.CapturableBeacon;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.packets.BeaconSyncClientboundPacket;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class BeaconPlacement extends ProductionPlacement implements RangeIndicator {
    public BlockPos beaconPos;
    private MobEffect auraEffect = null;
    private boolean beaconActive = false;
    public BeaconPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        for (BuildingBlock bb : blocks)
            if (bb.getBlockState().getBlock() == Blocks.BEACON)
                beaconPos = bb.getBlockPos();
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        if (!(getBuilding() instanceof CapturableBeacon) && !ownerName.isBlank())
            sendWarning("destroy_warning");
    }

    @Override
    public String getUpgradedName() {
        String name = I18n.get("buildings.neutral.reignofnether.capturable_beacon");
        if (getAuraEffect() == MobEffects.LUCK)
            return name + " (" + I18n.get("ability.reignofnether.beacon_aura.wealth") + ")";
        else if (getAuraEffect() == MobEffects.DIG_SPEED)
            return name + " (" + I18n.get("ability.reignofnether.beacon_aura.haste") + ")";
        else if (getAuraEffect() == MobEffects.REGENERATION)
            return name + " (" + I18n.get("ability.reignofnether.beacon_aura.regeneration") + ")";
        else if (getAuraEffect() == MobEffects.DAMAGE_BOOST)
            return name + " (" + I18n.get("ability.reignofnether.beacon_aura.strength") + ")";
        else if (getAuraEffect() == MobEffects.DAMAGE_RESISTANCE)
            return name + " (" + I18n.get("ability.reignofnether.beacon_aura.resistance") + ")";
        else
            return name;
    }

    private Block getBeaconBlock() {
        return level.getBlockState(beaconPos).getBlock();
    }

    private BlockEntity getBeaconBlockEntity() {
        return level.getBlockEntity(beaconPos);
    }

    public MobEffect getAuraEffect() {
        if (getBeaconBlockEntity() != null)
            return auraEffect;
        return null;
    }
    public boolean isBeaconActive() { return auraEffect != null && beaconActive; }


    public static MobEffect getMobEffectForAction(UnitAction action) {
        return switch (action) {
            case BEACON_HASTE -> MobEffects.DIG_SPEED;
            case BEACON_REGENERATION -> MobEffects.REGENERATION;
            case BEACON_RESISTANCE -> MobEffects.DAMAGE_RESISTANCE;
            case BEACON_WEALTH -> MobEffects.LUCK;
            case BEACON_STRENGTH -> MobEffects.DAMAGE_BOOST;
            default -> null;
        };
    }

    public static UnitAction getActionForMobEffect(MobEffect effect) {
        if (effect == MobEffects.DIG_SPEED)
            return UnitAction.BEACON_HASTE;
        else if (effect == MobEffects.REGENERATION)
            return UnitAction.BEACON_REGENERATION;
        else if (effect == MobEffects.DAMAGE_RESISTANCE)
            return UnitAction.BEACON_RESISTANCE;
        else if (effect == MobEffects.LUCK)
            return UnitAction.BEACON_WEALTH;
        else if (effect == MobEffects.DAMAGE_BOOST)
            return UnitAction.BEACON_STRENGTH;
        return UnitAction.NONE;
    }

    public void activate(MobEffect effect) {
        beaconActive = true;
        auraEffect = effect;
        if (!level.isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_ACTIVATE, beaconPos);
            BeaconSyncClientboundPacket.syncBeacon(getActionForMobEffect(effect), originPos, true);
        }
    }

    public void deactivate() {
        beaconActive = false;
        auraEffect = null;
        if (!level.isClientSide()) {
            SoundClientboundPacket.playSoundAtPos(SoundAction.BEACON_DEACTIVATE, beaconPos);
            BeaconSyncClientboundPacket.syncBeacon(UnitAction.NONE, originPos, false);
        }
    }

    // serverside only
    public void setAuraEffect(MobEffect effect) {
        // turn off the beacon
        // after delay, turn on the beacon and change the effect
        if (isBeaconActive()) {
            deactivate();
            CompletableFuture.delayedExecutor(2500, TimeUnit.MILLISECONDS).execute(() -> {
                activate(effect);
            });
        } else {
            activate(effect);
        }
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);
        if (tickLevel.isClientSide && tickAgeAfterBuilt > 0 && tickAgeAfterBuilt % 100 == 0)
            updateHighlightBps();

        if (isBeaconActive() && tickAgeAfterBuilt > 0 && tickAgeAfterBuilt % 20 == 0 &&
                !this.level.isClientSide()) {

            List<LivingEntity> nearbyEntities = MiscUtil.getEntitiesWithinRange(
                    new Vector3d(this.centrePos.getX(), this.centrePos.getY(), this.centrePos.getZ()),
                    RANGE,
                    LivingEntity.class,
                    this.level);

            for (LivingEntity le : nearbyEntities) {
                boolean isOwnedOrFriendlyUnit = le instanceof Unit unit && (unit.getOwnerName().equals(this.ownerName) ||
                        AlliancesServerEvents.isAllied(this.ownerName, unit.getOwnerName()));
                boolean isFriendlyPlayer = le instanceof Player player && !player.isCreative() && !player.isSpectator() &&
                        (player.getName().getString().equals(ownerName) || AlliancesServerEvents.isAllied(player.getName().getString(), ownerName));

                if ((isOwnedOrFriendlyUnit || isFriendlyPlayer) &&
                        (isFriendlyPlayer || auraEffect != MobEffects.LUCK) &&
                        getBeaconBlockEntity() != null) {
                    if (auraEffect != MobEffects.REGENERATION) {
                        if (le instanceof WorkerUnit || auraEffect != MobEffects.DIG_SPEED) {
                            le.addEffect(new MobEffectInstance(auraEffect, 25, 0));
                        }
                    } else if (tickAgeAfterBuilt % 80 == 0) { // only 1hp/4s
                        le.addEffect(new MobEffectInstance(auraEffect, 60, 0));
                    }
                }
            }

            if (tickAgeAfterBuilt % 20 == 0 && getAuraEffect() == MobEffects.LUCK && isBeaconActive()) {
                ResourcesServerEvents.addSubtractResources(new Resources(this.ownerName, 1, 1, 1));
            }
        }
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        sendWarning("completed_warning");
    }

    public void sendWarning(String msg) {
        if (!level.isClientSide) {
            PlayerServerEvents.sendMessageToAllPlayersNoNewlines("");
            BeaconPlacement beacon = BuildingUtils.getBeacon(level.isClientSide);
            if (beacon != null && msg.equals("upgraded_warning")) {
                String upgradeName = "";
                if (beacon.getUpgradeLevel() == 1) upgradeName = Component.translatable("buildings.neutral.reignofnether.beacon.upgrade.beacon_level1").getString();
                else if (beacon.getUpgradeLevel() == 2) upgradeName = Component.translatable("buildings.neutral.reignofnether.beacon.upgrade.beacon_level2").getString();
                else if (beacon.getUpgradeLevel() == 3) upgradeName = Component.translatable("buildings.neutral.reignofnether.beacon.upgrade.beacon_level3").getString();
                else if (beacon.getUpgradeLevel() == 4) upgradeName = Component.translatable("buildings.neutral.reignofnether.beacon.upgrade.beacon_level4").getString();
                else if (beacon.getUpgradeLevel() == 5) upgradeName = Component.translatable("buildings.neutral.reignofnether.beacon.upgrade.beacon_level5").getString();

                PlayerServerEvents.sendMessageToAllPlayersNoNewlines("buildings.neutral.reignofnether.beacon.upgrade_warning",
                        true, ownerName, upgradeName, beacon.getUpgradeLevel(), Beacon.MAX_UPGRADE_LEVEL);
            } else {
                PlayerServerEvents.sendMessageToAllPlayersNoNewlines("buildings.neutral.reignofnether.beacon." + msg,
                        true, ownerName);
            }
            if (beacon != null && beacon.getUpgradeLevel() >= Beacon.MAX_UPGRADE_LEVEL && !msg.equals("destroy_warning")) {
                PlayerServerEvents.sendMessageToAllPlayersNoNewlines("buildings.neutral.reignofnether.beacon.time_to_win",
                        false, ownerName, PlayerServerEvents.getBeaconWinTime(ownerName));
            }
            PlayerServerEvents.sendMessageToAllPlayersNoNewlines("");
            if (SurvivalServerEvents.isEnabled() && !msg.equals("destroy_warning"))
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
            else
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ENEMY);
        }
    }

    public static final int RANGE = 40;
    private final Set<BlockPos> borderBps = new HashSet<>();

    private int getBorderRange() {
        return isBuilt && getUpgradeLevel() > 0 ? RANGE : 0;
    }

    @Override
    public void updateHighlightBps() {
        if (!level.isClientSide())
            return;
        this.borderBps.clear();
        this.borderBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(centrePos,
                getBorderRange() - BlockClientEvents.VISIBLE_BORDER_ADJ, level));
    }

    @Override
    public Set<BlockPos> getHighlightBps() {
        return borderBps;
    }

    @Override
    public boolean showOnlyWhenSelected() {
        return true;
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp) {
        BlockPos worldBp = relativeBp.offset(this.originPos);
        Block block = this.getLevel().getBlockState(worldBp).getBlock();
        Block blockAbove = this.getLevel().getBlockState(worldBp).getBlock();
        return block != Blocks.BEACON && blockAbove != Blocks.BEACON;
    }

    public void changeBeaconStructure(int structureLevel) {
        String newStructureName = switch (structureLevel) {
            case 1 -> Beacon.structureNameT1;
            case 2 -> Beacon.structureNameT2;
            case 3 -> Beacon.structureNameT3;
            case 4 -> Beacon.structureNameT4;
            case 5 -> Beacon.structureNameT5;
            default -> Beacon.structureName;
        };
        ArrayList<BuildingBlock> newBlocks = BuildingBlockData.getBuildingBlocksFromNbt(newStructureName, this.getLevel());
        setBlocks(getAbsoluteBlockData(newBlocks, this.getLevel(), originPos, rotation));
        super.refreshBlocks();
    }
}
