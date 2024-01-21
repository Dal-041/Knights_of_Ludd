package org.selkie.kol.impl.abilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.abilities.EmergencyBurnAbility;
import com.fs.starfarer.api.impl.campaign.ids.Pings;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FleetMemberDamageLevel;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JumpAbilityPullsar extends BaseDurationAbility {

	public static final float CR_COST_MULT = 0.1f;
	public static final float FUEL_USE_MULT = 1f;
	
	protected boolean canUseToJumpToSystem() {
		return true;
	}
	
	protected Boolean primed = null;
	protected NascentGravityWellAPI well = null;
	protected EveryFrameScript ping = null;
	
	@Override
	protected void activateImpl() {
		if (Global.getSector().isPaused()) return;
		
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;

		if (isOnCooldown()) return;
		
		if (fleet.isInHyperspaceTransition()) return;
		
		if (fleet.isInHyperspace() && canUseToJumpToSystem()) {
			ping = Global.getSector().addPing(fleet, Pings.TRANSVERSE_JUMP);
			primed = true;
		} else {
			deactivate();
		}
	}

	@Override
	public void deactivate() {
		if (ping != null) {
			Global.getSector().removeScript(ping);
			ping = null;
		}
		super.deactivate();
	}

	@Override
	protected void applyEffect(float amount, float level) {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;
		
		if (level > 0 && level < 1 && amount > 0) {
			float activateSeconds = getActivationDays() * Global.getSector().getClock().getSecondsPerDay();
			float speed = fleet.getVelocity().length();
			float acc = Math.max(speed, 200f)/activateSeconds + fleet.getAcceleration();
			float ds = acc * amount;
			if (ds > speed) ds = speed;
			Vector2f dv = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(fleet.getVelocity()));
			dv.scale(ds);
			fleet.setVelocity(fleet.getVelocity().x - dv.x, fleet.getVelocity().y - dv.y);
			return;
		}
		
		if (level == 1 && primed != null) {
			if (fleet.isInHyperspace() && canUseToJumpToSystem()) {
				float crCostFleetMult = fleet.getStats().getDynamic().getValue(Stats.DIRECT_JUMP_CR_MULT);
				if (crCostFleetMult > 0) {
					for (FleetMemberAPI member : getNonReadyShips()) {
						if ((float) Math.random() < EmergencyBurnAbility.ACTIVATION_DAMAGE_PROB) {
							Misc.applyDamage(member, null, FleetMemberDamageLevel.LOW, false, null, null,
									true, null, member.getShipName() + " suffers damage from Fracture Jump activation");
						}
					}
					for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
						float crLoss = member.getDeployCost() * CR_COST_MULT * crCostFleetMult;
						member.getRepairTracker().applyCREvent(-crLoss, "Fracture jump");
					}
				}
				
				float cost = computeFuelCost();
				fleet.getCargo().removeFuel(cost);


				WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker(new Random());
				for (StarSystemAPI sys : Global.getSector().getStarSystems()) {
					if (sys.getStar() != null && sys.getStar().getTypeId().equals("zea_star_black_neutron")) picker.add(sys);
				}

				if (picker.isEmpty()) {
					primed = null;
					return;
				}

				StarSystemAPI system = picker.pick();

				if (system == null || system.getStar() == null) {
					primed = null;
					return;
				}

				float radius = system.getStar().getRadius();

				Vector2f destOffset = Misc.getUnitVectorAtDegreeAngle((float) (Math.random() * 360f));
				destOffset.scale(radius*3f + ((float)Math.random()*(radius*3f)));

				Vector2f.add(system.getStar().getLocation(), destOffset, destOffset);
				SectorEntityToken token = system.createToken(destOffset.x, destOffset.y);
				
				JumpDestination dest = new JumpDestination(token, null);
				Global.getSector().doHyperspaceTransition(fleet, fleet, dest);
			}


			
			primed = null;
			well = null;
		}
	}
	
	@Override
	protected String getActivationText() {
		return super.getActivationText();
		//return "Initiating jump";
	}


	@Override
	protected void deactivateImpl() {
		cleanupImpl();
	}
	
	@Override
	protected void cleanupImpl() {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;
	}
	@Override
	public boolean isUsable() {
		if (!super.isUsable()) return false;
		if (getFleet() == null) return false;
		
		CampaignFleetAPI fleet = getFleet();
		
		if (fleet.isInHyperspaceTransition()) return false;
		
		if (TutorialMissionIntel.isTutorialInProgress()) return false;
		
		if (canUseToJumpToSystem() && fleet.isInHyperspace()) {
			return true;
		}
		
		return false;
	}

	
	@Override
	public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;
		
		Color gray = Misc.getGrayColor();
		Color highlight = Misc.getHighlightColor();
		Color fuel = Global.getSettings().getColor("progressBarFuelColor");
		Color bad = Misc.getNegativeHighlightColor();
		
		LabelAPI title = tooltip.addTitle("Fracture Jump to a Black Pullsar");

		float pad = 10f;
		
		tooltip.addPara("Jump across space to the gravity well of a random Black Neutron Star.", pad);
		
		float fuelCost = computeFuelCost();
		float supplyCost = computeSupplyCost();
		
		if (supplyCost > 0) {
			tooltip.addPara("Jumping like this consumes %s fuel and slightly reduces the combat readiness" +
						" of all ships, costing up to %s supplies to recover.", pad,
						highlight,
						Misc.getRoundedValueMaxOneAfterDecimal(fuelCost),
						Misc.getRoundedValueMaxOneAfterDecimal(supplyCost));
		} else {
			tooltip.addPara("Jumping consumes %s fuel.", pad,
					highlight,
					Misc.getRoundedValueMaxOneAfterDecimal(fuelCost));
		}
		
		
		if (TutorialMissionIntel.isTutorialInProgress()) { 
			tooltip.addPara("Can not be used right now.", bad, pad);
		}
		
		if (!fleet.isInHyperspace()) {
			if (fuelCost > fleet.getCargo().getFuel()) {
				tooltip.addPara("Not enough fuel.", bad, pad);
			}
		
			List<FleetMemberAPI> nonReady = getNonReadyShips();
			if (!nonReady.isEmpty()) {
				//tooltip.addPara("Not all ships have enough combat readiness to initiate an emergency burn. Ships that require higher CR:", pad);
				tooltip.addPara("Some ships don't have enough combat readiness to safely initiate a fracture jump " +
								"and may suffer damage if the ability is activated:", pad, 
								Misc.getNegativeHighlightColor(), "may suffer damage");
				int j = 0;
				int max = 7;
				float initPad = 5f;
				for (FleetMemberAPI member : nonReady) {
					if (j >= max) {
						if (nonReady.size() > max + 1) {
							tooltip.addToGrid(0, j++, "... and several other ships", "", bad);
							break;
						}
					}
					String str = "";
					if (!member.isFighterWing()) {
						str += member.getShipName() + ", ";
						str += member.getHullSpec().getHullNameWithDashClass();
					} else {
						str += member.getVariant().getFullDesignationWithHullName();
					}
					
					tooltip.addPara(BaseIntelPlugin.INDENT + str, initPad);
					initPad = 0f;
				}
			}
		}

		if (!fleet.isInHyperspace()) {
			tooltip.addPara("Must be in hyperspace.", bad, pad);
		}
		
		addIncompatibleToTooltip(tooltip, expanded);
	}

	public boolean hasTooltip() {
		return true;
	}
	
	@Override
	public void fleetLeftBattle(BattleAPI battle, boolean engagedInHostilities) {
		if (engagedInHostilities) {
			deactivate();
		}
	}
	
	@Override
	public void fleetOpenedMarket(MarketAPI market) {
		deactivate();
	}
	
	
	protected List<FleetMemberAPI> getNonReadyShips() {
		List<FleetMemberAPI> result = new ArrayList<FleetMemberAPI>();
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return result;
		
		//float crCostFleetMult = fleet.getStats().getDynamic().getValue(Stats.EMERGENCY_BURN_CR_MULT);
		float crCostFleetMult = 1f;
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			//if (member.isMothballed()) continue;
			float crLoss = member.getDeployCost() * CR_COST_MULT * crCostFleetMult;
			if (Math.round(member.getRepairTracker().getCR() * 100) < Math.round(crLoss * 100)) {
				result.add(member);
			}
		}
		return result;
	}

	protected float computeFuelCost() {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return 0f;
		
		float cost = fleet.getLogistics().getFuelCostPerLightYear() * FUEL_USE_MULT;
		return cost;
	}
	
	protected float computeSupplyCost() {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return 0f;
		
		//float crCostFleetMult = fleet.getStats().getDynamic().getValue(Stats.EMERGENCY_BURN_CR_MULT);
		float crCostFleetMult = 1f;
		
		float cost = 0f;
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			cost += member.getDeploymentPointsCost() * CR_COST_MULT * crCostFleetMult;
		}
		return cost;
	}
	
	
	
	protected boolean showAlarm() {
		if (getFleet() != null && !getFleet().isInHyperspace()) return false;
		return !getNonReadyShips().isEmpty() && !isOnCooldown() && !isActiveOrInProgress() && isUsable();
	}
	
//	@Override
//	public boolean isUsable() {
//		return super.isUsable() && 
//					getFleet() != null && 
//					//getNonReadyShips().isEmpty() &&
//					(getFleet().isAIMode() || computeFuelCost() <= getFleet().getCargo().getFuel());
//	}
	
	@Override
	public float getCooldownFraction() {
		if (showAlarm()) {
			return 0f;
		}
		return super.getCooldownFraction();
	}
	@Override
	public boolean showCooldownIndicator() {
		return super.showCooldownIndicator();
	}
	@Override
	public boolean isOnCooldown() {
		return super.getCooldownFraction() < 1f;
	}

	@Override
	public Color getCooldownColor() {
		if (showAlarm()) {
			Color color = Misc.getNegativeHighlightColor();
			return Misc.scaleAlpha(color, Global.getSector().getCampaignUI().getSharedFader().getBrightness() * 0.5f);
		}
		return super.getCooldownColor();
	}

	@Override
	public boolean isCooldownRenderingAdditive() {
		if (showAlarm()) {
			return true;
		}
		return false;
	}
}





