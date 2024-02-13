package org.selkie.kol.impl.combat.madness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.terrain.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CombatPulsarCorona implements CombatAuroraRenderer.CombatAuroraRendererDelegate, CombatFlareManager.CombatFlareManagerDelegate {

    public static final float CR_LOSS_MULT_GLOBAL = 0.25f;
    public String terrainId = "Combat_Pulsar";
    public String name;
    public CombatEntityAPI entity;

    public static class CombatCoronaParams {
        public float bandWidthInEngine;
        public float middleRadius;
        public CombatEntityAPI relatedEntity;
        public String name;
        public float windBurnLevel;
        public float flareProbability;
        public float crLossMult;

        public CombatCoronaParams(float bandWidthInEngine, float middleRadius,
                                  CombatEntityAPI relatedEntity,
                                  float windBurnLevel, float flareProbability, float crLossMult) {
            this.bandWidthInEngine = bandWidthInEngine;
            this.middleRadius = middleRadius;
            this.relatedEntity = relatedEntity;
            this.name = name;
            this.windBurnLevel = windBurnLevel;
            this.flareProbability = flareProbability;
            this.crLossMult = crLossMult;
        }
    }

    protected SpriteAPI texture = null;
    protected Color color;

    protected CombatAuroraRenderer renderer;
    protected CombatFlareManager flareManager;
    protected CombatCoronaParams params;

    protected CombatRangeBlockerUtil blocker = null;

    public void init(String terrainId, CombatEntityAPI entity, Object param) {
        this.terrainId = terrainId;
        this.entity = entity;
        params = (CombatCoronaParams) param;
        name = params.name;
        if (name == null) {
            name = "Corona";
        }
    }

    public String getNameForTooltip() {
        return "Corona";
    }

    Object writeReplace() {
        return this;
    }

    protected boolean shouldPlayLoopOne() {
        if (Global.getCombatEngine().getPlayerShip() == null) return false;
        return false; //getSpec().getLoopOne() != null && !flareManager.isInActiveFlareArc(Global.getCombatEngine().getPlayerShip().getLocation());
    }

    protected boolean shouldPlayLoopTwo() {
        if (Global.getCombatEngine().getPlayerShip() == null) return false;
        return false; //getSpec().getLoopOne() != null && flareManager.isInActiveFlareArc(Global.getCombatEngine().getPlayerShip().getLocation());
    }



    transient private EnumSet<CampaignEngineLayers> layers = EnumSet.of(CampaignEngineLayers.TERRAIN_7);
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return layers;
    }

    public CombatCoronaParams getParams() {
        return params;
    }

    public void advance(float amount) {
        //super.advance(amount);
        renderer.advance(amount);
        flareManager.advance(amount);

        if (amount > 0 && blocker != null) {
            blocker.updateLimits(entity, params.relatedEntity, 0.5f);
            blocker.advance(amount, 100f, 0.5f);
        }
    }

    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (blocker != null && !blocker.wasEverUpdated()) {
            blocker.updateAndSync(entity, params.relatedEntity, 0.5f);
        }
        renderer.render(viewport.getAlphaMult());
    }

    public float getRenderRange() {
        FlareManager.Flare curr = flareManager.getActiveFlare();
        if (curr != null) {
            float outerRadiusWithFlare = computeRadiusWithFlare(flareManager.getActiveFlare());
            return outerRadiusWithFlare + 300000f;
        }
        return 1000000f;
    }

    public boolean containsPoint(Vector2f point, float radius) {
        if (blocker != null && blocker.isAnythingShortened()) {
            float angle = Misc.getAngleInDegrees(this.entity.getLocation(), point);
            float dist = Misc.getDistance(this.entity.getLocation(), point);
            float max = blocker.getCurrMaxAt(angle);
            if (dist > max) return false;
        }

        if (flareManager.isInActiveFlareArc(point)) {
            float outerRadiusWithFlare = computeRadiusWithFlare(flareManager.getActiveFlare());
            float dist = Misc.getDistance(this.entity.getLocation(), point);
            if (dist > outerRadiusWithFlare + radius) return false;
            if (dist + radius < params.middleRadius - params.bandWidthInEngine / 2f) return false;
            return true;
        }
        float dist = Misc.getDistance(this.entity.getLocation(), point);
        if (dist > getMaxRadiusForContains() + radius) return false;
        if (dist < getMinRadiusForContains() - radius) return false;
        return true;
    }

    protected float getMinRadiusForContains() {
        return params.middleRadius - params.bandWidthInEngine / 2f;
    }

    protected float getMaxRadiusForContains() {
        return params.middleRadius + params.bandWidthInEngine / 2f;
    }

    protected float computeRadiusWithFlare(FlareManager.Flare flare) {
        float inner = getAuroraInnerRadius();
        float outer = params.middleRadius + params.bandWidthInEngine * 1f; //0.5
        float thickness = outer - inner;

        thickness *= flare.extraLengthMult;
        thickness += flare.extraLengthFlat;

        return inner + thickness;
    }

    protected float getExtraSoundRadius() {
        float base = 100f;

        float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), Global.getSector().getPlayerFleet().getLocation());
        float extra = 0f;
        if (flareManager.isInActiveFlareArc(angle)) {
            extra = computeRadiusWithFlare(flareManager.getActiveFlare()) - params.bandWidthInEngine;
        }
        //System.out.println("Extra: " + extra);
        return base + extra;
    }

    public void applyEffect(CombatEntityAPI entity, float amount) {
        if (entity instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) entity;

            boolean inFlare = false;
            if (flareManager.isInActiveFlareArc(ship.getLocation())) {
                inFlare = true;
            }

            float intensity = getIntensityAtPoint(ship.getLocation());
            if (intensity <= 0) return;

            String buffId = getModId();
            float buffDur = 0.1f;

            // CR loss and peak time reduction
            for (FleetMemberAPI member : ship.getFleetMember().getFleetData().getMembersListCopy()) {
                float recoveryRate = member.getStats().getBaseCRRecoveryRatePercentPerDay().getModifiedValue();
                float lossRate = member.getStats().getBaseCRRecoveryRatePercentPerDay().getBaseValue();

                float resistance = member.getStats().getDynamic().getValue(Stats.CORONA_EFFECT_MULT);
                //if (inFlare) loss *= 2f;
                float lossMult = 1f;
                if (inFlare) lossMult = 2f;
                float adjustedLossMult = (0f + params.crLossMult * intensity * resistance * lossMult * CR_LOSS_MULT_GLOBAL);

                float loss = (-1f * recoveryRate + -1f * lossRate * adjustedLossMult) * amount * 0.01f;
                float curr = member.getRepairTracker().getBaseCR();
                if (loss > curr) loss = curr;
                if (resistance > 0) { // not actually resistance, the opposite
                    if (inFlare) {
                        member.getRepairTracker().applyCREvent(loss, "flare", "Solar flare effect");
                    } else {
                        member.getRepairTracker().applyCREvent(loss, "corona", "Star corona effect");
                    }
                }

                // needs to be applied when resistance is 0 to immediately cancel out the debuffs (by setting them to 0)
                float peakFraction = 1f / Math.max(1.3333f, 1f + params.crLossMult * intensity);
                float peakLost = 1f - peakFraction;
                peakLost *= resistance;
                float degradationMult = 1f + (params.crLossMult * intensity * resistance) / 2f;
                member.getBuffManager().addBuffOnlyUpdateStat(new PeakPerformanceBuff(buffId + "_1", 1f - peakLost, buffDur));
                member.getBuffManager().addBuffOnlyUpdateStat(new CRLossPerSecondBuff(buffId + "_2", degradationMult, buffDur));
            }

            // "wind" effect - adjust velocity
            float maxSpeed = ship.getMaxSpeed();
            float currSpeed = ship.getVelocity().length();

            float maxWindBurn = params.windBurnLevel;
            if (inFlare) {
                maxWindBurn *= 2f;
            }


            float currWindBurn = intensity * maxWindBurn;
            float maxFleetBurnIntoWind = maxSpeed - Math.abs(currWindBurn);

            float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), ship.getLocation());
            Vector2f windDir = Misc.getUnitVectorAtDegreeAngle(angle);
            if (currWindBurn < 0) {
                windDir.negate();
            }

            Vector2f velDir = Misc.normalise(new Vector2f(ship.getVelocity()));
            velDir.scale(currSpeed);

            float fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir);

            float accelMult = 0.5f;
            if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
                accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind);
            }
            float shipAccelMult = ship.getMutableStats().getAcceleration().getModifiedValue();
            if (shipAccelMult > 0) {// && fleetAccelMult < 1) {
                accelMult /= shipAccelMult;
            }

            float seconds = amount;

            Vector2f vel = ship.getVelocity();
            windDir.scale(seconds * ship.getAcceleration() * accelMult);
            ship.getVelocity().set(vel.x + windDir.x, vel.y + windDir.y);

            Color glowColor = getAuroraColorForAngle(angle);
            int alpha = glowColor.getAlpha();
            if (alpha < 75) {
                glowColor = Misc.setAlpha(glowColor, 75);
            }
            // visual effects - glow, tail


            float dist = Misc.getDistance(this.entity.getLocation(), ship.getLocation());
            float check = 100f;
            if (params.relatedEntity != null) check = params.relatedEntity.getCollisionRadius() * 0.5f;
            if (dist > check) {
                float durIn = 1f;
                float durOut = 10f;
                Misc.normalise(windDir);
                float sizeNormal = 5f + 10f * intensity;
                float sizeFlare = 10f + 15f * intensity;
                /* Only applies to the little ship entities within a fleet on the campaign map - render separately if desired
                for (FleetMemberViewAPI view : fleet.getViews()) {
                    if (inFlare) {
                        view.getWindEffectDirX().shift(getModId() + "_flare", windDir.x * sizeFlare, durIn, durOut, 1f);
                        view.getWindEffectDirY().shift(getModId() + "_flare", windDir.y * sizeFlare, durIn, durOut, 1f);
                        view.getWindEffectColor().shift(getModId() + "_flare", glowColor, durIn, durOut, intensity);
                    } else {
                        view.getWindEffectDirX().shift(getModId(), windDir.x * sizeNormal, durIn, durOut, 1f);
                        view.getWindEffectDirY().shift(getModId(), windDir.y * sizeNormal, durIn, durOut, 1f);
                        view.getWindEffectColor().shift(getModId(), glowColor, durIn, durOut, intensity);
                    }
                }
                */
            }
        }
    }

    public float getIntensityAtPoint(Vector2f point) {
        float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), point);
        float maxDist = params.bandWidthInEngine;
        if (flareManager.isInActiveFlareArc(angle)) {
            maxDist = computeRadiusWithFlare(flareManager.getActiveFlare());
        }
        float minDist = params.relatedEntity.getCollisionRadius();
        float dist = Misc.getDistance(point, params.relatedEntity.getLocation());

        if (dist > maxDist) return 0f;

        float intensity = 1f;
        if (minDist < maxDist) {
            intensity = 1f - (dist - minDist) / (maxDist - minDist);
            //intensity = 0.5f + intensity * 0.5f;
            if (intensity < 0) intensity = 0;
            if (intensity > 1) intensity = 1;
        }

        return intensity;
    }

    public Color getNameColor() {
        Color bad = Misc.getNegativeHighlightColor();
        Color base = Color.gray;
        //bad = Color.red;
        return Misc.interpolateColor(base, bad, Global.getSector().getCampaignUI().getSharedFader().getBrightness() * 1f);
    }

    public float getAuroraAlphaMultForAngle(float angle) {
        return 1f;
    }

    public float getAuroraBandWidthInTexture() {
        return 256f;
        //return 512f;
    }

    public float getAuroraTexPerSegmentMult() {
        return 1f;
        //return 2f;
    }

    public Vector2f getAuroraCenterLoc() {
        return params.relatedEntity.getLocation();
    }

    public Color getAuroraColorForAngle(float angle) {
        if (color == null) {
            if (params.relatedEntity instanceof PlanetAPI) {
                color = ((PlanetAPI)params.relatedEntity).getSpec().getCoronaColor();
                //color = Misc.interpolateColor(color, Color.white, 0.5f);
            } else {
                color = Color.white;
            }
            color = Misc.setAlpha(color, 25);
        }
        if (flareManager.isInActiveFlareArc(angle)) {
            return flareManager.getColorForAngle(color, angle);
        }
        return color;
    }

    public float getAuroraInnerRadius() {
        return params.relatedEntity.getCollisionRadius() + 5f;
    }

    public float getAuroraOuterRadius() {
        return params.middleRadius + params.bandWidthInEngine * 0.5f;
    }

    public float getAuroraShortenMult(float angle) {
        return 0.85f + flareManager.getShortenMod(angle);
    }

    public float getAuroraInnerOffsetMult(float angle) {
        return flareManager.getInnerOffsetMult(angle);
    }

    public SpriteAPI getAuroraTexture() {
        return texture;
    }

    public CombatRangeBlockerUtil getAuroraBlocker() {
        return blocker;
    }

    public float getAuroraThicknessFlat(float angle) {
//	float shorten = blocker.getShortenAmountAt(angle);
//	if (shorten > 0) return -shorten;
//	if (true) return -4000f;

        if (flareManager.isInActiveFlareArc(angle)) {
            return flareManager.getExtraLengthFlat(angle);
        }
        return 0;
    }

    public float getAuroraThicknessMult(float angle) {
        if (flareManager.isInActiveFlareArc(angle)) {
            return flareManager.getExtraLengthMult(angle);
        }
        return 1f;
    }

    public java.util.List<Color> getFlareColorRange() {
        List<Color> result = new ArrayList<Color>();

        if (params.relatedEntity instanceof PlanetAPI) {
            Color color = ((PlanetAPI)params.relatedEntity).getSpec().getCoronaColor();
            result.add(Misc.setAlpha(color, 255));
        } else {
            result.add(Color.white);
        }
        //result.add(Misc.setAlpha(getAuroraColorForAngle(0), 127));
        return result;
    }

    public float getFlareArcMax() {
        return 60;
    }

    public float getFlareArcMin() {
        return 30;
    }

    public float getFlareExtraLengthFlatMax() {
        return 500;
    }

    public float getFlareExtraLengthFlatMin() {
        return 200;
    }

    public float getFlareExtraLengthMultMax() {
        return 1.5f;
    }

    public float getFlareExtraLengthMultMin() {
        return 1;
    }

    public float getFlareFadeInMax() {
        return 10f;
    }

    public float getFlareFadeInMin() {
        return 3f;
    }

    public float getFlareFadeOutMax() {
        return 10f;
    }

    public float getFlareFadeOutMin() {
        return 3f;
    }

    public float getFlareOccurrenceAngle() {
        return 0;
    }

    public float getFlareOccurrenceArc() {
        return 360f;
    }

    public float getFlareProbability() {
        return params.flareProbability;
    }

    public float getFlareSmallArcMax() {
        return 20;
    }

    public float getFlareSmallArcMin() {
        return 10;
    }

    public float getFlareSmallExtraLengthFlatMax() {
        return 100;
    }

    public float getFlareSmallExtraLengthFlatMin() {
        return 50;
    }

    public float getFlareSmallExtraLengthMultMax() {
        return 1.05f;
    }

    public float getFlareSmallExtraLengthMultMin() {
        return 1;
    }

    public float getFlareSmallFadeInMax() {
        return 2f;
    }

    public float getFlareSmallFadeInMin() {
        return 1f;
    }

    public float getFlareSmallFadeOutMax() {
        return 2f;
    }

    public float getFlareSmallFadeOutMin() {
        return 1f;
    }

    public float getFlareShortenFlatModMax() {
        return 0.05f;
    }

    public float getFlareShortenFlatModMin() {
        return 0.05f;
    }

    public float getFlareSmallShortenFlatModMax() {
        return 0.05f;
    }

    public float getFlareSmallShortenFlatModMin() {
        return 0.05f;
    }

    public int getFlareMaxSmallCount() {
        return 0;
    }

    public int getFlareMinSmallCount() {
        return 0;
    }

    public float getFlareSkipLargeProbability() {
        return 0f;
    }

    public CombatEntityAPI getFlareCenterEntity() {
        return this.entity;
    }

    public float getMaxEffectRadius(Vector2f locFrom) {
        float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), locFrom);
        float maxDist = params.bandWidthInEngine;
        if (flareManager.isInActiveFlareArc(angle)) {
            maxDist = computeRadiusWithFlare(flareManager.getActiveFlare());
        }
        return maxDist;
    }
    public float getMinEffectRadius(Vector2f locFrom) {
        return 0f;
    }

    public float getOptimalEffectRadius(Vector2f locFrom) {
        return params.relatedEntity.getCollisionRadius();
    }

    public boolean canPlayerHoldStationIn() {
        return false;
    }

    public CombatFlareManager getFlareManager() {
        return flareManager;
    }

    public String getModId() {
        return terrainId + "_stat_mod";
    }

}
