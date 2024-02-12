package org.selkie.kol.impl.combat.madness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer;
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class CombatAuroraRenderer {
    public static interface CombatAuroraRendererDelegate {
        float getAuroraInnerRadius();
        float getAuroraOuterRadius();
        Vector2f getAuroraCenterLoc();

        Color getAuroraColorForAngle(float angle);
        float getAuroraAlphaMultForAngle(float angle);

        float getAuroraShortenMult(float angle);
        float getAuroraInnerOffsetMult(float angle);

        float getAuroraThicknessMult(float angle);
        float getAuroraThicknessFlat(float angle);


        float getAuroraTexPerSegmentMult();
        float getAuroraBandWidthInTexture();

        SpriteAPI getAuroraTexture();
        CombatRangeBlockerUtil getAuroraBlocker();
    }

    private CombatAuroraRendererDelegate delegate;
    private float phaseAngle;
    public CombatAuroraRenderer(CombatAuroraRendererDelegate delegate) {
        this.delegate = delegate;
    }


    public void advance(float amount) {
        float days = Global.getSector().getClock().convertToDays(amount);
        phaseAngle += days * 360f * 0.5f;
        phaseAngle = Misc.normalizeAngle(phaseAngle);
    }


    public void render(float alphaMult) {
        if (alphaMult <= 0) return;

        float bandWidthInTexture = delegate.getAuroraBandWidthInTexture();
        float bandIndex;

        float radStart = delegate.getAuroraInnerRadius();
        float radEnd = delegate.getAuroraOuterRadius();;

        if (radEnd < radStart + 10f) radEnd = radStart + 10f;

        float circ = (float) (Math.PI * 2f * (radStart + radEnd) / 2f);
        float pixelsPerSegment = 50f;
        float segments = Math.round(circ / pixelsPerSegment);

        float startRad = (float) Math.toRadians(0);
        float endRad = (float) Math.toRadians(360f);
        float spanRad = Math.abs(endRad - startRad);
        float anglePerSegment = spanRad / segments;

        Vector2f loc = delegate.getAuroraCenterLoc();
        float x = loc.x;
        float y = loc.y;


        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //GL11.glDisable(GL11.GL_TEXTURE_2D);

        delegate.getAuroraTexture().bindTexture();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        float thickness = (radEnd - radStart) * 1f;
        float radius = radStart;

        float texProgress = 0f;
        float texHeight = delegate.getAuroraTexture().getTextureHeight();
        float imageHeight = delegate.getAuroraTexture().getHeight();
        float texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness;

        texPerSegment *= delegate.getAuroraTexPerSegmentMult();

        float totalTex = Math.max(1f, Math.round(texPerSegment * segments));
        texPerSegment = totalTex / segments;

        float texWidth = delegate.getAuroraTexture().getTextureWidth();
        float imageWidth = delegate.getAuroraTexture().getWidth();

//		GL11.glDisable(GL11.GL_TEXTURE_2D);
//		GL11.glDisable(GL11.GL_BLEND);
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

//		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
//		SectorEntityToken star = ((StarSystemAPI)playerFleet.getContainingLocation()).getStar();
//		float distToStar = Misc.getDistance(playerFleet.getLocation(), star.getLocation());// - star.getRadius();
//		System.out.println("Dist to outer star: " + distToStar);
//		System.out.println("Aurora outer: " + delegate.getAuroraOuterRadius());

        CombatRangeBlockerUtil blocker = delegate.getAuroraBlocker();

        for (int iter = 0; iter < 2; iter++) {
            if (iter == 0) {
                bandIndex = 1;
            } else {
                bandIndex = 0;
            }

            float leftTX = (float) bandIndex * texWidth * bandWidthInTexture / imageWidth;
            float rightTX = (float) (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (float i = 0; i < segments + 1; i++) {

                float segIndex = i % (int) segments;

                //float phaseAngleRad = (float) Math.toRadians(phaseAngle + segIndex * 10) + (segIndex * anglePerSegment * 10f);
                float phaseAngleRad;
                if (iter == 0) {
                    phaseAngleRad = (float) Math.toRadians(phaseAngle) + (segIndex * anglePerSegment * 10f);
                } else { //if (iter == 1) {
                    phaseAngleRad = (float) Math.toRadians(-phaseAngle) + (segIndex * anglePerSegment * 5f);
                }


                float angle = (float) Math.toDegrees(segIndex * anglePerSegment);
                if (iter == 1) angle += 180;

                float blockerMax = 100000f;
                if (blocker != null) {
                    blockerMax = blocker.getCurrMaxAt(angle);
                    //blockerMax *= 1.5f;
                    blockerMax *= 0.75f;
                    if (blockerMax > blocker.getMaxRange()) {
                        blockerMax = blocker.getMaxRange();
                    }
                    //blockerMax += 1500f;
                }

                float pulseSin = (float) Math.sin(phaseAngleRad);
                //if (delegate instanceof PulsarBeamTerrainPlugin) pulseSin += 1f;
                float pulseMax = thickness * delegate.getAuroraShortenMult(angle);
//				if (pulseMax < 0) {
//					pulseMax = -pulseMax;
//					//pulseSin += 1f;
//				}
                if (pulseMax > blockerMax * 0.5f) {
                    pulseMax = blockerMax * 0.5f;
                }
                float pulseAmount = pulseSin * pulseMax;
                float pulseInner = pulseAmount * 0.1f;
                pulseInner *= delegate.getAuroraInnerOffsetMult(angle);
                //pulseInner *= Math.max(0, pulseSin - 0.5f);
                //pulseInner *= 0f;

                float r = radius;

                float thicknessMult = delegate.getAuroraThicknessMult(angle);
                float thicknessFlat = delegate.getAuroraThicknessFlat(angle);

                float theta = anglePerSegment * segIndex;;
                float cos = (float) Math.cos(theta);
                float sin = (float) Math.sin(theta);

                float rInner = r - pulseInner;
                if (rInner < r * 0.9f) rInner = r * 0.9f;

                float rOuter = (r + thickness * thicknessMult - pulseAmount + thicknessFlat);

                if (blocker != null) {
                    if (rOuter > blockerMax - pulseAmount) {
//						float fraction = rOuter / (r + thickness * thicknessMult + thicknessFlat);
//						rOuter = blockerMax * fraction;
                        rOuter = blockerMax - pulseAmount;
                        //rOuter = blockerMax;
                        if (rOuter < r) rOuter = r;
                    }
                    if (rInner > rOuter) {
                        rInner = rOuter;
                    }
                }

                float x1 = cos * rInner;
                float y1 = sin * rInner;
                float x2 = cos * rOuter;
                float y2 = sin * rOuter;

                x2 += (float) (Math.cos(phaseAngleRad) * pixelsPerSegment * 0.33f);
                y2 += (float) (Math.sin(phaseAngleRad) * pixelsPerSegment * 0.33f);

                Color color = delegate.getAuroraColorForAngle(angle);
                float alpha = delegate.getAuroraAlphaMultForAngle(angle);
                if (blocker != null) {
                    alpha *= blocker.getAlphaAt(angle);
                }
//				color = Color.white;
//				alphaMult = alpha = 1f;
                GL11.glColor4ub((byte)color.getRed(),
                        (byte)color.getGreen(),
                        (byte)color.getBlue(),
                        (byte)((float) color.getAlpha() * alphaMult * alpha));

                GL11.glTexCoord2f(leftTX, texProgress);
                GL11.glVertex2f(x1, y1);
                GL11.glTexCoord2f(rightTX, texProgress);
                GL11.glVertex2f(x2, y2);

                texProgress += texPerSegment * 1f;
            }
            GL11.glEnd();

            GL11.glRotatef(180, 0, 0, 1);
        }
        GL11.glPopMatrix();

//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }



    public float getRenderDistMax(float angle) {
        float radStart = delegate.getAuroraInnerRadius();
        float radEnd = delegate.getAuroraOuterRadius();

        if (radEnd < radStart + 10f) radEnd = radStart + 10f;

        float angleRad = (float) Math.toRadians(angle);

        float thickness = (radEnd - radStart) * 1f;
        float radius = radStart;
        CombatRangeBlockerUtil blocker = delegate.getAuroraBlocker();

        float max = 0;
        for (int i = 0; i < 2; i++) {
            float phaseAngleRad;
            if (i == 0) {
                phaseAngleRad = (float) Math.toRadians(phaseAngle) + (angleRad * 10f);
            } else {
                phaseAngleRad = (float) Math.toRadians(-phaseAngle) + (angle * 5f);
                angle += 180;
            }

            float blockerMax = 100000f;
            if (blocker != null) {
                blockerMax = blocker.getCurrMaxAt(angle);
                blockerMax *= 1.5f;
                if (blockerMax > blocker.getMaxRange()) {
                    blockerMax = blocker.getMaxRange();
                }
            }

            float pulseSin = (float) Math.sin(phaseAngleRad);
            float pulseMax = thickness * delegate.getAuroraShortenMult(angle);
            if (pulseMax > blockerMax * 0.5f) {
                pulseMax = blockerMax * 0.5f;
            }
            float pulseAmount = pulseSin * pulseMax;

            float thicknessMult = delegate.getAuroraThicknessMult(angle);
            float thicknessFlat = delegate.getAuroraThicknessFlat(angle);

            float rOuter = (radius + thickness * thicknessMult - pulseAmount + thicknessFlat);

            if (blocker != null) {
                if (rOuter > blockerMax - pulseAmount) {
                    rOuter = blockerMax - pulseAmount;
                    if (rOuter < radius) rOuter = radius;
                }
            }
            if (rOuter > max) max = rOuter;
        }

        return 10000000f; //return max;
    }
}