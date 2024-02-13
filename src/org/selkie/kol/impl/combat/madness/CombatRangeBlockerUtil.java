package org.selkie.kol.impl.combat.madness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

public class CombatRangeBlockerUtil {

    private int resolution;
    private float maxRange;

    private float degreesPerUnit;
    private float [] limits;
    private float [] curr;
    //private float [] alphas;

    private boolean wasUpdated = false;
    private boolean isAnythingShortened = false;

    public CombatRangeBlockerUtil(int resolution, float maxRange) {
        this.resolution = resolution;
        this.maxRange = maxRange;

        //degreesPerUnit = (float) Math.floor(360f / (float) resolution);
        degreesPerUnit = 360f / (float) resolution;

        limits = new float [resolution];
        curr = new float [resolution];
        //alphas = new float [resolution];
    }


    public boolean wasEverUpdated() {
        return wasUpdated;
    }


    public void updateAndSync(CombatEntityAPI entity, CombatEntityAPI exclude, float diffMult) {
        updateLimits(entity, exclude, diffMult);
        sync();
//		for (int i = 0; i < resolution; i++) {
//			alphas[i] = 1f;
//		}
    }

    public void sync() {
        for (int i = 0; i < resolution; i++) {
            curr[i] = limits[i];
        }
    }

    public float getShortenAmountAt(float angle) {
        return maxRange - getCurrMaxAt(angle);
    }

    public float getCurrMaxAt(float angle) {
        angle = Misc.normalizeAngle(angle);

        float index = angle / 360f * resolution;
        int i1 = (int) Math.floor(index);
        int i2 = (int) Math.ceil(index);
        while (i1 >= resolution) i1 -= resolution;
        while (i2 >= resolution) i2 -= resolution;

        float v1 = curr[i1];
        float v2 = curr[i2];

        return v1 + (v2 - v1) * (index - (int) index);

        //int index = getIndexForAngle(angle);
        //return curr[index];
    }

    public float getAlphaAt(float angle) {
        return 1f;
//		int index = getIndexForAngle(angle);
//		return alphas[index];
    }

    public void advance(float amount, float minApproachSpeed, float diffMult) {
        for (int i = 0; i < resolution; i++) {
            curr[i] = Misc.approach(curr[i], limits[i], minApproachSpeed, diffMult, amount);
//			if (curr[i] > limits[i] + 100f) {
//				alphas[i] = Misc.approach(alphas[i], 0f, 1f, 1f, amount);
//			} else {
//				alphas[i] = Misc.approach(alphas[i], 1f, 1f, 1f, amount);
//			}
        }
    }

    public void updateLimits(CombatEntityAPI entity, CombatEntityAPI exclude, float diffMult) {
        //if (true) return;

        for (int i = 0; i < resolution; i++) {
            limits[i] = maxRange;
        }

        isAnythingShortened = false;

        for (ShipAPI ent : Global.getCombatEngine().getShips()) {
            if (ent == entity || ent == exclude || ent.isFighter() || ent.isPhased()) continue;
            float dist = Misc.getDistance(entity.getLocation(), ent.getLocation());
            if (dist > maxRange) continue;

            float graceRadius = 25f;
            float span = Misc.computeAngleSpan(ent.getCollisionRadius()*1.0f + graceRadius, dist);

            if (!ent.isAlive()) span *= 0.35f;

            float angle = Misc.getAngleInDegrees(entity.getLocation(), ent.getLocation());

            float offsetSize = maxRange * 0.1f;

            float spanOffset = span * 0.4f * 1f / diffMult;
            spanOffset = 0f;
            for (float f = angle - span/2f - spanOffset; f <= angle + span/2f - spanOffset; f += degreesPerUnit) {
                float offset = Math.abs(f - angle) / (span / 2f);
                if (offset > 1) offset = 1;
                offset = (1f - (float) Math.cos(offset * 3.1416f / 2f));
                //offset = (float) Math.sqrt(offset);
//				offset += 1f;
                offset *= offset;
//				offset *= offset;
//				offset -= 1f;
                //offset *= planet.getRadius() + graceRadius;
                offset *= offsetSize;


                int index = getIndexForAngle(f);
                limits[index] = Math.min(dist - (ent.getCollisionRadius()) * 0.5f + offset, limits[index]);
                isAnythingShortened = true;
            }
        }

        for (CombatEntityAPI ent : Global.getCombatEngine().getAsteroids()) {
            if (ent == entity || ent == exclude) continue;
            float dist = Misc.getDistance(entity.getLocation(), ent.getLocation());
            if (dist > maxRange) continue;

            float graceRadius = 15f;
            float span = Misc.computeAngleSpan(ent.getCollisionRadius()*0.8f + graceRadius, dist);

            float angle = Misc.getAngleInDegrees(entity.getLocation(), ent.getLocation());

            float offsetSize = maxRange * 0.1f;

            float spanOffset = span * 0.4f * 1f / diffMult;
            spanOffset = 0f;
            for (float f = angle - span/2f - spanOffset; f <= angle + span/2f - spanOffset; f += degreesPerUnit) {
                float offset = Math.abs(f - angle) / (span / 2f);
                if (offset > 1) offset = 1;
                offset = (1f - (float) Math.cos(offset * 3.1416f / 2f));
                //offset = (float) Math.sqrt(offset);
//				offset += 1f;
                offset *= offset;
//				offset *= offset;
//				offset -= 1f;
                //offset *= planet.getRadius() + graceRadius;
                offset *= offsetSize;


                int index = getIndexForAngle(f);
                limits[index] = Math.min(dist - (ent.getCollisionRadius()) * 0.5f + offset, limits[index]);
                isAnythingShortened = true;
            }
        }

        wasUpdated = true;
    }

    public void block(float angle, float arc, float limit) {
        //float radius = Misc.computeAngleRadius(angle, limit);
        float offsetSize = Math.max(limit, maxRange * 0.1f);

        for (float f = angle - arc/2f; f <= angle + arc/2f; f += degreesPerUnit) {

            float offset = Math.abs(f - angle) / (arc / 2f);
            if (offset > 1) offset = 1;
            offset = (1f - (float) Math.cos(offset * 3.1416f / 2f));
            offset += 1f;
            offset *= offset;
            offset -= 1f;
            offset *= offsetSize;
            //offset = 0f;
            offset = Math.abs(offset);

            int index = getIndexForAngle(f);
            limits[index] = Math.min(limit + offset, limits[index]);
            isAnythingShortened = true;
        }
    }

    public int getIndexForAngle(float angle) {
        angle = Misc.normalizeAngle(angle);

        int index = (int)Math.round(angle / 360f * (float) resolution);
        if (index < 0) index = 0;
        //if (index > resolution - 1) index = resolution - 1;
        while (index >= resolution) index -= resolution;

        return index;
    }

    public boolean isAnythingShortened() {
        return isAnythingShortened;
    }


    public float getMaxRange() {
        return maxRange;
    }


}