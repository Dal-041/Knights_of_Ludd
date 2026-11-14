package org.selkie.kol;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class OpenGlUtils {
    // credit to ruddygreat for this class <3
    // used with permission

    /**
     * Draws a textured ring of a given thickness at a given location. winding & scrolling is counter-clockwise; sprite width must be a power of 2 for best results.
     * @param loc the center of the ring
     * @param radius the radius of the ring
     * @param thickness the thickness of the ring
     * @param numPoints the number of points around the edge
     * @param numRepetitions the number of times that the sprite should repeat, along the sprite's +X axis
     * @param scrollSpeed the speed at which the sprite scrolls, along the sprite's +X axis
     * @param sprite the sprite
     */
    public static void drawTexturedRing(Vector2f loc, float radius, float thickness, int numPoints, int numRepetitions, float scrollSpeed, SpriteAPI sprite, float facing, float alpha) {
        float[] angles = new float[numPoints + 1];
        for (int i = 0; i < numPoints + 1; i++) {
            angles[i] = facing + (360f / numPoints) * i;
        }
        float[] radii = {radius - thickness / 2f, radius + thickness / 2f};
        drawTexturedRingGrid(loc, radii, angles, null, numRepetitions, scrollSpeed, sprite, facing, alpha, false);
    }

    /**
     * Draws a section of a textured ring as a grid of vertices, so its opacity can vary both around and across the band.
     * The texture is mapped as if the section were part of a full ring, so adjacent sections line up seamlessly.
     * @param radii vertex radii, ascending; the first is the ring's inner edge and the last its outer edge
     * @param angles vertex angles in degrees, ascending
     * @param alphas per-vertex alpha multiplier indexed [radius][angle], or null for fully opaque
     * @param facing the angle the texture starts at
     * @param rotated true to lay the texture across the band and let it repeat around the ring instead
     */
    public static void drawTexturedRingGrid(Vector2f loc, float[] radii, float[] angles, float[][] alphas, int numRepetitions, float scrollSpeed, SpriteAPI sprite, float facing, float alpha, boolean rotated) {
        if (radii.length < 2 || angles.length < 2) return;

        float scrollFromTime = getTextureScroll(scrollSpeed);
        float innerRadius = radii[0];
        float thickness = Math.max(radii[radii.length - 1] - innerRadius, 0.001f);

        int numVerts = angles.length;
        float[] cos = new float[numVerts];
        float[] sin = new float[numVerts];
        float[] texX = new float[numVerts];
        for (int i = 0; i < numVerts; i++) {
            double rad = Math.toRadians(angles[i]);
            cos[i] = (float) Math.cos(rad);
            sin[i] = (float) Math.sin(rad);
            texX[i] = scrollFromTime + numRepetitions * (angles[i] - facing) / 360f;
        }

        float[] vertices = new float[numVerts * 4];
        float[] texcoords = new float[numVerts * 4];
        float[] colors = new float[numVerts * 8];
        java.util.Arrays.fill(colors, 1f);

        sprite.bindTexture();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // one quad strip per row of the grid, counter-clockwise winding
        for (int row = 0; row < radii.length - 1; row++) {
            float inner = radii[row];
            float outer = radii[row + 1];
            float innerTexY = (inner - innerRadius) / thickness;
            float outerTexY = (outer - innerRadius) / thickness;

            for (int i = 0; i < numVerts; i++) {
                int indexReal = i * 4;
                vertices[indexReal] = loc.x + inner * cos[i];
                vertices[indexReal + 1] = loc.y + inner * sin[i];
                vertices[indexReal + 2] = loc.x + outer * cos[i];
                vertices[indexReal + 3] = loc.y + outer * sin[i];

                if (rotated) {
                    texcoords[indexReal] = innerTexY;
                    texcoords[indexReal + 1] = texX[i];
                    texcoords[indexReal + 2] = outerTexY;
                    texcoords[indexReal + 3] = texX[i];
                } else {
                    texcoords[indexReal] = texX[i];
                    texcoords[indexReal + 1] = innerTexY;
                    texcoords[indexReal + 2] = texX[i];
                    texcoords[indexReal + 3] = outerTexY;
                }

                int colorIndex = i * 8;
                colors[colorIndex + 3] = alpha * (alphas == null ? 1f : alphas[row][i]);
                colors[colorIndex + 7] = alpha * (alphas == null ? 1f : alphas[row + 1][i]);
            }
            drawPoints(vertices, texcoords, colors, GL_QUAD_STRIP, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /**
     * How far a texture scrolling at the given speed has moved by now, along its +X axis, in whole textures.
     * Wrapped to a single repeat: the raw offset grows into the tens of thousands, where a float can no longer tell
     * neighbouring vertices of a fine mesh apart and the texture smears across it.
     */
    public static float getTextureScroll(float scrollSpeed) {
        double scroll = (double) Global.getCombatEngine().getTotalElapsedTime(false) * -scrollSpeed;
        return (float) (((scroll % 1.0) + 1.0) % 1.0);
    }

    /** Draws with a per-vertex RGBA color array instead of a single color. */
    public static void drawPoints(float[] vertices, float[] texCoords, float[] colors, int mode, int blendSrc, int blendDest) {
        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(colors.length);
        colorBuffer.put(colors);
        colorBuffer.flip();

        glPushClientAttrib(GL_CLIENT_VERTEX_ARRAY_BIT);
        glEnableClientState(GL_COLOR_ARRAY);
        glColorPointer(4, 0, colorBuffer);
        drawPoints(vertices, texCoords, Color.WHITE, 1f, mode, blendSrc, blendDest);
        glPopClientAttrib();
    }

    public static void drawPoints(float[] vertices, float[] texCoords, Color color, float alphaMult, int mode, int blendSrc, int blendDest) {

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        FloatBuffer texCoordBuffer = BufferUtils.createFloatBuffer(texCoords.length);

        vertexBuffer.put(vertices);
        vertexBuffer.flip();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.flip();

        Misc.setColor(color, alphaMult);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(blendSrc, blendDest);
        glPushClientAttrib(GL_CLIENT_VERTEX_ARRAY_BIT);
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(2, 0, vertexBuffer);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2, 0, texCoordBuffer);
        glDrawArrays(mode, 0, vertices.length / 2);
        glPopClientAttrib();
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }

    public static void drawPoints(float[] vertices, float[] texCoords, Color color, float alphaMult, int mode) {
        drawPoints(vertices, texCoords, color, alphaMult, mode, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
}
