package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;

public class ManageElysianCorruptingheart implements FleetEventListener {
	public static final String MEMKEY_KOL_ELYSIAN_BOSS2_DONE = "$kol_elysian_boss2_done";

	//Totally not adapted from Diable or anything :>
	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

		if(Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_ELYSIAN_BOSS2_DONE)
	                && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_ELYSIAN_BOSS2_DONE)){
	            return;
		}

		boolean salvaged1 = false;
		boolean killed1 = false;
		if(fleet.getFlagship()==null || !fleet.getFlagship().getHullSpec().getBaseHullId().startsWith("abyss_boss_corruptingheart")) {

			//remove the fleet if flag is dead
			if (!fleet.getMembersWithFightersCopy().isEmpty()) {
				SectorEntityToken source = fleet.getCurrentAssignment().getTarget();
				fleet.clearAssignments();
				fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 9999);
			}

			//boss is dead,
			killed1 = true;
		}

		if (killed1) {
			for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
				if (f.getHullId().startsWith("abyss_boss_corruptingheart")) {
					salvaged1 = true;
					//set memkey that the wreck must never spawn
					Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_ELYSIAN_BOSS2_DONE, true);
				}
			}

			//spawn a derelict if it wasn't salvaged
			if (!salvaged1) {
				//make sure there is a valid location to avoid spawning in the sun
				Vector2f location = fleet.getLocation();
				if(location == null){
					location = primaryWinner.getLocation();
				}
				//spawn the derelict object
				SectorEntityToken wreck = MagicCampaign.createDerelict(
						"abyss_boss_corruptingheart_Unholy",
						ShipRecoverySpecial.ShipCondition.WRECKED,
						false,
						-1,
						false,
						//orbitCenter,angle,radius,period);
						fleet.getStarSystem().getCenter(), VectorUtils.getAngle(new Vector2f(), location), location.length(), 360);
				//MagicCampaign.placeOnStableOrbit(wreck, true);
				wreck.setName("Wreck of the AI flagship");
				wreck.setFacing((float) Math.random() * 360f);

				//set memkey that the wreck exist
				Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_ELYSIAN_BOSS2_DONE, true);
			}
		}
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	    fleet.removeEventListener(this);
	}
}

