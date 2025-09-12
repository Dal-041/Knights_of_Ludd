package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.DroneStrikeStatsAIInfoProvider;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DroneEMPStrikeStats extends BaseShipSystemScript implements DroneStrikeStatsAIInfoProvider {
// Itsa terminator but its blu
	public static class DroneMissileScript extends BaseCombatLayeredRenderingPlugin {
		protected ShipAPI drone;
		protected MissileAPI missile;
		protected boolean done;
		
		public DroneMissileScript(ShipAPI drone, MissileAPI missile) {
			super();
			this.drone = drone;
			this.missile = missile;
			missile.setNoFlameoutOnFizzling(true);
			//missile.setFlightTime(missile.getMaxFlightTime() - 1f);
		}

		@Override
		public void advance(float amount) {
			super.advance(amount);
			
			if (done) return;
			
			CombatEngineAPI engine = Global.getCombatEngine();
			
			missile.setEccmChanceOverride(1f);
			missile.setOwner(drone.getOriginalOwner());
			
			drone.getLocation().set(missile.getLocation());
			drone.getVelocity().set(missile.getVelocity());
			drone.setCollisionClass(CollisionClass.FIGHTER);
			drone.setFacing(missile.getFacing());
			drone.getEngineController().fadeToOtherColor(this, new Color(0,0,0,0), new Color(0,0,0,0), 1f, 1f);

			
			float dist = Misc.getDistance(missile.getLocation(), missile.getStart());
			float jitterFraction = dist / missile.getMaxRange();
			jitterFraction = Math.max(jitterFraction, missile.getFlightTime() / missile.getMaxFlightTime());
			
			missile.setSpriteAlphaOverride(0f);
			float jitterMax = 1f + 10f * jitterFraction;
			drone.setJitter(this, new Color(50,100,255, (int)(25 + 50 * jitterFraction)), 1f, 10, 1f, jitterMax);
			
			
//			if (true && !done && missile.getFlightTime() > 1f) {
//				Vector2f damageFrom = new Vector2f(drone.getLocation());
//				damageFrom = Misc.getPointWithinRadius(damageFrom, 20);
//				engine.applyDamage(drone, damageFrom, 1000000f, DamageType.ENERGY, 0, true, false, drone, false);
//			}
			
			boolean droneDestroyed = drone.isHulk() || drone.getHitpoints() <= 0;
			if (missile.isFizzling() || (missile.getHitpoints() <= 0 && !missile.didDamage()) || droneDestroyed) {
				drone.getVelocity().set(0, 0);
				missile.getVelocity().set(0, 0);
				
				if (!droneDestroyed) {
					Vector2f damageFrom = new Vector2f(drone.getLocation());
					damageFrom = Misc.getPointWithinRadius(damageFrom, 20);
					engine.applyDamage(drone, damageFrom, 1000000f, DamageType.ENERGY, 0, true, false, drone, false);
				}
				missile.interruptContrail();
				engine.removeEntity(drone);
				engine.removeEntity(missile);
				
				missile.explode();
				
				done = true;
				return;
			}
			if (missile.didDamage()) {
				drone.getVelocity().set(0, 0);
				missile.getVelocity().set(0, 0);
				
				Vector2f damageFrom = new Vector2f(drone.getLocation());
				damageFrom = Misc.getPointWithinRadius(damageFrom, 20);
				engine.applyDamage(drone, damageFrom, 1000000f, DamageType.ENERGY, 0, true, false, drone, false);
				missile.interruptContrail();
				engine.removeEntity(drone);
				engine.removeEntity(missile);
				done = true;
				return;
			}
			
		}

		@Override
		public boolean isExpired() {
			return done;
		}
		
		
	}
	
	
	
	protected String getWeaponId() {
		return "kol_terminator_emp_missile";
	}
	protected int getNumToFire() {
		return 1;
	}
	
	protected WeaponAPI weapon;
	protected boolean fired = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			//player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}
		
		if (weapon == null) {
			weapon = Global.getCombatEngine().createFakeWeapon(ship, getWeaponId());
		}
		
		for (ShipAPI drone : getDrones(ship)) {
			drone.setExplosionScale(0.67f);
			drone.setExplosionVelocityOverride(new Vector2f());
			drone.setExplosionFlashColorOverride(new Color(50, 100, 255, 255));
		}
		
		if (effectLevel > 0 && !fired) {
			if (!getDrones(ship).isEmpty()) {
				ShipAPI target = findTarget(ship);
				convertDrones(ship, target);
			}
		} else if (state == State.IDLE){
			fired = false;
		}
	}
	
	public void convertDrones(ShipAPI ship, final ShipAPI target) {
		CombatEngineAPI engine = Global.getCombatEngine();
		fired = true;
		forceNextTarget = null;
		int num = 0;
		
		List<ShipAPI> drones = getDrones(ship);
		if (target != null) {
			Collections.sort(drones, new Comparator<ShipAPI>() {
				public int compare(ShipAPI o1, ShipAPI o2) {
					float d1 = Misc.getDistance(o1.getLocation(), target.getLocation());
					float d2 = Misc.getDistance(o2.getLocation(), target.getLocation());
					return (int)Math.signum(d1 - d2);
				}
			});
		} else {
			Collections.shuffle(drones);
		}
		
		for (ShipAPI drone : drones) {
			if (num < getNumToFire()) {
				MissileAPI missile = (MissileAPI) engine.spawnProjectile(
						ship, weapon, getWeaponId(), 
						new Vector2f(drone.getLocation()), drone.getFacing(), new Vector2f(drone.getVelocity()));
				if (target != null && missile.getAI() instanceof GuidedMissileAI) {
					GuidedMissileAI ai = (GuidedMissileAI) missile.getAI();
					ai.setTarget(target);
				}
				//missile.setHitpoints(missile.getHitpoints() * drone.getHullLevel());
				missile.setEmpResistance(10000);
				
				float base = missile.getMaxRange();
				float max = getMaxRange(ship);
				missile.setMaxRange(max);
				missile.setMaxFlightTime(missile.getMaxFlightTime() * max/base);
				
				drone.getWing().removeMember(drone);
				drone.setWing(null);
				drone.setExplosionFlashColorOverride(new Color(255, 100, 50, 255));
				engine.addLayeredRenderingPlugin(new DroneMissileScript(drone, missile));
				
//				engine.removeEntity(drone);
//				drone.getVelocity().set(0, 0);
//				drone.setHulk(true);
//				drone.setHitpoints(-1f);
				
				//float thickness = 16f;
//				EmpArcParams params = new EmpArcParams();
//				params.segmentLengthMult = 4f;
//				//params.glowSizeMult = 0.5f;
//				params.brightSpotFadeFraction = 0.33f;
//				params.brightSpotFullFraction = 1f;
////				params.movementDurMax = 0.2f;
//				params.flickerRateMult = 0.7f;
				
				
				float thickness = 26f;
				float coreWidthMult = 0.67f;
				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getLocation(), ship,
						missile.getLocation(), missile, thickness, new Color(100,100,255,255), Color.white, null);
				arc.setCoreWidthOverride(thickness * coreWidthMult);
				arc.setSingleFlickerMode();
			} else {
				if (drone.getShipAI() != null) {
					drone.getShipAI().cancelCurrentManeuver();
				}
			}
			num++;
		}
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		// never called
	}
	
	protected ShipAPI forceNextTarget = null;
	protected ShipAPI findTarget(ShipAPI ship) {
		if (getDrones(ship).isEmpty()) {
			return null;
		}
		
		if (forceNextTarget != null && forceNextTarget.isAlive()) {
			return forceNextTarget;
		}
		
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		
		// If not the player:
		// The AI sets forceNextTarget, so if we're here, that target got destroyed in the last frame
		// or it's using a different AI
		// so, find *something* as a failsafe
		
		if (!player) {
			Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
			if (test instanceof ShipAPI) {
				target = (ShipAPI) test;
				float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
				float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
				if (dist > range + radSum) target = null;
			}
			if (target == null) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FRIGATE, range, true);
			}
			return target;
		}
		
		// Player ship
		
		if (target != null) return target; // was set with R, so, respect that
		
		// otherwise, find the nearest thing to the mouse cursor, regardless of if it's in range
		
		target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FIGHTER, Float.MAX_VALUE, true);
		if (target != null && target.isFighter()) {
			ShipAPI nearbyShip = Misc.findClosestShipEnemyOf(ship, target.getLocation(), HullSize.FRIGATE, 100, false);
			if (nearbyShip != null) target = nearbyShip;
		}
		if (target == null) {
			target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FIGHTER, range, true);
		}
		
		return target;
	}

	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}

	public List<ShipAPI> getDrones(ShipAPI ship) {
		List<ShipAPI> result = new ArrayList<ShipAPI>();
		for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
			if (bay.getWing() == null) continue;
			for (ShipAPI drone : bay.getWing().getWingMembers()) {
				result.add(drone);
			}
		}
		return result;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		if (getDrones(ship).isEmpty()) {
			return "NO DRONES";
		}
		
		float range = getMaxRange(ship);
		
		ShipAPI target = findTarget(ship);
		if (target == null) {
			if (ship.getMouseTarget() != null) {
				float dist = Misc.getDistance(ship.getLocation(), ship.getMouseTarget());
				float radSum = ship.getCollisionRadius();
				if (dist + radSum > range) {
					return "OUT OF RANGE";
				}
			}
			return "NO TARGET";
		}
		
		float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
		float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
		if (dist > range + radSum) {
			return "OUT OF RANGE";
		}
		
		return "READY";
	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (ship != null && ship.getSystem() != null && ship.getSystem().getState() != SystemState.IDLE) {
			return true; // preventing out-of-ammo click when launching last drone
		}
		return !getDrones(ship).isEmpty();
//		if (true) return true;
//		ShipAPI target = findTarget(ship);
//		return target != null && target != ship;
	}
	
	public float getMaxRange(ShipAPI ship) {
		if (weapon == null) {
			weapon = Global.getCombatEngine().createFakeWeapon(ship, getWeaponId());
		}
		//return weapon.getRange();
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(weapon.getRange());
	}
	public boolean dronesUsefulAsPD() {
		return true;
	}
	public boolean droneStrikeUsefulVsFighters() {
		return false;
	}
	public int getMaxDrones() {
		return 2;
	}
	public float getMissileSpeed() {
		return weapon.getProjectileSpeed();
	}
	public void setForceNextTarget(ShipAPI forceNextTarget) {
		this.forceNextTarget = forceNextTarget;
	}
	public ShipAPI getForceNextTarget() {
		return forceNextTarget;
	}
}








