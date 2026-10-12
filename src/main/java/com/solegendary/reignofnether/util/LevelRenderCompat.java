package com.solegendary.reignofnether.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

/**
 * Helpers for drawing world-space geometry from {@link RenderLevelStageEvent} handlers.
 *
 * <p>Two properties of 1.21.1's level renderer govern when queued geometry actually reaches the
 * screen, and both are easy to get wrong because nothing in the event API hints at them.
 *
 * <h2>The model-view matrix is read at flush time, not at draw time</h2>
 *
 * A queued vertex is baked with whatever matrix the handler hands the consumer, but the batch is
 * only turned into a draw call later, and {@code BufferUploader#_drawWithShader} reads
 * {@code RenderSystem.getModelViewMatrix()} at that point:
 *
 * <pre>{@code
 * vertexbuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), ...);
 * }</pre>
 *
 * {@code LevelRenderer#renderLevel} multiplies the camera rotation into that stack at line 994,
 * which is after it has already dispatched the terrain stages:
 *
 * <pre>{@code
 *  965  renderSectionLayer(solid(), ...)          -> AFTER_SOLID_BLOCKS
 *  967  renderSectionLayer(cutoutMipped(), ...)  -> AFTER_CUTOUT_MIPPED_BLOCKS
 *  969  renderSectionLayer(cutout(), ...)        -> AFTER_CUTOUT_BLOCKS   <-- mod draws here
 *  994  matrix4fstack.mul(p_254120_); applyModelViewMatrix();           <-- camera rotation lands
 * 1045  dispatchRenderStage(AFTER_ENTITIES, this, posestack, ...)      <-- model-view now correct
 * }</pre>
 *
 * Anything queued at one of those early stages is therefore drawn with the leftover model-view:
 * the camera rotation is missing, so the geometry keeps a fixed orientation relative to the world
 * axes while the rest of the frame rotates, and it drifts across the screen as the camera turns.
 *
 * <p>The pose handed to the handler is not the place that rotation hides. At those stages the event
 * is constructed with a {@code null} pose and substitutes a fresh identity {@code PoseStack}, so
 * the matrix baked into the vertices must be nothing but the camera-relative translation - which is
 * what {@link MyRenderer} does.
 *
 * <p>{@link #withCameraModelView} installs the event's own model-view for the duration of the
 * draw, so the flush sees the same matrix the rest of the level was drawn with.
 *
 * <h2>Nothing flushes the batches afterwards</h2>
 *
 * Vanilla empties the buffer source at line 1172/1173 (fabulous branch) or 1197/1198, and the
 * crumbling buffer at line 1170 - all before the {@code AFTER_TRANSLUCENT_BLOCKS} dispatch at 1177.
 * Geometry queued at {@code AFTER_TRANSLUCENT_BLOCKS} therefore survives into the GUI pass, whose
 * {@code GuiGraphics#flush} is the next {@code endBatch()} to run - with an orthographic projection
 * and a Z-translated model-view, which displaces it by a constant world-axis offset.
 * {@link #flushWorldGeometry()} ends the stage's geometry where it was queued.
 */
public class LevelRenderCompat {

    private static final Minecraft MC = Minecraft.getInstance();

    /**
     * {@code true} for the stages 1.21.1 dispatches before it applies the camera rotation to
     * {@code RenderSystem}'s model-view stack (see {@code LevelRenderer#renderLevel} line 994).
     * Only these need {@link #withCameraModelView}.
     */
    public static boolean isBeforeCameraModelView(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_SKY
                || stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS
                || stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS
                || stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
    }

    /**
     * Runs {@code draw} with the stage's model-view matrix installed on {@link RenderSystem}, so
     * that any batch flushed inside is drawn with the camera rotation like the rest of the level.
     *
     * <p>Only valid for the stages {@link #isBeforeCameraModelView} reports {@code true} for -
     * applying it where the stack already carries the rotation would rotate twice. The matrix is
     * pushed and popped around the call, so the rest of the frame is unaffected either way.
     */
    public static void withCameraModelView(RenderLevelStageEvent evt, Runnable draw) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(evt.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        try {
            draw.run();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /**
     * Flushes everything the stage queued into the level's buffer sources.
     *
     * <p>Flushing by RenderType is not enough to be safe: the helpers in {@link MyRenderer} pick
     * their own RenderType internally (vanilla {@code lines()} for the plain line boxes, the mod's
     * two custom line types, {@code entityTranslucent} for the solid fills), and every RenderType
     * without a fixed buffer of its own shares one {@code ByteBufferBuilder}. A type left unflushed
     * stays in that shared builder until some unrelated {@code endBatch} runs, and is then drawn
     * with whatever matrix that call happens to have. The no-argument {@code endBatch()} empties all
     * of it.
     *
     * <p>The handlers for one stage run in sequence and each of them calls this, so the last one
     * to run covers everything the earlier ones queued regardless of registration order.
     */
    public static void flushWorldGeometry() {
        MC.renderBuffers().bufferSource().endBatch();
        MC.renderBuffers().crumblingBufferSource().endBatch();
    }

    /** Convenience for {@link #withCameraModelView} plus {@link #flushWorldGeometry}. */
    public static void drawAndFlush(RenderLevelStageEvent evt, Runnable draw) {
        withCameraModelView(evt, () -> {
            draw.run();
            flushWorldGeometry();
        });
    }
}