package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.UnitItemButton;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.items.unititems.EmptyUnitItem;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // UnitItem that the player right-clicked or is left-click dragging
    // Used for: dropping, giving to another unit, selling and rearranging inventory
    public static UnitItem actionableUnitItem = null;
    public static int actionableInvIndex = 0;
    public static UUID actionableInvUUID = null;

    private static int mouseX = 0;
    private static int mouseY = 0;
    private static int mouseLeftDownX = 0;
    private static int mouseLeftDownY = 0;

    public static final ArrayList<Button> renderedButtons = new ArrayList<>();

    // items moused over
    private static final ArrayList<ItemEntity> preselectedItems = new ArrayList<>();

    public static void addPreselectedItem(ItemEntity itemEntity) {
        if (!FogOfWarClientEvents.isInBrightChunk(itemEntity))
            return;
        preselectedItems.add(itemEntity);
    }
    public static void clearPreselectedItems() {
        preselectedItems.clear();
    }
    public static ArrayList<ItemEntity> getPreselectedItems() {
        return preselectedItems;
    }

    public static boolean hasActionableItem() {
        return actionableUnitItem != null && (mouseX != mouseLeftDownX || mouseY != mouseLeftDownY);
    }

    public static boolean shouldRenderUnitInventory(Unit unit) {
        return unit instanceof UnitInventory inv &&
                (unit instanceof HeroUnit ||
                        !inv.getAllItems().isEmpty());
    }

    public static void syncInventory(int unitId, List<ItemStack> items) {
        if (MC.level != null && MC.level.getEntity(unitId) instanceof UnitInventory inv)
            for (int i = 0; i < items.size() && i < inv.getAllItems().size(); i++)
                inv.set(i, items.get(i));
    }

    /*
    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_SPACE) {
            if (HudClientEvents.hudSelectedEntity instanceof UnitInventory inv) {
                if (Keybindings.shiftMod.isDown())
                    inv.tryAdding(new ItemStack(Items.DIAMOND_SWORD));
                else if (Keybindings.ctrlMod.isDown())
                    inv.tryAdding(new ItemStack(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get()));
                else
                    inv.tryAdding(new ItemStack(Items.TOTEM_OF_UNDYING));
            }
        }
    }
     */

    private static final int BUTTON_WIDTH = 22;
    public static final int INV_WIDTH = BUTTON_WIDTH * 2;
    public static final int INV_HEIGHT = BUTTON_WIDTH * 3;

    public static RectZone renderUnitInventory(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, UnitInventory inv) {
        ItemClientEvents.renderedButtons.clear();
        for (int i = 0; i < inv.getAllItems().size(); i++) {
            ItemStack itemStack = inv.getAllItems().get(i);
            UnitItem unitItem = ItemUtil.getUnitItem(itemStack.getItem());
            if (unitItem instanceof EmptyUnitItem emptyItem) {
                ItemClientEvents.renderedButtons.add(emptyItem.getEmptySlotButton(i, hasActionableItem(), (Unit) inv));
            } else if (unitItem != null) {
                ItemClientEvents.renderedButtons.add(unitItem.getButton(i, itemStack, (Unit) inv));
            }
        }
        int i = 0;
        for (Button button : ItemClientEvents.renderedButtons) {
            int xi = i % 2 == 0 ? x : x + BUTTON_WIDTH;
            int yi = y + ((i / 2) * BUTTON_WIDTH);
            button.render(guiGraphics, xi, yi, mouseX, mouseY);
            i += 1;
        }

        for (Button button : ItemClientEvents.renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(guiGraphics, mouseX, mouseY);

        return RectZone.getZoneByLW(x, y, INV_WIDTH, INV_HEIGHT);
    }

    @SubscribeEvent
    public static void onLeftMouseRelease(ScreenEvent.MouseButtonReleased.Post evt) {
        if (MC.player == null || evt.getButton() != GLFW.GLFW_MOUSE_BUTTON_1)
            return;

        for (Button button : renderedButtons)
            button.checkClickedReleased((int) evt.getMouseX(), (int) evt.getMouseY(), true);

        if (hasActionableItem() &&
            HudClientEvents.hudSelectedEntity instanceof UnitInventory inv &&
            HudClientEvents.hudSelectedEntity instanceof Unit unit
        ) {
            String playerName = MC.player.getName().getString();
            Button mousedOverButton = getMousedOverButton();
            Button hudMousedOverButton = HudClientEvents.getMousedOverButton();
            if (mousedOverButton instanceof UnitItemButton uiButton) {
                inv.swapSlots(actionableInvIndex, uiButton.invIndex);
                ItemServerboundPacket.swap(playerName, ((Entity) inv).getId(), actionableInvIndex, uiButton.invIndex);
            } else if (hudMousedOverButton != null &&
                    hudMousedOverButton.entity instanceof HeroUnit &&
                    hudMousedOverButton.entity != HudClientEvents.hudSelectedEntity &&
                    hudMousedOverButton.entity instanceof UnitInventory) {
                Relationship rlu = UnitClientEvents.getPlayerToEntityRelationship(hudMousedOverButton.entity);
                if (rlu == Relationship.FRIENDLY || rlu == Relationship.OWNED) {
                    // Give via group button
                    unit.getCheckpoints().clear();
                    unit.getCheckpoints().add(new Checkpoint(hudMousedOverButton.entity, true));
                    ItemServerboundPacket.give(playerName, ((Entity) inv).getId(), actionableInvUUID, hudMousedOverButton.entity.getId());
                }
            } else if (!HudClientEvents.isMouseOverAnyButtonOrHud()) {
                BuildingPlacement bpl = BuildingClientEvents.getPreselectedBuilding();
                Relationship rl = bpl != null ? BuildingClientEvents.getPlayerToBuildingRelationship(bpl) : null;

                if (!UnitClientEvents.getPreselectedUnits().isEmpty()) {
                    LivingEntity le = UnitClientEvents.getPreselectedUnits().get(0);
                    Relationship rlu = UnitClientEvents.getPlayerToEntityRelationship(le);
                    if (le instanceof HeroUnit &&
                        le instanceof UnitInventory &&
                        le != HudClientEvents.hudSelectedEntity &&
                        (rlu == Relationship.FRIENDLY || rlu == Relationship.OWNED)) {
                        // Give via direct entity
                        unit.getCheckpoints().clear();
                        unit.getCheckpoints().add(new Checkpoint(le, true));
                        ItemServerboundPacket.give(playerName, ((Entity) inv).getId(), actionableInvUUID, le.getId());
                    }
                } else if (bpl != null && bpl.getBuilding() instanceof AbstractMarket &&
                        (rl == Relationship.FRIENDLY || rl == Relationship.OWNED)) {
                    // sell at market
                    unit.getCheckpoints().clear();
                    unit.getCheckpoints().add(new Checkpoint(bpl.originPos, true));
                    ItemServerboundPacket.sell(playerName, ((Entity) inv).getId(), actionableInvUUID, bpl.originPos);
                } else {
                    BlockPos bp = CursorClientEvents.getPreselectedBlockPos();
                    // drop on ground
                    unit.getCheckpoints().clear();
                    unit.getCheckpoints().add(new Checkpoint(bp, true));
                    ItemServerboundPacket.drop(playerName, ((Entity) inv).getId(), actionableInvUUID, bp);
                }
            }
        }
        actionableUnitItem = null;
        mouseLeftDownX = 0;
        mouseLeftDownY = 0;
    }

    private static Button getMousedOverButton() {
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        return null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS ||
                HudClientEvents.isMouseOverAnyButtonOrHud())
            return;
        if (MC.level != null && OrthoviewClientEvents.isEnabled()) {
            for (ItemEntity itemEntity : preselectedItems) {
                ResourceSource res = ResourceSources.getFromItem(itemEntity.getItem().getItem());
                boolean isResourceItem = res != null && res.resourceValue > 0;
                if (ItemUtil.isUnitItem(itemEntity) || isResourceItem || ItemUtil.isPreparedEdibleFood(itemEntity.getItem().getItem())) {
                    MyRenderer.drawBoxBottom(
                            evt.getPoseStack(),
                            itemEntity.getBoundingBox().inflate(0.25, 0, 0.25),
                            1, 1, 1,
                            CursorClientEvents.isRightClickDown() ? 1.0f : 0.25f
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();
        // clear to avoid hiding ghost renders if the player happens to mouse back over this exact pixel
        if (mouseX != mouseLeftDownX || mouseY != mouseLeftDownY) {
            mouseLeftDownX = 0;
            mouseLeftDownY = 0;
        }

        if (OrthoviewClientEvents.isEnabled() && MC.screen instanceof TopdownGui) {
            for (ItemEntity itemEntity : preselectedItems) {
                UnitItem unitItem = ItemUtil.getUnitItem(itemEntity.getItem().getItem());
                if (unitItem != null && unitItem.enableTooltip) {
                    MyRenderer.renderItemEntityTooltip(evt.getGuiGraphics(), unitItem, itemEntity.getItem(), evt.getMouseX(), evt.getMouseY());
                    break;
                } else if (ItemUtil.isPreparedEdibleFood(itemEntity.getItem().getItem())) {
                    UnitItem foodUnitItem = new EdibleFoodItem(itemEntity.getItem().getItem());
                    if (foodUnitItem.enableTooltip) {
                        MyRenderer.renderTooltip(evt.getGuiGraphics(), foodUnitItem.getTooltip(itemEntity.getItem()), evt.getMouseX(), evt.getMouseY());
                    }
                    break;
                }
            }
            if (hasActionableItem() && HudClientEvents.hudSelectedEntity instanceof Unit unit) {
                actionableUnitItem.getButton(0, new ItemStack(actionableUnitItem.item), unit)
                        .renderGhost(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
            }
        }
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!(MC.screen instanceof TopdownGui))
            return;
        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
            }
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            mouseLeftDownX = (int) evt.getMouseX();
            mouseLeftDownY = (int) evt.getMouseY();
        }
    }
}
