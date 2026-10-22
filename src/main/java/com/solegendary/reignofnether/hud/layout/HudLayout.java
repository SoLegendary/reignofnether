package com.solegendary.reignofnether.hud.layout;

import com.solegendary.reignofnether.ui.Rect;

/**
 * Where every region of the RTS HUD goes, in HUD units. This is the only place screen positions are
 * decided; the panels, the minimap and the tutorial arrows all read from here.
 *
 * <pre>
 *  [resources........]                              [clock][utility bar]
 *  [groups][queue]                                           [alert]
 *
 *                          [targeting hint]
 *                              [toast]
 *  [command card]     [selection, as wide as needed]        [tools] <map>
 * </pre>
 * The bottom row is floating cards, not a bar: the command card and selection only show when something
 * is selected, so the world stays visible between and behind them. The minimap is a diamond.
 */
public record HudLayout(
        int width,
        int height,
        Rect console,
        Rect commandCard,
        Rect selection,
        Rect tools,
        Rect minimap,
        int toastBottom,
        boolean compact
) {
    public static final int MARGIN = 8;
    public static final int GAP = 6;
    public static final int CONSOLE_HEIGHT = 118;
    public static final int CONSOLE_PAD = 7;
    public static final int TOP_CARD_H = 32; // resource bar, clock, alert and utility bar

    public static final int CELL = 29;
    public static final int CELL_GAP_X = 4;
    public static final int CELL_GAP_Y = 3;
    public static final int CARD_HEADER = 11;

    public static final int TOOL = 18;
    public static final int TOOL_GAP = 2;
    public static final int TOOL_COLS = 1;
    public static final int TOOL_ROWS = 5;

    public static final int MAP = 116;
    public static final int MAP_PAD = 2;

    /** Below this width, the selection panel drops secondary text. */
    public static final int COMPACT_WIDTH = 900;

    public static int cardWidth() {
        return CommandGrid.COLS * CELL + (CommandGrid.COLS - 1) * CELL_GAP_X;
    }

    public static int cardHeight() {
        return CARD_HEADER + CommandGrid.ROWS * CELL + (CommandGrid.ROWS - 1) * CELL_GAP_Y;
    }

    public static HudLayout compute(int width, int height) {
        Rect console = new Rect(0, height - CONSOLE_HEIGHT, width, CONSOLE_HEIGHT);
        int innerTop = console.y() + CONSOLE_PAD;
        int innerH = CONSOLE_HEIGHT - CONSOLE_PAD * 2;

        Rect card = new Rect(MARGIN + CONSOLE_PAD, innerTop, cardWidth(), innerH);

        Rect map = new Rect(width - MARGIN - MAP, height - MAP_PAD - MAP, MAP, MAP);

        int toolsW = TOOL_COLS * TOOL + (TOOL_COLS - 1) * TOOL_GAP;
        int toolsH = TOOL_ROWS * TOOL + (TOOL_ROWS - 1) * TOOL_GAP;
        // beside the diamond's left corner, centred on it
        Rect tools = new Rect(map.x() - GAP - toolsW, map.centerY() - toolsH / 2, toolsW, toolsH);

        // the most room the selection card can take between the command card and the tools, panel padding included
        int selX = card.right() + CONSOLE_PAD * 2 + GAP;
        int selRight = tools.x() - GAP * 2 - CONSOLE_PAD;
        Rect selection = new Rect(selX, innerTop, Math.max(0, selRight - selX), innerH);

        return new HudLayout(width, height, console, card, selection, tools, map,
                console.y() - GAP * 2, width < COMPACT_WIDTH);
    }

    /** Top-left corner of the given command card slot. */
    public int slotX(int slot) {
        return commandCard.x() + CommandGrid.col(slot) * (CELL + CELL_GAP_X);
    }

    public int slotY(int slot) {
        return commandCard.y() + CARD_HEADER + CommandGrid.row(slot) * (CELL + CELL_GAP_Y);
    }

    /** Top-left corner of a minimap tool tile, filled left to right from the top row. */
    public int toolX(int index) {
        return tools.x() + (index % TOOL_COLS) * (TOOL + TOOL_GAP);
    }

    public int toolY(int index) {
        return tools.y() + (index / TOOL_COLS) * (TOOL + TOOL_GAP);
    }
}
