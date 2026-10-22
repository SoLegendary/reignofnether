package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.UnitStatType;
import com.solegendary.reignofnether.unit.controls.SlimeJumpMoveControl;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.hud.TooltipColours.*;

// Renders a Unit's portrait including its animated head, name, healthbar, list of stats and UI frames for these

public class PortraitRendererUnit<T extends LivingEntity, M extends EntityModel<T>, R extends LivingEntityRenderer<T, M>> {
    private static final Minecraft MC = Minecraft.getInstance();

    public R renderer;
    public Model model;

    private final int size = 46;
    private final int offsetY = 105;
    private final float standardEyeHeight = 1.74f; // height for most humanoid mobs

    // change these randomly every few seconds to make the head look around
    private int lookX = 0;
    private int lookY = 0;
    private int lastLookTargetX = 0;
    private int lastLookTargetY = 0;
    private int lookTargetX = 0;
    private int lookTargetY = 0;
    private int ticksLeft = 0;
    private final int ticksLeftMin = 30;
    private final int ticksLeftMax = 60;
    private final int lookRangeX = 100;
    private final int lookRangeY = 40;

    public PortraitRendererUnit() {
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

    private static boolean checkedFreshAnimations = false;
    private static boolean isFreshAnimationsInstalled = false;

    private static boolean shouldAnimate() {
        if (!checkedFreshAnimations && HudClientEvents.hudSelectedEntity != null) {
            boolean b = false;
            for (String s : MC.getResourceManager().listPacks().map(PackResources::packId)
                .toList()) {
                if (s.toLowerCase().contains("freshanimations")) {
                    b = true;
                    break;
                }
            }
            isFreshAnimationsInstalled = b;
            checkedFreshAnimations = true;
        }
        return !isFreshAnimationsInstalled ||
                HudClientEvents.hudSelectedEntity instanceof AbstractIllager ||
                HudClientEvents.hudSelectedEntity instanceof HeroUnit;
    }

    public void tickAnimation() {
        ticksLeft -= 1;
        if (ticksLeft <= 0) {
            this.randomiseAnimation(false);
        }

        int lookSpeedX = Math.abs(lastLookTargetX - lookX) / 20;
        int lookSpeedY = Math.abs(lastLookTargetY - lookY) / 20;

        if (lookX < lookTargetX) {
            lookX += lookSpeedX;
        }
        if (lookX > lookTargetX) {
            lookX -= lookSpeedX;
        }
        if (lookY < lookTargetY) {
            lookY += lookSpeedY;
        }
        if (lookY > lookTargetY) {
            lookY -= lookSpeedY;
        }

        if (Math.abs(lookTargetX - lookX) < lookSpeedX) {
            lookX = lookTargetX;
        }
        if (Math.abs(lookTargetY - lookY) < lookSpeedY) {
            lookY = lookTargetY;
        }
    }

    /**
     * Draws the selected entity's animated 3D model inside a box (HUD units). Sizes are relative to the
     * 60x60 frame the per-mob offsets in {@link PortraitRendererModifiers} were tuned for.
     */
    public void drawEntity(GuiGraphics g, LivingEntity entity, int x, int y, int w, int h) {
        float k = Math.min(w, h) / 60f;
        float drawX = x + w / 2f;
        float drawY = y + (h - 60 * k) / 2f + entity.getEyeHeight() / standardEyeHeight * offsetY * k;
        Pair<Integer, Integer> mods = PortraitRendererModifiers.getPortraitRendererModifiers(entity);
        drawY += mods.getFirst() * k;
        float sizeFinal = (size + mods.getSecond()) * k;

        ItemStack bannerStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        boolean hasBanner = bannerStack.getItem() instanceof BannerItem;
        if (hasBanner)
            entity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        drawEntityOnScreen(g.pose(), entity, drawX, drawY, sizeFinal);
        if (hasBanner)
            entity.setItemSlot(EquipmentSlot.HEAD, bannerStack);
    }

    /** One line of the unit's stats, as shown on the selection panel's stat cards. */
    public record Stat(ResourceLocation icon, UnitStatType type, String value, int colour) { }

    public static List<Stat> computeStats(Unit unit) {
        List<Stat> stats = new ArrayList<>();

        if (unit instanceof AttackerUnit attackerUnit) {
            double atkDmg = attackerUnit.getUnitAttackDamage() + AttackerUnit.getWeaponDamageModifier(attackerUnit);
            if (unit instanceof CreeperUnit cUnit && cUnit.isPowered())
                atkDmg *= CreeperUnit.CHARGED_DAMAGE_MULT;
            if (unit instanceof WorkerUnit)
                atkDmg = (int) attackerUnit.getUnitAttackDamage();
            double atkDmgRounded = Math.round(atkDmg);
            String atkStr = Math.abs(atkDmgRounded - atkDmg) < 0.1d
                    ? String.valueOf((int) atkDmgRounded)
                    : new DecimalFormat("###.#").format(atkDmg);
            if (unit instanceof Mob mob && mob.isVehicle() && mob.getFirstPassenger() instanceof AttackerUnit passenger)
                atkStr += "+" + (int) passenger.getUnitAttackDamage();

            int dmgColour = attackerUnit.getDamageTooltipColour();
            if (attackerUnit.getUnitAttackDamage() < attackerUnit.getBaseUnitAttackDamage())
                dmgColour = RED;
            else if (attackerUnit.getUnitAttackDamage() > attackerUnit.getBaseUnitAttackDamage())
                dmgColour = GREEN;
            stats.add(new Stat(icon("sword"), UnitStatType.ATTACK_DAMAGE, atkStr, dmgColour));

            float aps = Math.round(attackerUnit.getNonBaseAttacksPerSecond() * 100f) / 100f;
            float baseAps = Math.round(attackerUnit.getBaseAttacksPerSecond() * 100f) / 100f;
            int apsColour = aps > baseAps ? GREEN : aps < baseAps ? RED : WHITE;
            stats.add(new Stat(icon("sparkler"), UnitStatType.ATTACK_SPEED,
                    new DecimalFormat("###.##").format(attackerUnit.getAttacksPerSecond()), apsColour));

            BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison(unit);
            GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            String range = garr != null ? String.valueOf(garr.getAttackRange()) : String.valueOf((int) attackerUnit.getAttackRange());
            stats.add(new Stat(icon("bow"), UnitStatType.RANGE, range, attackerUnit.hasBonusRange() ? GREEN : WHITE));
        }

        stats.add(armourStat(unit));

        if (((LivingEntity) unit).getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            float ms = unit.getMovementSpeed() * unit.getSpeedModifier();
            int msInt = (int) (ms * 101);
            if (unit instanceof SlimeUnit slimeUnit && slimeUnit.isUsingJumpingMovement())
                msInt /= SlimeJumpMoveControl.MOVESPEED_MULTIPLIER;
            double msBase = ((LivingEntity) unit).getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            int msColour = msBase < ms ? GREEN : msBase > ms ? RED : WHITE;
            stats.add(new Stat(icon("boots"), UnitStatType.MOVEMENT_SPEED, String.valueOf(msInt), msColour));
        }
        return stats;
    }

    /** The armour stat; its colour tells the armour type, which the stat card also spells out. */
    private static Stat armourStat(Unit unit) {
        double physical = unit.getUnitPhysicalArmorPercentage();
        double ranged = unit.getUnitRangedArmorPercentage();
        double magic = unit.getUnitMagicArmorPercentage();
        double resist = unit.getUnitResistPercentage();
        double highest = 0;
        String str = "0%";
        int colour = WHITE;
        int types = 0;

        if (physical != 0) { str = (int) (physical * 100) + "%"; colour = GREEN; highest = Math.max(highest, physical); types++; }
        if (ranged != 0) { str = (int) (ranged * 100) + "%"; colour = YELLOW; highest = Math.max(highest, ranged); types++; }
        if (resist != 0) { str = (int) (resist * 100) + "%"; colour = LIGHT_BLUE; highest = Math.max(highest, resist); types++; }
        if (magic != 0 && resist <= 0) { // resist includes magic
            str = (int) (magic * 100) + "%"; colour = DARK_BLUE; highest = Math.max(highest, magic); types++;
        }
        if (types > 1 && highest > 0) {
            str = (int) (highest * 100) + "%+";
            colour = LIGHT_BLUE;
        }
        if (str.contains("-"))
            colour = RED;
        return new Stat(icon("chestplate"), UnitStatType.ARMOUR, str, colour);
    }

    /** What the armour colour means, for the stat card's second line. */
    public static String armourKind(int colour) {
        if (colour == GREEN) return "physical";
        if (colour == YELLOW) return "ranged";
        if (colour == DARK_BLUE) return "magic";
        if (colour == LIGHT_BLUE) return "resist";
        if (colour == RED) return "negative";
        return "none";
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/" + name + ".png");
    }

    public static Resources resourcesHeld(Animal animal) {
        Resources resources = new Resources("", 0, 0, 0, 0);
        for (ItemStack itemStack : ResourceSources.getFoodItemsFromAnimal(animal)) {
            ResourceSource res = ResourceSources.getFromItem(itemStack.getItem());
            if (res != null)
                resources.food += res.resourceValue * itemStack.getCount();
        }
        return resources;
    }

    private void drawEntityOnScreen(PoseStack poseStack, LivingEntity entity, float x, float y, float size) {
        float f = (float) Math.atan(-lookX / 40F);
        float g = (float) Math.atan(-lookY / 40F);
        PoseStack poseStackModel = RenderSystem.getModelViewStack();
        poseStackModel.pushPose();
        poseStackModel.translate(0, 0, 1050.0D);
        poseStackModel.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        poseStack.pushPose();
        // x/y go on the GUI pose (not the model-view stack) so the model follows the HUD scale
        poseStack.translate(x, y, 10.0D);
        poseStack.scale(size, size, size);
        float h = entity.yBodyRot; // bodyYaw;
        float i = entity.getYRot(); // getYaw();
        float j = entity.getXRot(); // getPitch();
        float k = entity.yHeadRotO; // prevHeadYaw;
        float l = entity.yHeadRot; // headYaw;
        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion2 = Axis.XP.rotationDegrees(g * 20.0F);
        quaternion.mul(quaternion2);
        poseStack.mulPose(quaternion);
        if (shouldAnimate()) {
            entity.yBodyRot = 180.0F + f * 20.0F;
            entity.setYRot(180.0F + f * 40.0F);
            entity.setXRot(-g * 20.0F);
            entity.yHeadRot = entity.getYRot();
            entity.yHeadRotO = entity.getYRot();
        }
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);
        quaternion2.conjugate();
        entityrenderdispatcher.overrideCameraOrientation(quaternion2);

        // for some reason this snippet causes drawLineBox to draw lines in completely wrong locations while in
        // spectator mode
        RenderSystem.runAsFancy(() -> {
            try {
                MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
                entityrenderdispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, immediate, 15728880);
                immediate.endBatch();
            } catch (ReportedException e) {
                //System.out.println("Caught reportedException: " + e);
            }
        });
        entityrenderdispatcher.setRenderShadow(true);

        if (shouldAnimate()) {
            entity.yBodyRot = h;
            entity.setYRot(i);
            entity.setXRot(j);
            entity.yHeadRotO = k;
            entity.yHeadRot = l;
        }
        poseStackModel.popPose();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
    }
}
