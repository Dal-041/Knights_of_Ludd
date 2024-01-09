package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.RingSystemTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class AbyssAsteroidField extends AsteroidFieldTerrainPlugin {

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

            if (fleet.hasTag(excludeTag)) return;

//			float penalty = getBurnPenalty(fleet);
//			fleet.getStats().addTemporaryModMult(0.1f, getModId() + "_1",
//								"Inside " + getNameForTooltip().toLowerCase(), 1f - penalty,
//								fleet.getStats().getFleetwideMaxBurnMod());

            if (Misc.isSlowMoving(fleet)) {
                fleet.getStats().addTemporaryModMult(0.1f, getModId() + "_2",
                        "Hiding inside " + getNameForTooltip().toLowerCase(), RingSystemTerrainPlugin.getVisibilityMult(fleet),
                        fleet.getStats().getDetectedRangeMod());
            }
//			if (fleet.isPlayerFleet()) {
//				System.out.println("efwefwe");
//			}
            if (!fleet.isInHyperspaceTransition()) {
                String key = "$asteroidImpactTimeout";
                String sKey = "$skippedImpacts";
                String recentKey = "$recentImpact";
                float probPerSkip = 0.15f;
                float maxProb = 1f;
                float maxSkipsToTrack = 7;
                float durPerSkip = 0.2f;
                MemoryAPI mem = fleet.getMemoryWithoutUpdate();
                if (!mem.contains(key)) {
                    float expire = mem.getExpire(sKey);
                    if (expire < 0) expire = 0;

                    float hitProb = Misc.getFleetRadiusTerrainEffectMult(fleet) * 0.5f;
                    //hitProb = 0.33f;
                    hitProb = 0.5f;
                    //hitProb = 1f;
                    hitProb = expire / durPerSkip * probPerSkip;
                    if (hitProb > maxProb) hitProb = maxProb;
                    if ((float) Math.random() < hitProb) {
                        boolean hadRecent = mem.is(recentKey, true);
                        hadRecent &= (float) Math.random() > 0.5f;
                        fleet.addScript(new AbyssAsteroidImpact(fleet, hadRecent));
                        mem.set(sKey, true, 0);
                        mem.set(recentKey, true, 0.5f + 1f * (float) Math.random());
                    } else {
                        mem.set(sKey, true, Math.min(expire + durPerSkip, maxSkipsToTrack * durPerSkip));
                    }
                    mem.set(key, true, (float) (0.05f + 0.1f * Math.random()));
                    //mem.set(key, true, (float) (0.01f + 0.02f * Math.random()));
                }
            }
        }
    }

    @Override
    public boolean hasAIFlag(Object flag, CampaignFleetAPI fleet) {
        if (!fleet.hasTag(excludeTag)) return flag == TerrainAIFlags.REDUCES_SPEED_LARGE || flag == TerrainAIFlags.DANGEROUS_UNLESS_GO_SLOW;
        return flag == TerrainAIFlags.REDUCES_DETECTABILITY;
    }

}
