package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.client.Minecraft;
import org.joml.Vector3d;

public class MinimapViewQuadRenderer {
    // Perpendicular half-width of the view-quad outline, in world blocks.
    // Must stay >= 0.5 or a near-horizontal edge can fall between integer rows and leave gaps.
    private static final double VIEW_QUAD_HALF_WIDTH = 1.5;

    private static final Minecraft MC = Minecraft.getInstance();

    public static int[][] updateMapViewQuad(int[][] mapColoursOverlays) {
        if (MC.level == null || MC.player == null) {
            return mapColoursOverlays;
        }

        // get world position of corners of the screen
        int yOffset = 0;//(int) (MC.player.getY() - 100) * 5;

        Vector3d tl = MiscUtil.screenPosToWorldPos(MC, 0, -yOffset);
        Vector3d bl = MiscUtil.screenPosToWorldPos(MC, 0, MC.getWindow().getGuiScaledHeight() - yOffset);
        Vector3d br = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), MC.getWindow().getGuiScaledHeight() - yOffset);
        Vector3d tr = MiscUtil.screenPosToWorldPos(MC, MC.getWindow().getGuiScaledWidth(), -yOffset);

        Vector3d[] corners = new Vector3d[] { tl, bl, br, tr };
        // adjust corners according to camera angle
        Vector3d lookVector = MiscUtil.getPlayerLookVector(MC);
        corners[0] = MyMath.addVector3d(corners[0], lookVector, 90 - OrthoviewClientEvents.getCamRotY());
        corners[1] = MyMath.addVector3d(corners[1], lookVector, 75 - OrthoviewClientEvents.getCamRotY());
        corners[2] = MyMath.addVector3d(corners[2], lookVector, 75 - OrthoviewClientEvents.getCamRotY());
        corners[3] = MyMath.addVector3d(corners[3], lookVector, 90 - OrthoviewClientEvents.getCamRotY());

        // rasterise the four edges directly instead of testing every column on the map against them
        for (int i = 0; i < corners.length; i++) {
            int j = (i + 1) % corners.length;
            mapColoursOverlays = drawThickLineOnOverlay(mapColoursOverlays,
                    corners[i].x, corners[i].z,
                    corners[j].x, corners[j].z
            );
        }
        return mapColoursOverlays;
    }

    /**
     * Rasterises one thick line segment into mapColoursOverlays, in world coordinates.
     * Builds the segment's oriented bounding rectangle and scanline-fills it, so cost is
     * O(length * width) rather than O(map area).
     */
    private static int[][] drawThickLineOnOverlay(int[][] mapColoursOverlays, double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4) {
            return mapColoursOverlays;
        }

        // unit vector along the segment, and unit vector perpendicular to it
        double ax = dx / len, az = dz / len;
        double px = -az, pz = ax;

        // extend by halfWidth at both ends (square caps) so adjacent edges of the quad
        // meet without leaving a notch at each corner
        double ex1 = x1 - ax * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH, ez1 = z1 - az * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH;
        double ex2 = x2 + ax * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH, ez2 = z2 + az * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH;

        double[] qx = {
                ex1 + px * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH, ex2 + px * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH,
                ex2 - px * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH, ex1 - px * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH
        };
        double[] qz = {
                ez1 + pz * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH, ez2 + pz * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH,
                ez2 - pz * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH, ez1 - pz * MinimapViewQuadRenderer.VIEW_QUAD_HALF_WIDTH
        };
        int size = mapColoursOverlays.length;

        // world -> overlay array offsets, matching x0 = x - xc_world + worldRadius
        int xOff = MinimapClientEvents.getWorldRadius() - MinimapClientEvents.getMapCentreWorldX();
        int zOff = MinimapClientEvents.getWorldRadius() - MinimapClientEvents.getMapCentreWorldZ();

        double minZw = Math.min(Math.min(qz[0], qz[1]), Math.min(qz[2], qz[3]));
        double maxZw = Math.max(Math.max(qz[0], qz[1]), Math.max(qz[2], qz[3]));

        int z0Start = Math.max(0, (int) Math.ceil(minZw) + zOff);
        int z0End = Math.min(size - 1, (int) Math.floor(maxZw) + zOff);

        for (int z0 = z0Start; z0 <= z0End; z0++) {
            double zw = z0 - zOff; // world z of this row

            // intersect the row with each edge; for a convex quad the span is [min, max]
            double xEnter = Double.POSITIVE_INFINITY;
            double xExit = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                double za = qz[i], zb = qz[j];
                if (za == zb) {
                    continue; // horizontal edge contributes nothing to this row's span
                }
                if (zw < Math.min(za, zb) || zw > Math.max(za, zb)) {
                    continue;
                }
                double t = (zw - za) / (zb - za);
                double xi = qx[i] + t * (qx[j] - qx[i]);
                if (xi < xEnter) xEnter = xi;
                if (xi > xExit) xExit = xi;
            }
            if (xEnter > xExit) {
                continue;
            }

            int x0Start = Math.max(0, (int) Math.ceil(xEnter) + xOff);
            int x0End = Math.min(size - 1, (int) Math.floor(xExit) + xOff);

            for (int x0 = x0Start; x0 <= x0End; x0++) {
                mapColoursOverlays[x0][z0] = -1;
            }
        }
        return mapColoursOverlays;
    }
}
