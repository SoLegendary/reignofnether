
package com.solegendary.reignofnether.healthbars;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class HealthBarClientEvents {

    private static final List<LivingEntity> renderedEntities = new ArrayList<>();
    private static final Minecraft MC = Minecraft.getInstance();

    public enum RenderMode {
        GUI_ICON,
        GUI_PORTRAIT,
        IN_WORLD_ORTHOVIEW,
        IN_WORLD_FIRST_PERSON
    }

    @SubscribeEvent
    public static void entityRender(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<?>> evt) {
        if (!UnitClientEvents.renderingGhosts)
            prepareRenderInWorld(evt.getEntity());
    }
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;

        Camera camera = MC.gameRenderer.getMainCamera();
        renderBarsInWorld(evt.getPartialTick(), evt.getPoseStack(), camera);
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || MC.level == null || MC.isPaused())
            return;
        // ran twice per tick back when this listened to PlayerTickEvent START and END, keep that speed
        BarStates.tick();
        BarStates.tick();
    }

    private static boolean shouldShowHealthBar(Entity entity, Minecraft client) {
        return entity instanceof LivingEntity && !(entity instanceof ArmorStand) &&
                   (!entity.isInvisibleTo(client.player) || entity.isCurrentlyGlowing() || entity.isOnFire() ||
                   entity instanceof Creeper && ((Creeper) entity).isPowered() ||
                   hasAnyEquippedItem(entity)) &&
                 entity != client.player &&
                !entity.isSpectator();
    }

    private static boolean hasAnyEquippedItem(Entity entity) {
        for (var stack : entity.getAllSlots())
            if (!stack.isEmpty())
                return true;
        return false;
    }

    private static void prepareRenderInWorld(LivingEntity entity) {
        Minecraft MC = Minecraft.getInstance();

        if (!shouldShowHealthBar(entity, MC))
            return;

        BarStates.getState(entity, BarState.BarStateType.HEALTH);
        if (entity.getHealth() >= entity.getMaxHealth() && entity.getAbsorptionAmount() <= 0)
            return;
        if (entity.getAbsorptionAmount() > 0)
            BarStates.getState(entity, BarState.BarStateType.ABSORB);

        renderedEntities.add(entity);
    }

    private static void renderBarsInWorld(float partialTick, PoseStack matrix, Camera camera) {
        Minecraft MC = Minecraft.getInstance();

        if (camera == null)
            camera = MC.getEntityRenderDispatcher().camera;

        if (camera == null) {
            renderedEntities.clear();
            return;
        }

        if (renderedEntities.isEmpty())
            return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                GL11.GL_ZERO);

        for (LivingEntity entity : renderedEntities) {
            float scaleToGui = 0.025f;
            boolean sneaking = entity.isCrouching();
            float height = entity.getBbHeight() + 0.6F - (sneaking ? 0.25F : 0.0F);

            double x = Mth.lerp((double) partialTick, entity.xo, entity.getX());
            double y = Mth.lerp((double) partialTick, entity.yo, entity.getY());
            double z = Mth.lerp((double) partialTick, entity.zo, entity.getZ());

            Vec3 camPos = camera.getPosition();
            double camX = camPos.x();
            double camY = camPos.y();
            double camZ = camPos.z();

            matrix.pushPose();
            matrix.translate(x - camX, (y + height) - camY, z - camZ);
            matrix.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            matrix.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            matrix.scale(-scaleToGui, -scaleToGui, scaleToGui);

            // calculate bar width based in entity max health (1hp : 2px)
            int barWidth;
            if (entity instanceof Unit)
                barWidth = (int) entity.getMaxHealth();
            else
                barWidth = (int) entity.getMaxHealth() * 2;

            barWidth = Math.min(barWidth, 120);
            barWidth = Math.max(barWidth, 20);

            renderForEntity(matrix, entity, 0, 0, barWidth,
                    OrthoviewClientEvents.isEnabled() ? RenderMode.IN_WORLD_ORTHOVIEW : RenderMode.IN_WORLD_FIRST_PERSON);

            renderAbsorbForEntity(matrix, entity, 0, 0, barWidth,
                    OrthoviewClientEvents.isEnabled() ? RenderMode.IN_WORLD_ORTHOVIEW : RenderMode.IN_WORLD_FIRST_PERSON);

            matrix.popPose();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        renderedEntities.clear();
    }

    public static void renderForEntity(PoseStack matrix, LivingEntity entity, double x, double y,
                                       float width, RenderMode renderMode) {
        BarState state = BarStates.getState(entity, BarState.BarStateType.HEALTH);

        float percent = Math.min(1, Math.min(state.amount, entity.getMaxHealth()) / entity.getMaxHealth());
        float percent2 = Math.min(state.previousAmountDisplay, entity.getMaxHealth()) / entity.getMaxHealth();

        render(matrix, percent, percent2, x, y, width, renderMode, BarState.BarStateType.HEALTH);
    }

    public static void renderAbsorbForEntity(PoseStack matrix, LivingEntity entity, double x, double y,
                                       float width, RenderMode renderMode) {
        if (entity.getAbsorptionAmount() > 0) {
            BarState state = BarStates.getState(entity, BarState.BarStateType.ABSORB);

            float percent = Math.min(1, Math.min(state.amount, MiscUtil.getMaxAbsorptionAmount(entity)) / MiscUtil.getMaxAbsorptionAmount(entity));
            float percent2 = Math.min(state.previousAmountDisplay, MiscUtil.getMaxAbsorptionAmount(entity)) / MiscUtil.getMaxAbsorptionAmount(entity);

            render(matrix, percent, percent2, x, y, width, renderMode, BarState.BarStateType.ABSORB, Theme.BUFF);
        }
    }

    public static void renderForBuilding(PoseStack matrix, BuildingPlacement building, double x, double y,
                                         float width, RenderMode renderMode) {
        float health = building.getHealth();
        if (health > building.getMaxHealth())
            health = building.getMaxHealth();
        float percent = health / (float) building.getMaxHealth();
        render(matrix, percent, percent, x, y, width, renderMode, BarState.BarStateType.HEALTH);
    }

    private static void render(PoseStack matrix, float percent, float percent2, double x, double y,
                               float width, RenderMode renderMode, BarState.BarStateType barStateType) {
        render(matrix, percent, percent2, x, y, width, renderMode, barStateType, Theme.hpColor(percent));
    }

    /**
     * Flat bar like {@link Theme#bar}: ink track, the recently lost amount in a darker shade of the fill,
     * the fill, and a light top / dark bottom line. Quads are drawn back to front, each a step closer.
     */
    private static void render(PoseStack matrix, float percent, float percent2, double x, double y,
                               float width, RenderMode renderMode, BarState.BarStateType barStateType, int colour) {
        Matrix4f m4f = matrix.last().pose();
        boolean inWorld = renderMode == RenderMode.IN_WORLD_FIRST_PERSON || renderMode == RenderMode.IN_WORLD_ORTHOVIEW;
        double h = switch (renderMode) {
            case GUI_ICON -> 4;
            case GUI_PORTRAIT -> 12;
            case IN_WORLD_FIRST_PERSON -> 6;
            case IN_WORLD_ORTHOVIEW -> 10;
        };
        int z = 0;
        // absorption sits on top of the health bar in the world, so it has no track of its own
        if (!(barStateType == BarState.BarStateType.ABSORB && inWorld))
            quad(m4f, x, y, width, 1, h, Theme.INK, z++, inWorld);
        quad(m4f, x, y, width, percent2, h, (colour & 0xFEFEFE) >> 1 | 0xFF000000, z++, inWorld);
        quad(m4f, x, y, width, percent, h, colour, z++, inWorld);
        if (h >= 3) {
            quad(m4f, x, y, width, percent, 1, 0x59FFFFFF, z, inWorld);
            quad(m4f, x, y + h - 1, width, percent, 1, 0x40000000, z, inWorld);
        }
    }

    /** One flat quad from the bar's left edge, percent of width wide. */
    private static void quad(Matrix4f matrix4f, double x, double y, float width, float percent, double h,
                             int argb, int zOffset, boolean inWorld) {
        if (percent <= 0)
            return;
        float half = width / 2;
        float x0 = (float) (-half + x);
        float x1 = (float) (-half + percent * width + x);
        float y0 = (float) y;
        float y1 = (float) (y + h);
        float z = zOffset * (inWorld ? -0.1F : 0.1F);
        int a = argb >>> 24, r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix4f, x0, y0, z).color(r, g, b, a).endVertex();
        buffer.vertex(matrix4f, x0, y1, z).color(r, g, b, a).endVertex();
        buffer.vertex(matrix4f, x1, y1, z).color(r, g, b, a).endVertex();
        buffer.vertex(matrix4f, x1, y0, z).color(r, g, b, a).endVertex();
        tessellator.end();
    }
}
