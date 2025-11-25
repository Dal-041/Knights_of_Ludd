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

        float angleDiffPerPoint = 360f / numPoints;
        float scrollPerPoint = (float) numRepetitions / numPoints;
        float scrollFromTime = Global.getCombatEngine().getTotalElapsedTime(false) * -scrollSpeed;
        float outertexScroll = sprite.getTextureHeight();

        float[] vertices = new float[(numPoints + 1) * 4];
        float[] texcoords = new float[(numPoints + 1) *  4];

        //counter-clockwise winding starting from 3 oclock
        for (int i = 0; i < numPoints + 1; i++) {

            Vector2f innerLoc = MathUtils.getPointOnCircumference(loc, radius - (thickness / 2f), facing + (angleDiffPerPoint * i));
            Vector2f outerLoc = MathUtils.getPointOnCircumference(loc, radius + (thickness / 2f), facing + (angleDiffPerPoint * i));

            int indexReal = i * 4;
            vertices[indexReal] = innerLoc.x; //inner vec x
            vertices[indexReal + 1] = innerLoc.y; //inner vec y
            vertices[indexReal + 2] = outerLoc.x; //outer vec x
            vertices[indexReal + 3] = outerLoc.y; //outer vec y

            texcoords[indexReal] = scrollFromTime + (scrollPerPoint * i); //inner vec x
            texcoords[indexReal + 1] = 0; //inner vec y
            texcoords[indexReal + 2] = scrollFromTime + (scrollPerPoint * i); //outer vec x
            texcoords[indexReal + 3] = outertexScroll; //outer vec y

        }

        sprite.bindTexture();
        drawPoints(vertices, texcoords, Color.WHITE, alpha, GL_QUAD_STRIP);
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
