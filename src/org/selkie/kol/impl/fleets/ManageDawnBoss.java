package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Drops;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.intel.ZeaAbilityIntel;

public class ManageDawnBoss implements FleetEventListener {

	//Totally not adapted from Diable or anything :>
	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

		if(Global.getSector().getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_DAWN_BOSS_DONE)
	                && Global.getSector().getMemoryWithoutUpdate().getBoolean(ZeaMemKeys.ZEA_DAWN_BOSS_DONE)) {
	            return;
		}


		if(fleet.getFlagship()==null || !fleet.getFlagship().getHullSpec().getBaseHullId().startsWith(ZeaStaticStrings.ZEA_BOSS_NIAN)) {
	            
			//remove the fleet if flag is dead
			if(!fleet.getMembersWithFightersCopy().isEmpty()){
	                SectorEntityToken source = fleet.getCurrentAssignment().getTarget();
	                fleet.clearAssignments();
	                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source , 9999);
			}
	            
			//boss is dead,
			if (!Global.getSector().getPlayerStats().getGrantedAbilityIds().contains(ZeaStaticStrings.abilityJumpDawn)) {
				Global.getSector().getPlayerFleet().addAbility(ZeaStaticStrings.abilityJumpDawn);
				Global.getSector().getCharacterData().getMemoryWithoutUpdate().set("$ability:" + ZeaStaticStrings.abilityJumpDawn, true, 0);
				Global.getSector().getCharacterData().addAbility(ZeaStaticStrings.abilityJumpDawn);

				ZeaAbilityIntel notif = new ZeaAbilityIntel(fleet.getFaction().getCrest(), "Luna Sea");
				Global.getSector().getIntelManager().addIntel(notif);
				notif.endAfterDelay(14);
			}

			boolean salvaged=false;
			for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()){
				if(f.getHullId().startsWith(ZeaStaticStrings.ZEA_BOSS_NIAN)) {
					salvaged=true;
					//set memkey that the wreck must never spawn
					Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_DAWN_BOSS_DONE,true);

					f.getVariant().removeTag(ZeaMemKeys.BOSS_TAG);
				}
	        }
	            
	            //spawn a derelict if it wasn't salvaged
			if(!salvaged){
	                //make sure there is a valid location to avoid spawning in the sun
	                Vector2f location = fleet.getLocation();
	            if(location==null){
	                location = primaryWinner.getLocation();
	            }
	                
	            //spawn the derelict object
	            SectorEntityToken wreck = MagicCampaign.createDerelict(
						ZeaStaticStrings.ZEA_BOSS_NIAN_SALVATION,
	                    ShipRecoverySpecial.ShipCondition.WRECKED,
	                    false,
	                    -1,
	                    true,
	                    //orbitCenter,angle,radius,period);
	                    fleet.getStarSystem().getCenter(),VectorUtils.getAngle(new Vector2f(), location),location.length(),360);
	            //MagicCampaign.placeOnStableOrbit(wreck, true);
	            wreck.setName("Wreck of the Dawntide flagship");
	            wreck.setFacing((float)Math.random()*360f);
				wreck.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);

				ZeaUtils.bossWreckCleaner(wreck, true);

	            //set memkey that the wreck exist
	            Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_DAWN_BOSS_DONE,true);
	        }

			//Replacement cache
			LocationAPI system = primaryWinner.getContainingLocation();
			SectorEntityToken wreck = system.addCustomEntity(null, "Ejected Cache", Entities.EQUIPMENT_CACHE_SMALL, Factions.NEUTRAL);
			wreck.setFacing((float)Math.random()*360f);
			wreck.addDropRandom(Drops.GUARANTEED_ALPHA, 1);
			wreck.addDropRandom(ZeaDrops.ZEA_WEAPONS_HIGH, 6);
			wreck.addDropRandom(ZeaDrops.ZEA_WEAPONS_HIGH, 6);
			wreck.addDropRandom(ZeaDrops.TECHMINING_FIRST_FIND, 6);
			wreck.addDropRandom(ZeaDrops.OMEGA_WEAPONS_SMALL, 3);
			wreck.addDropRandom(ZeaDrops.OMEGA_WEAPONS_MEDIUM, 2);
			wreck.addDropRandom(ZeaDrops.OMEGA_WEAPONS_LARGE, 1);
			wreck.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
			wreck.setLocation(primaryWinner.getLocation().getX(), primaryWinner.getLocation().getY());
	    }
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	    fleet.removeEventListener(this);
	}
}

