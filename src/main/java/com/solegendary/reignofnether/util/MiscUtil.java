package com.solegendary.reignofnether.util;


import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.time.NightCircleMode;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.AbstractMeleeAttackUnitGoal;
import com.solegendary.reignofnether.unit.goals.FlyingMoveToTargetGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.PhantomSummon;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.text.WordUtils;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.resources.BlockUtils.isLeafBlock;
import static com.solegendary.reignofnether.resources.BlockUtils.isLogBlock;
import static net.minecraft.util.Mth.cos;
import static net.minecraft.util.Mth.sin;

public class MiscUtil {

    public static void shootFirework(Level level, Vec3 vec3) {
        CompoundTag explosion = new CompoundTag();
        explosion.put("Colors", new IntArrayTag(new int[]{0xF0F0F0}));
        explosion.putByte("Type", (byte) 0b0);
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

    // prevent flying mobs from floating above trees and buildings (or they're effectively unreachable)
    // also used to move the camera Y pos up and down to prevent clipping inside of blocks
    public static boolean isGroundBlock(Level level, BlockPos bp) {
        BlockState bs = level.getBlockState(bp);
        Block block = bs.getBlock();
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

    // excludes trees and buildings
    public static BlockPos getHighestGroundBlock(Level level, BlockPos blockPos) {
        int y = level.getHeight();
        BlockState bs;
        BlockPos bp;
        do {
            bp = new BlockPos(blockPos.getX(), y, blockPos.getZ());
            bs = level.getBlockState(bp);
            y -= 1;
        } while((bs.isAir() ||
                BuildingUtils.isPosInsideAnyBuilding(level.isClientSide, bp) ||
                bs.getBlock() == Blocks.LIGHT ||
                bs.getBlock() == Blocks.STRUCTURE_VOID ||
                (!isSolidBlocking(level, bp) &&
                bs.getFluidState().isEmpty()) ||
                bs.is(BlockTags.LEAVES) ||
                bs.is(BlockTags.LOGS) || bs.is(BlockTags.PLANKS)) && y > -63);
        return new BlockPos(blockPos.getX(), y, blockPos.getZ());
    }

    public static BlockPos getHighestNonAirBlock(Level level, BlockPos blockPos, boolean ignoreLeaves) {
        int y = level.getHeight();
        BlockState bs;
        BlockPos bp;
        do {
            bp = new BlockPos(blockPos.getX(), y, blockPos.getZ());
            bs = level.getBlockState(bp);
            y -= 1;
        } while((bs.isAir() ||
                bs.getBlock() == Blocks.LIGHT ||
                bs.getBlock() == Blocks.STRUCTURE_VOID ||
                (!isSolidBlocking(level, bp) && bs.getFluidState().isEmpty()) ||
                (ignoreLeaves && bs.is(BlockTags.LEAVES))) && y > -63);
        return new BlockPos(blockPos.getX(), y, blockPos.getZ());
    }
    public static BlockPos getHighestNonAirBlock(Level level, BlockPos blockPos) {
        return getHighestNonAirBlock(level, blockPos, false);
    }

    public static boolean listContainsObjectValue(List<Object> objs, String obj){
        return objs.stream().anyMatch(o -> o.equals(obj));
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
            return new Vector3d(0,0,0);
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
        List<LivingEntity> nearbyEntities = MiscUtil.getEntitiesWithinRange(unitPosition, range, LivingEntity.class, level);

        double closestDist = range;
        LivingEntity closestTarget = null;
        boolean neutralAggro = unitMob.level().getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get();

        for (LivingEntity tle : nearbyEntities) {
            if (isIdleOrMoveAttackable(unitMob, tle, neutralAggro) && hasLineOfSightForAttacks(unitMob, tle)) {
                double dist = unitMob.position().distanceTo(tle.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = tle;
                }
            }
        }
        return closestTarget;
    }

    // does not cover explicit attack commands
    private static boolean isIdleOrMoveAttackable(Mob unitMob, LivingEntity targetEntity, boolean neutralAggro) {
        Relationship rs = Relationship.NEUTRAL;
        if (unitMob instanceof Unit)
            rs = UnitServerEvents.getUnitToEntityRelationship((Unit) unitMob, targetEntity);

        if (targetEntity instanceof Player player && (player.isCreative() || player.isSpectator()))
            return false;

        // If the relationship is FRIENDLY, do not allow the attack
        if (rs == Relationship.FRIENDLY)
            return false;

        // Prevents certain attacks based on specific unit and goal conditions
        if (targetEntity instanceof Unit unit &&
                unit.getMoveGoal() instanceof FlyingMoveToTargetGoal &&
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
            if (isBuildingAttackable(unitMob, building) && !(building instanceof BridgePlacement)) {
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
    private static boolean isBuildingAttackable(Mob unitMob, BuildingPlacement building) {
        Relationship relationship = UnitServerEvents.getUnitToBuildingRelationship((Unit) unitMob, building);

        if (relationship == Relationship.FRIENDLY) {
            return false;
        }
        boolean neutralAggro = unitMob.level().getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get();
        boolean neutralUnit = ((Unit) unitMob).getOwnerName().isBlank();

        if (relationship == Relationship.NEUTRAL && neutralAggro)
            return true;

        return relationship == Relationship.HOSTILE;
    }


    private static boolean hasLineOfSightForAttacks(Mob mob, LivingEntity targetEntity) {
        return mob.hasLineOfSight(targetEntity) || mob instanceof GhastUnit ||
                (mob instanceof Unit unit && GarrisonableBuilding.getGarrison((Unit) mob) != null);
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
        }
        else
            return new ArrayList<>();
    }

    // accepts a list of strings to draw at the top left to track debug data
    //MiscUtil.drawDebugStrings(evt.getMatrixStack(), MC.font, new String[] {
    //});
    public static void drawDebugStrings(GuiGraphics guiGraphics, Font font, String[] strings) {
        int y = 200 - (strings.length * 10);
        for (String str : strings) {
            guiGraphics.drawString(font, str, 0,y, 0xFFFFFF);
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
        }
        else { // prevent colours going < 0
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
        String last3Digits = msStr.substring(msStr.length()-3);
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
            return new Vector3d(0,0,0);
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

    // get the tops of all blocks which are of at a certain horizontal distance away from the centrePos
    public static Set<BlockPos> getRangeIndicatorCircleBlocks(BlockPos centrePos, int radius, Level level) {
        if (radius <= 0)
            return Set.of();

        ArrayList<BlockPos> bps = new ArrayList<>();

        Set<BlockPos> nightCircleBps;
        if (TimeClientEvents.nightCircleMode == NightCircleMode.NO_OVERLAPS)
            nightCircleBps = MiscUtil.CircleUtil.getCircleWithCulledOverlaps(centrePos, radius, TimeClientEvents.nightSourceOrigins);
        else
            nightCircleBps = MiscUtil.CircleUtil.getCircle(centrePos, radius);

        for (BlockPos bp : nightCircleBps) {
            for (int i = 0; i < 3 ; i++) {
                int x = bp.getX();
                int z = bp.getZ();
                if (i == 1)
                    x += 1;
                else if (i == 2)
                    z += 1;

                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockPos topBp = new BlockPos(x, groundY, z);
                bps.add(topBp);

                int y = 1;
                if (level.getBlockState(topBp).getBlock() instanceof LeavesBlock) {
                    BlockPos bottomBp;
                    BlockState bs;
                    do {
                        bottomBp = topBp.offset(0,-y,0);
                        bs = level.getBlockState(bottomBp);
                        y += 1;
                    } while (y < 30 && (bs.getBlock() instanceof LeavesBlock || !bs.isSolid()));
                    if (!level.getBlockState(bottomBp.above()).isSolid())
                        bps.add(bottomBp);
                }
            }
        }
        return new HashSet<>(bps);
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
            if (TimeClientEvents.nightCircleMode == NightCircleMode.NO_OVERLAPS) {
                for (Pair<BlockPos, Integer> os : overlapSources) {
                    Vec2 centre1 = new Vec2(center.getX(),center.getZ());
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

    // eg. entity.reignofnether.zombie_unit -> zombie
    public static String getSimpleEntityName(Entity entity) {
        if (entity instanceof PhantomSummon)
            return "Phantom";

        if (entity instanceof Unit) {
            if (entity.hasCustomName()) {
                return entity.getType()
                        .getDescription()
                        .getString()
                        .replace(" ", "")
                        .replace("entity.reignofnether.", "")
                        .replace("_unit", "")
                        .replace(".none", "");
            } else {
                return entity.getName()
                        .getString()
                        .replace(" ", "")
                        .replace("entity.reignofnether.", "")
                        .replace("_unit", "")
                        .replace(".none", "");
            }
        } else if (entity != null) {
            return entity.getName().getString().toLowerCase();
        }
        return "";
    }
}
