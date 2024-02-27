package org.selkie.kol.fleets;

import com.fs.starfarer.api.impl.campaign.ids.*;
import org.magiclib.util.MagicCampaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import org.selkie.kol.helpers.KOLStaticStrings;

public class SpawnInvictus {
	
	public static boolean spawnInvictus() {
	        
		SectorEntityToken target=null;
		if(Global.getSector().getEntityById("chalcedon")!=null && Global.getSector().getEntityById("chalcedon").getFaction() == Global.getSector().getFaction(Factions.LUDDIC_PATH)){
	            target = Global.getSector().getEntityById("chalcedon");
	        } else {
	            for(MarketAPI markets : Global.getSector().getEconomy().getMarketsCopy()){
	                if(markets.getFaction().getId().equals(Factions.LUDDIC_PATH)){
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

	            PersonAPI invictusCaptain = MagicCampaign.createCaptainBuilder(Factions.LUDDIC_PATH).create();

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
	            String variant = "kol_invictus_lp_Hallowed";
	            CampaignFleetAPI invictusFleet = MagicCampaign.createFleetBuilder()
	                    .setFleetName("Hammer of Ludd")
	                    .setFleetFaction(Factions.LUDDIC_PATH)
	                    .setFleetType(FleetTypes.TASK_FORCE)
	                    .setFlagshipName("In the Glory of the Blessed")
	                    .setFlagshipVariant(variant)
	                    .setCaptain(invictusCaptain)
	                    .setMinFP(400) //support fleet
	                    .setQualityOverride(2f)
	                    .setAssignment(FleetAssignment.DEFEND_LOCATION)
	                    .setAssignmentTarget(target)
	                    .setIsImportant(true)
	                    .setTransponderOn(true)
	                    .create();
	            invictusFleet.setDiscoverable(true);
				invictusFleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
				invictusFleet.getMemoryWithoutUpdate().set(KOLStaticStrings.BOSS_INVICTUS_KEY, true);

				invictusFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
				invictusFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
	            invictusFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
	            invictusFleet.addEventListener(new ManageInvictus());
	        }

		return true;
	}
}
