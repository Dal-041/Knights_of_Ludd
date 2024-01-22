package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberViewAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.terrain.*;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class AbyssPulsarBeamTerrainPlugin extends BaseRingTerrain implements PulsarRenderer.PulsarRendererDelegate {

    public float PULSAR_ARC = 1 / ((float) Math.PI * 2f) * 360f;
    public float visMult = 1f;
    public boolean single = false;
    public String nameTooltip = "Pulsar Beam";

    public String spriteCat = "terrain";
    public String spriteKey = "pulsar";
    //public static float PULSAR_ARC = 0.25f / ((float) Math.PI * 2f) * 360f;

    transient protected SpriteAPI flareTexture = null;
    transient Color color = null;

    transient protected PulsarRenderer flare1, flare2;
    protected StarCoronaTerrainPlugin.CoronaParams params;
    protected transient RangeBlockerUtil blocker = null;

    protected float pulsarAngle = (float) Math.random() * 360f;
    protected float pulsarRotation = -1f * (10f + (float) Math.random() * 10f);

    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        params = (StarCoronaTerrainPlugin.CoronaParams) param;
        name = params.name;
        if (name == null) {
            name = "Pulsar Beam";
        }
    }

    public void setFlareTexture(SpriteAPI texture) {
        flareTexture = texture;
    }

    public void multiplyArc(float mult) {
        PULSAR_ARC = mult / ((float) Math.PI * 2f) * 360f;
        //if (mult >= 2f) flareTexture = Global.getSettings().getSprite("terrain", "wavefront");
    }

    public String getNameForTooltip() {
        return nameTooltip;
    }

    @Override
    protected Object readResolve() {
        super.readResolve();
        /*
            "aurora":"graphics/planets/aurorae.png",
			#"aurora_neutron":"graphics/planets/aurorae2.png",
			"pulsar":"graphics/fx/beam_pulsar.png",
			#"pulsar2":"graphics/fx/beam_pulsar2.png",
			#"wavefront":"graphics/planets/aurorae.png",
			"wavefront":"graphics/planets/rings_ice0.png",
        */
        //flareTexture = Global.getSettings().getSprite("graphics/planets/aurorae2.png");
        if (spriteCat == null) {
            spriteCat = "terrain";
            spriteKey = "pulsar";
        }
        flareTexture = Global.getSettings().getSprite(spriteCat, spriteKey);

        layers = EnumSet.of(CampaignEngineLayers.TERRAIN_7);
        if (blocker == null) {
            //blocker = new RangeBlockerUtil(360, super.params.bandWidthInEngine + 1000f);
            blocker = new RangeBlockerUtil(720 * 2, super.params.bandWidthInEngine + 1000f);
        }

        flare1 = new PulsarRenderer(this);
        if (!single) flare2 = new PulsarRenderer(this);
        return this;
    }

    Object writeReplace() {
        return this;
    }

    @Override
    protected boolean shouldPlayLoopOne() {
        return super.shouldPlayLoopOne() && containsEntity(Global.getSector().getPlayerFleet());
    }

    @Override
    protected float getLoopOneVolume() {
        float intensity = getIntensityAtPoint(Global.getSector().getPlayerFleet().getLocation());
        return intensity;
    }

    @Override
    protected float getExtraSoundRadius() {
        return 0f;
//		float base = super.getExtraSoundRadius();
//
//		//float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), Global.getSector().getPlayerFleet().getLocation());
//		float extra = 0f;
//
//		return base + extra;
    }


    transient private EnumSet<CampaignEngineLayers> layers = EnumSet.of(CampaignEngineLayers.TERRAIN_7);
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return layers;
    }

    public StarCoronaTerrainPlugin.CoronaParams getParams() {
        return params;
    }

    public void advance(float amount) {
        super.advance(amount);

        float days = Global.getSector().getClock().convertToDays(amount);
        pulsarAngle += pulsarRotation * days * 1f;
        pulsarAngle = Misc.normalizeAngle(pulsarAngle);
        //pulsarAngle += pulsarRotation * days * 0.5f;

//		if (params.relatedEntity instanceof PlanetAPI) {
//			PlanetAPI planet = (PlanetAPI) params.relatedEntity;
//			planet.getSpec().setTilt(pulsarAngle);
//			planet.applySpecChanges();
//		}

        flare1.advance(amount);
        if (!single) flare2.advance(amount);

        flare1.setCurrAngle(pulsarAngle);
        if (!single) flare2.setCurrAngle(pulsarAngle + 180f);

        //pulsarAngle += pulsarRotation * days * 10.25f;
        //pulsarAngle += pulsarRotation * days * 5f;

        if (amount > 0 && blocker != null) {
            blocker.updateLimits(entity, params.relatedEntity, 0.5f);
            //blocker.sync();
            blocker.advance(amount, 100f, 0.5f);
        }


    }

    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (blocker != null && !blocker.wasEverUpdated()) {
            blocker.updateAndSync(entity, params.relatedEntity, 0.1f);
        }


        if (isNearViewport(pulsarAngle, viewport)) {
            flare1.render(viewport.getAlphaMult());
        }
//		else {
//			System.out.println("SKIP1");
//		}

        if (!single && isNearViewport(pulsarAngle + 180f, viewport)) {
            flare2.render(viewport.getAlphaMult());
        }
//		else {
//			System.out.println("SKIP2");
//		}
    }


    protected boolean isNearViewport(float angle, ViewportAPI viewport) {
        float wClose = getPulsarInnerWidth();
        float wFar = getPulsarOuterWidth();
        float distClose = getPulsarInnerRadius();
        float distFar = getPulsarOuterRadius();

        float length = distFar - distClose;
        float incr = (float) Math.ceil((distFar - distClose) / 2000f);

        for (float dist = wClose; dist < distFar; dist += incr) {
            Vector2f test = Misc.getUnitVectorAtDegreeAngle(angle);
            test.scale(dist);
            Vector2f.add(test, entity.getLocation(), test);

            float testDist = wClose + (wFar - wClose) * (dist - distClose) / length;
            testDist *= 0.5f;
            if (viewport.isNearViewport(test, testDist + 500f)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public float getRenderRange() {
        return getPulsarOuterRadius() + 1000f;
    }

    @Override
    public boolean containsPoint(Vector2f point, float radius) {
        if (blocker != null && blocker.isAnythingShortened()) {
            float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), point);
            float dist = Misc.getDistance(this.entity.getLocation(), point);
            float max = blocker.getCurrMaxAt(angle);
            if (dist > max) return false;
        }

        if (!Misc.isInArc(pulsarAngle, PULSAR_ARC, entity.getLocation(), point) &&
                (single || !Misc.isInArc(pulsarAngle + 180f, PULSAR_ARC, entity.getLocation(), point))) {
            return false;
        }

        float dist = Misc.getDistance(this.entity.getLocation(), point);
        if (dist < getPulsarInnerRadius()) return false;

        return super.containsPoint(point, radius);
    }



    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

            float intensity = getIntensityAtPoint(fleet.getLocation());
            if (intensity <= 0) return;
            if (fleet.hasTag("zea_rulesfortheebutnotforme")) return;

            String buffId = getModId();
            float buffDur = 0.1f;

            // CR loss
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                float recoveryRate = member.getStats().getBaseCRRecoveryRatePercentPerDay().getModifiedValue();
                float lossRate = member.getStats().getBaseCRRecoveryRatePercentPerDay().getBaseValue();

                float resistance = member.getStats().getDynamic().getValue(Stats.CORONA_EFFECT_MULT);
                //if (inFlare) loss *= 2f;
                float lossMult = 1f;
                float adjustedLossMult = (0f + params.crLossMult * intensity * resistance * lossMult * StarCoronaTerrainPlugin.CR_LOSS_MULT_GLOBAL);

                float loss = (-1f * recoveryRate + -1f * lossRate * adjustedLossMult) * days * 0.01f;
                float curr = member.getRepairTracker().getBaseCR();
                if (loss > curr) loss = curr;

                if (resistance > 0) {
                    member.getRepairTracker().applyCREvent(loss, "corona", "Pulsar beam effect");
                }

                float peakFraction = 1f / Math.max(1.3333f, 1f + params.crLossMult * intensity);
                float peakLost = 1f - peakFraction;
                peakLost *= resistance;

                float degradationMult = 1f + (params.crLossMult * intensity * resistance) / 2f;

                member.getBuffManager().addBuffOnlyUpdateStat(new PeakPerformanceBuff(buffId + "_1", 1f - peakLost, buffDur));
                member.getBuffManager().addBuffOnlyUpdateStat(new CRLossPerSecondBuff(buffId + "_2", degradationMult, buffDur));
            }

            // "wind" effect - adjust velocity
            float maxFleetBurn = fleet.getFleetData().getBurnLevel();
            float currFleetBurn = fleet.getCurrBurnLevel();

            float maxWindBurn = params.windBurnLevel;


            float currWindBurn = intensity * maxWindBurn;
            float maxFleetBurnIntoWind = maxFleetBurn - Math.abs(currWindBurn);

            float angle = Misc.getAngleInDegreesStrict(this.entity.getLocation(), fleet.getLocation());
            Vector2f windDir = Misc.getUnitVectorAtDegreeAngle(angle);
            if (currWindBurn < 0) {
                windDir.negate();
            }

            Vector2f velDir = Misc.normalise(new Vector2f(fleet.getVelocity()));
            velDir.scale(currFleetBurn);

            float fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir);

            float accelMult = 0.5f;
            if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
                accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind);
            }

            float seconds = days * Global.getSector().getClock().getSecondsPerDay();

            Vector2f vel = fleet.getVelocity();
            windDir.scale(seconds * fleet.getAcceleration() * accelMult);
            fleet.setVelocity(vel.x + windDir.x, vel.y + windDir.y);

//			if (fleet.getOrbit() != null) {
//				fleet.setOrbit(null);
//			}

            Color glowColor = getPulsarColorForAngle(angle);
            int alpha = glowColor.getAlpha();
            if (alpha < 75) {
                glowColor = Misc.setAlpha(glowColor, 75);
            }
            // visual effects - glow, tail

            float durIn = 1f;
            float durOut = 3f;
            Misc.normalise(windDir);
            float sizeNormal = 10f + 25f * intensity;
            for (FleetMemberViewAPI view : fleet.getViews()) {
                view.getWindEffectDirX().shift(getModId(), windDir.x * sizeNormal, durIn, durOut, 1f);
                view.getWindEffectDirY().shift(getModId(), windDir.y * sizeNormal, durIn, durOut, 1f);
                view.getWindEffectColor().shift(getModId(), glowColor, durIn, durOut, intensity);
            }
        }
    }



    public float getIntensityAtPoint(Vector2f point) {
        float maxDist = params.bandWidthInEngine;
        float minDist = params.relatedEntity.getRadius();
        float dist = Misc.getDistance(point, params.relatedEntity.getLocation());

        if (dist > maxDist) return 0f;

        float intensity = 1f;
        if (minDist < maxDist) {
            intensity = 1f - (dist - minDist) / (maxDist - minDist);
            //intensity = 0.5f + intensity * 0.5f;
            if (intensity < 0) intensity = 0;
            if (intensity > 1) intensity = 1;
        }

        float angle = Misc.getAngleInDegreesStrict(params.relatedEntity.getLocation(), point);
        float diff = Misc.getAngleDiff(angle, pulsarAngle);
        if (!single) diff = Math.min(diff, Misc.getAngleDiff(angle, pulsarAngle + 180f));
        float maxDiff = PULSAR_ARC / 2f;
        if (diff > maxDiff) diff = maxDiff;

        if (diff > maxDiff * 0.5f) {
            intensity *= 0.25f + 0.75f * (1f - (diff - maxDiff * 0.5f) / (maxDiff * 0.5f));
        }

        return intensity;
    }



    @Override
    public Color getNameColor() {
        Color bad = Misc.getNegativeHighlightColor();
        Color base = super.getNameColor();
        //bad = Color.red;
        return Misc.interpolateColor(base, bad, Global.getSector().getCampaignUI().getSharedFader().getBrightness() * 1f);
    }

    public boolean hasTooltip() {
        return true;
    }

    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        float small = 5f;
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();
        Color fuel = Global.getSettings().getColor("progressBarFuelColor");
        Color bad = Misc.getNegativeHighlightColor();

        tooltip.addTitle(getNameForTooltip());
        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).getText1(), pad);

        float nextPad = pad;
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad);
            nextPad = small;
        }
        tooltip.addPara("Reduces the combat readiness of " +
                "all ships caught in the pulsar beam at a rapid pace, and blows the fleet off-course.", nextPad);
        tooltip.addPara("The magnitude of the effect drops off rapidly with distance from the source.", pad);

        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad);
            tooltip.addPara("Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.", small);
        }

        //tooltip.addPara("Does not stack with other similar terrain effects.", pad);
    }

    public boolean isTooltipExpandable() {
        return true;
    }

    public float getTooltipWidth() {
        return 350f;
    }

    public String getTerrainName() {
        return super.getTerrainName();
    }

    public String getEffectCategory() {
        return null; // to ensure multiple coronas overlapping all take effect
        //return "corona_" + (float) Math.random();
    }

    public boolean hasAIFlag(Object flag, CampaignFleetAPI fleet) {
        if (fleet != null && containsEntity(fleet)) {
            return hasAIFlag(flag);
        }
        return false;
    }

    public boolean hasAIFlag(Object flag) {
        return flag == TerrainAIFlags.CR_DRAIN ||
                flag == TerrainAIFlags.BREAK_OTHER_ORBITS ||
                flag == TerrainAIFlags.EFFECT_DIMINISHED_WITH_RANGE;
    }

    public float getMaxEffectRadius(Vector2f locFrom) {
        //float angle = Misc.getAngleInDegrees(params.relatedEntity.getLocation(), locFrom);
        float maxDist = params.bandWidthInEngine;
        return maxDist;
    }
    public float getMinEffectRadius(Vector2f locFrom) {
        return 0f;
    }

    public float getOptimalEffectRadius(Vector2f locFrom) {
        return params.relatedEntity.getRadius();
    }

    public boolean canPlayerHoldStationIn() {
        return false;
    }


    public RangeBlockerUtil getPulsarBlocker() {
        //return null;
        return blocker;
    }

    public Vector2f getPulsarCenterLoc() {
        return params.relatedEntity.getLocation();
    }

    public Color getPulsarColorForAngle(float angle) {
        if (color == null) {
            Color c = Color.white;
            if (params.relatedEntity instanceof PlanetAPI) {
                c = ((PlanetAPI)params.relatedEntity).getSpec().getCoronaColor();
            } else {
                c = Color.white;
            }
            float alpha = 1f;
            color = Misc.setAlpha(c, (int) (200 * alpha));
            return color;
        } else {
            return color;
        }
    }


    public float getPulsarInnerRadius() {
        return params.relatedEntity.getRadius();
    }


    public float getPulsarOuterRadius() {
        return params.middleRadius + params.bandWidthInEngine * 0.5f;
    }

    public float getPulsarInnerWidth() {
        //PULSAR_ARC = 1f / ((float) Math.PI * 2f) * 360f;
        return PULSAR_ARC / 360f * 2f * (float) Math.PI * getPulsarInnerRadius();
        //return PULSAR_ARC / 360f * 2f * (float) Math.PI * getPulsarInnerRadius();
    }

    public float getPulsarOuterWidth() {
        float r1 = getPulsarInnerRadius();
        float r2 = getPulsarOuterRadius();
        return getPulsarInnerWidth() * r2 / r1;
    }

    public float getPulsarScrollSpeed() {
        return 50f;
    }

    public SpriteAPI getPulsarTexture() {
        return flareTexture;
    }
}





