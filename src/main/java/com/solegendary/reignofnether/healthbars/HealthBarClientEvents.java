
package com.solegendary.reignofnether.healthbars;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class HealthBarClientEvents {

    private static final ResourceLocation GUI_BARS_TEXTURES = ResourceLocation.fromNamespaceAndPath(
                ReignOfNether.MOD_ID, "textures/hud/healthbars.png");

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
    public static void playerTick(PlayerTickEvent evt) {
        if (!evt.player.level().isClientSide)
            return;
        BarStates.tick();
    }

    private static boolean shouldShowHealthBar(Entity entity, Minecraft client) {
        return entity instanceof LivingEntity && !(entity instanceof ArmorStand) &&
                   (!entity.isInvisibleTo(client.player) || entity.isCurrentlyGlowing() || entity.isOnFire() ||
                   entity instanceof Creeper && ((Creeper) entity).isPowered() ||
                   StreamSupport.stream(entity.getAllSlots().spliterator(), false).anyMatch(is -> !is.isEmpty())) &&
                 entity != client.player &&
                !entity.isSpectator();
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

            render(matrix, percent, percent2, x, y, width, renderMode, BarState.BarStateType.ABSORB,
                    0, 1f, 1f,
                    0, 0.5f, 0.5f
            );
        }
    }

    public static void renderManaForEntity(PoseStack matrix, HeroUnit heroUnit, double x, double y,
                                           float width, RenderMode renderMode) {
        BarState state = BarStates.getState((LivingEntity) heroUnit, BarState.BarStateType.MANA);

        float percent = Math.min(1, Math.min(state.amount, heroUnit.getMaxMana()) / heroUnit.getMaxMana());
        float percent2 = Math.min(state.previousAmountDisplay, heroUnit.getMaxMana()) / heroUnit.getMaxMana();

        render(matrix, percent, percent2, x, y, width, renderMode, BarState.BarStateType.MANA,
                0.3f, 0.3f, 1f,
                0.15f, 0.15f, 0.5f
        );
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
        // base colour on percentage health remaining (green @ 100%, yellow @ 50%, red @ 0%)
        float r = Math.max(0, Math.min(1, 2-percent*2));
        float g = Math.max(0, Math.min(1, percent*2));
        float b = 0;
        float r2 = r * 0.5f;
        float g2 = g * 0.5f;
        float b2 = b * 0.5f;
        render(matrix, percent, percent2, x, y, width, renderMode, barStateType, r, g, b, r2, g2, b2);
    }

    private static void render(PoseStack matrix, float percent, float percent2, double x, double y,
                               float width, RenderMode renderMode, BarState.BarStateType barStateType, float r, float g, float b, float r2, float g2, float b2) {
        int zOffset = 0;
        Matrix4f m4f = matrix.last().pose();
        if (!(barStateType == BarState.BarStateType.ABSORB && (renderMode == RenderMode.IN_WORLD_ORTHOVIEW || renderMode == RenderMode.IN_WORLD_FIRST_PERSON)))
            drawBar(m4f, x, y, width, 1, 0.35f,0.35f,0.35f, zOffset++, renderMode);
        drawBar(m4f, x, y, width, percent2, r2,g2,b2, zOffset++, renderMode);
        drawBar(m4f, x, y, width, percent, r,g,b, zOffset, renderMode);
    }

    private static void drawBar(Matrix4f matrix4f, double x, double y, float width, float percent,
                                float r, float g, float b, int zOffset, RenderMode renderMode) {
        float c = 0.00390625F;
        int u = 0;
        int v = 6 * 5 * 2 + 5;
        int uw = Mth.ceil(92 * percent);
        int vh = 5;

        double size = percent * width;
        double h = 10;

        if (renderMode == RenderMode.GUI_ICON)
            h = 4;
        else if (renderMode == RenderMode.GUI_PORTRAIT)
            h = 12;
        else if (renderMode == RenderMode.IN_WORLD_FIRST_PERSON)
            h = 6;
        else if (renderMode == RenderMode.IN_WORLD_ORTHOVIEW)
            h = 10;

        RenderSystem.setShaderColor(r, g, b, 1);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GUI_BARS_TEXTURES);
        RenderSystem.enableBlend();

        float half = width / 2;

        float zOffsetAmount = renderMode == RenderMode.IN_WORLD_FIRST_PERSON || renderMode == RenderMode.IN_WORLD_ORTHOVIEW ? -0.1F : 0.1F;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        buffer.vertex(matrix4f, (float) (-half + x), (float) y, zOffset * zOffsetAmount)
                .uv(u * c, v * c).endVertex();
        buffer.vertex(matrix4f, (float) (-half + x), (float) (h + y), zOffset * zOffsetAmount)
                .uv(u * c, (v + vh) * c).endVertex();
        buffer.vertex(matrix4f, (float) (-half + size + x), (float) (h + y), zOffset * zOffsetAmount)
                .uv((u + uw) * c, (v + vh) * c).endVertex();
        buffer.vertex(matrix4f, (float) (-half + size + x), (float) y, zOffset * zOffsetAmount)
                .uv(((u + uw) * c), v * c).endVertex();
        tessellator.end();

        // reset color
        RenderSystem.setShaderColor(1,1,1,1);
    }
}