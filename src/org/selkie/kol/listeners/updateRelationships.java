package org.selkie.kol.listeners;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionResult;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionState;
import com.fs.starfarer.api.util.Misc;

public class updateRelationships extends BaseCampaignEventListener {
    public updateRelationships(boolean permaRegister) {
		super(permaRegister);
	}

	@Override
    public void reportPlayerReputationChange(String factionId, float delta) {
    	if (factionId.equals("luddic_church")) {
    		if (Misc.getCommissionFactionId() != null ? Misc.getCommissionFactionId().equals("knights_of_selkie") : false) {
    			if (Global.getSector().getFaction("luddic_church").getRelToPlayer().getLevel().isAtWorst(RepLevel.INHOSPITABLE)) {
    				List<IntelInfoPlugin> intels =  Global.getSector().getIntelManager().getIntel(FactionCommissionIntel.class);
    				for (IntelInfoPlugin intel : intels) {
    					FactionCommissionIntel intelTemp = (FactionCommissionIntel) intel;
    					if (!intelTemp.isCompleted()) {
    						intelTemp.setMissionResult(new MissionResult(-1, null));
    						intelTemp.setMissionState(MissionState.COMPLETED);
    						intelTemp.endMission();
    						intelTemp.sendUpdateIfPlayerHasIntel(null, false);
    						Global.getSector().getFaction("luddic_church").adjustRelationship("player", delta, RepLevel.HOSTILE);
    						Global.getSector().getFaction("knights_of_selkie").adjustRelationship("player", delta, RepLevel.HOSTILE);
    					}                          
    				};
    			}
            }
        }
    }
		
	@Override
	public void reportEconomyTick (int iterIndex) {
		if (Global.getSector().getFaction("luddic_church") != null && Global.getSector().getFaction("knights_of_selkie") != null) {
			FactionAPI church = Global.getSector().getFaction("luddic_church");
			FactionAPI knights = Global.getSector().getFaction("knights_of_selkie");
			for(FactionAPI faction:Global.getSector().getAllFactions()) {
				knights.setRelationship(faction.getId(), church.getRelationship(faction.getId()));
			}
		}
	}
}
