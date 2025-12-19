package com.solegendary.reignofnether.unit;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;

public class NonUnitClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean isMoveCheckpointGreen = true;

    public static boolean canControlAllMobs() {
        return MC.player != null &&
                (ResearchClient.hasCheat("wouldyoukindly"));
    }

    public static boolean canAttack(LivingEntity le) {
        if (le instanceof PathfinderMob mob) {
            if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ||
                mob.getAttributes().hasAttribute(Attributes.ATTACK_SPEED))
                return true;

            for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals())
                if (wrappedGoal.getGoal() instanceof NearestAttackableTargetGoal)
                    return true;
        }
        return false;
    }

    // override attack and random move goals while we have an active command
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (MC.level == null)
            return;

        // AFTER_CUTOUT_BLOCKS lets us see checkpoints through leaves
        if (OrthoviewClientEvents.isEnabled() && evt.getStage() == AFTER_CUTOUT_BLOCKS) {
            VertexConsumer vertexConsumerLine = MC.renderBuffers().bufferSource().getBuffer(RenderType.LINES);
            ResourceLocation rl = ResourceLocation.parse("forge:textures/white.png");
            VertexConsumer vertexConsumerEntityTranslucent = MC.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(rl));
            for (LivingEntity le : UnitClientEvents.getSelectedUnits()) {
                if (le instanceof PathfinderMob mob && !(le instanceof Unit) && le.isAlive() && !le.isRemoved()) {
                    float entityYOffset = 1.74f - le.getEyeHeight() - 1;
                    Vec3 firstPos = le.getEyePosition().add(0, entityYOffset,0);

                    if (mob.getTarget() != null && !mob.getTarget().isDeadOrDying()) {
                        MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, firstPos, mob.getTarget().getEyePosition(), 1, 0, 0, 0.5f);
                    }
                    else if (!mob.getNavigation().isDone() && mob.getNavigation().getTargetPos() != null) {

                        double dist = Math.sqrt(mob.distanceToSqr(Vec3.atCenterOf(mob.getNavigation().getTargetPos())));
                        float a = (float) Math.min(1, dist / 4) - 0.2f;

                        if (a > 0) {
                            BlockPos bp = mob.getNavigation().getTargetPos().below();
                            Vec3 pos = new Vec3(bp.getX() + 0.5f, bp.getY() + 1.0f, bp.getZ() + 0.5f);
                            MyRenderer.drawLine(evt.getPoseStack(), vertexConsumerLine, firstPos, pos, isMoveCheckpointGreen ? 0 : 1, isMoveCheckpointGreen ? 1 : 0, 0, a);

                            if (MC.level.getBlockState(bp.offset(0, 1, 0)).getBlock() instanceof SnowLayerBlock) {
                                AABB aabb = new AABB(bp);
                                aabb = aabb.setMaxY(aabb.maxY + 0.13f);
                                MyRenderer.drawSolidBox(
                                        evt.getPoseStack(),
                                        vertexConsumerEntityTranslucent,
                                        aabb,
                                        Direction.UP,
                                        isMoveCheckpointGreen ? 0 : 1,
                                        isMoveCheckpointGreen ? 1 : 0,
                                        0,
                                        a * 0.5f,
                                        ResourceLocation.parse("forge:textures/white.png")
                                );
                            } else {
                                MyRenderer.drawBlockFace(evt.getPoseStack(), vertexConsumerEntityTranslucent, Direction.UP, bp, isMoveCheckpointGreen ? 0 : 1, isMoveCheckpointGreen ? 1 : 0, 0, a * 0.5f);
                            }
                        }
                    } else {
                        mob.getNavigation().stop();
                        mob.setTarget(null);
                    }
                }
            }
        }
    }
}