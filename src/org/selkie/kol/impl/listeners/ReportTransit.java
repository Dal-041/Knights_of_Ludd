package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.listeners.GateTransitListener;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionResult;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionState;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.GateCMD;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CampaignEntity;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.util.List;

public class ReportTransit implements GateTransitListener {

	@Override
	public void reportFleetTransitingGate(CampaignFleetAPI fleet, SectorEntityToken gateFrom, SectorEntityToken gateTo) {
		if (!fleet.isPlayerFleet() || gateFrom == null || Global.getSector().getClock().getCycle() <= 206) return;

		if (Math.random() <= 0.05f) { //0.05f
			StarSystemAPI destSys = Global.getSector().getStarSystem(PrepareAbyss.nullspaceSysName);
			if (destSys == null) return;
			SectorEntityToken dest = destSys.getEntityById(PrepareAbyss.nullgateID);
			float dist = Misc.getDistanceLY(dest, gateTo);

			if (dist < 12f) {
				fleet.getContainingLocation().removeEntity(fleet);
				dest.getContainingLocation().addEntity(fleet);
				Global.getSector().setCurrentLocation(dest.getContainingLocation());
				fleet.setLocation(dest.getLocation().x,
						dest.getLocation().y);
				fleet.setNoEngaging(1.0f);
				fleet.clearAssignments();


				GateCMD.notifyScanned(dest);
				GateEntityPlugin.getGateData().scanned.add(dest);
			}
		}
	}
}
