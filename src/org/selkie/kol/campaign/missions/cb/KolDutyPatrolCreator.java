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
 * Knights duties board, placeholder duty: clear raiders harassing a Luddic world.
 * Based on KolCh1PirateCreator (vanilla CBPirate sizes and rewards): the target system is near a Luddic world
 * (a Church, Knights or Luddic Majority market). The raiders are pirates, or sometimes a Remnant fleet: a standard
 * spawn of the same size as the pirate option, patrolling the system the same way.
 * Offer text: the rules.csv trigger KolDutyPatrolCreatorOfferDesc.
 */
public class KolDutyPatrolCreator extends BaseCustomBountyCreator {

	public static float PREFER_RANGE_LY = 6f;

	public static float PROB_REMNANT = 0.3f;

	protected boolean remnant = false;

	/** A random Luddic world: Church or Knights held, or with a Luddic majority. */
	protected MarketAPI pickLuddicWorld(HubMissionWithBarEvent mission) {
		com.fs.starfarer.api.util.WeightedRandomPicker<MarketAPI> picker = new com.fs.starfarer.api.util.WeightedRandomPicker<>(mission.getGenRandom());
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.isHidden()) continue;
			String faction = market.getFactionId();
			if (Factions.LUDDIC_CHURCH.equals(faction) || KolStaticStrings.kolFactionID.equals(faction)
					|| market.hasCondition(com.fs.starfarer.api.impl.campaign.ids.Conditions.LUDDIC_MAJORITY)) {
				picker.add(market, market.getSize());
			}
		}
		return picker.pick();
	}

	@Override
	public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
		return 1f;
	}

	@Override
	public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
		return " - Raiders of the Faithful";
	}

	@Override
	public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
		CustomBountyData data = new CustomBountyData();
		data.difficulty = difficulty;

		mission.requireSystemInterestingAndNotUnsafeOrCore();
		mission.requireSystemNotHasPulsar();
		MarketAPI anchor = pickLuddicWorld(mission);
		if (anchor != null && anchor.getPrimaryEntity() != null) {
			mission.preferSystemWithinRangeOf(anchor.getPrimaryEntity().getLocationInHyperspace(), PREFER_RANGE_LY);
		}
		remnant = mission.rollProbability(PROB_REMNANT);
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
		mission.triggerCreateFleet(size, quality, remnant ? Factions.REMNANTS : Factions.PIRATES, type, data.system);
		mission.triggerSetFleetOfficers(oNum, oQuality);
		mission.triggerAutoAdjustFleetSize(size, size.next());
		if (remnant) mission.triggerSetStandardAggroNonPirateFlags(); else mission.triggerSetStandardAggroPirateFlags();
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
