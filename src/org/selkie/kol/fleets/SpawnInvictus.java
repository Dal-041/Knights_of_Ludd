package org.selkie.kol.fleets;

import com.fs.starfarer.api.impl.campaign.ids.*;
import org.magiclib.util.MagicCampaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import org.selkie.kol.helpers.KolStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;

public class SpawnInvictus {

	public static boolean spawnInvictus() {
	        
		SectorEntityToken target=null;
		if(Global.getSector().getEntityById(ZeaStaticStrings.CHALCEDON)!=null && Global.getSector().getEntityById(ZeaStaticStrings.CHALCEDON).getFaction() == Global.getSector().getFaction(Factions.LUDDIC_PATH)){
	            target = Global.getSector().getEntityById(ZeaStaticStrings.CHALCEDON);
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

	            CampaignFleetAPI invictusFleet = MagicCampaign.createFleetBuilder()
	                    .setFleetName("Hammer of Ludd")
	                    .setFleetFaction(Factions.LUDDIC_PATH)
	                    .setFleetType(FleetTypes.TASK_FORCE)
	                    .setFlagshipName("In the Glory of the Blessed")
	                    .setFlagshipVariant(KolStaticStrings.KOL_INVICTUS_LP_HALLOWED)
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
				invictusFleet.getMemoryWithoutUpdate().set(KolStaticStrings.KolMemKeys.KOL_BOSS_LP_INVICTUS_FLEET, true);

				invictusFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
				invictusFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
	            invictusFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
	            invictusFleet.addEventListener(new ManageInvictus());
	        }

		return true;
	}
}
