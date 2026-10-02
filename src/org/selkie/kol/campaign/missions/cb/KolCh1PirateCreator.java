package org.selkie.kol.campaign.missions.cb;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import org.selkie.kol.helpers.KolStaticStrings;
import org.lwjgl.util.vector.Vector2f;

/**
 * Pirates raiding the ore lighters that supply Cygnus' yards (Knights Chapter 1 make-work bounty).
 * Based on vanilla's CBPirate (0.98a): same fleet sizes per difficulty and the same reward table,
 * but always offered (no faction check) and placed near Battlestation Cygnus.
 * Offer text: the rules.csv trigger KolCh1PirateCreatorOfferDesc (this class's simple name + "OfferDesc").
 */
public class KolCh1PirateCreator extends BaseCustomBountyCreator {

	public static float PROB_IN_SYSTEM_WITH_BASE = 0.5f;
	public static float PREFER_RANGE_LY = 10f;

	@Override
	public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
		return 1f;
	}

	@Override
	public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
		return " - Lighter Raiders";
	}

	@Override
	public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
		CustomBountyData data = new CustomBountyData();
		data.difficulty = difficulty;

		mission.requireSystemInterestingAndNotUnsafeOrCore();
		mission.requireSystemNotHasPulsar();
		MarketAPI cygnus = Global.getSector().getEconomy().getMarket(KolStaticStrings.KOL_CYGNUS);
		if (cygnus != null && cygnus.getPrimaryEntity() != null) {
			Vector2f home = cygnus.getPrimaryEntity().getLocationInHyperspace();
			mission.preferSystemWithinRangeOf(home, PREFER_RANGE_LY);
		}
		if (difficulty >= 4 && mission.rollProbability(PROB_IN_SYSTEM_WITH_BASE)) {
			mission.preferSystemHasBase(Factions.PIRATES);
		}
		StarSystemAPI system = mission.pickSystem();
		data.system = system;
		if (system == null) return null;

		FleetSize size;
		FleetQuality quality;
		String type;
		OfficerQuality oQuality;
		OfficerNum oNum;

		if (difficulty <= 0) {
			size = FleetSize.TINY; quality = FleetQuality.VERY_LOW;
			oQuality = OfficerQuality.LOWER; oNum = OfficerNum.FC_ONLY; type = FleetTypes.PATROL_SMALL;
		} else if (difficulty == 1) {
			size = FleetSize.VERY_SMALL; quality = FleetQuality.VERY_LOW;
			oQuality = OfficerQuality.LOWER; oNum = OfficerNum.FC_ONLY; type = FleetTypes.PATROL_SMALL;
		} else if (difficulty == 2) {
			size = FleetSize.SMALL; quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.LOWER; oNum = OfficerNum.FEWER; type = FleetTypes.PATROL_SMALL;
		} else if (difficulty == 3) {
			size = FleetSize.SMALL; quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT; type = FleetTypes.PATROL_MEDIUM;
		} else if (difficulty <= 5) {
			size = FleetSize.MEDIUM; quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT; type = FleetTypes.PATROL_MEDIUM;
		} else if (difficulty == 6) {
			size = FleetSize.LARGE; quality = FleetQuality.DEFAULT;
			oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.DEFAULT; type = FleetTypes.PATROL_LARGE;
		} else if (difficulty == 7) {
			size = FleetSize.LARGE; quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.MORE; type = FleetTypes.PATROL_LARGE;
		} else if (difficulty == 8) {
			size = FleetSize.VERY_LARGE; quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.DEFAULT; oNum = OfficerNum.MORE; type = FleetTypes.PATROL_LARGE;
		} else if (difficulty == 9) {
			size = FleetSize.HUGE; quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.HIGHER; oNum = OfficerNum.MORE; type = FleetTypes.PATROL_LARGE;
		} else {
			size = FleetSize.MAXIMUM; quality = FleetQuality.HIGHER;
			oQuality = OfficerQuality.HIGHER; oNum = OfficerNum.MORE; type = FleetTypes.PATROL_LARGE;
		}

		beginFleet(mission, data);
		mission.triggerCreateFleet(size, quality, Factions.PIRATES, type, data.system);
		mission.triggerSetFleetOfficers(oNum, oQuality);
		mission.triggerAutoAdjustFleetSize(size, size.next());
		mission.triggerSetStandardAggroPirateFlags();
		mission.triggerPickLocationAtInSystemJumpPoint(data.system);
		mission.triggerSpawnFleetAtPickedLocation(null, null);
		mission.triggerOrderFleetPatrol(data.system, true, Tags.JUMP_POINT, Tags.SALVAGEABLE, Tags.PLANET, Tags.STATION);
		data.fleet = createFleet(mission, data);
		if (data.fleet == null) return null;

		setRepChangesBasedOnDifficulty(data, difficulty);
		data.baseReward = CBStats.getBaseBounty(difficulty, CBStats.PIRATE_MULT, mission);

		return data;
	}
}
