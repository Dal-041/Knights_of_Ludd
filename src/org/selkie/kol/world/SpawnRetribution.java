package org.selkie.kol.world;

import org.magiclib.util.MagicCampaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

public class SpawnRetribution {
	
	public static boolean spawnRetribution() {
	        
		SectorEntityToken target=null;
		if(Global.getSector().getEntityById("epiphany")!=null && Global.getSector().getEntityById("epiphany").getFaction() == Global.getSector().getFaction("luddic_path")){
	            target = Global.getSector().getEntityById("epiphany");
	        } else {
	            for(MarketAPI markets : Global.getSector().getEconomy().getMarketsCopy()){
	                if(markets.getFaction().getId().equals("luddic_path")){
	                    if(target==null || 
	                    (markets.hasSubmarket(Submarkets.GENERIC_MILITARY) 
	                    && (!target.getMarket().hasSubmarket(Submarkets.GENERIC_MILITARY)
	                    || markets.getSize()>target.getMarket().getSize()))) {
	                        target=markets.getPrimaryEntity();
	                    }
	                }
	            }
	        }
	        if(target!=null) {

	            PersonAPI RetributionCaptain = MagicCampaign.createCaptainBuilder("luddic_path").create();

	            /**
	            * Creates a fleet with a defined flagship and optional escort
	            * 
	            * @param fleetName
	            * @param fleetFaction
	            * @param fleetType
	            * campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
	            * @param flagshipName
	            * Optional flagship name
	            * @param flagshipVariant
	            * @param captain
	            * PersonAPI, can be NULL for random captain, otherwise use createCaptain() 
	            * @param supportFleet
	            * Optional escort ship VARIANTS and their NUMBERS
	            * @param minFP
	            * Minimal fleet size, can be used to adjust to the player's power, set to 0 to ignore
	            * @param reinforcementFaction
	            * Reinforcement faction, if the fleet faction is a "neutral" faction without ships
	            * @param qualityOverride
	            * Optional ship quality override, default to 2 (no D-mods) if null or <0
	            * @param spawnLocation
	            * Where the fleet will spawn, default to assignmentTarget if NULL
	            * @param assignment
	            * campaign.FleetAssignment, default to orbit aggressive
	            * @param assignementTarget
	            * @param isImportant
	            * @param transponderOn
	            * @return 
	            */
	            String variant = "kol_boss_ret_lp_Hull";
	            CampaignFleetAPI RetributionFleet = (CampaignFleetAPI)MagicCampaign.createFleetBuilder()
	                    .setFleetName("Ludd's Embrace")
	                    .setFleetFaction("luddic_path")
	                    .setFleetType(FleetTypes.TASK_FORCE)
	                    .setFlagshipName("To Them Eternal Rest")
	                    .setFlagshipVariant(variant)
	                    .setCaptain(RetributionCaptain)
	                    .setMinFP(275) //support fleet
	                    .setQualityOverride(2f)
	                    .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
	                    .setAssignmentTarget(target)
	                    .setIsImportant(true)
	                    .setTransponderOn(true)
	                    .create();
	            RetributionFleet.setDiscoverable(false);
	            //RetributionFleet.addTag(Tags.NEUTRINO); //?
	            RetributionFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
	            RetributionFleet.addEventListener(new ManageRetribution());
	        }

		return true;
	}
}
