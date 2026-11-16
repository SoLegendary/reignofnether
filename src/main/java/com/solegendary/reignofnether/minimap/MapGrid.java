package com.solegendary.reignofnether.minimap;

/** A square of map colours (RGB, row-major from minX/minZ), filled row by row. */
public final class MapGrid {

    public final int minX, minZ, size;
    public final int[] rgb;
    // rows filled so far, top down; read from another thread by the loading screen
    public volatile int rows = 0;

    public MapGrid(int minX, int minZ, int size) {
        this.minX = minX;
        this.minZ = minZ;
        this.size = size;
        this.rgb = new int[size * size];
    }

    public boolean contains(int x, int z) {
        return x >= minX && z >= minZ && x < minX + size && z < minZ + size;
    }

    public int get(int x, int z) {
        return rgb[(z - minZ) * size + (x - minX)];
    }

    public void set(int x, int z, int colour) {
        rgb[(z - minZ) * size + (x - minX)] = colour;
    }
}
