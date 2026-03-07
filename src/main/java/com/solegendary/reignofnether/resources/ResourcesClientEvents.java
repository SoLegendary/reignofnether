package com.solegendary.reignofnether.resources;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.getSelectedUnits;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResourcesClientEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    private final static Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) {
            return;
        }

        for (Resources resources : resourcesList)
            resources.tick();
    }

    @SubscribeEvent
    public static void onClientTick(ScreenEvent.Render evt) {
        if (HudClientEvents.hudSelectedEntity instanceof WorkerUnit workerUnit &&
            UnitClientEvents.getPlayerToEntityRelationship((LivingEntity) workerUnit) == Relationship.OWNED &&
            MC.level != null && !HudClientEvents.isMouseOverAnyButtonOrHud()) {

            BlockPos preSelBp = CursorClientEvents.getPreselectedBlockPos();
            ResourceSource res = ResourceSources.getFromBlockPos(preSelBp, MC.level);
            if (res != null && res.resourceValue > 0 && !BuildingUtils.isPosInsideAnyNonBridgeBuilding(true, preSelBp)) {
                String str = switch (res.resourceName) {
                    case FOOD -> "\uE000  " + res.resourceValue;
                    case WOOD -> "\uE001  " + res.resourceValue;
                    case ORE -> "\uE002  " + res.resourceValue;
                    case NONE -> "";
                };
                MyRenderer.renderTooltip(evt.getGuiGraphics(),
                        List.of(FormattedCharSequence.forward(str, MyRenderer.iconStyle)),
                        evt.getMouseX(), evt.getMouseY()
                );
            }
        }
    }

    public static void syncResources(Resources serverResources) {
        resourcesList.removeIf(resources -> resources.ownerName.equals(serverResources.ownerName));
        resourcesList.add(serverResources);
    }

    // should never be run from clientside except via packet
    public static void addSubtractResources(Resources serverResources) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(serverResources.ownerName)) {
                resources.changeOverTime(serverResources.food, serverResources.wood, serverResources.ore);
            }
    }

    public static void addSubtractResourcesInstantly(Resources serverResources) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(serverResources.ownerName)) {
                resources.changeInstantly(serverResources.food, serverResources.wood, serverResources.ore);
            }
    }

    public static Resources getOwnResources() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            return getResources(MC.player.getName().getString());
        }
        return null;
    }

    public static Resources getResources(String playerName) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(playerName)) {
                return resources;
            }
        return null;
    }

    public static void showWarning(String ownerName, String msg) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getName().getString().equals(ownerName)) {
            String loc = I18n.get(msg);
            HudClientEvents.showTemporaryMessage(loc);

            // remove checkpoints from a failed building placement since the client has no knowledge of resource costs
            if (msg.contains("not_enough")) {
                for (LivingEntity entity : getSelectedUnits())
                    if (entity instanceof Unit unit)
                        if (((Entity) unit).level().isClientSide() && !Keybindings.shiftMod.isDown())
                            unit.getCheckpoints().clear();
            }
        }
    }

    // floating text for resource dropoffs
    public static final List<FloatingText> floatingTexts = new ArrayList<>();
    public static final int FLOATING_TEXT_MAX_AGE = 200;

    private static class FloatingText {
        String text;
        BlockPos pos;
        int tickAge;

        public FloatingText(String text, BlockPos pos, int tickAge) {
            this.text = text;
            this.pos = pos;
            this.tickAge = tickAge;
        }
    }

    public static void addFloatingTextsFromResources(Resources res, BlockPos pos) {
        if (!OrthoviewClientEvents.isEnabled()) {
            return;
        }
        if (!Minecraft.getInstance().player.getName().getString().equals(res.ownerName)) {
            return;
        }

        int tickAge = 0;
        if (res.food > 0) {
            floatingTexts.add(new FloatingText("+" + res.food + "  \uE000", pos, tickAge));
            tickAge -= 25;
        }
        if (res.wood > 0) {
            floatingTexts.add(new FloatingText("+" + res.wood + "  \uE001", pos, tickAge));
            tickAge -= 25;
        }
        if (res.ore > 0) {
            floatingTexts.add(new FloatingText("+" + res.ore + "  \uE002", pos, tickAge));
        }
    }

    // Render floating text for dropped-off resources
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        for (FloatingText floatingText : floatingTexts) {

            if (floatingText.tickAge > 0) {
                Minecraft MC = Minecraft.getInstance();
                MutableComponent component = Component.literal(floatingText.text).withStyle(MyRenderer.iconStyle);

                PoseStack poseStack = evt.getPoseStack();
                Camera camera = MC.getEntityRenderDispatcher().camera;
                Vec3 camPos = camera.getPosition();

                poseStack.pushPose();
                poseStack.translate(floatingText.pos.getX() - camPos.x(),
                    floatingText.pos.getY() - camPos.y() + 2.5 + (floatingText.tickAge / 20f),
                    floatingText.pos.getZ() - camPos.z()
                );
                poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
                poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
                if (OrthoviewClientEvents.isEnabled()) {
                    poseStack.scale(-0.075F, -0.075F, 0.075F);
                } else {
                    poseStack.scale(-0.05F, -0.05F, 0.05F);
                }
                Font font = MC.font;

                float f2 = (float) (-font.width(component) / 2);
                float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
                float alphaPercent =
                    ((FLOATING_TEXT_MAX_AGE - floatingText.tickAge) * 1.5f) / (float) FLOATING_TEXT_MAX_AGE;
                if (alphaPercent < 0.05) {
                    alphaPercent = 0.05f;
                }
                if (alphaPercent > 1) {
                    alphaPercent = 1f;
                }
                int textCol = 0x00FFFFFF + ((int) (0xFF * alphaPercent) << 24);
                int bgCol = (int) (f1 * 255.0F * alphaPercent) << 24;

                font.drawInBatch(component,
                    f2,
                    0,
                    textCol,
                    false,
                    poseStack.last().pose(),
                    MC.renderBuffers().bufferSource(),
                    Font.DisplayMode.NORMAL,
                    bgCol,
                    255
                );

                poseStack.popPose();
            }
            floatingText.tickAge += 1;
        }
        floatingTexts.removeIf(t -> t.tickAge > FLOATING_TEXT_MAX_AGE);
    }
}
