package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.unit.Unit;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.villagers.VillagerUnitModel;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

// Renders a Unit's portrait including its animated head, name, healthbar, list of stats and UI frames for these

class PortraitRendererUnit<T extends LivingEntity, M extends EntityModel<T>, R extends LivingEntityRenderer<T, M>> {
    public R renderer;
    public Model model;

    public final int frameWidth = 60;
    public final int frameHeight = 60;

    private final int headSize = 46;
    private final int headOffsetX = 31;
    private final int headOffsetY = 105;
    private final float standardEyeHeight = 1.74f; // height for most humanoid mobs

    // change these randomly every few seconds to make the head look around
    private int lookX = 0;
    private int lookY = 0;
    private int lastLookTargetX = 0;
    private int lastLookTargetY = 0;
    private int lookTargetX = 0;
    private int lookTargetY = 0;
    private int ticksLeft = 0;
    private final int ticksLeftMin = 60;
    private final int ticksLeftMax = 120;
    private final int lookRangeX = 100;
    private final int lookRangeY = 40;

    private static final ResourceLocation[] TEXTURE_STAT_ICONS = {
            new ResourceLocation("reignofnether", "textures/icons/items/sword.png"), // DAMAGE
            new ResourceLocation("reignofnether", "textures/icons/items/sparkler.png"), // ATTACK SPEED
            new ResourceLocation("reignofnether", "textures/icons/items/bow.png"), // RANGE
            new ResourceLocation("reignofnether", "textures/icons/items/chestplate.png"), // ARMOUR
            new ResourceLocation("reignofnether", "textures/icons/items/boots.png"), // MOVE SPEED
    };


    public PortraitRendererUnit(R renderer) {
        this.renderer = renderer;
    }


    public void randomiseAnimation(Boolean randomisePos) {
        if (randomisePos) {
            lookX = MyMath.randRangeInt(-lookRangeX, lookRangeX);
            lookY = MyMath.randRangeInt(-lookRangeY, lookRangeY);
        }
        ticksLeft = MyMath.randRangeInt(ticksLeftMin, ticksLeftMax);

        lastLookTargetX = lookTargetX;
        lastLookTargetY = lookTargetY;

        while (Math.abs(lookTargetX - lookX) < lookRangeX / 2)
            lookTargetX = MyMath.randRangeInt(-lookRangeX, lookRangeX);
        while (Math.abs(lookTargetY - lookY) < lookRangeY / 2)
            lookTargetY = MyMath.randRangeInt(-lookRangeY, lookRangeY);
    }

    public void tickAnimation() {
        ticksLeft -= 1;
        if (ticksLeft <= 0)
            this.randomiseAnimation(false);

        int lookSpeedX = Math.abs(lastLookTargetX - lookX) / 20;
        int lookSpeedY = Math.abs(lastLookTargetY - lookY) / 20;

        if (lookX < lookTargetX)
            lookX += lookSpeedX;
        if (lookX > lookTargetX)
            lookX -= lookSpeedX;
        if (lookY < lookTargetY)
            lookY += lookSpeedY;
        if (lookY > lookTargetY)
            lookY -= lookSpeedY;

        if (Math.abs(lookTargetX - lookX) < lookSpeedX)
            lookX = lookTargetX;
        if (Math.abs(lookTargetY - lookY) < lookSpeedY)
            lookY = lookTargetY;
    }

    // Render the portrait including:
    // - background frame
    // - moving head
    // - healthbar
    // - unit name
    // Must be called from DrawScreenEvent
    public RectZone render(PoseStack poseStack, String name, int x, int y, LivingEntity entity) {
        // draw name
        GuiComponent.drawString(
                poseStack, Minecraft.getInstance().font,
                name,
                x+4,y-9,
                0xFFFFFFFF
        );

        int bgCol = 0x0;
        switch (UnitClientEvents.getPlayerToEntityRelationship(entity.getId())) {
            case OWNED    -> bgCol = 0x90000000;
            case FRIENDLY -> bgCol = 0x90009000;
            case NEUTRAL  -> bgCol = 0x90909000;
            case HOSTILE  -> bgCol = 0x90900000;
        }
        MyRenderer.renderFrameWithBg(poseStack, x, y,
                frameWidth,
                frameHeight,
                bgCol);

        RectZone rectZone = RectZone.getZoneByLW(x, y, frameWidth, frameHeight);

        int drawX = x + headOffsetX;
        int drawY = y + (int) (entity.getEyeHeight() / standardEyeHeight * headOffsetY);

        // hide all model parts except the head
        setNonHeadModelVisibility(this.model, false);
        List<RenderLayer<T, M>> layers = null;
        if (renderer != null) {
            layers = renderer.layers;
            renderer.layers = List.of();
        }

        drawEntityOnScreen(poseStack, entity, drawX, drawY, headSize);
        if (renderer != null && layers != null)
            renderer.layers = layers;
        setNonHeadModelVisibility(this.model, true);

        // draw health bar and write min/max hp
        HealthBarClientEvents.renderForEntity(poseStack, entity,
                x+(frameWidth/2f), y+frameHeight-15,
                frameWidth-9, HealthBarClientEvents.RenderMode.GUI_PORTRAIT);

        String healthText = "";
        float health = entity.getHealth();
        if (health >= 1)
            healthText = String.valueOf((int) health);
        else
            healthText = String.valueOf(health).substring(0,3);

        GuiComponent.drawCenteredString(
                poseStack, Minecraft.getInstance().font,
                healthText + "/" + (int) entity.getMaxHealth(),
                x+(frameWidth/2), y+frameHeight-13,
                0xFFFFFFFF
        );
        return rectZone;
    }

    public RectZone renderStats(PoseStack poseStack, String name, int x, int y, Unit unit) {
        int width2 = 43;
        MyRenderer.renderFrameWithBg(poseStack, x, y,
                width2,
                frameHeight,
                0xA0000000);

        int blitXIcon = x + 6;
        int blitYIcon = y + 7;
        for (int i = 0; i < TEXTURE_STAT_ICONS.length; i++) {
            MyRenderer.renderIcon(
                    poseStack,
                    TEXTURE_STAT_ICONS[i],
                    blitXIcon, blitYIcon, 8
            );
            String statString = "";

            switch (i) {
                case 0 -> statString = String.valueOf((int) unit.getDamage()); // DAMAGE
                case 1 -> statString = String.valueOf((int) (100 / unit.getAttackCooldown())); // ATTACK SPEED
                case 2 -> statString = String.valueOf((int) (unit.getAttackRange())); // RANGE
                case 3 -> statString = String.valueOf(((LivingEntity) unit).getArmorValue()); // ARMOUR
                case 4 -> {
                    AttributeInstance ms = ((LivingEntity) unit).getAttribute(Attributes.MOVEMENT_SPEED);
                    statString = ms != null ? String.valueOf((int) (ms.getValue() * 100)) : "0"; // MOVE SPEED
                }
            }
            GuiComponent.drawString(poseStack, Minecraft.getInstance().font, statString, blitXIcon + 13, blitYIcon, 0xFFFFFF);
            blitYIcon += 10;
        }
        return RectZone.getZoneByLW(x, y, width2, frameHeight);
    }

    private void setNonHeadModelVisibility(Model model, boolean visibility) {

        if (model instanceof HumanoidModel) {
            ((HumanoidModel<?>) model).hat.visible = visibility;
            ((HumanoidModel<?>) model).body.visible = visibility;
            ((HumanoidModel<?>) model).rightArm.visible = visibility;
            ((HumanoidModel<?>) model).leftArm.visible = visibility;
            ((HumanoidModel<?>) model).rightLeg.visible = visibility;
            ((HumanoidModel<?>) model).leftLeg.visible = visibility;
        }
        //else if (model instanceof VillagerUnitModel)
        //    ((VillagerUnitModel<?>) model).armsVisible = visibility;

        // hide all non-head models attached to root
        if (model instanceof HierarchicalModel)
            setNonHeadRootModelVisibility(((HierarchicalModel<?>) model).root(), visibility);
    }

    private void setNonHeadRootModelVisibility(ModelPart root, boolean visibility) {
        root.children.forEach((String name, ModelPart modelPart) -> {
            if (!name.contentEquals("head"))
                modelPart.visible = visibility;
        });
    }

    private void drawEntityOnScreen(PoseStack poseStack, LivingEntity entity, int x, int y, int size) {

        float f = (float) Math.atan((double) (-lookX / 40F));
        float g = (float) Math.atan((double) (-lookY / 40F));
        PoseStack poseStackModel = RenderSystem.getModelViewStack();
        poseStackModel.pushPose();
        poseStackModel.translate((double) x, (double) y, 1050.0D);
        poseStackModel.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 1000.0D);
        poseStack.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion2 = Vector3f.XP.rotationDegrees(g * 20.0F);
        quaternion.mul(quaternion2);
        poseStack.mulPose(quaternion);
        float h = entity.yBodyRot; // bodyYaw;
        float i = entity.getYRot(); // getYaw();
        float j = entity.getXRot(); // getPitch();
        float k = entity.yHeadRotO; // prevHeadYaw;
        float l = entity.yHeadRot; // headYaw;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion2.conj();
        entityrenderdispatcher.setRenderShadow(false);
        entityrenderdispatcher.overrideCameraOrientation(quaternion2);

        // for some reason this snippet causes drawLineBox to draw lines in completely wrong locations while in spectator mode
        RenderSystem.runAsFancy(() -> {
            MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
            entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, immediate,
                    15728880);
            immediate.endBatch();
        });
        entityrenderdispatcher.setRenderShadow(true);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.yHeadRotO = k;
        entity.yHeadRot = l;
        poseStackModel.popPose();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}
