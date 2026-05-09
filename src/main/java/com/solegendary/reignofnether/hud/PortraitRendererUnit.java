package com.solegendary.reignofnether.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.abilities.ToggleShield;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.hud.passives.EnchantmentIcon;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitStatType;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.monsters.CreeperUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.apache.commons.lang3.text.WordUtils;
import org.joml.Quaternionf;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

// Renders a Unit's portrait including its animated head, name, healthbar, list of stats and UI frames for these

public class PortraitRendererUnit<T extends LivingEntity, M extends EntityModel<T>, R extends LivingEntityRenderer<T, M>> {
    private static final Minecraft MC = Minecraft.getInstance();

    public R renderer;
    public Model model;

    public final int frameWidth = 60;
    public final int frameHeight = 60;

    public final int statsWidth = 45;
    public final int statsHeight = 60;

    private final int size = 46;
    private final int offsetX = 31;
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

    public final int HERO_Y_OFFSET = 10;

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

    // Render the portrait including:
    // - background frame
    // - moving head
    // - healthbar
    // - unit name
    // Must be called from DrawScreenEvent
    public RectZone render(GuiGraphics guiGraphics, String name, int x, int y, int mouseX, int mouseY, LivingEntity entity) {
        if (entity instanceof HeroUnit)
            y -= HERO_Y_OFFSET;

        Relationship rs = UnitClientEvents.getPlayerToEntityRelationship(entity);
        int bgCol = PlayerColors.getPlayerPortraitDisplayColorHex(entity instanceof Unit u ? u.getOwnerName() : null);
        if (entity instanceof Player player) {
            bgCol = 0xA0000000 | PlayerColors.getPlayerAllianceColorHex(player.getName().getString());
        }
        int frameHeightPlus = 0;
        if (entity instanceof HeroUnit) {
            frameHeightPlus = HERO_Y_OFFSET;
        }
        MyRenderer.renderFrameWithBg(guiGraphics, x, y, frameWidth, frameHeight + frameHeightPlus, bgCol);

        // remember 0,0 is top left
        int drawX = x + offsetX;
        int drawY = y + (int) (entity.getEyeHeight() / standardEyeHeight * offsetY);

        //if (entity instanceof HeroUnit)
        //    drawY += HERO_Y_OFFSET;

        int sizeFinal = size;

        Pair<Integer, Integer> yAndScaleOffsets = PortraitRendererModifiers.getPortraitRendererModifiers(entity);

        drawY += yAndScaleOffsets.getFirst();// + UnitClientEvents.yOffset;
        sizeFinal += yAndScaleOffsets.getSecond();// + UnitClientEvents.scale;

        boolean hasBanner = false;
        ItemStack bannerStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (bannerStack.getItem() instanceof BannerItem) {
            hasBanner = true;
            entity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        drawEntityOnScreen(guiGraphics.pose(), entity, drawX, drawY, sizeFinal);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,0,2000);
        name = WordUtils.capitalize(name);

        if (rs != Relationship.OWNED && entity instanceof Unit unit && unit.getOwnerName().length() > 0) {
            name += " (" + unit.getOwnerName() + ")";
        }

        // draw name (unless a player, since their nametag will be rendered anyway)
        if (entity instanceof HeroUnit) {
            y -= 6;
        }
        int xOrig = x;
        if (entity instanceof Unit unit) {
            for (EnchantmentIcon passiveIcon : unit.getPassiveIcons()) {
                passiveIcon.render(guiGraphics, x - 2, y - 16, mouseX, mouseY);
                if (passiveIcon.isMouseOver(mouseX, mouseY))
                    passiveIcon.renderTooltip(guiGraphics, mouseX, mouseY);
                x += EnchantmentIcon.ICON_SIZE * 2;
            }
            if (!unit.getPassiveIcons().isEmpty())
                x += EnchantmentIcon.ICON_SIZE / 2;
        }
        if (!(entity instanceof Player)) {
            guiGraphics.drawString(Minecraft.getInstance().font, name, x + 4, y - 9, 0xFFFFFFFF);
        }
        x = xOrig;
        if (entity instanceof HeroUnit) {
            y += 14;
        }

        RectZone rectZone = RectZone.getZoneByLW(x, y, frameWidth, frameHeight);

        if (entity instanceof HeroUnit heroUnit) {
            // draw health bar and write min/max hp
            HealthBarClientEvents.renderForEntity(guiGraphics.pose(),
                    entity,
                    x + (frameWidth / 2f),
                    y + frameHeight - 22,
                    frameWidth - 9,
                    HealthBarClientEvents.RenderMode.GUI_PORTRAIT
            );
            HealthBarClientEvents.renderAbsorbForEntity(guiGraphics.pose(),
                    entity,
                    x + (frameWidth / 2f),
                    y + frameHeight - 22,
                    frameWidth - 9,
                    HealthBarClientEvents.RenderMode.GUI_PORTRAIT
            );
            HealthBarClientEvents.renderManaForEntity(guiGraphics.pose(),
                    heroUnit,
                    x + (frameWidth / 2f),
                    y + frameHeight - 12,
                    frameWidth - 9,
                    HealthBarClientEvents.RenderMode.GUI_PORTRAIT
            );
        } else {
            HealthBarClientEvents.renderForEntity(guiGraphics.pose(),
                    entity,
                    x + (frameWidth / 2f),
                    y + frameHeight - 14,
                    frameWidth - 9,
                    HealthBarClientEvents.RenderMode.GUI_PORTRAIT
            );
            HealthBarClientEvents.renderAbsorbForEntity(guiGraphics.pose(),
                    entity,
                    x + (frameWidth / 2f),
                    y + frameHeight - 14,
                    frameWidth - 9,
                    HealthBarClientEvents.RenderMode.GUI_PORTRAIT
            );
        }

        ArrayList<String> texts = new ArrayList<>();
        String healthText = "";
        float health = entity.getHealth();
        health += entity.getAbsorptionAmount();
        if (health >= 1) {
            healthText = String.valueOf((int) health);
        } else {
            healthText = String.valueOf(health).substring(0, 3);
        }
        float maxHealth = entity.getMaxHealth();
        if (entity.getAbsorptionAmount() > 0)
            maxHealth += MiscUtil.getMaxAbsorptionAmount(entity);

        healthText += "/" + ((int) maxHealth);
        texts.add(healthText);

        if (entity instanceof HeroUnit heroUnit) {
            String manaText = "";
            float mana = heroUnit.getMana();
            if (mana >= 1) {
                manaText = String.valueOf((int) mana);
            } else {
                manaText = String.valueOf(mana).substring(0, 3);
            }
            manaText += "/" + (int) heroUnit.getMaxMana();
            texts.add(manaText);
        }

        renderStatText(texts, entity, x, (entity instanceof HeroUnit ? y - HERO_Y_OFFSET + 2 : y), guiGraphics);

        if (hasBanner)
            entity.setItemSlot(EquipmentSlot.HEAD, bannerStack);

        return rectZone;
    }

    private void renderStatText(List<String> texts, LivingEntity entity, int x, int y, GuiGraphics guiGraphics) {
        // need to render like this instead of GuiComponent.drawCenteredString, so it's layered above the portrait entity
        Minecraft MC = Minecraft.getInstance();
        MultiBufferSource.BufferSource multibuffersource$buffersource =
                MultiBufferSource.immediate(Tesselator.getInstance()
                        .getBuilder());

        for (int i = 0; i < texts.size(); i++) {
            FormattedCharSequence pTooltips = FormattedCharSequence.forward(texts.get(i), Style.EMPTY);
            ClientTooltipComponent clientTooltip = ClientTooltipComponent.create(pTooltips);
            guiGraphics.pose().translate(0.0, 0.0, 400.0);
            int x0 = x + (frameWidth / 2);
            int xC = (x0 - MC.font.width(texts.get(i)) / 2);
            clientTooltip.renderText(MC.font,
                    xC,
                    y + frameHeight - 2 + ((i-1) * 10),
                    guiGraphics.pose().last().pose(),
                    multibuffersource$buffersource
            );
        }
        multibuffersource$buffersource.endBatch();
        guiGraphics.pose().popPose();
    }

    private class RenderedStat {
        public ResourceLocation icon;
        public String text;
        public UnitStatType type;
        public int colour = 0xFFFFFF;
        public int textXOffset = 0;

        public RenderedStat(ResourceLocation icon, String text, UnitStatType type) {
            this.icon = icon;
            this.text = text;
            this.type = type;
        }
        public RenderedStat(ResourceLocation icon, String text, UnitStatType type, int colour, int xOffset) {
            this.icon = icon;
            this.text = text;
            this.type = type;
            this.colour = colour;
            this.textXOffset = xOffset;
        }
    }

    public RectZone renderStats(GuiGraphics guiGraphics, String name, int x, int y, int mouseX, int mouseY, Unit unit) {
        int statsHeightPlus = 0;
        if (unit instanceof HeroUnit) {
            y -= HERO_Y_OFFSET;
            statsHeightPlus = HERO_Y_OFFSET;
        }
        MyRenderer.renderFrameWithBg(guiGraphics, x, y, statsWidth, statsHeight + statsHeightPlus, 0xA0000000);

        int blitXIcon = x + 6;
        int blitYIcon = y + 7;

        ArrayList<RenderedStat> renderedStats = new ArrayList<>();

        if (unit instanceof AttackerUnit attackerUnit) {
            double atkDmg = attackerUnit.getUnitAttackDamage() + AttackerUnit.getWeaponDamageModifier(attackerUnit);
            if (unit instanceof CreeperUnit cUnit && cUnit.isPowered()) {
                atkDmg *= CreeperUnit.CHARGED_DAMAGE_MULT;
            }
            if (unit instanceof WorkerUnit) {
                atkDmg = (int) attackerUnit.getUnitAttackDamage();
            }
            double atkDmgRounded = Math.round(atkDmg);
            String atkStr;
            if (Math.abs(atkDmgRounded - atkDmg) < 0.1d) {
                atkStr = String.valueOf((int) atkDmgRounded); // attacks per second
            } else {
                DecimalFormat df1 = new DecimalFormat("###.#");
                atkStr = String.valueOf(df1.format(atkDmg)); // attacks per second
            }
            if (unit instanceof Mob mob && mob.isVehicle() && mob.getFirstPassenger() instanceof AttackerUnit passenger) {
                atkStr += "+" + (int) passenger.getUnitAttackDamage();
            }
            renderedStats.add(new RenderedStat(
                    ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/icons/items/sword.png"),
                    atkStr,
                    UnitStatType.ATTACK_DAMAGE,
                    attackerUnit.hasBonusDamage() ? 0xFF2BFF2B : 0xFFFFFFFF,
                    0
            ));
            DecimalFormat df2 = new DecimalFormat("###.##");

            int atkSpdColour = 0xFFFFFFFF;
            float attacksPerSecond = Math.round(attackerUnit.getAttacksPerSecond() * 100f) / 100f;
            float baseAttacksPerSecond = Math.round(attackerUnit.getBaseAttacksPerSecond() * 100f) / 100f;

            if (attacksPerSecond > baseAttacksPerSecond) {
                atkSpdColour = 0xFF2BFF2B;
            } else if (attacksPerSecond < baseAttacksPerSecond) {
                atkSpdColour = 0xFFFC3838;
            }
            renderedStats.add(new RenderedStat(
                    ResourceLocation.fromNamespaceAndPath("reignofnether","textures/icons/items/sparkler.png"),
                    String.valueOf(df2.format(attackerUnit.getAttacksPerSecond())),
                    UnitStatType.ATTACK_SPEED,
                    atkSpdColour,
                    0
            ));
            String rangeStr;
            BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison(unit);
            GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
            if (garr != null) {
                rangeStr = String.valueOf(garr.getAttackRange());
            } else {
                rangeStr = String.valueOf((int) (attackerUnit.getAttackRange()));
            }
            renderedStats.add(new RenderedStat(
                    ResourceLocation.fromNamespaceAndPath("reignofnether","textures/icons/items/bow.png"),
                    rangeStr,
                    UnitStatType.RANGE
            ));
        }

        int armourColor = 0xFFFFFFFF;
        double physicalArmour = unit.getUnitPhysicalArmorPercentage();
        double rangedArmour = unit.getUnitRangedArmorPercentage();
        double magicArmour = unit.getUnitMagicArmorPercentage();
        double resistArmour = unit.getUnitResistPercentage();
        double avgArmourInv = 1;

        String armourStr = "0%";
        int nonZeroArmourTypes = 0;

        if (physicalArmour != 0) {
            armourStr = (int) (physicalArmour * 100) + "%";
            armourColor = 0xFF2BFF2B;
            avgArmourInv *= (1 - physicalArmour);
            nonZeroArmourTypes += 1;
        }
        if (rangedArmour != 0) {
            armourStr = (int) (rangedArmour * 100) + "%";
            armourColor = 0xFFFCFC2B;
            avgArmourInv *= (1 - rangedArmour);
            nonZeroArmourTypes += 1;
        }
        if (resistArmour != 0) {
            armourStr = (int) (resistArmour * 100) + "%";
            armourColor = 0xFF42DDDD;
            avgArmourInv *= (1 - resistArmour);
            nonZeroArmourTypes += 1;
        }
        if (magicArmour != 0 && resistArmour <= 0) { // resistArmour includes magicArmour
            armourStr = (int) (magicArmour * 100) + "%";
            armourColor = 0xFF5B5BFC;
            avgArmourInv *= (1 - magicArmour);
            nonZeroArmourTypes += 1;
        }
        if (nonZeroArmourTypes > 1) {
            armourStr = "~" + (int) ((1 - avgArmourInv) * 100) + "%";
            armourColor = 0xFF42DDDD;
        }
        if (armourStr.contains("~") && armourStr.contains("-")) {
            armourStr = armourStr.replace("~", "");
        }
        if (armourStr.contains("-")) {
            armourColor = 0xFFFC3838;
        }

        renderedStats.add(new RenderedStat(
                ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/icons/items/chestplate.png"),
                armourStr,
                UnitStatType.ARMOUR,
                armourColor,
                armourStr.startsWith("~") || armourStr.startsWith("-") ? -4 : 0
        ));
        AttributeInstance ms = ((LivingEntity) unit).getAttribute(Attributes.MOVEMENT_SPEED);
        int msInt = ms != null ? (int) (ms.getValue() * 101) : 0;
        if (unit instanceof Slime) {
            msInt *= 0.45f;
        }
        if (unit instanceof BruteUnit pbUnit && pbUnit.isHoldingUpShield) {
            msInt *= ToggleShield.MOVESPEED_MULTIPLIER;
        }
        int msColour = 0xFFFFFFFF;
        double msAttr = ((Mob) unit).getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        if (msAttr < unit.getMovementSpeed() || (unit instanceof BruteUnit bruteUnit && bruteUnit.isHoldingUpShield)) {
            msColour = 0xFF2BFF2B;
        } else if (msAttr > unit.getMovementSpeed()) {
            msColour = 0xFFFC3838;
        }
        renderedStats.add(new RenderedStat(
                ResourceLocation.fromNamespaceAndPath("reignofnether","textures/icons/items/boots.png"),
                String.valueOf(msInt),
                UnitStatType.MOVEMENT_SPEED,
                msColour,
                0
        ));

        if (unit instanceof HeroUnit heroUnit) {
            int heroLvl = heroUnit.getHeroLevel();
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    fcs((heroLvl >= 10 ? "Lv " : "Lvl ") + heroLvl, true),
                    blitXIcon + 1,
                    blitYIcon - 1,
                    0xFFFFFFFF
            );
            blitYIcon += HERO_Y_OFFSET + 1;
        }

        // render based on prepped strings/icons
        for (RenderedStat renderedStat : renderedStats) {
            MyRenderer.renderIcon(guiGraphics, renderedStat.icon, blitXIcon, blitYIcon, 8);

            boolean isMouseOver =
                    mouseX >= blitXIcon - 1 &&
                    mouseY >= blitYIcon - 1 &&
                    mouseX < blitXIcon + statsWidth - 11 &&
                    mouseY < blitYIcon + 9;

            // highlight on hover
            if (isMouseOver) {
                guiGraphics.fill( // x1,y1, x2,y2,
                    blitXIcon - 1,
                    blitYIcon - 1,
                    blitXIcon + statsWidth - 11,
                    blitYIcon + 9,
                    0x32FFFFFF);
                MyRenderer.renderTooltip(guiGraphics, unit.getStatTooltip(renderedStat.type), mouseX, mouseY);
            }

            guiGraphics.drawString(
                Minecraft.getInstance().font,
                renderedStat.text,
                blitXIcon + 13 + renderedStat.textXOffset,
                blitYIcon,
                renderedStat.colour
            );
            blitYIcon += 10;
        }
        return RectZone.getZoneByLW(x, y, statsWidth, statsHeight);
    }

    public RectZone renderHeroLevelAndExp(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, HeroUnit heroUnit) {
        int width = 101;
        int height = 5;

        y -= HERO_Y_OFFSET;

        ResourceLocation expBarEmptyRl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/experience_bar_empty.png");
        RenderSystem.setShaderTexture(0, expBarEmptyRl);
        guiGraphics.blit(expBarEmptyRl,
                x, y, 0,
                0,0, // where on texture to start drawing from
                width, height, // dimensions of blit texture
                width, height // size of texture itself (if < dimensions, texture is repeated)
        );
        ResourceLocation expBarFullRl = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/experience_bar_full.png");
        RenderSystem.setShaderTexture(0, expBarFullRl);
        float expPercent = (float) heroUnit.getExpOnCurrentLevel() / heroUnit.getExpToNextlevel();
        if (heroUnit.getHeroLevel() >= HeroUnit.MAX_LEVEL)
            expPercent = 1.0f;
        guiGraphics.blit(expBarFullRl,
                x, y, 0,
                0,0, // where on texture to start drawing from
                Math.round((float) width * expPercent), height, // dimensions of blit texture
                width, height // size of texture itself (if < dimensions, texture is repeated)
        );
        return RectZone.getZoneByLW(x, y-2, width, height+4);
    }


    public RectZone renderResourcesHeld(GuiGraphics guiGraphics, int x, int y, Unit unit) {
        Resources resources = Resources.getTotalResourcesFromItems(unit.getItems());
        return renderResourcesHeld(guiGraphics, x, y, resources, Unit.atMaxResources(unit));
    }

    public RectZone renderResourcesHeld(GuiGraphics guiGraphics, int x, int y, Animal animal) {
        Resources resources = new Resources("",0,0,0);
        for (ItemStack itemStack : ResourceSources.getFoodItemsFromAnimal(animal)) {
            ResourceSource res = ResourceSources.getFromItem(itemStack.getItem());
            if (res != null)
                resources.food += res.resourceValue * itemStack.getCount();
        }
        return renderResourcesHeld(guiGraphics, x, y, resources, false);
    }

    public RectZone renderResourcesHeld(GuiGraphics guiGraphics, int x, int y, Resources resources, boolean redText) {
        MyRenderer.renderFrameWithBg(guiGraphics, x, y, statsWidth, statsHeight, 0xA0000000);

        int blitXIcon = x + 6;
        int blitYIcon = y + 7;

        // prep strings/icons to render
        List<ResourceLocation> textureStatIcons = List.of(ResourceLocation.fromNamespaceAndPath("reignofnether",
                "textures/icons/items/wheat.png"
            ),
            ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/icons/items/wood.png"),
            ResourceLocation.fromNamespaceAndPath("reignofnether", "textures/icons/items/iron_ore.png")
        );

        List<String> statStrings = List.of(String.valueOf(resources.food),
            String.valueOf(resources.wood),
            String.valueOf(resources.ore)
        );

        // render based on prepped strings/icons
        for (int i = 0; i < statStrings.size(); i++) {

            if (!statStrings.get(i).equals("0")) {
                MyRenderer.renderIcon(guiGraphics, textureStatIcons.get(i), blitXIcon, blitYIcon, 8);
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    statStrings.get(i),
                    blitXIcon + 12,
                    blitYIcon,
                    redText ? 0xFF2525 : 0xFFFFFF
                );
                blitYIcon += 10;
            }
        }
        return RectZone.getZoneByLW(x, y, statsWidth, statsHeight);
    }

    private void drawEntityOnScreen(PoseStack poseStack, LivingEntity entity, int x, int y, int size) {
        float f = (float) Math.atan(-lookX / 40F);
        float g = (float) Math.atan(-lookY / 40F);
        PoseStack poseStackModel = RenderSystem.getModelViewStack();
        poseStackModel.pushPose();
        poseStackModel.translate(x, y, 1050.0D);
        poseStackModel.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 10.0D);
        poseStack.scale((float) size, (float) size, (float) size);
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
