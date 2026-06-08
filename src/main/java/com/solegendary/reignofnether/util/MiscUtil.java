package com.solegendary.reignofnether.util;


import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.BloodMoon;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.blocks.WraithSnowLayerBlock;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.nether.NetherBlocks;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.blocks.NightCircleMode;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.AbstractMeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.BoggedUnit;
import com.solegendary.reignofnether.unit.units.monsters.PhantomSummon;
import com.solegendary.reignofnether.unit.units.monsters.WraithUnit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.unit.units.villagers.WindcallerUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.resources.BlockUtils.isLeafBlock;
import static com.solegendary.reignofnether.resources.BlockUtils.isLogBlock;
import static net.minecraft.util.Mth.cos;
import static net.minecraft.util.Mth.sin;


public class MiscUtil {

    private static final Random RANDOM = new Random();

    private static final int[] DYE_COLORS = {
            0xF0F0F0, 0xEB8844, 0xC354CD, 0x6689D3,
            0xDECF2A, 0x41CD34, 0xD88198, 0x434343,
            0xABABAB, 0x287697, 0x7B2FBE, 0x253193,
            0x51301A, 0x3B511A, 0xB3312C, 0x1E1B1B
    };

    public static void shootFirework(Level level, Vec3 vec3) {
        CompoundTag explosion = new CompoundTag();
        int color = DYE_COLORS[level.random.nextInt(DYE_COLORS.length)];
        explosion.put("Colors", new IntArrayTag(new int[]{color}));
        explosion.putByte("Type", (byte) 0);
        ListTag explosions = new ListTag();
        explosions.add(explosion);
        CompoundTag explosionsAndFlight = new CompoundTag();
        explosionsAndFlight.put("Explosions", explosions);
        explosionsAndFlight.putByte("Flight", (byte) 0b1);
        CompoundTag fireworks = new CompoundTag();
        fireworks.put("Fireworks", explosionsAndFlight);
        ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
        itemStack.setTag(fireworks);
        FireworkRocketEntity entity = new FireworkRocketEntity(level, null, vec3.x, vec3.y(), vec3.z, itemStack);
        level.addFreshEntity(entity);
        entity.moveTo(vec3);
    }

    public static void doRandomFireworkExplosion(Level level, Vec3 vec3) {
        CompoundTag explosion = new CompoundTag();
        int color = DYE_COLORS[level.random.nextInt(DYE_COLORS.length)];
        explosion.put("Colors", new IntArrayTag(new int[]{color}));
        byte type = (byte) level.random.nextInt(5);
        explosion.putByte("Type", type);
        ListTag explosions = new ListTag();
        explosions.add(explosion);
        CompoundTag explosionsAndFlight = new CompoundTag();
        explosionsAndFlight.put("Explosions", explosions);
        explosionsAndFlight.putByte("Flight", (byte) 0b1);
        CompoundTag fireworks = new CompoundTag();
        fireworks.put("Fireworks", explosionsAndFlight);
        ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET);
        itemStack.setTag(fireworks);
        FireworkRocketEntity entity = new FireworkRocketEntity(level, null, vec3.x, vec3.y(), vec3.z, itemStack);
        level.addFreshEntity(entity);
        entity.moveTo(vec3);
        entity.lifetime = 0;
    }

    // prevent flying mobs from floating above trees and buildings (or they're effectively unreachable)
    // also used to move the camera Y pos up and down to prevent clipping inside of blocks
    public static boolean isGroundBlock(Level level, BlockPos bp) {
        BlockState bs = level.getBlockState(bp);
        if (isLogBlock(bs) || isLeafBlock(bs) || bs.isAir() ||
                BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), bp))
            return false;
        return true;
    }

    public static void addUnitCheckpoint(Unit unit, BlockPos blockPos, boolean green) {
        if (((Entity) unit).level().isClientSide()) {
            boolean clearExisting = !Keybindings.shiftMod.isDown();
            if (clearExisting)
                unit.getCheckpoints().clear();
            unit.getCheckpoints().add(new Checkpoint(blockPos, green));
        }
    }

    public static void addUnitCheckpoint(Unit unit, int id, boolean green) {
        Level level = ((Entity) unit).level();
        if (level.isClientSide() && !Keybindings.shiftMod.isDown()) {
            unit.getCheckpoints().clear();
            unit.getCheckpoints().add(new Checkpoint(level.getEntity(id), green));
        }
    }

    // returns a list of all BlockPos values between two points
    public static List<BlockPos> getLine2D(BlockPos start, BlockPos end) {
        List<BlockPos> result = new ArrayList<>();

        int x0 = start.getX();
        int z0 = start.getZ();
        int x1 = end.getX();
        int z1 = end.getZ();

        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);

        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        int err = dx - dz;

        while (true) {
            result.add(new BlockPos(x0, 0, z0));

            if (x0 == x1 && z0 == z1) break;

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z0 += sz;
            }
        }
        return result;
    }

    // excludes trees and buildings
    public static BlockPos getHighestGroundBlock(Level level, BlockPos blockPos) {
        int y = level.getHeight();
        BlockState bs;
        BlockPos bp;
        do {
            bp = new BlockPos(blockPos.getX(), y, blockPos.getZ());
            bs = level.getBlockState(bp);
            y -= 1;
        } while ((bs.isAir() ||
                BuildingUtils.isPosInsideAnyBuilding(level.isClientSide, bp) ||
                bs.getBlock() == Blocks.LIGHT ||
                bs.getBlock() == Blocks.STRUCTURE_VOID ||
                (!isSolidBlocking(level, bp) &&
                        bs.getFluidState().isEmpty()) ||
                bs.is(BlockTags.LEAVES) ||
                bs.is(BlockTags.LOGS) || bs.is(BlockTags.PLANKS)) && y > -63);
        return new BlockPos(blockPos.getX(), y, blockPos.getZ());
    }

    public static BlockPos getHighestNonAirBlock(Level level, BlockPos blockPos, boolean ignoreLeaves, boolean ignoreStructureVoid) {
        int y = level.getHeight();
        BlockState bs;
        BlockPos bp;
        do {
            bp = new BlockPos(blockPos.getX(), y, blockPos.getZ());
            bs = level.getBlockState(bp);
            if (!ignoreStructureVoid && bs.getBlock() == Blocks.STRUCTURE_VOID) {
                break;
            }
            y -= 1;
        } while ((bs.isAir() ||
                bs.getBlock() == Blocks.LIGHT ||
                (!isSolidBlocking(level, bp) && bs.getFluidState().isEmpty()) ||
                (ignoreLeaves && bs.is(BlockTags.LEAVES))) && y > -63);
        return new BlockPos(blockPos.getX(), y, blockPos.getZ());
    }

    public static BlockPos getHighestNonAirBlock(Level level, BlockPos blockPos, boolean ignoreLeaves) {
        return getHighestNonAirBlock(level, blockPos, ignoreLeaves, true);
    }

    public static BlockPos getHighestNonAirBlock(Level level, BlockPos blockPos) {
        return getHighestNonAirBlock(level, blockPos, false);
    }

    public static boolean listContainsObjectValue(List<Object> objs, String obj) {
        for (Object o : objs) {
            if (!o.equals(obj)) continue;
            return true;
        }
        return false;
    }

    public static boolean isLeftClickDown(Minecraft MC) {
        return GLFW.glfwGetMouseButton(MC.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
    }

    public static boolean isRightClickDown(Minecraft MC) {
        return GLFW.glfwGetMouseButton(MC.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;
    }

    // converts a 2d screen position to a 3d world position while in ortho view
    public static Vector3d screenPosToWorldPos(Minecraft MC, int mouseX, int mouseY) {
        if (MC.player == null) {
            return new Vector3d(0, 0, 0);
        }
        int winWidth = MC.getWindow().getGuiScaledWidth();
        int winHeight = MC.getWindow().getGuiScaledHeight();

        // at winHeight=240, zoom=10, screen is 20 blocks high, so PTB=240/20=24
        float pixelsToBlocks = winHeight / OrthoviewClientEvents.getZoom();

        // make mouse coordinate origin centre of screen
        float x = (mouseX - (float) winWidth / 2) / pixelsToBlocks;
        float y = 0;
        float z = (mouseY - (float) winHeight / 2) / pixelsToBlocks;

        double camRotYRads = Math.toRadians(OrthoviewClientEvents.getCamRotY());
        z = z / (float) (Math.sin(camRotYRads));

        Vec2 XZRotated = MyMath.rotateCoords(x, z, OrthoviewClientEvents.getCamRotX());

        // for some reason position is off by some y coord so just move it down manually
        return new Vector3d(
                MC.player.xo - XZRotated.x,
                MC.player.yo + y + 1.5f,
                MC.player.zo - XZRotated.y
        );
    }

    // distance to dropoff point but with more lenient Y range
    public static boolean isMobInRangeOfPos(BlockPos pos, LivingEntity mob, float range) {
        Vec2 pos2d = new Vec2(pos.getX() + 0.5f, pos.getZ() + 0.5f);
        Vec2 mob2d = new Vec2((float) mob.getX(), (float) mob.getZ());

        return pos.distToCenterSqr(mob.getX(), mob.getY(), mob.getZ()) < range * range ||
                (pos2d.distanceToSqr(mob2d) < range * range && (pos.getY() - mob.getY()) < 16);
    }

    // returns a random order of orthogonally adjacent blocks
    public static ArrayList<BlockPos> findAdjacentBlocks(BlockPos originPos, Predicate<BlockPos> condition) {
        ArrayList<BlockPos> adjBps = new ArrayList<>();
        ArrayList<BlockPos> retBps = new ArrayList<>();

        adjBps.add(originPos.above());
        adjBps.add(originPos.below());
        adjBps.add(originPos.north());
        adjBps.add(originPos.south());
        adjBps.add(originPos.east());
        adjBps.add(originPos.west());

        Collections.shuffle(adjBps);
        for (BlockPos bp : adjBps)
            if (condition.test(bp))
                retBps.add(bp);
        return retBps;
    }

    public static LivingEntity findClosestAttackableEntity(Mob unitMob, float range, ServerLevel level) {
        Vector3d unitPosition = new Vector3d(unitMob.position().x, unitMob.position().y, unitMob.position().z);
        var pos = new Vec3(unitPosition.x, unitPosition.y, unitPosition.z);
        boolean neutralAggro = unitMob.level().getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get();
        AABB aabb = new AABB(
                unitPosition.x - range,
                unitPosition.y - range,
                unitPosition.z - range,
                unitPosition.x + range,
                unitPosition.y + range,
                unitPosition.z + range
        );
        var entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
        entities.sort(Comparator.comparingDouble(
                it -> it.position().distanceTo(pos)
        ));

        // Determine priority effect filter for specific unit types
        Predicate<LivingEntity> priorityFilter = null;
        if (unitMob instanceof BoggedUnit) {
            priorityFilter = e -> !e.hasEffect(MobEffects.POISON);
        } else if (unitMob instanceof WraithUnit) {
            priorityFilter = e -> !e.hasEffect(MobEffectRegistrar.FEARFUL.get());
        } else if (unitMob instanceof WitherSkeletonUnit) {
            priorityFilter = e -> e.hasEffect(MobEffects.WITHER);
        } else if (unitMob instanceof WindcallerUnit) {
            priorityFilter = e -> !e.hasEffect(MobEffects.LEVITATION);
        }

        Vec3 unitVec = new Vec3(unitPosition.x, unitPosition.y, unitPosition.z);

        Predicate<LivingEntity> baseFilter = e ->
                e.position().distanceTo(unitVec) <= range &&
                e.level().getWorldBorder().isWithinBounds(e.blockPosition());

        // Try priority pass first, then fall back to base filter
        List<Predicate<LivingEntity>> passes = priorityFilter != null
                ? List.of(baseFilter.and(priorityFilter), baseFilter)
                : List.of(baseFilter);

        for (Predicate<LivingEntity> filter : passes) {
            for (LivingEntity entity : entities) {
                if (filter.test(entity) &&
                        isIdleOrMoveAttackable(unitMob, entity, neutralAggro) &&
                        hasLineOfSightForAttacks(unitMob, entity)) {
                    return entity;
                }
            }
        }
        return null;
    }

    // does not cover explicit attack commands
    private static boolean isIdleOrMoveAttackable(Mob unitMob, LivingEntity targetEntity, boolean neutralAggro) {
        Relationship rs = Relationship.NEUTRAL;
        if (unitMob instanceof Unit) {
            rs = UnitServerEvents.getUnitToEntityRelationship((Unit) unitMob, targetEntity);

            // don't aggro against blood moon enemies as a ghast so that buildings don't get friendly fired
            if (targetEntity instanceof Unit targetUnit &&
                    targetUnit.getOwnerName().equals(BloodMoon.ENEMY_NAME) &&
                    unitMob instanceof GhastUnit)
                return false;
        }

        if (targetEntity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return false;

        // If the relationship is FRIENDLY, do not allow the attack
        if (rs == Relationship.FRIENDLY)
            return false;

        // Prevents certain attacks based on specific unit and goal conditions
        if (targetEntity instanceof Unit unit &&
                unit.isFlyingUnit() &&
                unitMob instanceof AttackerUnit attackerUnit &&
                attackerUnit.getAttackGoal() instanceof AbstractMeleeAttackUnitGoal) {
            return false;
        }
        boolean isPassiveNonUnit = !(targetEntity instanceof Unit) &&
                (targetEntity instanceof Animal || targetEntity instanceof AbstractFish || targetEntity instanceof Villager);

        // Checks if neutral units can be attacked based on neutralAggro flag and other conditions
        boolean canAttackNeutral =
                rs == Relationship.NEUTRAL && neutralAggro &&
                        !(targetEntity instanceof Vex) &&
                        !(targetEntity instanceof ArmorStand) &&
                        !(targetEntity instanceof PhantomSummon) &&
                        !isPassiveNonUnit;

        return (rs == Relationship.HOSTILE || canAttackNeutral) &&
                targetEntity.getId() != unitMob.getId();
    }


    public static BuildingPlacement findClosestAttackableBuilding(Mob unitMob, float range, ServerLevel level) {
        List<BuildingPlacement> buildings = unitMob.level().isClientSide() ?
                BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        double closestDist = range;
        BuildingPlacement closestBuilding = null;

        for (BuildingPlacement building : buildings) {
            // Check if the building is attackable, taking into account the relationship
            if (isBuildingAutoAttackable(unitMob, building) && !(building.getBuilding() instanceof AbstractBridge)) {
                BlockPos attackPos = building.getClosestGroundPos(unitMob.blockPosition(), 1);
                double dist = Math.sqrt(unitMob.blockPosition().distSqr(attackPos));
                if (dist < closestDist) {
                    closestDist = dist;
                    closestBuilding = building;
                }
            }
        }
        return closestBuilding;
    }

    // neutral -> neutral ❌
    // owned -> neutral ✔ (if neutral aggro on)
    // neutral -> owned ✔ (if neutral aggro on)
    // owned -> owned ✔ (if hostile)
    private static boolean isBuildingAutoAttackable(Mob unitMob, BuildingPlacement building) {
        if (!building.isAttackable())
            return false;

        Relationship relationship = UnitServerEvents.getUnitToBuildingRelationship((Unit) unitMob, building);

        if (relationship == Relationship.FRIENDLY) {
            return false;
        }
        boolean neutralAggro = unitMob.level().getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get();

        if (relationship == Relationship.NEUTRAL && neutralAggro)
            return true;

        return relationship == Relationship.HOSTILE;
    }


    private static boolean hasLineOfSightForAttacks(Mob mob, LivingEntity targetEntity) {
        return mob.hasLineOfSight(targetEntity) || mob instanceof GhastUnit ||
                (mob instanceof Unit unit && GarrisonableBuildingAddon.getGarrison((Unit) mob) != null);
    }

    public static <T extends Entity> List<T> getEntitiesWithinRange(Vec3 pos, float range, Class<T> entityType, Level level) {
        return getEntitiesWithinRange(new Vector3d(pos.x, pos.y, pos.z), range, entityType, level);
    }

    public static <T extends Entity> List<T> getEntitiesWithinRange(Vector3d pos, float range, Class<T> entityType, Level level) {
        AABB aabb = new AABB(
                pos.x - range,
                pos.y - range,
                pos.z - range,
                pos.x + range,
                pos.y + range,
                pos.z + range
        );

        if (level != null) {
            List<T> entities = level.getEntitiesOfClass(entityType, aabb);
            List<T> entitiesInRange = new ArrayList<>();

            for (Entity entity : entities)
                if (entity.position().distanceTo(new Vec3(pos.x, pos.y, pos.z)) <= range &&
                        entity.level().getWorldBorder().isWithinBounds(entity.blockPosition()))
                    entitiesInRange.add((T) entity);

            return entitiesInRange;
        } else
            return new ArrayList<>();
    }

    // accepts a list of strings to draw at the top left to track debug data
    //MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
    //});
    public static void drawDebugStrings(GuiGraphics guiGraphics, Font font, String[] strings) {
        int y = 200 - (strings.length * 10);
        for (String str : strings) {
            guiGraphics.drawString(font, str, 0, y, 0xFFFFFF);
            y += 10;
        }
    }

    public static Relationship getClientsideRelationship(String playerName1, String playerName2) {
        if (playerName1.isEmpty() || playerName2.isEmpty())
            return Relationship.NEUTRAL;
        else if (playerName1.equals(playerName2))
            return Relationship.OWNED;
        else if (AlliancesClient.isAllied(playerName1, playerName2))
            return Relationship.FRIENDLY;
        else
            return Relationship.HOSTILE;
    }

    // lightens or darkens a hex RGB value
    public static int shadeHexRGB(int col, float mult) {
        int red = (col >> 16) & 0xFF;
        int green = (col >> 8) & 0xFF;
        int blue = (col) & 0xFF;

        if (mult > 1) { // prevent colours going > 255 (0xFF)
            red = Math.min(Math.round(red * mult), 0xFF);
            green = Math.min(Math.round(green * mult), 0xFF);
            blue = Math.min(Math.round(blue * mult), 0xFF);
        } else { // prevent colours going < 0
            red = Math.max(Math.round(red * mult), 0);
            green = Math.max(Math.round(green * mult), 0);
            blue = Math.max(Math.round(blue * mult), 0);
        }
        return (red << 16) | (green << 8) | (blue);
    }

    // convert col from RGB -> BGR (for some reason setPixelRGBA reads them backwards)
    public static int reverseHexRGB(int col) {
        int red = (col >> 16) & 0xFF;
        int green = (col >> 8) & 0xFF;
        int blue = (col) & 0xFF;

        return (blue << 16) | (green << 8) | (red);
    }

    // get a float that ranges between 0-1 (1 cycle per second) based on the system clock
    // used for oscillating an alpha value to make a rendered object pulse
    public static float getOscillatingFloat(double min, double max) {
        return getOscillatingFloat(min, max, 0);
    }

    public static float getOscillatingFloat(double min, double max, long timeOffset) {
        long ms = System.currentTimeMillis() + timeOffset;
        String msStr = String.valueOf(ms);
        String last3Digits = msStr.substring(msStr.length() - 3);
        double msOsc = (Math.abs(Double.parseDouble(last3Digits) - 500) / 250) - 1; // +-1 along linear scale
        msOsc = (Math.asin(msOsc) / Math.PI) + 0.5d; // 0-1 along sin scale
        msOsc *= (max - min);
        msOsc += min;
        return (float) msOsc;
    }

    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    // gets the unit vector in the direction of player facing (same as camera)
    // calcs from https://stackoverflow.com/questions/65897792/3d-vector-coordinates-from-x-and-y-rotation
    public static Vector3d getPlayerLookVector(Minecraft MC) {
        if (MC.player == null)
            return new Vector3d(0, 0, 0);
        float a = (float) Math.toRadians(MC.player.getYRot());
        float b = (float) Math.toRadians(MC.player.getXRot());
        return new Vector3d(-cos(b) * sin(a), -sin(b), cos(b) * cos(a));
    }

    // get the world position of the centre of the screen (as though the cursor was over it)
    public static Vec3 getOrthoviewCentreWorldPos(Minecraft MC) {
        Vector3d centrePosd = MiscUtil.screenPosToWorldPos(MC,
                MC.getWindow().getGuiScaledWidth() / 2,
                MC.getWindow().getGuiScaledHeight() / 2
        );
        Vector3d lookVector = MiscUtil.getPlayerLookVector(MC);
        Vector3d cursorWorldPosNear = MyMath.addVector3d(centrePosd, lookVector, -200);
        Vector3d cursorWorldPosFar = MyMath.addVector3d(centrePosd, lookVector, 200);
        return CursorClientEvents.getRefinedCursorWorldPos(cursorWorldPosNear, cursorWorldPosFar);
    }

    public static Set<BlockPos> getRangeIndicatorCircleBlocks(BlockPos centrePos, int radius, Level level) {
        return getRangeIndicatorCircleBlocks(centrePos, radius, level, false);
    }

    // get the tops of all blocks which are of at a certain horizontal distance away from the centrePos
    public static Set<BlockPos> getRangeIndicatorCircleBlocks(BlockPos centrePos, int radius, Level level, boolean isNightSource) {
        if (radius <= 0)
            return Set.of();

        Set<BlockPos> circleBps;
        if (BlockClientEvents.nightCircleMode == NightCircleMode.NO_OVERLAPS && isNightSource)
            circleBps = MiscUtil.CircleUtil.getCircleWithCulledOverlaps(centrePos, radius, BlockClientEvents.nightSourceOrigins);
        else
            circleBps = MiscUtil.CircleUtil.getCircle(centrePos, radius);

        return new HashSet<>(getHeightAdjustedBlockPoses(level, circleBps.stream().toList()));
    }

    // like getRangeIndicatorCircleBlocks but returns ALL blocks in the circle, not just on the edge
    public static Set<BlockPos> getRangeIndicatorFilledCircleBlocks(BlockPos centrePos, int radius, Level level) {
        if (radius <= 0)
            return Set.of();

        ArrayList<BlockPos> bps = new ArrayList<>();

        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                BlockPos bp = new BlockPos(centrePos.getX() + x, centrePos.getY(), centrePos.getZ() + z);
                if (bp.distToCenterSqr(centrePos.getX(), centrePos.getY(), centrePos.getZ()) < radius * radius) {
                    bps.add(bp);
                }
            }
        }
        return new HashSet<>(getHeightAdjustedBlockPoses(level, bps));
    }

    // given a 2d set of blockPoses, adjust them so they are of the topmost ground block within 3 blocks
    private static List<BlockPos> getHeightAdjustedBlockPoses(Level level, List<BlockPos> bps) {
        ArrayList<BlockPos> returnBps = new ArrayList<>();
        for (BlockPos bp : bps) {
            for (int i = 0; i < 3; i++) {
                int x = bp.getX();
                int z = bp.getZ();
                if (i == 1)
                    x += 1;
                else if (i == 2)
                    z += 1;

                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockPos topBp = new BlockPos(x, groundY, z);
                returnBps.add(topBp);

                int y = 1;
                if (level.getBlockState(topBp).getBlock() instanceof LeavesBlock) {
                    BlockPos bottomBp;
                    BlockState bs;
                    do {
                        bottomBp = topBp.offset(0, -y, 0);
                        bs = level.getBlockState(bottomBp);
                        y += 1;
                    } while (y < 30 && (bs.getBlock() instanceof LeavesBlock || !bs.isSolid()));
                    if (!level.getBlockState(bottomBp.above()).isSolid())
                        returnBps.add(bottomBp);
                }
            }
        }
        return returnBps;
    }

    public static class CircleUtil {

        private static final Map<Integer, Set<BlockPos>> circleCache = new HashMap<>();

        private static final int HASH_GRID_SIZE = 5;

        private static final Map<String, Set<BlockPos>> spatialHashMap = new HashMap<>();

        private static String getHashKey(BlockPos point) {
            int x = point.getX() / HASH_GRID_SIZE;
            int z = point.getZ() / HASH_GRID_SIZE;
            return x + ":" + z;
        }

        private static void addPointToSpatialHashMap(BlockPos point, String hashKey) {
            spatialHashMap.putIfAbsent(hashKey, new HashSet<>());
            spatialHashMap.get(hashKey).add(point);
        }

        public static Set<BlockPos> getCircleWithCulledOverlaps(BlockPos center, int radius, List<Pair<BlockPos, Integer>> overlapSources) {
            if (radius <= 0)
                return new HashSet<>();

            // skip rendering entirely if we are fully inside another circle
            if (BlockClientEvents.nightCircleMode == NightCircleMode.NO_OVERLAPS) {
                for (Pair<BlockPos, Integer> os : overlapSources) {
                    Vec2 centre1 = new Vec2(center.getX(), center.getZ());
                    Vec2 centre2 = new Vec2(os.getFirst().getX(), os.getFirst().getZ());
                    int overlapRange = os.getSecond();
                    if (!center.equals(os.getFirst()) && radius < overlapRange && centre1.distanceToSqr(centre2) < radius * radius)
                        return Set.of();
                }
            }
            Set<BlockPos> circleBps = getCircle(center, radius);

            for (Pair<BlockPos, Integer> os : overlapSources) {
                circleBps.removeIf(bp -> {
                    Vec2 centre1 = new Vec2(bp.getX(), bp.getZ());
                    Vec2 centre2 = new Vec2(os.getFirst().getX(), os.getFirst().getZ());
                    int range = os.getSecond();
                    return !center.equals(os.getFirst()) && centre1.distanceToSqr(centre2) < range * range;
                });
            }
            return circleBps;
        }

        public static Set<BlockPos> getCircle(BlockPos center, int radius) {
            if (radius <= 0)
                return new HashSet<>();

            if (!circleCache.containsKey(radius)) {
                circleCache.put(radius, computeCircleEdge(radius));
            }

            Set<BlockPos> cachedCircle = circleCache.get(radius);
            Set<BlockPos> translatedCircle = new HashSet<>(cachedCircle.size());

            int cx = center.getX();
            int cy = center.getY();
            int cz = center.getZ();

            for (BlockPos pos : cachedCircle) {
                translatedCircle.add(new BlockPos(cx + pos.getX(), cy, cz + pos.getZ()));
            }

            return translatedCircle;
        }

        private static Set<BlockPos> computeCircleEdge(int radius) {
            Set<BlockPos> circleBlocks = new HashSet<>(8 * radius);

            int x = radius;
            int z = 0;
            int decisionOver2 = 1 - x;

            while (x >= z) {
                addSymmetricPoints(circleBlocks, x, z);
                z++;
                if (decisionOver2 <= 0) {
                    decisionOver2 += 2 * z + 1;
                } else {
                    x--;
                    decisionOver2 += 2 * (z - x) + 1;
                }
            }

            return circleBlocks;
        }

        private static void addSymmetricPoints(Set<BlockPos> circleBlocks, int x, int z) {
            circleBlocks.add(new BlockPos(x, 0, z));
            circleBlocks.add(new BlockPos(-x, 0, z));
            circleBlocks.add(new BlockPos(x, 0, -z));
            circleBlocks.add(new BlockPos(-x, 0, -z));

            if (x != z) {
                circleBlocks.add(new BlockPos(z, 0, x));
                circleBlocks.add(new BlockPos(-z, 0, x));
                circleBlocks.add(new BlockPos(z, 0, -x));
                circleBlocks.add(new BlockPos(-z, 0, -x));
            }
        }
    }

    public static FormattedCharSequence fcs(String string) {
        return FormattedCharSequence.forward(string, Style.EMPTY);
    }

    public static FormattedCharSequence fcsIcons(String string) {
        return FormattedCharSequence.forward(string, MyRenderer.iconStyle);
    }

    public static FormattedCharSequence fcs(String string, boolean bold) {
        return FormattedCharSequence.forward(string, bold ? Style.EMPTY.withBold(true) : Style.EMPTY);
    }

    public static FormattedCharSequence fcs(String string, Style style) {
        return FormattedCharSequence.forward(string, style);
    }

    public static boolean isSolidBlocking(Level level, BlockPos bp) {
        BlockState bs = level.getBlockState(bp);
        return !bs.getCollisionShape(level, bp).isEmpty() && bs.isSolid();
    }

    public static String capitaliseAndSpace(String str) {
        String spacedStr = str.replace('_', ' ');
        spacedStr = spacedStr.replace('-', ' ');
        spacedStr = spacedStr.replace('.', ' ');
        return WordUtils.capitalize(spacedStr);
    }

    public static float getMaxAbsorptionAmount(LivingEntity entity) {
        MobEffectInstance mei = entity.getEffect(MobEffects.ABSORPTION);
        if (mei != null) {
            return (mei.getAmplifier() + 1) * 4.0f;
        }
        return entity.getAbsorptionAmount();
    }

    // eg. Zombie -> entity.reignofnether.zombie
    public static String getEntityIconName(Entity entity) {
        return entity.getType().getDescriptionId()
                .replace("entity.reignofnether.", "")
                .replace("_unit", "")
                .toLowerCase();
    }

    // eg. Zombie
    public static String getSimpleEntityName(Entity entity) {
        if (entity instanceof PhantomSummon)
            return "Phantom";

        if (entity instanceof Unit) {
            if (entity.hasCustomName()) {
                return entity.getType()
                        .getDescription()
                        .getString();
            } else {
                return entity.getName()
                        .getString();
            }
        } else if (entity != null) {
            return entity.getName().getString().toLowerCase();
        }
        return "";
    }

    public static boolean isOnNetherTerrain(LivingEntity le) {
        if (le instanceof Unit unit && unit.isFlyingUnit()) {
            BlockPos groundPos = getHighestNonAirBlock(le.level(), le.getOnPos(), false);
            return NetherBlocks.isNetherBlock(le.level(), groundPos);
        }
        return (le.getVehicle() != null && NetherBlocks.isNetherBlock(le.level(), le.getVehicle().getOnPos())) ||
                (NetherBlocks.isNetherBlock(le.level(), le.getOnPos()));
    }

    public static void runServerCommand(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(),
                command
        );
    }

    public static void runPlayerCommand(ServerPlayer player, String command) {
        player.server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack(),
                command
        );
    }

    public static void addParticleExplosion(SimpleParticleType particleType, int amount, Level level, Vec3 pos) {
        addParticleExplosion(particleType, amount, level, pos, 0.2f);
    }

    public static void addParticleExplosion(SimpleParticleType particleType, int amount, Level level, Vec3 pos, double velocityScale) {
        RandomSource rand = RandomSource.create();
        for (int j = 0; j < amount; ++j) {
            double d0 = rand.nextGaussian() * velocityScale;
            double d1 = rand.nextGaussian() * velocityScale;
            double d2 = rand.nextGaussian() * velocityScale;
            if (level.isClientSide()) {
                level.addParticle(particleType, pos.x, pos.y, pos.z, d0, d1, d2);
            } else {
                ((ServerLevel) level).sendParticles(particleType, pos.x, pos.y, pos.z, 1, d0, d1, d2, 0);
            }
        }
    }

    // called for flying windcallers and levitating mobs
    public static void spawnFlyingCloudParticles(Entity entity) {
        double px = entity.getX();
        double py = entity.getY();
        double pz = entity.getZ();

        // Spawn a loose ring of cloud puffs around the feet
        int numPuffs = 1;
        for (int i = 0; i < numPuffs; i++) {
            double angle = (entity.tickCount * 0.25 + (Math.PI * 2.0 / numPuffs) * i) % (Math.PI * 2.0);
            double radius = 0.3 + RANDOM.nextDouble() * 0.2;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = -0.1 + RANDOM.nextDouble() * 0.1; // slightly below/at foot level

            // Gentle upward and outward drift
            double vx = ox * 0.015;
            double vy = 0.005 + RANDOM.nextDouble() * 0.01;
            double vz = oz * 0.015;

            entity.level().addParticle(
                    ParticleTypes.CLOUD,
                    px + ox, py + oy, pz + oz,
                    vx, vy, vz
            );
        }

        // Occasional extra wisp for density variation
        if (entity.tickCount % 10 == 0) {
            double ox = (RANDOM.nextDouble() - 0.5) * 0.5;
            double oz = (RANDOM.nextDouble() - 0.5) * 0.5;
            entity.level().addParticle(
                    ParticleTypes.CLOUD,
                    px + ox, py - 0.05, pz + oz,
                    0, 0.008, 0
            );
        }
    }


    public static ResourceLocation getTextureForBlock(@NotNull Block block) {
        if (block == Blocks.COMMAND_BLOCK)
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png");
        BlockState defaultState = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(defaultState);
        TextureAtlasSprite sprite = model.getParticleIcon();
        String path = sprite.contents().name().getPath();
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/" + path + ".png");
    }

    public static boolean canWearChristmasHat(LivingEntity entity) {
        boolean isFarmer = entity instanceof VillagerUnit vUnit && vUnit.getUnitProfession() == VillagerUnitProfession.FARMER;
        return List.of(
                EntityRegistrar.VILLAGER_UNIT.get(),
                EntityRegistrar.VINDICATOR_UNIT.get(),
                EntityRegistrar.EVOKER_UNIT.get(),
                EntityRegistrar.MILITIA_UNIT.get(),
                EntityRegistrar.PILLAGER_UNIT.get(),
                EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get(),
                EntityRegistrar.ZOMBIE_UNIT.get(),
                EntityRegistrar.HUSK_UNIT.get(),
                EntityRegistrar.DROWNED_UNIT.get(),
                EntityRegistrar.SKELETON_UNIT.get(),
                EntityRegistrar.STRAY_UNIT.get(),
                EntityRegistrar.BOGGED_UNIT.get(),
                EntityRegistrar.GRUNT_UNIT.get(),
                EntityRegistrar.BRUTE_UNIT.get(),
                EntityRegistrar.HEADHUNTER_UNIT.get(),
                EntityRegistrar.WITHER_SKELETON_UNIT.get()
        ).contains(entity.getType()) && !entity.hasItemInSlot(EquipmentSlot.HEAD) && !isFarmer;
    }

    public static boolean isChristmasSeason() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DATE) >= 19 && calendar.get(Calendar.DATE) <= 26;
    }

    public static boolean isNewYearsSeason() {
        Calendar calendar = Calendar.getInstance();
        return (calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DATE) == 31) ||
                (calendar.get(Calendar.MONTH) + 1 == 1 && calendar.get(Calendar.DATE) == 1);
    }

    public static ResourceLocation getFactionIcon(Faction faction) {
        return switch (faction) {
            case VILLAGERS -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
            case MONSTERS -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png");
            case PIGLINS -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png");
            case NONE, NEUTRAL -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/sheep.png");
        };
    }

    public static boolean isConnected() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static boolean isSnowLayerBlock(Block block) {
        return block instanceof WraithSnowLayerBlock ||
                block instanceof SnowLayerBlock;
    }

    public static String getFactionName(Faction faction) {
        return I18n.get(String.format("hud.faction.reignofnether.%s", faction.toString().toLowerCase()));
    }

    private record ColorEntry(int mapColorId, int hex, String englishName) {}

    private static final ColorEntry[] COLOR_ENTRIES = {
        // default
        new ColorEntry(MapColor.SNOW.id,                0xE9ECEC, "white"),

        new ColorEntry(MapColor.COLOR_BLACK.id,         0x141519, "black"),
        new ColorEntry(MapColor.COLOR_BLUE.id,          0x35399D, "blue"),
        new ColorEntry(MapColor.COLOR_BROWN.id,         0x724728, "brown"),
        new ColorEntry(MapColor.COLOR_CYAN.id,          0x158991, "cyan"),
        new ColorEntry(MapColor.COLOR_GRAY.id,          0x3E4447, "gray"),
        new ColorEntry(MapColor.COLOR_GREEN.id,         0x546D1B, "green"),
        new ColorEntry(MapColor.COLOR_LIGHT_BLUE.id,    0x3AAFD9, "light_blue"),
        new ColorEntry(MapColor.COLOR_LIGHT_GRAY.id,    0x8E8E86, "light_gray"),
        new ColorEntry(MapColor.COLOR_LIGHT_GREEN.id,   0x70B919, "lime"),
        new ColorEntry(MapColor.COLOR_MAGENTA.id,       0xBD44B3, "magenta"),
        new ColorEntry(MapColor.COLOR_ORANGE.id,        0xF07613, "orange"),
        new ColorEntry(MapColor.COLOR_PINK.id,          0xED8DAC, "pink"),
        new ColorEntry(MapColor.COLOR_PURPLE.id,        0x792AAC, "purple"),
        new ColorEntry(MapColor.COLOR_RED.id,           0xA12722, "red"),
        new ColorEntry(MapColor.COLOR_YELLOW.id,        0xF8C627, "yellow"),
    };

    private static final Map<Integer, ColorEntry> COLOR_MAP = new HashMap<>() {};

    static {
        for (ColorEntry e : COLOR_ENTRIES) {
            COLOR_MAP.put(e.mapColorId, e);
            COLOR_MAP.put(e.hex, e);
        }
    }

    public static String getColorName(int colorIdOrHex, boolean english) {
        ColorEntry entry = COLOR_MAP.getOrDefault(colorIdOrHex, COLOR_ENTRIES[0]);
        return english ? entry.englishName : String.format("color.reignofnether.%s", entry.englishName);
    }
}
