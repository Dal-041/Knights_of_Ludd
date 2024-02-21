package org.selkie.kol.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionResult;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionState;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Nex_MarketCMD;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.SectorManager;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.util.List;

import static org.selkie.kol.world.Crusaders.MEMKEY_KOL_SCHISMED;

public class UpdateRelationships extends BaseCampaignEventListener {
    public UpdateRelationships(boolean permaRegister) {
		super(permaRegister);
	}

	@Override
    public void reportPlayerReputationChange(String factionId, float delta) {
		if (Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_SCHISMED)
				&& Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_SCHISMED)) {
			return;
		}
    	if (factionId.equals(Factions.LUDDIC_CHURCH)) {
			Global.getSector().getFaction(KOL_ModPlugin.kolID).adjustRelationship("player", delta);
    		if (Misc.getCommissionFactionId() != null && Misc.getCommissionFactionId().equals(KOL_ModPlugin.kolID)) {
    			if (Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getRelToPlayer().getRepInt() <= -50) {
    				List<IntelInfoPlugin> intels =  Global.getSector().getIntelManager().getIntel(FactionCommissionIntel.class);
    				for (IntelInfoPlugin intel : intels) {
    					FactionCommissionIntel intelTemp = (FactionCommissionIntel) intel;
    					if (!intelTemp.isCompleted()) {
    						intelTemp.setMissionResult(new MissionResult(-1, null));
    						intelTemp.setMissionState(MissionState.COMPLETED);
    						intelTemp.endMission();
    						intelTemp.sendUpdateIfPlayerHasIntel(null, false);
    						Global.getSector().getFaction(Factions.LUDDIC_CHURCH).adjustRelationship("player", delta, RepLevel.HOSTILE);
    						Global.getSector().getFaction(KOL_ModPlugin.kolID).adjustRelationship("player", delta, RepLevel.HOSTILE);
    					}
    				}
    			}
            }
        }
		if (factionId.equals(KOL_ModPlugin.kolID)) {
			Global.getSector().getFaction(Factions.LUDDIC_CHURCH).adjustRelationship("player", delta);
			if (Misc.getCommissionFactionId() != null && Misc.getCommissionFactionId().equals(Factions.LUDDIC_CHURCH)) {
				if (Global.getSector().getFaction(KOL_ModPlugin.kolID).getRelToPlayer().getRepInt() <= -50) {
					List<IntelInfoPlugin> intels =  Global.getSector().getIntelManager().getIntel(FactionCommissionIntel.class);
					for (IntelInfoPlugin intel : intels) {
						FactionCommissionIntel intelTemp = (FactionCommissionIntel) intel;
						if (!intelTemp.isCompleted()) {
							intelTemp.setMissionResult(new MissionResult(-1, null));
							intelTemp.setMissionState(MissionState.COMPLETED);
							intelTemp.endMission();
							intelTemp.sendUpdateIfPlayerHasIntel(null, false);
							Global.getSector().getFaction(Factions.LUDDIC_CHURCH).adjustRelationship("player", delta, RepLevel.HOSTILE);
							Global.getSector().getFaction(KOL_ModPlugin.kolID).adjustRelationship("player", delta, RepLevel.HOSTILE);
						}
					}
				}
			}
		}
		returnKOLMarkets();
    }

	//The Knights inherit relationships from the church, but not vice versa
	@Override
	public void reportEconomyTick (int iterIndex) {
		if (Global.getSector().getFaction(Factions.LUDDIC_CHURCH) != null && Global.getSector().getFaction(KOL_ModPlugin.kolID) != null) {
			FactionAPI church = Global.getSector().getFaction(Factions.LUDDIC_CHURCH);
			FactionAPI knights = Global.getSector().getFaction(KOL_ModPlugin.kolID);
			for(FactionAPI faction:Global.getSector().getAllFactions()) {
				knights.setRelationship(faction.getId(), church.getRelationship(faction.getId()));
			}
		}
	}

	public void returnKOLMarkets() {
		if (Global.getSector().getEconomy() == null) return;
		boolean nex = KOL_ModPlugin.haveNex;
		if (Global.getSector().getEconomy().getMarket("kol_cygnus") != null) {
			MarketAPI cygnus = Global.getSector().getEconomy().getMarket("kol_cygnus");
			if (cygnus.getFactionId().equals(Factions.LUDDIC_CHURCH)) {
				if (nex) {
					SectorManager.transferMarket(cygnus, Global.getSector().getFaction(KOL_ModPlugin.kolID), Global.getSector().getFaction(Factions.LUDDIC_CHURCH), false, false, null, 0, false);
				} else {
					cygnus.setFactionId(KOL_ModPlugin.kolID);
				}
			}
		}
		if (Global.getSector().getEconomy().getMarket("kol_lyra") != null) {
			MarketAPI lyra = Global.getSector().getEconomy().getMarket("kol_lyra");
			if (lyra.getFactionId().equals(Factions.LUDDIC_CHURCH)) {
				if (nex) {
					SectorManager.transferMarket(lyra, Global.getSector().getFaction(KOL_ModPlugin.kolID), Global.getSector().getFaction(Factions.LUDDIC_CHURCH), false, false, null, 0, false);
				} else {
					lyra.setFactionId(KOL_ModPlugin.kolID);
				}
			}
		}
	}
}
