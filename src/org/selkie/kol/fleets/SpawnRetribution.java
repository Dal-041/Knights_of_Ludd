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
import org.selkie.kol.impl.helpers.ZeaStaticStrings;

public class SpawnRetribution {
	
	public static boolean spawnRetribution() {
	        
		SectorEntityToken target=null;
		if(Global.getSector().getEntityById(ZeaStaticStrings.EPIPHANY)!=null && Global.getSector().getEntityById(ZeaStaticStrings.EPIPHANY).getFaction() == Global.getSector().getFaction(Factions.LUDDIC_PATH)){
	            target = Global.getSector().getEntityById(ZeaStaticStrings.EPIPHANY);
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

	            PersonAPI RetributionCaptain = MagicCampaign.createCaptainBuilder(Factions.LUDDIC_PATH).create();

	            CampaignFleetAPI RetributionFleet = MagicCampaign.createFleetBuilder()
	                    .setFleetName("Ludd's Embrace")
	                    .setFleetFaction(Factions.LUDDIC_PATH)
	                    .setFleetType(FleetTypes.TASK_FORCE)
	                    .setFlagshipName("To Them Eternal Rest")
	                    .setFlagshipVariant(ZeaStaticStrings.KOL_BOSS_RET_LP_OVERDRIVEN)
	                    .setCaptain(RetributionCaptain)
	                    .setMinFP(275) //support fleet
	                    .setQualityOverride(2f)
	                    .setAssignment(FleetAssignment.DEFEND_LOCATION)
	                    .setAssignmentTarget(target)
	                    .setIsImportant(true)
	                    .setTransponderOn(true)
	                    .create();
	            RetributionFleet.setDiscoverable(true);
				RetributionFleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
				RetributionFleet.getMemoryWithoutUpdate().set(KOLStaticStrings.BOSS_RETRIBTUION_KEY, true);
				RetributionFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);

				RetributionFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
				RetributionFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
	            RetributionFleet.addEventListener(new ManageRetribution());

	        }

		return true;
	}
}
