package com.solegendary.reignofnether.hud.layout;

import com.solegendary.reignofnether.ui.Rect;

/**
 * Where every region of the RTS HUD goes, in HUD units. This is the only place screen positions are
 * decided; the panels, the minimap and the tutorial arrows all read from here.
 *
 * <pre>
 *  [resources........]                              [clock][utility bar]
 *  [groups]                                                  [alert]
 *  [queue]
 *
 *                          [targeting hint]
 *                              [toast]
 *  [command card | portrait | stats | slots | selection]      [tools] <map>
 * </pre>
 * The bottom row is one bar that is always there. Up to {@link #MAX_BAR} units wide it fills the screen; on wider
 * screens it splits: commands and selection stay on the left, only as wide as what is selected, the map stays on
 * the right, and the world shows between them.
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
        boolean compact,
        boolean split,
        boolean bigMap
) {
    public static final int MARGIN = 8;
    public static final int GAP = 6;
    public static final int CONSOLE_HEIGHT = 118;
    public static final int CONSOLE_PAD = 7;

    // the HUD's three button sizes: every clickable tile is one of these
    public static final int CELL = 29;       // command card, groups, queue, utility bar, item shop, inventory
    public static final int TILE_SMALL = 21; // selection unit tiles, start-position markers, minimap tools
    public static final int ZOOM = TILE_SMALL; // map zoom + and -, as big as the map toggles beside them

    public static final int TOP_CARD_H = CELL + 6; // resource bar, clock, alert and utility bar (a CELL row, padded)

    public static final int CELL_GAP_X = 4;
    public static final int CELL_GAP_Y = 3;
    public static final int CARD_HEADER = 11;

    public static final int TOOL = TILE_SMALL;
    public static final int TOOL_GAP = 2;
    public static final int TOOL_COLS = 2;
    public static final int TOOL_ROWS = 5;

    public static final int MAP = 116;
    public static final int MAP_PAD = 1;

    /** Below this width, the selection panel drops the stats column. */
    public static final int COMPACT_WIDTH = 750;
    /** Above this width, the bar splits into its left and right parts. */
    public static final int MAX_BAR = 880;

    public static int cardWidth() {
        return CommandGrid.COLS * CELL + (CommandGrid.COLS - 1) * CELL_GAP_X;
    }

    public static int cardHeight() {
        return CARD_HEADER + CommandGrid.ROWS * CELL + (CommandGrid.ROWS - 1) * CELL_GAP_Y;
    }

    public static HudLayout compute(int width, int height) {
        return compute(width, height, false);
    }

    /** With bigMap the map moves up and grows, and its tools go in one row below it. */
    public static HudLayout compute(int width, int height, boolean bigMap) {
        Rect console = new Rect(0, height - CONSOLE_HEIGHT, width, CONSOLE_HEIGHT);
        int innerTop = console.y() + CONSOLE_PAD;
        int innerH = CONSOLE_HEIGHT - CONSOLE_PAD * 2;

        Rect card = new Rect(MARGIN + CONSOLE_PAD, innerTop, cardWidth(), innerH);

        Rect map;
        Rect tools;
        if (bigMap) {
            int toolsW = TOOL_COLS * TOOL_ROWS * (TOOL + TOOL_GAP) - TOOL_GAP;
            tools = new Rect(width - MARGIN - toolsW, height - MARGIN / 2 - TOOL, toolsW, TOOL);
            int side = Math.min(toolsW, height - CONSOLE_HEIGHT);
            map = new Rect(width - MARGIN - side, tools.y() - GAP - side, side, side);
        } else {
            map = new Rect(width - MARGIN - MAP, height - MAP_PAD - MAP, MAP, MAP);
            int toolsW = TOOL_COLS * TOOL + (TOOL_COLS - 1) * TOOL_GAP;
            int toolsH = TOOL_ROWS * TOOL + (TOOL_ROWS - 1) * TOOL_GAP;
            // beside the diamond's left corner, centred on it
            tools = new Rect(map.x() - GAP - toolsW, map.centerY() - toolsH / 2, toolsW, toolsH);
        }

        // the most room the selection card can take between the command card and the tools, panel padding included
        int selX = card.right() + CONSOLE_PAD * 2;
        int selRight = tools.x() - GAP * 2 - CONSOLE_PAD;
        Rect selection = new Rect(selX, innerTop, Math.max(0, selRight - selX), innerH);

        return new HudLayout(width, height, console, card, selection, tools, map,
                console.y() - GAP * 2, width < COMPACT_WIDTH, width > MAX_BAR, bigMap);
    }

    /** Top-left corner of the given command card slot. */
    public int slotX(int slot) {
        return commandCard.x() + CommandGrid.col(slot) * (CELL + CELL_GAP_X);
    }

    public int slotY(int slot) {
        return commandCard.y() + CARD_HEADER + CommandGrid.row(slot) * (CELL + CELL_GAP_Y);
    }

    /** Top-left corner of a minimap tool tile: a row under the big map, else the column by the map comes first. */
    public int toolX(int tool) {
        if (bigMap)
            return tools.x() + tool * (TOOL + TOOL_GAP);
        return tools.x() + (tool < TOOL_ROWS ? TOOL + TOOL_GAP : 0);
    }

    public int toolY(int tool) {
        return bigMap ? tools.y() : tools.y() + tool % TOOL_ROWS * (TOOL + TOOL_GAP);
    }
}
