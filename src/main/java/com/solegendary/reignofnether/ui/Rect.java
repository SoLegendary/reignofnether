package com.solegendary.reignofnether.ui;

/** Axis-aligned rectangle in HUD units. */
public record Rect(int x, int y, int w, int h) {

    public static final Rect EMPTY = new Rect(0, 0, 0, 0);

    public int right() { return x + w; }

    public int bottom() { return y + h; }

    public int centerX() { return x + w / 2; }

    public int centerY() { return y + h / 2; }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    public boolean intersects(Rect o) {
        return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
    }

    public boolean isEmpty() {
        return w <= 0 || h <= 0;
    }
}
