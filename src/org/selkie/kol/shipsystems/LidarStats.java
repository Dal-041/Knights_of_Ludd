package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LidarStats extends BaseShipSystemScript {

	public static String LIDAR_WINDUP = "lidar_windup";
	
	public static Color WEAPON_GLOW = new Color(255,50,50,155);
	
	public static float RANGE_BONUS = 100f;
	public static float PASSIVE_RANGE_BONUS = 0f;
	public static float ROF_BONUS = 2f;
	public static float RECOIL_BONUS = 85f;
	public static float PROJECTILE_SPEED_BONUS = 50f;
	
	public static float SPEED_PENALTY = -50f;
	public static float WEAPON_TURN_PENALTY = -50f;
	
	public static String SYSID = "unset";
	
	
	public static class LidarDishData {
		public float turnDir;
		public float turnRate;
		public float angle;
		public float phase;
		public float count;
		public WeaponAPI w;
	}
	
	protected List<LidarDishData> dishData = new ArrayList<LidarStats.LidarDishData>();
	protected boolean needsUnapply = false;
	protected boolean playedWindup = false;
	
	protected boolean inited = false;
	public void init(ShipAPI ship) {
		if (inited) return;
		inited = true;
		
		needsUnapply = true;
		
		int turnDir = 1;
		float index = 0f;
		float count = 0f;
		for (WeaponAPI w : ship.getAllWeapons()) {
			if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
				count++;
			}
		}
		List<WeaponAPI> lidar = new ArrayList<WeaponAPI>();
		for (WeaponAPI w : ship.getAllWeapons()) {
			if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
				lidar.add(w);
			}
		}
		Collections.sort(lidar, new Comparator<WeaponAPI>() {
			public int compare(WeaponAPI o1, WeaponAPI o2) {
				return (int) Math.signum(o1.getSlot().getLocation().x - o2.getSlot().getLocation().x);
			}
		});
		for (WeaponAPI w : lidar) {
			if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
				w.setSuspendAutomaticTurning(true);
				LidarDishData data = new LidarDishData();
				data.turnDir = Math.signum(turnDir);
				data.turnRate = 0.5f;
				data.turnRate = 0.1f;
				data.w = w;
				data.angle = 0f;
				data.phase = index / count;
				data.count = count;
				dishData.add(data);
				turnDir = -turnDir;
				index++;
			}
		}
		SYSID = ship.getSystem().getId();
	}
	
	public void rotateLidarDishes(boolean active, float effectLevel) {
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		
		float turnRateMult = 1f;
		if (active) {
			turnRateMult = 20f;
		}
		//turnRateMult = 0.1f;
		//boolean first = true;
		for (LidarDishData data : dishData) {
			float arc = data.w.getArc();
			float useTurnDir = data.turnDir;
			if (active) {
				useTurnDir = Misc.getClosestTurnDirection(data.angle, 0f);
			}
			float delta = useTurnDir * amount * data.turnRate * turnRateMult * arc;
			if (active && effectLevel > 0f && Math.abs(data.angle) < Math.abs(delta * 1.5f)) {
				data.angle = 0f;
			} else {
				data.angle += delta;
				data.phase += 1f * amount;
				if (arc < 360f) {
					if (data.angle > arc/2f && data.turnDir > 0f) {
						data.angle = arc/2f;
						data.turnDir = -1f;
					}
					if (data.angle < -arc/2f && data.turnDir < 0f) {
						data.angle = -arc/2f;
						data.turnDir = 1f;
					}
				} else {
					data.angle = data.angle % 360f;
				}
			}
			

			float facing = data.angle + data.w.getArcFacing() + data.w.getShip().getFacing();
			data.w.setFacing(facing);
			data.w.updateBeamFromPoints();
//			if (first) {
//				System.out.println("Facing: " + facing);
//				first = false;
//			}
		}
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null || ship.isHulk()) {
			if (needsUnapply) {
				unmodify(id, stats);
				for (WeaponAPI w : ship.getAllWeapons()) {
					if (!w.isDecorative() && w.getSlot().isTurret() &&
							(w.getType() == WeaponType.BALLISTIC || w.getType() == WeaponType.ENERGY) && w.getSize() == WeaponSize.LARGE) {
						w.setGlowAmount(0, null);
					}
				}
				needsUnapply = false;
			}
			return;
		}
		
		init(ship);
		
		//lidarFacingOffset += am
		
		boolean active = state == State.IN || state == State.ACTIVE || state == State.OUT;
		
		rotateLidarDishes(active, effectLevel);
		
		if (active) {
			for (WeaponAPI w : ship.getAllWeapons()) {
				if (!w.getSpec().hasTag(Tags.LIDAR) && (w.getSize() != WeaponSize.LARGE || w.getType() == WeaponType.MISSILE)) {
					w.setForceNoFireOneFrame(true); //w.disable();
				}
			}
			modify(id, stats, state == State.IN ? 1 : effectLevel);
			needsUnapply = true;
		} else {
			if (needsUnapply) {
				unmodify(id, stats);
				for (WeaponAPI w : ship.getAllWeapons()) {
					if (w.getSlot().isSystemSlot()) continue;
					if (!w.isDecorative() && w.getSlot().isTurret() &&
							(w.getType() == WeaponType.BALLISTIC || w.getType() == WeaponType.ENERGY) && w.getSize() == WeaponSize.LARGE) {
						w.setGlowAmount(0, null);
					}
				}
				needsUnapply = false;
			}

		}

		if(state == State.IN){
			Color glowColor = WEAPON_GLOW;

			float lidarRange = 500;
			for (WeaponAPI w : ship.getAllWeapons()) {
				if (!w.isDecorative() && w.getSlot().isTurret() &&
						(w.getType() == WeaponType.BALLISTIC || w.getType() == WeaponType.ENERGY) && w.getSize() == WeaponSize.LARGE) {
					lidarRange = Math.max(lidarRange, w.getRange());
					w.setGlowAmount(effectLevel, glowColor);
				}
			}
			lidarRange += 100f;
			stats.getBeamWeaponRangeBonus().modifyFlat("lidararray", lidarRange); //TODO: 110 is itu fudging, actually calc this proper
		} else{
			stats.getBeamWeaponRangeBonus().unmodify("lidararray");
		}
		
		if (!active) return;
		

		for (WeaponAPI w : ship.getAllWeapons()) {
			if (w.getSlot().isSystemSlot()) continue;
			if (state == State.IN) {
				if (!(w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR))) {
					w.setForceNoFireOneFrame(true);
				}
			} else {
				if (!(!w.isDecorative() && w.getSlot().isTurret() && 
						(w.getType() == WeaponType.BALLISTIC || w.getType() == WeaponType.ENERGY))) {
					w.setForceNoFireOneFrame(true);
				}
			}
		}

		
		// always wait a quarter of a second before starting to fire the targeting lasers
		// this is the worst-case turn time required for the dishes to face front
		// doing this to keep the timing of the lidar ping sounds consistent relative
		// to when the windup sound plays
		float fireThreshold = 0.25f / 3.25f;
		fireThreshold += 0.02f; // making sure there's only 4 lidar pings; lines up with the timing of the lidardish weapon
		//fireThreshold = 0f;
		for (LidarDishData data : dishData) {
			boolean skip = data.phase % 1f > 1f / data.count;
			//skip = data.phase % 1f > 0.67f;
			skip = false;
			if (skip) continue;
			if (data.w.isDecorative() && data.w.getSpec().hasTag(Tags.LIDAR)) {
				if (state == State.IN && Math.abs(data.angle) < 5f && effectLevel >= fireThreshold) {
					data.w.setForceFireOneFrame(true);
				}
			}
		}
		
		if (((state == State.IN && effectLevel > 0.67f) || state == State.ACTIVE) && !playedWindup) {
			Global.getSoundPlayer().playSound(LIDAR_WINDUP, 1f, 1f, ship.getLocation(), ship.getVelocity());
			playedWindup = true;
		}
	}
	
	
	protected void modify(String id, MutableShipStatsAPI stats, float effectLevel) {
		float mult = 1f + ROF_BONUS * effectLevel;
		//float mult = 1f + ROF_BONUS;
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS*effectLevel);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS*effectLevel);
		stats.getBallisticRoFMult().modifyMult(id, mult);
		stats.getEnergyRoFMult().modifyMult(id, mult);
		//stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));
		stats.getMaxRecoilMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
		stats.getRecoilPerShotMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
		stats.getRecoilDecayMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
		
		stats.getBallisticProjectileSpeedMult().modifyPercent(id, PROJECTILE_SPEED_BONUS);
		stats.getEnergyProjectileSpeedMult().modifyPercent(id, PROJECTILE_SPEED_BONUS);
		stats.getMaxSpeed().modifyPercent(id, SPEED_PENALTY);
		stats.getWeaponTurnRateBonus().modifyPercent(id, WEAPON_TURN_PENALTY);
	}
	protected void unmodify(String id, MutableShipStatsAPI stats) {
		//stats.getBallisticWeaponRangeBonus().modifyPercent(id, PASSIVE_RANGE_BONUS);
		//stats.getEnergyWeaponRangeBonus().modifyPercent(id, PASSIVE_RANGE_BONUS);
		stats.getBallisticWeaponRangeBonus().unmodifyPercent(id);
		stats.getEnergyWeaponRangeBonus().unmodifyPercent(id);
		
		stats.getBallisticRoFMult().unmodifyMult(id);
		stats.getEnergyRoFMult().unmodifyMult(id);
		stats.getMaxRecoilMult().unmodifyMult(id);
		stats.getRecoilPerShotMult().unmodifyMult(id);
		stats.getRecoilDecayMult().unmodifyMult(id);
		
		stats.getBallisticProjectileSpeedMult().unmodifyPercent(id);
		stats.getEnergyProjectileSpeedMult().unmodifyPercent(id);
		
		stats.getMaxSpeed().unmodifyPercent(id);
		stats.getWeaponTurnRateBonus().unmodifyPercent(id);
		
		playedWindup = false;
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		// never called due to runScriptWhileIdle:true in the .system file
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		/*if (state == State.IDLE || state == State.COOLDOWN) {
			if (index == 3) {
				return new StatusData("weapon range +" + (int) PASSIVE_RANGE_BONUS + "%", false);
			}	
		}*/
		if (effectLevel <= 0f) return null;
		
		//float mult = 1f + ROF_BONUS;
		float mult = 1f + ROF_BONUS;
		float bonusPercent = (int) ((mult - 1f) * 100f);
		if (index == 5) {
			return new StatusData("ship speed " + (int) SPEED_PENALTY + "%", true);
		}
		if (index == 4) {
			return new StatusData("weapon turn rate " + (int) WEAPON_TURN_PENALTY + "%", true);
		}
		if (index == 3) {
			return new StatusData("weapon range +" + (int) RANGE_BONUS + "%", false);
		}
		if (index == 2) {
			return new StatusData("rate of fire +" + (int) bonusPercent + "%", false);
		}
//		if (index == 1) {
//			return new StatusData("ballistic flux use -" + (int) FLUX_REDUCTION + "%", false);
//		}
		if (index == 1) {
			return new StatusData("weapon recoil -" + (int) RECOIL_BONUS + "%", false);
		}
		if (index == 0 && PROJECTILE_SPEED_BONUS > 0) {
			return new StatusData("projectile speed +" + (int) PROJECTILE_SPEED_BONUS + "%", false);
		}
		return null;
	}
	
	public String getDisplayNameOverride(State state, float effectLevel) {
		if (state == State.IDLE || state == State.COOLDOWN) {
			return "lidar array idle";
		}
		return null;
	}
	
}
