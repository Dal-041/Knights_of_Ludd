package org.selkie.kol.campaign.intel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicChurchHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.group.KnightsOfLuddTakeoverExpedition;

import java.util.*;

import static org.selkie.kol.helpers.KolStaticStrings.KOL_FLEET_MERGED_KNIGHTS;
import static org.selkie.kol.helpers.KolStaticStrings.kolFactionID;
import static org.selkie.zea.fleets.ZeaFleetManager.copyFleetMembers;

public class KnightsTakeoverOverride implements EveryFrameScript {
    KnightsOfLuddTakeoverExpedition intelVanilla = null;
    float factorReplace = 0.6f; // TODO replace with accessible setting

    @Override
    public boolean isDone() {
        return LuddicChurchHostileActivityFactor.isDefeatedExpedition();
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (KnightsOfLuddTakeoverExpedition.get() != null) {
            intelVanilla = KnightsOfLuddTakeoverExpedition.get();
            if (!intelVanilla.getFleets().isEmpty() && !intelVanilla.getFleets().get(0).hasTag(KOL_FLEET_MERGED_KNIGHTS)) {
                //Fleets are spawned as a batch, using the params data, so we can manipulate all of them at once.
                for (CampaignFleetAPI fleet : intelVanilla.getFleets()) {
                    float size = fleet.getFleetData().getFleetPointsUsed();
                    int replacementSize = pruneFleetMembers(fleet, factorReplace);
                    FleetParamsV3 params = new FleetParamsV3();
                    params.factionId = kolFactionID;
                    params.ignoreMarketFleetSizeMult = true;
                    params.combatPts = replacementSize;
                    params.transportPts = 0;
                    params.freighterPts = 0;
                    params.linerPts = 0;
                    params.utilityPts = 0;
                    CampaignFleetAPI second = FleetFactoryV3.createFleet(params);
                    copyFleetMembers(kolFactionID, second, fleet, false);
                    fleet.getFleetData().sort();
                    fleet.addTag(KOL_FLEET_MERGED_KNIGHTS);
                }
            }
        }
    }

    public int pruneFleetMembers (CampaignFleetAPI fleet, float factor) {
        List<FleetMemberAPI> toRemove = new ArrayList<>();
        int removed = 0;
        for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            if (member.isFlagship() || member.isCivilian()) continue;
            if (!member.getVariant().getFittedWings().isEmpty()) {
                //remove fighter wings if needed
            }
            if (Math.random() < factor) {
                toRemove.add(member);
            }
        }
        for (FleetMemberAPI ship : toRemove) {
            removed += ship.getFleetPointCost();
            fleet.getMembersWithFightersCopy().remove(ship);
        }
        return removed;
    }
}
