package org.selkie.kol.campaign.missions.cb;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.WeightedRandomPicker;

/**
 * The mod's own copy of vanilla's custom bounty framework (BaseCustomBounty, 0.98a), for story bounties offered
 * from our own dialogue. Changes from vanilla:
 * - a global reference ($<missionId>_ref) and in-progress flag ($<missionId>_active) instead of a person reference,
 *   so rules can reach it with Call $global.<missionId>_ref and gate on the active flag;
 * - no bar-event giver;
 * - difficulty thresholds looked up by creator class (vanilla passes the mission class, so they never applied).
 * Difficulty history stays per mission id ($<missionId>_aggregateData), separate from vanilla's bounties.
 * Vanilla creators (CBPirate etc.) and KolCh1PirateCreator both work with it: creators take a HubMissionWithBarEvent.
 * Offer flow in rules: set $<missionId>_difficulty = LOW|NORMAL|HIGH in local memory, Call ... showBountyDetail
 * (fires <creatorId>OfferDesc), then AcceptMission or AbortMission.
 */
public class KolCustomBounty extends HubMissionWithBarEvent implements FleetEventListener {

	public static int NUM_TO_TRACK_FOR_DIFFICULTY = 3;
	
	public static enum Stage {
		BOUNTY,
		COMPLETED,
		FAILED,
		FAILED_NO_PENALTY,
	}
	
	public static enum DifficultyChoice {
		LOW,
		NORMAL,
		HIGH,
	}
	
	public static class AggregateBountyData {
		public List<Integer> completedDifficulty = new ArrayList<Integer>();
	}
	
	public String getAggregateDataKey() {
		return "$" + getMissionId() + "_aggregateData";
	}
	public AggregateBountyData getAggregateData() {
		//MemoryAPI memory = getPerson().getMemoryWithoutUpdate();
		MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
		AggregateBountyData data = (AggregateBountyData) memory.get(getAggregateDataKey());
		if (data == null) {
			data = new AggregateBountyData();
			memory.set(getAggregateDataKey(), data);
		}
		return data;
	}
	

	//protected FactionAPI faction;
	protected PersonAPI target;
	protected CustomBountyCreator creator;
	protected CustomBountyCreator creatorLow;
	protected CustomBountyCreator creatorNormal;
	protected CustomBountyCreator creatorHigh;
	protected CustomBountyData data;
	protected CustomBountyData dataLow;
	protected CustomBountyData dataNormal;
	protected CustomBountyData dataHigh;
	
	public List<CustomBountyCreator> getCreators() {
		return new ArrayList<CustomBountyCreator>();
	}
	
	protected int pickDifficulty(DifficultyChoice choice) {
		//if (true) return 6;
		if (difficultyOverride != null) return difficultyOverride;
		
		AggregateBountyData d = getAggregateData();
		
		float total = 0f;
		float count = 0f;
		for (Integer diff : d.completedDifficulty) {
			total += diff;
			count++;
		}
		
		float difficulty = total;
		if (count > 0) {
			difficulty /= Math.max(count, NUM_TO_TRACK_FOR_DIFFICULTY);
			difficulty = (int) difficulty;
		}
		
		int min = CustomBountyCreator.MIN_DIFFICULTY;
		int max = CustomBountyCreator.MAX_DIFFICULTY;
		
		switch (choice) {
		case LOW:
			min = 0;
			max = max - 3;
			difficulty = Math.min(difficulty - 3f, (int)(difficulty / 2f));
			break;
		case NORMAL:
			min = min + 1;
			max = max - 1;
			difficulty = difficulty + 1;
			break;
		case HIGH:
			min = 4;
			difficulty = difficulty + 3;
			break;
		}
		
		int result = (int)Math.round(difficulty);
		//result += genRandom.nextInt(2);
				
		if (result < min) {
			result = min;
		}
		if (result > max) {
			result = max;
		}
		return result;
	}
	
	protected CustomBountyCreator pickCreator(int difficulty, DifficultyChoice choice) {
		//if (true) return new CBRemnantPlus();
		//if (true) return new CBMercUW();
		if (creatorOverride != null) {
			try {
				return (CustomBountyCreator) creatorOverride.newInstance();
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		
		WeightedRandomPicker<CustomBountyCreator> picker = new WeightedRandomPicker<CustomBountyCreator>(genRandom);
		
		float quality = getQuality();
		float maxDiff = CustomBountyCreator.MAX_DIFFICULTY;
		
		for (CustomBountyCreator curr : getCreators()) {
			if (curr.getMinDifficulty() > difficulty) continue;
			if (curr.getMaxDifficulty() < difficulty) continue;
			
			
			if (choice == DifficultyChoice.HIGH) {
				int threshold = CBStats.getThresholdNotHigh(curr.getClass());
				if (difficulty >= threshold) continue;
			}
			if (choice == DifficultyChoice.NORMAL) {
				int threshold = CBStats.getThresholdNotNormal(curr.getClass());
				if (difficulty >= threshold) continue;
			}
			
			float probToSkip = (1.1f - quality) * (float) curr.getMinDifficulty() / maxDiff;
			if (rollProbability(probToSkip)) continue;
			
			picker.add(curr, curr.getFrequency(this, difficulty));
		}
		
		return picker.pick();
	}
	
	protected transient Class creatorOverride;
	protected transient Integer difficultyOverride;
	public void setTestMode(Class c, int difficulty) {
		genRandom = Misc.random;
		//genRandom = new Random(3454364663L);
		difficultyOverride = difficulty;
		creatorOverride = c;
	}
	
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		//genRandom = Misc.random;
		
		
		PersonAPI person = getPerson();
		if (person == null) return false;
		
		String id = getMissionId();
		if (!setGlobalReference("$" + id + "_ref", "$" + id + "_active")) {
			return false;
		}

		setStartingStage(Stage.BOUNTY);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		addNoPenaltyFailureStages(Stage.FAILED_NO_PENALTY);
		//setNoAbandon();
		
		
		connectWithMemoryFlag(Stage.BOUNTY, Stage.COMPLETED, person, "$" + id + "_completed");
		connectWithMemoryFlag(Stage.BOUNTY, Stage.FAILED, person, "$" + id + "_failed");
		
		addTag(Tags.INTEL_BOUNTY);
		
		
		int dLow = pickDifficulty(DifficultyChoice.LOW);
		creatorLow = pickCreator(dLow, DifficultyChoice.LOW);
		//creatorLow = new CBDerelict();
		if (creatorLow != null) {
			dataLow = creatorLow.createBounty(createdAt, this, dLow, Stage.BOUNTY);
		}
		if (dataLow == null || dataLow.fleet == null) return false;
		
		int dNormal = pickDifficulty(DifficultyChoice.NORMAL);
		creatorNormal = pickCreator(dNormal, DifficultyChoice.NORMAL);
		if (creatorNormal != null) {
			dataNormal = creatorNormal.createBounty(createdAt, this, dNormal, Stage.BOUNTY);
		}
		if (dataNormal == null || dataNormal.fleet == null) return false;
		
		int dHigh = pickDifficulty(DifficultyChoice.HIGH);
		creatorHigh = pickCreator(dHigh, DifficultyChoice.HIGH);
		if (creatorHigh != null) {
			dataHigh = creatorHigh.createBounty(createdAt, this, dHigh, Stage.BOUNTY);
		}
		//getPerson().getNameString() getPerson().getMarket();
		if (dataHigh == null || dataHigh.fleet == null) return false;

		
		return true;
	}
	
	protected void updateInteractionDataImpl() {
		String id = getMissionId();
		set("$" + id + "_barEvent", isBarEvent());
		set("$" + id + "_manOrWoman", getPerson().getManOrWoman());
		set("$" + id + "_reward", Misc.getWithDGS(getCreditsReward()));
		set("$bcb_barEvent", isBarEvent());
		set("$bcb_manOrWoman", getPerson().getManOrWoman());
		set("$bcb_reward", Misc.getWithDGS(getCreditsReward()));
		
		if (showData != null && showCreator != null) {
			showCreator.updateInteractionData(this, showData);
			
			set("$" + id + "_difficultyNum", showData.difficulty);
			set("$bcb_difficultyNum", showData.difficulty);
			
			if (showData.system != null) {
				set("$" + id + "_systemName", showData.system.getNameWithLowercaseType());
				set("$" + id + "_dist", getDistanceLY(showData.system.getCenter()));
				set("$bcb_systemName", showData.system.getNameWithLowercaseType());
				set("$bcb_dist", getDistanceLY(showData.system.getCenter()));
			}
			if (showData.market != null) {
				set("$" + id + "_targetMarketName", showData.market.getName());
				set("$bcb_targetMarketName", showData.market.getName());
				set("$" + id + "_targetMarketOnOrAt", showData.market.getOnOrAt());
				set("$bcb_targetMarketOnOrAt", showData.market.getOnOrAt());
			}
			set("$" + id + "_days", "" + (int) showCreator.getBountyDays());
			set("$bcb_days", "" + (int) showCreator.getBountyDays());
			
			if (showData.fleet != null) {
				PersonAPI p = showData.fleet.getCommander();
				set("$" + id + "_targetHeOrShe", p.getHeOrShe());
				set("$" + id + "_targetHisOrHer", p.getHisOrHer());
				set("$" + id + "_targetHimOrHer", p.getHimOrHer());
				set("$" + id + "_targetName", p.getNameString());
				set("$bcb_targetHeOrShe", p.getHeOrShe());
				set("$bcb_targetHisOrHer", p.getHisOrHer());
				set("$bcb_targetHimOrHer", p.getHimOrHer());
				set("$bcb_targetName", p.getNameString());
				
				set("$" + id + "_TargetHeOrShe", Misc.ucFirst(p.getHeOrShe()));
				set("$" + id + "_TargetHisOrHer", Misc.ucFirst(p.getHisOrHer()));
				set("$" + id + "_TargetHimOrHer", Misc.ucFirst(p.getHimOrHer()));
				set("$bcb_TargetHeOrShe", Misc.ucFirst(p.getHeOrShe()));
				set("$bcb_TargetHisOrHer", Misc.ucFirst(p.getHisOrHer()));
				set("$bcb_TargetHimOrHer", Misc.ucFirst(p.getHimOrHer()));
				
				set("$bcb_fleetName", showData.fleet.getName());
			}
		}
	}
	
	
	protected transient CustomBountyCreator showCreator;
	protected transient CustomBountyData showData;
	@Override
	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		
		if ("showBountyDetail".equals(action)) {
			String id = getMissionId();
			MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
			DifficultyChoice difficulty = (DifficultyChoice) Enum.valueOf(DifficultyChoice.class, memory.getString("$" + id + "_difficulty"));
			showCreator = creatorLow;
			showData = dataLow;
			switch (difficulty) {
			case LOW:
				showCreator = creatorLow;
				showData = dataLow;
				break;
			case NORMAL:
				showCreator = creatorNormal;
				showData = dataNormal;
				break;
			case HIGH:
				showCreator = creatorHigh;
				showData = dataHigh;
				break;
			}
			
			setCreditRewardApplyRelMult(showData.baseReward);
			
			updateInteractionData(dialog, memoryMap);
			String trigger = showCreator.getId() + "OfferDesc";
			FireBest.fire(null, dialog, memoryMap, trigger);
			
			if (showData != null && showData.system != null) {
				String icon = showCreator.getIconName();
				if (icon == null) icon = getIcon();
				String text = null;
				Set<String> tags = new LinkedHashSet<String>();
				tags.add(Tags.INTEL_MISSIONS);
				Color color = Misc.getBasePlayerColor();
			
				if (showData.system.getCenter() != null && showData.system.getCenter().getMarket() != null) {
					color = showData.system.getCenter().getMarket().getTextColorForFactionOrPlanet();
				} else if (showData.system.getCenter() instanceof PlanetAPI) {
					color = Misc.setAlpha(((PlanetAPI)showData.system.getCenter()).getSpec().getIconColor(), 255);
					color = Misc.setBrightness(color, 235);
				}
			
				dialog.getVisualPanel().showMapMarker(showData.system.getCenter(), 
							"Target: " + showData.system.getNameWithLowercaseTypeShort(), color, 
							true, icon, text, tags);
			}
			return true;
		} else if ("showBountyAssessment".equals(action) && showCreator != null) {
			showCreator.addIntelAssessment(dialog.getTextPanel(), this, showData);
			return true;
		}
		

		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}
	
	
	@Override
	public void accept(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		String id = getMissionId();
		MemoryAPI memory = memoryMap.get(MemKeys.LOCAL);
		DifficultyChoice difficulty = (DifficultyChoice) Enum.valueOf(DifficultyChoice.class, memory.getString("$" + id + "_difficulty"));
		List<Abortable> abort = new ArrayList<Abortable>();
		switch (difficulty) {
		case LOW:
			creator = creatorLow;
			data = dataLow;
			abort.addAll(dataNormal.abortWhenOtherVersionAccepted);
			abort.addAll(dataHigh.abortWhenOtherVersionAccepted);
			break;
		case NORMAL:
			creator = creatorNormal;
			data = dataNormal;
			abort.addAll(dataLow.abortWhenOtherVersionAccepted);
			abort.addAll(dataHigh.abortWhenOtherVersionAccepted);
			break;
		case HIGH:
			creator = creatorHigh;
			data = dataHigh;
			abort.addAll(dataLow.abortWhenOtherVersionAccepted);
			abort.addAll(dataNormal.abortWhenOtherVersionAccepted);
			break;
		}
		
		for (Abortable curr : abort) {
			curr.abort(this, false);
		}
		
		creatorLow = creatorNormal = creatorHigh = null;
		dataLow = dataNormal = dataHigh = null;
		
		MarketAPI createdAt = getPerson().getMarket();
		if (createdAt == null) createdAt = dialog.getInteractionTarget().getMarket();
		if (creator.getIconName() != null) {
			setIconName(creator.getIconName());
		}
		creator.notifyAccepted(createdAt, this, data);
		
		target = data.fleet.getCommander();
		data.fleet.addEventListener(this);
		makeImportant(data.fleet, "$" + id + "_target", Stage.BOUNTY);

		if (data.fleet.isHidden()) {
			MarketAPI market = Misc.getStationMarket(data.fleet);
			if (market != null) {
				SectorEntityToken station = Misc.getStationEntity(market, data.fleet);
				if (station != null) {
					makeImportant(station, "$" + id + "_target", Stage.BOUNTY);
				}
			}
		}
		
		if (!data.fleet.getFaction().isNeutralFaction()) {
			addTag(data.fleet.getFaction().getId());
		}
		
		if (creator.getBountyDays() > 0) {
			setTimeLimit(Stage.FAILED, creator.getBountyDays(), creator.getSystemWithNoTimeLimit(data));
		}
		//setTimeLimit(Stage.FAILED, 3, creator.getSystemWithNoTimeLimit(data));
		setCreditRewardApplyRelMult(data.baseReward);
		setRepRewardPerson(data.repPerson);
		setRepRewardFaction(data.repFaction);
		
		super.accept(dialog, memoryMap);
	}
	
	
	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		
	}
	
	
	
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		float pad = 3f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.BOUNTY) {
			if (currentStage == Stage.BOUNTY) {
				creator.addTargetLocationAndDescription(info, width, height, this, data);
			}
			creator.addFleetDescription(info, width, height, this, data);
		}
	}
	
	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.BOUNTY) {
			if (data.system != null) {
				creator.addTargetLocationAndDescriptionBulletPoint(info, tc, pad, this, data);
				//info.addPara("Target is in the " + data.system.getNameWithLowercaseTypeShort() + "", tc, pad);
				return true;
			}
		}
		return false;
	}
	

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return super.getMapLocation(map);
	}
	
	@Override
	protected void notifyEnding() {
		super.notifyEnding();
	}
	
	
	@Override
	protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		creator.notifyCompleted(this, data);
		
		AggregateBountyData d = getAggregateData();
		d.completedDifficulty.add(data.difficulty);
		while (d.completedDifficulty.size() > NUM_TO_TRACK_FOR_DIFFICULTY) {
			d.completedDifficulty.remove(0);
		}
	}
	
	@Override
	protected void endFailureImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		creator.notifyFailed(this, data);
		
		AggregateBountyData d = getAggregateData();
		d.completedDifficulty.add(0);
		while (d.completedDifficulty.size() > NUM_TO_TRACK_FOR_DIFFICULTY) {
			d.completedDifficulty.remove(0);
		}
		
	}
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isDone() || result != null) return;
		
		// also credit the player if they're in the same location as the fleet and nearby
		float distToPlayer = Misc.getDistance(fleet, Global.getSector().getPlayerFleet());
		boolean playerInvolved = battle.isPlayerInvolved() || (fleet.isInCurrentLocation() && distToPlayer < 2000f);
		
		if (battle.isInvolved(fleet) && !playerInvolved) {
			boolean cancelBounty = (fleet.isStationMode() && fleet.getFlagship() == null) ||
					(!fleet.isStationMode() && fleet.getFlagship() != null && fleet.getFlagship().getCaptain() != target);
			if (cancelBounty) {
				String id = getMissionId();
				getPerson().getMemoryWithoutUpdate().set("$" + id + "_failed", true);
				return;
			}
//			if (fleet.getFlagship() == null || fleet.getFlagship().getCaptain() != target) {
//				fleet.setCommander(fleet.getFaction().createRandomPerson());
//				//latestResult = new BountyResult(BountyResultType.END_OTHER, 0, null);
//				sendUpdateIfPlayerHasIntel(result, true);
//				return;
//			}
		}
		
		//CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		if (!playerInvolved || !battle.isInvolved(fleet) || battle.onPlayerSide(fleet)) {
			return;
		}
		
		if (fleet.isStationMode()) {
			if (fleet.getFlagship() != null) return;
		} else {
			if (fleet.getFlagship() != null && fleet.getFlagship().getCaptain() == target) return;
		}
		
		String id = getMissionId();
		getPerson().getMemoryWithoutUpdate().set("$" + id + "_completed", true);
		
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		if (isDone() || result != null) return;
		
		if (this.data.fleet == fleet) {
			String id = getMissionId();
			getPerson().getMemoryWithoutUpdate().set("$" + id + "_failed", true);
		}
	}
	
	
	@Override
	public String getBaseName() {
		if (creator != null) return creator.getBaseBountyName(this, data);
		return "Bounty";
	}
	
	protected String getMissionTypeNoun() {
		return "bounty";
	}
	
	public String getPostfixForState() {
//		if (isEnding()) {
//			return super.getPostfixForState();
//		}
//		if (true) {
//			return " - Unusual Remnant Fleet - Failed";
//		}
		String post = super.getPostfixForState();
		post = post.replaceFirst(" - ", "");
		if (!post.isEmpty()) post = " (" + post + ")";
		//if (creator != null) return creator.getBountyNamePostfix(this, data).replaceFirst(" - ", ": ") + post;
		if (creator != null) return creator.getBountyNamePostfix(this, data) + post;
		return super.getPostfixForState();
	}
	
	@Override
	public String getName() {
		return super.getName();
	}
	
	
	
	
}
