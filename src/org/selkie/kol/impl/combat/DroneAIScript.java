package org.selkie.kol.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.combat.SparkleControlScript;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.selkie.kol.impl.combat.SparkleControlScript.getDroneSharedData;

public class DroneAIScript implements MissileAIPlugin {

	private static float SOURCE_REJOIN = 200f;
	private static float SOURCE_REPEL = 50f;
	private static float SOURCE_COHESION = 600f;

	public static float MAX_FLOCK_RANGE = 500f;
	public static float MAX_HARD_AVOID_RANGE = 200f;
	public static float AVOID_RANGE = 50f;
	public static float COHESION_RANGE = 10f;
	public static float ATTRACTOR_LOCK_STOP_FLOCKING_ADD = 300f;
    private static Map distMult = new HashMap();
    static {
    	distMult.put(ShipAPI.HullSize.FIGHTER, 0.125f);
        distMult.put(ShipAPI.HullSize.FRIGATE, 0.17f);
        distMult.put(ShipAPI.HullSize.DESTROYER, 0.25f);
        distMult.put(ShipAPI.HullSize.CRUISER, 0.4f);
        distMult.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.6f);
    }
    
	protected MissileAPI missile;
	private float OFFSET;
	private float check;
	private boolean launch;
	protected IntervalUtil tracker = new IntervalUtil(0.05f, 0.1f);
	protected IntervalUtil updateListTracker = new IntervalUtil(0.05f, 0.1f);
	protected List<MissileAPI> missileList = new ArrayList<MissileAPI>();
	protected List<CombatEntityAPI> hardAvoidList = new ArrayList<CombatEntityAPI>();
	protected float r;
	protected CombatEntityAPI target;
	protected SparkleControlScript.SharedSparkleAIData data;

	public DroneAIScript(MissileAPI missile) {
		this.missile = missile;
		r = (float) Math.random();
		elapsed = -(float) Math.random() * 0.5f;
		
		data = getDroneSharedData(missile.getSource());

		this.OFFSET = (float)(Math.random() * 3.1415927410125732 * 2.0);

		this.launch = true;

		this.check = 0.0F;
		//updateHardAvoidList();
	}
	
	public void updateHardAvoidList() {
		hardAvoidList.clear();
		
		CollisionGridAPI grid = Global.getCombatEngine().getAiGridShips();
		Iterator<Object> iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof ShipAPI)) continue;
			
			ShipAPI ship = (ShipAPI) o;
			
			if (ship.isFighter()) continue;
			hardAvoidList.add(ship);
		}
		
		grid = Global.getCombatEngine().getAiGridAsteroids();
		iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof CombatEntityAPI)) continue;
			
			CombatEntityAPI asteroid = (CombatEntityAPI) o;
			hardAvoidList.add(asteroid);
		}
	}
	
	public void doFlocking() {
		if (missile.getSource() == null) return;
		
		ShipAPI source = missile.getSource();
		CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI.HullSize hullSize;
        if (source != null) {
        	hullSize = source.getHullSize();
        } else {
        	hullSize = HullSize.CAPITAL_SHIP;
        }

		
		float avoidRange = AVOID_RANGE;
		float cohesionRange = COHESION_RANGE;
		
        float sourceRejoin = source.getCollisionRadius() + (SOURCE_REJOIN * (float)distMult.get(hullSize));
        float sourceRepel = source.getCollisionRadius() + (SOURCE_REPEL * (float)distMult.get(hullSize));
        float sourceCohesion = source.getCollisionRadius() + (SOURCE_COHESION * (float)distMult.get(hullSize));
		
		float sin = (float) Math.sin(data.elapsed * 1f);
		float mult = 1f + sin * 0.25f;
		avoidRange *= mult;
		
		Vector2f total = new Vector2f();
		Vector2f attractor = getAttractorLoc();
		
		if (attractor != null) {
			float dist = Misc.getDistance(missile.getLocation(), attractor);
			Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), attractor));
			float f = dist / 200f;
			if (f > 1f) f = 1f;
			dir.scale(f * 3f);
			Vector2f.add(total, dir, total);
			
			avoidRange *= 3f;
		}
		
		boolean hardAvoiding = false;
		for (CombatEntityAPI other : hardAvoidList) {
			float dist = Misc.getDistance(missile.getLocation(), other.getLocation());
			float hardAvoidRange = other.getCollisionRadius() + avoidRange + 50f;
			if (dist < hardAvoidRange) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(other.getLocation(), missile.getLocation()));
				float f = 1f - dist / (hardAvoidRange);
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
				hardAvoiding = f > 0.5f;
			}
		}
		
		
		//for (MissileAPI otherMissile : missileList) {
		for (MissileAPI otherMissile : data.drones) {
			if (otherMissile == missile) continue;
			
			float dist = Misc.getDistance(missile.getLocation(), otherMissile.getLocation());
			
			
			float w = otherMissile.getMaxHitpoints();
			w = 1f;
			
			float currCohesionRange = cohesionRange;
			
			if (dist < avoidRange && otherMissile != missile && !hardAvoiding) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(otherMissile.getLocation(), missile.getLocation()));
				float f = 1f - dist / avoidRange;
				dir.scale(f * w);
				Vector2f.add(total, dir, total);
			}
			
			if (dist < currCohesionRange) {
				Vector2f dir = new Vector2f(otherMissile.getVelocity());
				Misc.normalise(dir);
				float f = 1f - dist / currCohesionRange;
				dir.scale(f * w);
				Vector2f.add(total, dir, total);
			}
			
//			if (dist < cohesionRange && dist > avoidRange) {
//				//Vector2f dir = Utils.getUnitVectorAtDegreeAngle(Utils.getAngleInDegrees(missile.getLocation(), mote.getLocation()));
//				Vector2f dir = Utils.getUnitVectorAtDegreeAngle(Utils.getAngleInDegrees(mote.getLocation(), missile.getLocation()));
//				float f = dist / cohesionRange - 1f;
//				dir.scale(f * 0.5f);
//				Vector2f.add(total, dir, total);
//			}
		}
		
		if (missile.getSource() != null) {
			float dist = Misc.getDistance(missile.getLocation(), source.getLocation());
			if (dist > sourceRejoin) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()));
				float f = dist / (sourceRejoin  + (400f * (float)distMult.get(hullSize))) - 1f;
				dir.scale(f * 0.5f);
				
				Vector2f.add(total, dir, total);
			}
			
			if (dist < sourceRepel) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), missile.getLocation()));
				float f = 1f - dist / sourceRepel;
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
			}
			
			if (dist < sourceCohesion && source.getVelocity().length() > 20f) {
				Vector2f dir = new Vector2f(source.getVelocity());
				Misc.normalise(dir);
				float f = 1f - dist / sourceCohesion;
				dir.scale(f * 1f);
				Vector2f.add(total, dir, total);
			}
			
			// if not strongly going anywhere, circle the source ship; only kicks in for lone motes
			if (total.length() <= 0.05f) {
				float offset = r > 0.5f ? 90f : -90f;
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(
						Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()) + offset);
				float f = 1f;
				dir.scale(f * 1f);
				Vector2f.add(total, dir, total);
			}
		}
		
		if (total.length() > 0) {
			float dir = Misc.getAngleInDegrees(total);
			engine.headInDirectionWithoutTurning(missile, dir, 10000);

			float correctAngle = dir;
			float aimAngle = 1.0F;

			correctAngle = (float)((double)correctAngle + (double)(aimAngle * 15.0F * this.check) * Math.cos((double)(this.OFFSET + this.missile.getElapsed() * 3.1415927F)));
			aimAngle = MathUtils.getShortestRotation(this.missile.getFacing(), correctAngle);

			if (aimAngle < 0.0F) {
				this.missile.giveCommand(ShipCommand.TURN_RIGHT);
			} else {
				this.missile.giveCommand(ShipCommand.TURN_LEFT);
			}

			if (Math.abs(aimAngle) < Math.abs(this.missile.getAngularVelocity()) * 0.1F) {
				this.missile.setAngularVelocity(aimAngle / 0.1F);
			}
			missile.getEngineController().forceShowAccelerating();
		}
	}
	
	//public void accumulate(FlockingData data, Vector2f )


	protected IntervalUtil flutterCheck = new IntervalUtil(2f, 4f);
	protected FaderUtil currFlutter = null;
	protected float flutterRemaining = 0f;
	
	protected float elapsed = 0f;
	public void advance(float amount) {
		if (missile.isFizzling()) return;
		if (missile.getSource() ==  null) return;
		
		elapsed += amount;

		updateListTracker.advance(amount);

		if (elapsed >= 0.5f) {
			
			boolean wantToFlock = !isTargetValid();
			if (data.targetLock != null) {
				float dist = Misc.getDistance(missile.getLocation(), data.targetLock.getLocation());
				if (dist > data.targetLock.getCollisionRadius() + ATTRACTOR_LOCK_STOP_FLOCKING_ADD) {
					wantToFlock = true;
				}
			}
			
			if (wantToFlock) {
				doFlocking();
			} else {
				CombatEngineAPI engine = Global.getCombatEngine();
				Vector2f targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50);
				engine.headInDirectionWithoutTurning(missile, Misc.getAngleInDegrees(missile.getLocation(), targetLoc), 10000);

				float correctAngle = VectorUtils.getAngle(this.missile.getLocation(), targetLoc);
				float aimAngle = 0.3F;

				correctAngle = (float)((double)correctAngle + (double)(aimAngle * 15.0F * this.check) * Math.cos((double)(this.OFFSET + this.missile.getElapsed() * 3.1415927F)));
				aimAngle = MathUtils.getShortestRotation(this.missile.getFacing(), correctAngle);

				if (aimAngle < 0.0F) {
					this.missile.giveCommand(ShipCommand.TURN_RIGHT);
				} else {
					this.missile.giveCommand(ShipCommand.TURN_LEFT);
				}

				if (Math.abs(aimAngle) < Math.abs(this.missile.getAngularVelocity()) * 0.1F) {
					this.missile.setAngularVelocity(aimAngle / 0.1F);
				}
				missile.getEngineController().forceShowAccelerating();
			}
		}
		
		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			if (elapsed >= 0.5f) {
				acquireNewTargetIfNeeded();
			}
			//causeEnemyMissilesToTargetThis();
		}
	}
	@SuppressWarnings("unchecked")
	protected boolean isTargetValid() {
		if (target == null || (target instanceof ShipAPI && ((ShipAPI)target).isPhased())) {
			return false;
		}
		CombatEngineAPI engine = Global.getCombatEngine();
		
		if (target != null && target instanceof ShipAPI && ((ShipAPI)target).isHulk()) return false;
		
		List list = null;
		if (target instanceof ShipAPI) {
			list = engine.getShips();
		} else {
			list = engine.getMissiles();
		}
		return target != null && list.contains(target) && target.getOwner() != missile.getOwner();
	}
	protected void acquireNewTargetIfNeeded() {
		if (data.targetLock != null) {
			target = data.targetLock;
			return;
		}
		
		CombatEngineAPI engine = Global.getCombatEngine();
		
		// want to: target nearest missile that is not targeted by another two motes already
		int owner = missile.getOwner();
		
		int maxMotesPerMissile = 2;
		float maxDistFromSourceShip = SparkleControlScript.MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD;
		float maxDistFromAttractor = SparkleControlScript.MAX_DIST_FROM_ATTRACTOR_TO_ENGAGE_AS_PD;
		
		float minDist = Float.MAX_VALUE;
		CombatEntityAPI closest = null;
		for (MissileAPI other : engine.getMissiles()) {
			if (other.getOwner() == owner) continue;
			if (other.getOwner() == 100) continue;
			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());
			
			if (distToTarget > minDist) continue;
			if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue;
			
			float distFromAttractor = Float.MAX_VALUE;
			if (data.droneTarget != null) {
				distFromAttractor = Misc.getDistance(other.getLocation(), data.droneTarget);
			}
			float distFromSource = Misc.getDistance(other.getLocation(), missile.getSource().getLocation());
			if (distFromSource > maxDistFromSourceShip &&
					distFromAttractor > maxDistFromAttractor) continue;
			
			if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue;
			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}
		
		for (ShipAPI other : engine.getShips()) {
			if (other.getOwner() == owner) continue;
			if (other.getOwner() == 100) continue;
			if (!other.isFighter()) continue;
			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());
			if (distToTarget > minDist) continue;
			if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue;
			
			float distFromAttractor = Float.MAX_VALUE;
			if (data.droneTarget != null) {
				distFromAttractor = Misc.getDistance(other.getLocation(), data.droneTarget);
			}
			float distFromSource = Misc.getDistance(other.getLocation(), missile.getSource().getLocation());
			if (distFromSource > maxDistFromSourceShip &&
					distFromAttractor > maxDistFromAttractor) continue;
			
			if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue;
			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}
		
		target = closest;
	}
	
	protected int getNumMotesTargeting(CombatEntityAPI other) {
		int count = 0;
		for (MissileAPI mote : data.drones) {
			if (mote == missile) continue;
			if (mote.getUnwrappedMissileAI() instanceof DroneAIScript) {
				DroneAIScript ai = (DroneAIScript) mote.getUnwrappedMissileAI();
				if (ai.getTarget() == other) {
					count++;
				}
			}
		}
		return count;
	}
	
	public Vector2f getAttractorLoc() {
		Vector2f attractor = null;
		if (data.droneTarget != null) {
			attractor = data.droneTarget;
			if (data.targetLock != null) {
				attractor = data.targetLock.getLocation();
			}
		}
		return attractor;
	}

	public CombatEntityAPI getTarget() {
		return target;
	}

	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}
	public void render() {
		
	}
}
