package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberViewAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.terrain.CRLossPerSecondBuff;
import com.fs.starfarer.api.impl.campaign.terrain.PeakPerformanceBuff;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class AbyssCorona extends StarCoronaTerrainPlugin {
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

            if (fleet.hasTag(excludeTag)) return;

            boolean inFlare = false;
            if (flareManager.isInActiveFlareArc(fleet)) {
                inFlare = true;
            }

            float intensity = getIntensityAtPoint(fleet.getLocation());
            if (intensity <= 0) return;

            String buffId = getModId();
            float buffDur = 0.1f;

            boolean protectedFromCorona = false;
            if (fleet.isInCurrentLocation() &&
                    Misc.getDistance(fleet, Global.getSector().getPlayerFleet()) < 500) {
                for (SectorEntityToken curr : fleet.getContainingLocation().getCustomEntitiesWithTag(Tags.PROTECTS_FROM_CORONA_IN_BATTLE)) {
                    float dist = Misc.getDistance(curr, fleet);
                    if (dist < curr.getRadius() + fleet.getRadius() + 10f) {
                        protectedFromCorona = true;
                        break;
                    }
                }
            }

            // CR loss and peak time reduction
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                float recoveryRate = member.getStats().getBaseCRRecoveryRatePercentPerDay().getModifiedValue();
                float lossRate = member.getStats().getBaseCRRecoveryRatePercentPerDay().getBaseValue();

                float resistance = member.getStats().getDynamic().getValue(Stats.CORONA_EFFECT_MULT);
                if (protectedFromCorona) resistance = 0f;
                //if (inFlare) loss *= 2f;
                float lossMult = 1f;
                if (inFlare) lossMult = 2f;
                float adjustedLossMult = (0f + params.crLossMult * intensity * resistance * lossMult * CR_LOSS_MULT_GLOBAL);

                float loss = (-1f * recoveryRate + -1f * lossRate * adjustedLossMult) * days * 0.01f;
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
            float maxFleetBurn = fleet.getFleetData().getBurnLevel();
            float currFleetBurn = fleet.getCurrBurnLevel();

            float maxWindBurn = params.windBurnLevel;
            if (inFlare) {
                maxWindBurn *= 2f;
            }


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
            float fleetAccelMult = fleet.getStats().getAccelerationMult().getModifiedValue();
            if (fleetAccelMult > 0) {// && fleetAccelMult < 1) {
                accelMult /= fleetAccelMult;
            }

            float seconds = days * Global.getSector().getClock().getSecondsPerDay();

            Vector2f vel = fleet.getVelocity();
            windDir.scale(seconds * fleet.getAcceleration() * accelMult);
            fleet.setVelocity(vel.x + windDir.x, vel.y + windDir.y);

            Color glowColor = getAuroraColorForAngle(angle);
            int alpha = glowColor.getAlpha();
            if (alpha < 75) {
                glowColor = Misc.setAlpha(glowColor, 75);
            }
            // visual effects - glow, tail


            float dist = Misc.getDistance(this.entity.getLocation(), fleet.getLocation());
            float check = 100f;
            if (params.relatedEntity != null) check = params.relatedEntity.getRadius() * 0.5f;
            if (dist > check) {
                float durIn = 1f;
                float durOut = 10f;
                Misc.normalise(windDir);
                float sizeNormal = 5f + 10f * intensity;
                float sizeFlare = 10f + 15f * intensity;
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
            }
        }
    }

    @Override
    public boolean hasAIFlag(Object flag, CampaignFleetAPI fleet) {
        if (!fleet.hasTag(excludeTag)) {
            return flag == TerrainAIFlags.CR_DRAIN ||
                    flag == TerrainAIFlags.BREAK_OTHER_ORBITS ||
                    flag == TerrainAIFlags.EFFECT_DIMINISHED_WITH_RANGE;
        }
        return flag == TerrainAIFlags.HIDING_STATIONARY;
    }
}
