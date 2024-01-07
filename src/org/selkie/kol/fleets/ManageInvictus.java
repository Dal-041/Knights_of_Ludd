package org.selkie.kol.fleets;

import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;

public class ManageInvictus implements FleetEventListener {
	private final String MEMKEY_KOL_LP_INVICTUS_DROPPED = "$kol_lp_invictus_dropped";

	//Totally not adapted from Diable or anything :>
	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
	        
		// ignore that whole ordeal if the Invictus already dropped
		if(Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_LP_INVICTUS_DROPPED) 
	                && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_LP_INVICTUS_DROPPED)){
	            return;
		}
	        
		if(fleet.getFlagship()==null || !fleet.getFlagship().getHullSpec().getBaseHullId().startsWith("kol_invictus_lp")){
	            
			//remove the fleet if flag is dead
			if(!fleet.getMembersWithFightersCopy().isEmpty()){
	                SectorEntityToken source = fleet.getCurrentAssignment().getTarget();
	                fleet.clearAssignments();
	                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source , 9999);
			}
	            
	            //boss is dead, 
			boolean salvaged=false;
			for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()){
				if(f.getHullId().startsWith("kol_invictus_lp")) salvaged=true;
	                
	                //set memkey that the wreck must never spawn
	                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_LP_INVICTUS_DROPPED,true); 
	        }
	            
	            //spawn a derelict if it wasn't salvaged
			if(!salvaged){
	                //make sure there is a valid location to avoid spawning in the sun
	                Vector2f location = fleet.getLocation();
	            if(location==new Vector2f()){
	                location = primaryWinner.getLocation();
	            }
	                
	                //spawn the derelict object
	                SectorEntityToken wreck = MagicCampaign.createDerelict(
	                        "kol_invictus_lp_Hallowed",
	                        ShipRecoverySpecial.ShipCondition.WRECKED,
	                        false,
	                        -1,
	                        true,
	                        //orbitCenter,angle,radius,period);
	                        fleet.getStarSystem().getCenter(),VectorUtils.getAngle(new Vector2f(), location),location.length(),360);               
	                MagicCampaign.placeOnStableOrbit(wreck, true);
	                wreck.setName("Wreck of the Pather Invictus");
	                wreck.setFacing((float)Math.random()*360f);
	                
	                //set memkey that the wreck exist
	                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_LP_INVICTUS_DROPPED,true); 
	        }
	    }
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	    fleet.removeEventListener(this);
	}
}

