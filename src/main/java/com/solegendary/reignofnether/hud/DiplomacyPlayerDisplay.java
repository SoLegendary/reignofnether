package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AllianceAction;
import com.solegendary.reignofnether.alliance.AllianceServerboundPacket;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerboundPacket;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

// Dashboard for RTS players to make/break alliances and send resources to each other
public class DiplomacyPlayerDisplay {

    private static final Minecraft MC = Minecraft.getInstance();

    public final RTSPlayer rtsPlayer;
    public final AbstractClientPlayer player;
    public final ResourceLocation defaultIconLocation = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png");
    private final Resources resourcesToSend;

    public DiplomacyPlayerDisplay(RTSPlayer rtsPlayer) {
        this.rtsPlayer = rtsPlayer;
        var server = MC.getCurrentServer();
        if (server != null) {
            this.player = MC.level.players().stream().filter(p -> p.getName().getString().equals(this.rtsPlayer.name)).findFirst().orElse(null);
        } else if (MC.player.getName().getString().equals(this.rtsPlayer.name)) {
            this.player = MC.player;
        } else {
            this.player = null;
        }
        resourcesToSend = new Resources(rtsPlayer.name,0,0,0);
    }
    private final static int frameBgColour = 0xA0000000;

    private static final int PLAYER_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 5; // frame containing player name + player icon + race icon
    private static final int RESOURCE_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 2; // frame containing a resource value + icon
    private static final int ALLIANCE_FRAME_WIDTH = Button.DEFAULT_ICON_FRAME_SIZE * 4; // frame containing a resource value + icon
    public static final int DISPLAY_WIDTH = PLAYER_FRAME_WIDTH + RESOURCE_FRAME_WIDTH * 4 + ALLIANCE_FRAME_WIDTH; // total width of a player display

    private void renderPlayer(GuiGraphics guiGraphics, int x, int y) {
        MyRenderer.renderFrameWithBg(guiGraphics,
                x, y,
                PLAYER_FRAME_WIDTH,
                Button.DEFAULT_ICON_FRAME_SIZE,
                frameBgColour
        );
        
        if (this.player != null && this.player.isSkinLoaded()) {
            var iconLocation = player.getSkinTextureLocation();
            //RenderSystem.setShaderTexture(0, iconLocation);
            // draw base layer
            guiGraphics.blit(iconLocation,
                    x + 4, y + 4,
                    Button.DEFAULT_ICON_SIZE, Button.DEFAULT_ICON_SIZE,
                    8.0f, 8.0f, // where on texture to start drawing from
                    8, 8, // dimensions of blit texture
                    64, 64 // size of texture itself (if < dimensions, texture is repeated)
            );
            // draw hat
            guiGraphics.blit(iconLocation,
                    x + 4, y + 4,
                    Button.DEFAULT_ICON_SIZE, Button.DEFAULT_ICON_SIZE,
                    40.0f, 8.0f, // where on texture to start drawing from
                    8, 8, // dimensions of blit texture
                    64, 64 // size of texture itself (if < dimensions, texture is repeated)
            );
        } else {
            MyRenderer.renderIcon(guiGraphics,
                    defaultIconLocation,
                    x + 4,
                    y + 4,
                    Button.DEFAULT_ICON_SIZE
            );
        }
        
        guiGraphics.drawString(
                MC.font,
                this.rtsPlayer.name,
                x + (Button.DEFAULT_ICON_FRAME_SIZE),
                y + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                0xFFFFFF
        );
    }

    private boolean isAllied() {
        return AlliancesClient.isAllied(MC.player.getName().getString(), rtsPlayer.name);
    }

    private boolean allianceRequested() {
        return false;
    }

    private boolean allianceReceived() {
        return false;
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
                frameBgColour
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
                () -> true,// this::isAllied,
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
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.change_resources"), Style.EMPTY))
        );
        changeResourceButton.bgIconResource = new ResourceLocation(ReignOfNether.MOD_ID, iconPath);
        changeResourceButton.render(guiGraphics, x + RESOURCE_FRAME_WIDTH, y, mouseX, mouseY);
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
                () -> true,// resourcesToSend.getTotalValue() > 0,
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
                () -> true,// resourcesToSend.getTotalValue() > 0 && isAllied(),
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
                () -> true,
                this::requestAlliance,
                null,
                List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.request_alliance"), Style.EMPTY))
        );
        Button allyCancelRequestButton = new Button(
                "Cancel Request Alliance",
                Button.DEFAULT_ICON_SIZE,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
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
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
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
        if (!isAllied() && !allianceRequested() && !allianceReceived()) {
            allianceStatusStr = "Enemy";
            allyRequestButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = allyRequestButton;
        } else if (!isAllied() && allianceRequested()) {
            allianceStatusStr = "Requested";
            allyCancelRequestButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = allyCancelRequestButton;
        } else if (!isAllied() && allianceReceived()) {
            allianceStatusStr = "Received";
            allyConfirmButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = allyConfirmButton;
        } else {
            allianceStatusStr = "Allied";
            disbandButton.render(guiGraphics, x, y, mouseX, mouseY);
            renderedButton = disbandButton;
        }
        MyRenderer.renderFrameWithBg(
                guiGraphics,
                x + Button.DEFAULT_ICON_FRAME_SIZE,
                y,
                RESOURCE_FRAME_WIDTH + allianceStatusStr.length() * 2,
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
        return renderedButton;
    }

    // render and return all relevant buttons
    public ArrayList<Button> render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        ArrayList<Button> renderedButtons = new ArrayList<>();
        this.renderPlayer(guiGraphics, x, y); // icon, name
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
        ResourcesServerboundPacket.sendResources(resourcesToSend, rtsPlayer.name);
        resetResources();
    }

    private void resetResources() {
        this.resourcesToSend.food = 0;
        this.resourcesToSend.wood = 0;
        this.resourcesToSend.ore = 0;
    }

    private void requestAlliance() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.REQUEST, rtsPlayer.name);
    }

    private void cancelAllianceRequest() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.CANCEL_REQUEST, rtsPlayer.name);
    }

    private void acceptAllianceRequest() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.ACCEPT_REQUEST, rtsPlayer.name);
    }

    private void disbandAlliance() {
        AllianceServerboundPacket.doAllianceAction(AllianceAction.DISBAND, rtsPlayer.name);
    }
}
