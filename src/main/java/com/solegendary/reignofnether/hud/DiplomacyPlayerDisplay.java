package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AllianceAction;
import com.solegendary.reignofnether.alliance.AllianceServerboundPacket;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerboundPacket;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

// Dashboard for RTS players to make/break alliances and send resources to each other
public class DiplomacyPlayerDisplay extends AbstractPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();
    private final Resources resourcesToSend;

    public DiplomacyPlayerDisplay(RTSPlayer rtsPlayer) {
        super(rtsPlayer);
        resourcesToSend = new Resources(rtsPlayer.name,0,0,0);
    }

    private static final int RESOURCE_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 2; // frame containing a resource value + icon
    private static final int ALLIANCE_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 4; // frame containing a resource value + icon
    public static final int DISPLAY_WIDTH = PLAYER_FRAME_WIDTH + RESOURCE_FRAME_WIDTH * 4 + ALLIANCE_FRAME_WIDTH; // total width of a player display

    private boolean isAllied() {
        return MC.player != null && AlliancesClient.isAllied(MC.player.getName().getString(), rtsPlayer.name);
    }

    private boolean allianceRequested() {
        return AlliancesClient.outboundPendingAlliances.contains(rtsPlayer.name);
    }

    private boolean allianceReceived() {
        return AlliancesClient.inboundPendingAlliances.contains(rtsPlayer.name);
    }

    private Button renderTradeResources(GuiGraphics guiGraphics, ResourceName resourceName,
                                      int x, int y, int mouseX, int mouseY) {
        String iconPath;
        String value;
        int color;
        switch (resourceName) {
            case FOOD -> {
                iconPath = "textures/icons/items/wheat.png";
                value = String.valueOf(resourcesToSend.food);
                color = 0xE8BC5F;
            }
            case WOOD -> {
                iconPath = "textures/icons/items/wood.png";
                value = String.valueOf(resourcesToSend.wood);
                color = 0xA3753B;
            }
            default -> {
                iconPath = "textures/icons/items/iron_ore.png";
                value = String.valueOf(resourcesToSend.ore);
                color = 0xFFF4ED;
            }
        }
        MyRenderer.renderFrameWithBg(guiGraphics,
                x, y,
                RESOURCE_FRAME_WIDTH,
                Button.DEFAULT_ICON_FRAME_SIZE,
                0xA0000000
        );
        guiGraphics.drawString(
                MC.font,
                value,
                x + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                color
        );

        Button changeResourceButton = new Button(
                "Change resources",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/corner_plus.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                this::isPlayerLoggedIn,
                () -> {
                    switch (resourceName) {
                        case FOOD -> resourcesToSend.food += Keybindings.shiftMod.isDown() ? 1000 : 100;
                        case WOOD -> resourcesToSend.wood += Keybindings.shiftMod.isDown() ? 1000 : 100;
                        default -> resourcesToSend.ore += Keybindings.shiftMod.isDown() ? 1000 : 100;
                    }
                    Resources res = ResourcesClientEvents.getOwnResources();
                    if (res != null) {
                        resourcesToSend.food = Math.min(resourcesToSend.food, res.food);
                        resourcesToSend.wood = Math.min(resourcesToSend.wood, res.wood);
                        resourcesToSend.ore = Math.min(resourcesToSend.ore, res.ore);
                    }
                },
                () -> {
                    switch (resourceName) {
                        case FOOD -> resourcesToSend.food -= Keybindings.shiftMod.isDown() ? 1000 : 100;
                        case WOOD -> resourcesToSend.wood -= Keybindings.shiftMod.isDown() ? 1000 : 100;
                        default -> resourcesToSend.ore -= Keybindings.shiftMod.isDown() ? 1000 : 100;
                    }
                    resourcesToSend.food = Math.max(resourcesToSend.food, 0);
                    resourcesToSend.wood = Math.max(resourcesToSend.wood, 0);
                    resourcesToSend.ore = Math.max(resourcesToSend.ore, 0);
                },
                switch (resourceName) {
                    case FOOD -> List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.send_food"), Style.EMPTY));
                    case WOOD -> List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.send_wood"), Style.EMPTY));
                    case ORE -> List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.send_ore"), Style.EMPTY));
                    case NONE -> null;
                }
        );
        changeResourceButton.bgIconResource = new ResourceLocation(ReignOfNether.MOD_ID, iconPath);
        changeResourceButton.render(guiGraphics, x + RESOURCE_FRAME_WIDTH, y, mouseX, mouseY);

        if (!isPlayerLoggedIn()) {
            guiGraphics.fill(
                    x, y,
                    x + RESOURCE_FRAME_WIDTH,
                    y + Button.DEFAULT_ICON_FRAME_SIZE,
                    0x99000000
            );
        }
        return changeResourceButton;
    }

    private List<Button> renderTradeConfirms(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        Button confirmButton = new Button(
                "Confirm trade",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> resourcesToSend.getTotalValue() > 0 && isPlayerLoggedIn(),
                this::sendResources,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.confirm_send_resources"), Style.EMPTY))
        );
        confirmButton.render(guiGraphics, x, y, mouseX, mouseY);

        Button cancelButton = new Button(
                "Cancel trade",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> resourcesToSend.getTotalValue() > 0,
                this::resetResources,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.cancel_send_resources"), Style.EMPTY))
        );
        cancelButton.render(guiGraphics, x + Button.DEFAULT_ICON_FRAME_SIZE, y, mouseX, mouseY);

        return List.of(confirmButton, cancelButton);
    }

    private Button renderAllianceButton(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        Button allyRequestButton = new Button(
                "Request Alliance",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sweet_berries.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                this::isPlayerLoggedIn,
                this::requestAlliance,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.request_alliance"), Style.EMPTY))
        );
        Button allyCancelRequestButton = new Button(
                "Cancel Request Alliance",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                this::cancelAllianceRequest,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.cancel_request_alliance"), Style.EMPTY))
        );
        allyCancelRequestButton.bgIconResource = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sweet_berries.png");
        Button allyConfirmButton = new Button(
                "Accept Alliance",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                this::acceptAllianceRequest,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.accept_alliance"), Style.EMPTY))
        );
        Button disbandButton = new Button(
                "Disband Alliance",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/crossed_swords.png"),
                (Keybinding) null,
                () -> false,
                () -> true,
                this::isAllied,
                this::disbandAlliance,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.disband_alliance"), Style.EMPTY))
        );

        Button renderedButton;
        String allianceStatusStr;
        int frameBgColour = 0xA0000000 | PlayerColors.getPlayerAllianceColorHex(rtsPlayer.name);
        int frameWidth = 50;

        if (!isAllied() && !allianceRequested() && !allianceReceived()) {
            allianceStatusStr = "Enemy";
            allyRequestButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = allyRequestButton;
            frameWidth = 46;
        } else if (!isAllied() && allianceRequested()) {
            allianceStatusStr = "Requested";
            allyCancelRequestButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = allyCancelRequestButton;
            frameWidth = 67;
        } else if (!isAllied() && allianceReceived()) {
            allianceStatusStr = "Accept?";
            allyConfirmButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = allyConfirmButton;
            frameWidth = 55;
        } else {
            allianceStatusStr = "Allied";
            disbandButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = disbandButton;
            frameWidth = 41;
            if (AlliancesClient.canControlAlly(rtsPlayer.name)) {
                allianceStatusStr += " (s)";
                frameWidth = 58;
            }
        }
        MyRenderer.renderFrameWithBg(
                guiGraphics,
                x + Button.DEFAULT_ICON_FRAME_SIZE,
                y,
                frameWidth,
                Button.DEFAULT_ICON_FRAME_SIZE,
                frameBgColour
        );
        guiGraphics.drawString(
                MC.font,
                allianceStatusStr,
                x + Button.DEFAULT_ICON_FRAME_SIZE + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                0xFFFFFF
        );
        if (!isPlayerLoggedIn()) {
            guiGraphics.fill(
                    x + Button.DEFAULT_ICON_FRAME_SIZE,
                    y,
                    x + (Button.DEFAULT_ICON_FRAME_SIZE * 2) + 4 + (allianceStatusStr.length() * 4),
                    y + Button.DEFAULT_ICON_FRAME_SIZE,
                    0x99000000
            );
        }
        return renderedButton;
    }

    // render and return all relevant buttons
    public ArrayList<Button> render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {

        int x0 = x;
        super.render(guiGraphics, x, y);
        ArrayList<Button> renderedButtons = new ArrayList<>();
        x += PLAYER_FRAME_WIDTH;
        renderedButtons.add(this.renderTradeResources(guiGraphics, ResourceName.FOOD, x, y, mouseX, mouseY));
        x += RESOURCE_FRAME_WIDTH + Button.DEFAULT_ICON_FRAME_SIZE;
        renderedButtons.add(this.renderTradeResources(guiGraphics, ResourceName.WOOD, x, y, mouseX, mouseY));
        x += RESOURCE_FRAME_WIDTH + Button.DEFAULT_ICON_FRAME_SIZE;
        renderedButtons.add(this.renderTradeResources(guiGraphics, ResourceName.ORE, x, y, mouseX, mouseY));
        x += RESOURCE_FRAME_WIDTH + (Button.DEFAULT_ICON_FRAME_SIZE * 1.25f);
        renderedButtons.addAll(this.renderTradeConfirms(guiGraphics, x, y, mouseX, mouseY));
        x += (Button.DEFAULT_ICON_FRAME_SIZE * 2.25f);
        renderedButtons.add(this.renderAllianceButton(guiGraphics, x, y, mouseX, mouseY));

        return renderedButtons;
    }

    private void sendResources() {
        if (MC.player != null) {
            ResourcesServerboundPacket.sendResources(resourcesToSend, MC.player.getName().getString());
            resetResources();
        }
    }

    private void resetResources() {
        this.resourcesToSend.food = 0;
        this.resourcesToSend.wood = 0;
        this.resourcesToSend.ore = 0;
    }

    private void requestAlliance() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.REQUEST, rtsPlayer.name);
        AlliancesClient.outboundPendingAlliances.add(rtsPlayer.name);
    }

    private void cancelAllianceRequest() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.CANCEL_REQUEST, rtsPlayer.name);
        AlliancesClient.outboundPendingAlliances.removeIf(p -> p.equals(rtsPlayer.name));
    }

    private void acceptAllianceRequest() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.ACCEPT_REQUEST, rtsPlayer.name);
    }

    private void disbandAlliance() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.DISBAND, rtsPlayer.name);
    }
}
