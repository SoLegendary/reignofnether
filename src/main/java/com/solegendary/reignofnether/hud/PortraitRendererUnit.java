package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.unit.units.monsters.WretchedWraithUnit;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.UnitStatType;
import com.solegendary.reignofnether.unit.controls.SlimeJumpMoveControl;
import com.solegendary.reignofnether.unit.modelling.renderers.EnchanterRenderer;
import com.solegendary.reignofnether.unit.modelling.renderers.RoyalGuardRenderer;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.monsters.SlimeUnit;
import com.solegendary.reignofnether.unit.units.villagers.EnchanterUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RoyalGuardUnit;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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
     * Draws the selected entity's animated 3D model inside a box (HUD units): the whole body, plus its mount and
     * riders, scaled from their bounding boxes to fit the box with the feet near the bottom.
     */
    public void drawEntity(GuiGraphics g, LivingEntity entity, int x, int y, int w, int h) {
        Entity root = entity.getRootVehicle();
        List<Entity> stack = root.getSelfAndPassengers().toList();

        float width = 0, height = 0;
        for (Entity e : stack) {
            float rs = rendererScale(e);
            width = Math.max(width, e.getBbWidth() * rs);
            height = Math.max(height, (float) (e.getY() - root.getY()) + e.getBbHeight() * rs);
        }
        float pad = 4;
        // ponytail: the hitbox, plus headroom for models that overshoot it (shields,
        // helmets); long mobs turning sideways (ravager) can still touch the edge, Clip keeps it in
        float scale = Math.min((w - 2 * pad) / (width * 1.3f), (h - 2 * pad) / (height * 1.2f));
        float drawY = y + h - pad; // feet on the bottom: the headroom goes on top, clear of the level badge
        drawEntityOnScreen(g.pose(), root, stack, x + w / 2f, drawY, scale);
    }

    // how far the renderer scales the model past the hitbox. Hitboxes already include the scale for nearly every mob
    // (wither skeletons, slimes by size, the necromancer's registration); these two are registered at vindicator size
    // and their hitbox only grows with getScale(), not with SCALE_MULT; the wretched wraith's model is about twice
    // its zombie-sized hitbox
    private static float rendererScale(Entity e) {
        if (e instanceof WretchedWraithUnit)
            return 2f;
        if (e instanceof RoyalGuardUnit)
            return RoyalGuardRenderer.SCALE_MULT;
        return e instanceof EnchanterUnit ? EnchanterRenderer.SCALE_MULT : 1f;
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
            if (dmgColour == WHITE && attackerUnit.getBuildingDamageBonus() > 0)
                dmgColour = GREEN;
            stats.add(new Stat(icon("sword"), UnitStatType.ATTACK_DAMAGE, atkStr, dmgColour));

            float aps = Math.round(attackerUnit.getNonBaseAttacksPerSecond() * 100f) / 100f;
            float baseAps = Math.round(attackerUnit.getBaseAttacksPerSecond() * 100f) / 100f;
            int apsColour = aps > baseAps ? GREEN : aps < baseAps ? RED : WHITE;
            stats.add(new Stat(icon("sparkler"), UnitStatType.ATTACK_SPEED,
                    new DecimalFormat("###.##").format(attackerUnit.getAttacksPerSecond()), apsColour));

            BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison(unit);
            GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            String range;
            if (((LivingEntity) attackerUnit).getFirstPassenger() instanceof AttackerUnit passengerUnit)
                range = String.valueOf((int) passengerUnit.getAttackRange());
            else
                range = garr != null ? String.valueOf(garr.getAttackRange()) : String.valueOf((int) attackerUnit.getAttackRange());
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

    /** The armour stat; its colour tells the armour type. */
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
        if (((Mob) unit).hasEffect(MobEffectRegistrar.ENCHANTED_TETHER.get())) {
            colour = GREEN;
            str = types > 0 && highest > 0 ? (int) (Mth.lerp(0.5, highest, 1.0) * 100) + "%+" : "50%";
        }
        if (str.contains("-"))
            colour = RED;
        return new Stat(icon("chestplate"), UnitStatType.ARMOUR, str, colour);
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

    private void drawEntityOnScreen(PoseStack poseStack, Entity root, List<Entity> stack, float x, float y, float size) {
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
        Quaternionf quaternion = Axis.ZP.rotationDegrees(180.0F);
        Quaternionf quaternion2 = Axis.XP.rotationDegrees(g * 20.0F);
        quaternion.mul(quaternion2);
        poseStack.mulPose(quaternion);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityrenderdispatcher.setRenderShadow(false);
        quaternion2.conjugate();
        entityrenderdispatcher.overrideCameraOrientation(quaternion2);

        boolean animate = shouldAnimate();
        // counteract siege mode sinking
        double siegeLift = root instanceof RavagerUnit ravagerUnit ? ravagerUnit.getSiegeSinkOffset(1f) : 0;
        for (Entity entity : stack) {
            // riders sit straight on top of their mount; horizontal seat offsets are dropped since we rotate the model
            double dy = entity.getY() - root.getY() + siegeLift;
            float h = 0, i = entity.getYRot(), j = entity.getXRot(), k = 0, l = 0;
            LivingEntity living = entity instanceof LivingEntity le ? le : null;
            if (animate) {
                if (living != null) {
                    h = living.yBodyRot; k = living.yHeadRotO; l = living.yHeadRot;
                    living.yBodyRot = 180.0F + f * 20.0F;
                    living.yHeadRot = 180.0F + f * 40.0F;
                    living.yHeadRotO = living.yHeadRot;
                }
                entity.setYRot(180.0F + f * 40.0F);
                entity.setXRot(-g * 20.0F);
            }
            ItemStack bannerStack = living != null ? living.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY;
            boolean hasBanner = bannerStack.getItem() instanceof BannerItem;
            if (hasBanner)
                living.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

            // for some reason this snippet causes drawLineBox to draw lines in completely wrong locations while in
            // spectator mode
            RenderSystem.runAsFancy(() -> {
                try {
                    MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
                    if (entity instanceof EnchanterUnit enchanterUnit)
                        enchanterUnit.renderTether = false;
                    entityrenderdispatcher.render(entity, 0.0D, dy, 0.0D, 0.0F, 1.0F, poseStack, immediate, 15728880);
                    immediate.endBatch();
                    if (entity instanceof EnchanterUnit enchanterUnit)
                        enchanterUnit.renderTether = true;
                } catch (ReportedException e) {
                    //System.out.println("Caught reportedException: " + e);
                }
            });

            if (hasBanner)
                living.setItemSlot(EquipmentSlot.HEAD, bannerStack);
            if (animate) {
                if (living != null) {
                    living.yBodyRot = h; living.yHeadRotO = k; living.yHeadRot = l;
                }
                entity.setYRot(i);
                entity.setXRot(j);
            }
        }
        entityrenderdispatcher.setRenderShadow(true);

        poseStackModel.popPose();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        // the model leaves its depth at GUI z ~1040, in front of everything drawn after it (level badge, tooltips);
        // the caller's Clip scissor keeps this clear to the portrait box
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }
}
