package org.selkie.kol.impl.combat.madness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarRenderer;
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.EnumSet;

public class CombatPulsarRenderer extends BaseCombatLayeredRenderingPlugin {
    public static interface CombatPulsarRendererDelegate {
        float getPulsarInnerRadius();
        float getPulsarOuterRadius();
        Vector2f getPulsarCenterLoc();

        float getPulsarInnerWidth();
        float getPulsarOuterWidth();

        Color getPulsarColorForAngle(float angle);

        SpriteAPI getPulsarTexture();
        CombatRangeBlockerUtil getPulsarBlocker();

        float getPulsarScrollSpeed();

        float getFXMult();
    }

    @Override
    public float getRenderRadius() {
        return delegate.getPulsarOuterRadius()*10;
    }

    protected float alphaMult = 0.4f;

    private CombatPulsarRendererDelegate delegate;
    private float texOffset = 0f;
    public CombatPulsarRenderer(CombatPulsarRendererDelegate delegate) {
        this.delegate = delegate;
    }

    private float currAngle;
    public float getCurrAngle() {
        return currAngle;
    }


    public void setCurrAngle(float currAngle) {
        this.currAngle = currAngle;
    }

    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER);
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return layers;
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        //float days = Global.getSector().getClock().convertToDays(amount);
        //texOffset += days * delegate.getFlareScrollSpeed();
        float imageWidth = delegate.getPulsarTexture().getWidth();
        texOffset += amount * delegate.getPulsarScrollSpeed() / imageWidth;
        while (texOffset > 1) texOffset--;

        if (!rendered && vertexBuffer != null) {
            Misc.cleanBuffer(vertexBuffer);
            Misc.cleanBuffer(textureBuffer);
            Misc.cleanBuffer(colorBuffer);
            vertexBuffer = textureBuffer = null;
            colorBuffer = null;
        }
        rendered = false;
    }

    transient private FloatBuffer vertexBuffer, textureBuffer;
    transient private ByteBuffer colorBuffer;
    transient private boolean rendered = false;

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        this.alphaMult = viewport.getAlphaMult() * delegate.getFXMult();
        if (viewport.getAlphaMult() <= 0) return;

        float distClose = delegate.getPulsarInnerRadius();
        float distFar = delegate.getPulsarOuterRadius();

        if (distFar < distClose + 10f) distFar = distClose + 10f;

        float length = distFar - distClose;

        float wClose = delegate.getPulsarInnerWidth();
        float wFar = delegate.getPulsarOuterWidth();

        float pixelsPerSegment = 25f;
        float segments = Math.round(wFar / pixelsPerSegment);
        pixelsPerSegment = wFar / segments;


        Vector2f loc = delegate.getPulsarCenterLoc();
        float x = loc.x;
        float y = loc.y;


        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //GL11.glDisable(GL11.GL_TEXTURE_2D);

        //GL11.glShadeModel(GL11.GL_SMOOTH);

        //delegate.getPulsarTexture().bindTexture();


        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        //GL11.glEnable(GL11.GL_DITHER);

        float texHeight = delegate.getPulsarTexture().getTextureHeight();
        float imageHeight = delegate.getPulsarTexture().getHeight();
        float texPerSegment = texHeight / segments;

        //texPerSegment *= 20f;

        float texWidth = delegate.getPulsarTexture().getTextureWidth();
        float imageWidth = delegate.getPulsarTexture().getWidth();

        CombatRangeBlockerUtil blocker = delegate.getPulsarBlocker();

        float numIter = (float)Math.ceil(distFar - distClose) / (imageWidth * texWidth);
        float widthFactor = ((wClose + wFar) / 2f) / (imageHeight * texHeight);
        numIter /= widthFactor;

        float texPerUnitLength = 1f / (imageWidth * widthFactor);

        float angle = currAngle;

        //Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);

        float fadeInDist = Math.min(1000f, length * 0.25f);
        float fadeOutDist = Math.min(1500f, length * 0.25f);

        boolean wireframe = false;
        //wireframe = true;
        if (wireframe) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            //GL11.glDisable(GL11.GL_BLEND);
        }

        float [] rPrev = new float [(int) segments + 1];
        float [] blockedPrev = new float [(int) segments + 1];

        float [] xPrev = new float [(int) segments + 1];
        float [] yPrev = new float [(int) segments + 1];

        float [] texPrev = new float [(int) segments + 1];

        int numInnerSegments = (int) ((length - fadeInDist - fadeOutDist) / (pixelsPerSegment * 5f));
        if (numInnerSegments < 1) numInnerSegments = 1;
        numInnerSegments = 1;

        int numSegments = 2 + numInnerSegments;
        float distPerInnerSegment = (length - fadeInDist - fadeOutDist) / (float) numInnerSegments;

        boolean arrays = false;
        //arrays = true;

        int numVertices = (numSegments) * ((int) segments + 1) * 2;
        //System.out.println("Num: " + numVertices);
        if (arrays) {
            if (vertexBuffer == null) {
                vertexBuffer = ByteBuffer.allocateDirect(numVertices * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            if (textureBuffer == null) {
                textureBuffer = ByteBuffer.allocateDirect(numVertices * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }
            if (colorBuffer == null) {
                colorBuffer = ByteBuffer.allocateDirect(numVertices * 4).order(ByteOrder.nativeOrder());
            }

            vertexBuffer.clear();
            textureBuffer.clear();
            colorBuffer.clear();
        }

        rendered = true;

//		for (int t = 0; t < 2; t++) {
//
//			SpriteAPI tex = Global.getSettings().getSprite("terrain", "pulsar");
//			if (t == 1) {
//				tex = Global.getSettings().getSprite("terrain", "pulsar2");
//			}
        delegate.getPulsarTexture().bindTexture();
        //int count = 0;
        //for (int j = 0; j < 3; j++) {
        for (int j = 0; j < numSegments; j++) {
            //for (int j = 1; j < 2; j++) {

            boolean isFirst = j == 0;
            boolean isLast = j == numSegments - 1;
            boolean isMid = !isFirst && !isLast;

            float alphaCloser = 1f;
            float alphaFarther = 1f;
            float r1 = distClose;
            float r2 = distFar;

            if (isFirst) {
                alphaCloser = 0f;
                alphaFarther = 1f;
                r1 = distClose;
                r2 = distClose + fadeInDist;
            } else if (isMid) {
                alphaCloser = 1f;
                alphaFarther = 1f;

                //r1 = distClose + fadeInDist;
                //r2 = distFar - fadeOutDist;
                r1 = distClose + (j - 1) * distPerInnerSegment + fadeInDist;
                r2 = r1 + distPerInnerSegment;
            } else if (isLast) {
                alphaCloser = 1f;
                alphaFarther = 0f;
//				r1 = distFar;
//				r2 = distFar + fadeOutDist;
                r1 = distFar - fadeOutDist;
                //r1 = distClose + (j - 1) * distPerInnerSegment + fadeInDist;
                r2 = distFar;
            }


            float w1 = wClose + (wFar - wClose) * (r1 - distClose) / length;
            float w2 = wClose + (wFar - wClose) * (r2 - distClose) / length;

            float arcClose = (float) Math.toRadians(Misc.computeAngleSpan(w1 / 2f, r1));
            float arcFar = (float) Math.toRadians(Misc.computeAngleSpan(w2 / 2f, r2));

            float closeAnglePerSegment = arcClose / segments;
            float farAnglePerSegment = arcFar / segments;

            float currCloseAngle = (float) Math.toRadians(angle) - arcClose / 2f;
            float currFarAngle = (float) Math.toRadians(angle) - arcFar / 2f;

            //float closeTX = 0f - texOffset;
            //float farTX = texWidth * numIter - texOffset;
            //texOffset = 0f;
//			float closeTX = texWidth * texPerUnitLength * (r1 - distClose) - texOffset;
//			float farTX = texWidth * texPerUnitLength * (r2 - distClose) - texOffset;

            // horizontal, i.e. along width of beam
            float texProgress = 0f;

            //texPerUnitLength * (r2 - r1)
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (float i = 0; i < segments + 1; i++) {
                float blockedAt = 1f;
                float blockerMax = 100000f;
                if (isMid && blocker != null) {
                    blockerMax = blocker.getCurrMaxAt((float) Math.toDegrees((currCloseAngle)));
                    if (blockerMax > blocker.getMaxRange()) {
                        blockerMax = blocker.getMaxRange();
                    }
                    if (blockerMax < fadeInDist + 100) {
                        blockerMax = fadeInDist + 100;
                    }
                    blockedAt = (blockerMax - r1) / (r2 - r1);
                    if (blockedAt > 1) blockedAt = 1;
                    if (blockedAt < 0) blockedAt = 0;

                    rPrev[(int) i] = Math.min(r2, blockerMax);
                    blockedPrev[(int) i] = blockedAt;
                }

                float curr1 = r1;
                float curr2 = r2;

                float extraAlpha = 1f;
//				if (isMid || isLast) {
//					if (curr1 > blockerMax) {
//						curr1 = blockerMax;
//						//curr2 = curr1 + distPerInnerSegment;
//						curr2 = curr1;
//						//blockedAt = 0f;
//						//extraAlpha = 0f;
//					}
//				}

                if (isLast) {
                    curr1 = rPrev[(int) i];
                    float block = blockedPrev[(int) i];
                    curr2 = curr1 + Math.max(300f, fadeOutDist * block);

//					if (block > 0.5f) {
//						curr2 = distFar;
//					}

                    //curr2 = curr1 + 200f;

                    w2 = wClose + (wFar - wClose) * (curr2 - distClose) / length;
                    arcFar = (float) Math.toRadians(Misc.computeAngleSpan(w2 / 2f, curr2));
                    farAnglePerSegment = arcFar / segments;
                    currFarAngle = (float) Math.toRadians(angle) - arcFar / 2f + farAnglePerSegment * i;
                }


                float cosClose = (float) Math.cos(currCloseAngle);
                float sinClose = (float) Math.sin(currCloseAngle);

                float cosFar = (float) Math.cos(currFarAngle);
                float sinFar = (float) Math.sin(currFarAngle);

                float x1 = cosClose * curr1;
                float y1 = sinClose * curr1;
                float x2 = cosFar * curr2;
                float y2 = sinFar * curr2;

                //if (j == 1 || j == 2) {
                if (isMid || isLast) {
                    x1 = xPrev[(int) i];
                    y1 = yPrev[(int) i];
                }

                //blockedAt = 1f;
                x2 = x1 + (x2 - x1) * blockedAt;
                y2 = y1 + (y2 - y1) * blockedAt;

                xPrev[(int) i] = x2;
                yPrev[(int) i] = y2;

                float closeTX = texWidth * texPerUnitLength * (curr1 - distClose) - texOffset;
                float farTX = texWidth * texPerUnitLength * ((curr1 + (curr2 - curr1) * blockedAt) - distClose) - texOffset;

                if (isMid || isLast) {
                    closeTX = texPrev[(int) i];
                }
                texPrev[(int) i] = farTX;

                float edgeMult = 1f;
                float max = 10;
                if (i < max) {
                    edgeMult = i / max;
                } else if (i > segments - 1 - max) {
                    edgeMult = 1f - (i - (segments - max)) / max;
                }

                Color color = delegate.getPulsarColorForAngle(angle);
                //color = new Color(100,165,255,200);

                if (arrays) {
                    vertexBuffer.put(x1).put(y1).put(x2).put(y2);
//					vertexBuffer.put((float) Math.random() * -100f).put((float) Math.random() * -100f).
//								put((float) Math.random() * -100f).put((float) Math.random() * -100f);
                    textureBuffer.put(closeTX).put(texProgress).put(farTX).put(texProgress);
                    colorBuffer.put((byte)color.getRed()).
                            put((byte)color.getGreen()).
                            put((byte)color.getBlue()).
                            put((byte)((float) color.getAlpha() * alphaMult * alphaCloser * edgeMult * extraAlpha));
                    colorBuffer.put((byte)color.getRed()).
                            put((byte)color.getGreen()).
                            put((byte)color.getBlue()).
                            put((byte)((float) color.getAlpha() * alphaMult * alphaFarther * edgeMult * extraAlpha));
                } else {
                    GL11.glColor4ub((byte)color.getRed(),
                            (byte)color.getGreen(),
                            (byte)color.getBlue(),
                            (byte)((float) color.getAlpha() * alphaMult * alphaCloser * edgeMult * extraAlpha));

                    GL11.glTexCoord2f(closeTX, texProgress);
                    GL11.glVertex2f(x1, y1);

                    GL11.glColor4ub((byte)color.getRed(),
                            (byte)color.getGreen(),
                            (byte)color.getBlue(),
                            (byte)((float) color.getAlpha() * alphaMult * alphaFarther * edgeMult * extraAlpha));
                    GL11.glTexCoord2f(farTX, texProgress);
                    GL11.glVertex2f(x2, y2);

                    //count += 2;
                }

                texProgress += texPerSegment * 1f;
                currCloseAngle += closeAnglePerSegment;
                currFarAngle += farAnglePerSegment;
            }
            GL11.glEnd();
        }

        //System.out.println("Count: " + count);


        if (arrays) {
            //System.out.println("Pos: " + colorBuffer.position() + ", size: " + colorBuffer.capacity());
            vertexBuffer.position(0);
            textureBuffer.position(0);
            colorBuffer.position(0);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

            GL11.glTexCoordPointer(2, 0, textureBuffer);
            GL11.glColorPointer(4, true, 0, colorBuffer);
            GL11.glVertexPointer(2, 0, vertexBuffer);


            GL11.glDrawArrays(GL11.GL_QUAD_STRIP, 0, numVertices);

            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        }


        if (wireframe) GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

        GL11.glPopMatrix();

//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

}
