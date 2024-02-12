package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelativisticDriftStats extends BaseShipSystemScript {
	private static final Integer TURN_ACC_BUFF = 1000;
	private static final Integer TURN_RATE_BUFF = 500;
	private static final Integer ACCEL_BUFF = 500;
	private static final Integer DECCEL_BUFF = 300;
	private static final Integer SPEED_BUFF = 100;
	private static final Integer TIME_BUFF = 10;
	public static final float INSTANT_BOOST_FLAT = 30f;
	public static final float INSTANT_BOOST_MULT = 2f;

	private static final Color ENGINE_COLOR = new Color(200, 10, 255);
	private static final Color BOOST_COLOR = new Color(175, 175, 255, 200);
	private static final Vector2f ZERO = new Vector2f();

	private final Object ENGINEKEY2 = new Object();
	private final Map<Integer, Float> engState = new HashMap<>();
	private boolean ended = false;
	private float boostScale = 0.75f;
	private float boostVisualDir = 0f;
	private boolean boostForward = false;

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) return;
		float shipRadius = ship.getCollisionRadius();
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		if (Global.getCombatEngine().isPaused()) amount = 0f;

		ship.getEngineController().extendFlame(ENGINEKEY2, 3f * effectLevel, 3f * effectLevel, 9f * effectLevel);

		if (!ended) {
			/* Unweighted direction calculation for visual purposes - 0 degrees is forward */
			Vector2f direction = new Vector2f();
			if (ship.getEngineController().isAccelerating()) {
				direction.y += 1f;
			} else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
				direction.y -= 1f;
			}
			if (ship.getEngineController().isStrafingLeft()) {
				direction.x -= 1f;
			} else if (ship.getEngineController().isStrafingRight()) {
				direction.x += 1f;
			}
			if (direction.length() <= 0f) {
				direction.y = 1f;
			}
			boostVisualDir = MathUtils.clampAngle(VectorUtils.getFacing(direction) - 90f);
		}

		if (state == State.IN) {

			List<ShipEngineControllerAPI.ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
			for (int i = 0; i < engList.size(); i++) {
				ShipEngineControllerAPI.ShipEngineAPI eng = engList.get(i);
				if (eng.isSystemActivated()) {
					float targetLevel = getSystemEngineScale(eng, boostVisualDir) * 4f;
					Float currLevel = engState.get(i);
					if (currLevel == null) {
						currLevel = 0f;
					}
					if (currLevel > targetLevel) {
						currLevel = Math.max(targetLevel, currLevel - (amount * 2f));
					} else {
						currLevel = Math.min(targetLevel, currLevel + (amount * 2f));
					}
					engState.put(i, currLevel);
					ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
				}
			}
		}

		//ship can reorient
		stats.getTurnAcceleration().modifyPercent(id, TURN_ACC_BUFF * effectLevel);
		stats.getMaxTurnRate().modifyPercent(id, TURN_RATE_BUFF * effectLevel);

		//ship can slightly jump forward
		stats.getMaxSpeed().modifyPercent(id, SPEED_BUFF * effectLevel);
		stats.getAcceleration().modifyPercent(id, ACCEL_BUFF);
		stats.getDeceleration().modifyPercent(id, DECCEL_BUFF);

		//time drift
		float timeMult = 1f + TIME_BUFF * effectLevel;
		stats.getTimeMult().modifyMult(id, timeMult);
		if (ship == Global.getCombatEngine().getPlayerShip()) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / timeMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}


		if (state == State.OUT) {
			/* Black magic to counteract the effects of maneuvering penalties/bonuses on the effectiveness of this system */
			float decelMult = Math.max(0.5f, Math.min(2f, stats.getDeceleration().getModifiedValue() / stats.getDeceleration().getBaseValue()));
			float adjFalloffPerSec = 0.25f * (float) Math.pow(decelMult, 0.5);

			if (boostForward) {
				ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
				ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
			} else {
				ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
			}

			if (amount > 0f) {
				ship.getVelocity().scale((float) Math.pow(adjFalloffPerSec, amount));
			}

			List<ShipEngineControllerAPI.ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
			for (int i = 0; i < engList.size(); i++) {
				ShipEngineControllerAPI.ShipEngineAPI eng = engList.get(i);
				if (eng.isSystemActivated()) {
					float targetLevel = getSystemEngineScale(eng, boostVisualDir) * effectLevel;
					if (targetLevel >= (1f - 0.15f/0.9f)) {
						targetLevel = 1f;
					} else {
						targetLevel = targetLevel / (1f - 0.15f/0.9f);
					}
					engState.put(i, targetLevel);
					ship.getEngineController().setFlameLevel(eng.getEngineSlot(), targetLevel);
				}
			}
		} else if (state == State.ACTIVE) {
			ship.getEngineController().getExtendLengthFraction().advance(amount * 2f);
			ship.getEngineController().getExtendWidthFraction().advance(amount * 2f);
			ship.getEngineController().getExtendGlowFraction().advance(amount * 2f);
			List<ShipEngineControllerAPI.ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
			for (int i = 0; i < engList.size(); i++) {
				ShipEngineControllerAPI.ShipEngineAPI eng = engList.get(i);
				if (eng.isSystemActivated()) {
					float targetLevel = getSystemEngineScale(eng, boostVisualDir) * 4;
					Float currLevel = engState.get(i);
					if (currLevel == null) {
						currLevel = 0f;
					}
					if (currLevel > targetLevel) {
						currLevel = Math.max(targetLevel, currLevel - (amount * 2f));
					} else {
						currLevel = Math.min(targetLevel, currLevel + (amount * 2f));
					}
					engState.put(i, currLevel);
					ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
				}
			}
		}

		if (state == State.OUT) {
			if (!ended) {
				Vector2f direction = new Vector2f();
				boostForward = false;
				boostScale = 0.75f;
				if (ship.getEngineController().isAccelerating()) {
					direction.y += 0.55f; //0.75f - 0.2f
					boostScale -= 0.1f;
					boostForward = true;
				} else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
					direction.y -= 0.4f; //0.75f - 0.35f ?
					boostScale -= 0.35f;
				}
				if (ship.getEngineController().isStrafingLeft()) {
					direction.x -= 1f;
					boostScale += 0.3f; //from 0.25f an increase
					boostForward = false;
				} else if (ship.getEngineController().isStrafingRight()) {
					direction.x += 1f;
					boostScale += 0.3f;
					boostForward = false;
				}
				if (direction.length() <= 0f) {
					direction.y = 0.55f; //0.75f - 0.2f ?
					boostScale -= 0.2f;
				}
				Misc.normalise(direction);
				VectorUtils.rotate(direction, ship.getFacing() - 90f, direction);
				direction.scale(((ship.getMaxSpeedWithoutBoost() * INSTANT_BOOST_MULT) + INSTANT_BOOST_FLAT) * boostScale);
				Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());
				ended = true;

				float duration = (float) Math.sqrt(shipRadius) / 25f;
				ship.getEngineController().getExtendLengthFraction().advance(1f);
				ship.getEngineController().getExtendWidthFraction().advance(1f);
				ship.getEngineController().getExtendGlowFraction().advance(1f);
				for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
					float level = 1f;
					if (eng.isSystemActivated()) {
						level = getSystemEngineScale(eng, boostVisualDir);
					}
					if ((eng.isActive() || eng.isSystemActivated()) && (level > 0f)) {
						Color bigBoostColor = new Color(
								Math.round(0.1f * ENGINE_COLOR.getRed()),
								Math.round(0.1f * ENGINE_COLOR.getGreen()),
								Math.round(0.1f * ENGINE_COLOR.getBlue()),
								Math.round(0.3f * ENGINE_COLOR.getAlpha() * level));
						Color boostColor = new Color(BOOST_COLOR.getRed(), BOOST_COLOR.getGreen(), BOOST_COLOR.getBlue(),
								Math.round(BOOST_COLOR.getAlpha() * level));
						Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, bigBoostColor,
								20f * boostScale * eng.getEngineSlot().getWidth(), duration);
						Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, boostColor,
								10f * boostScale * eng.getEngineSlot().getWidth(), 0.15f);
					}
				}
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}
		ended = false;
		boostScale = 0.75f;
		boostVisualDir = 0f;
		boostForward = false;
		engState.clear();

		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);

		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		stats.getTimeMult().unmodify(id);

		if (ship == Global.getCombatEngine().getPlayerShip()) {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased relativity", false);
		}
		return null;
	}


	public float getActiveOverride(ShipAPI ship) { return -1; }
	public float getInOverride(ShipAPI ship) {
		return -1;
	}
	public float getOutOverride(ShipAPI ship) {
		return -1;
	}
	public float getRegenOverride(ShipAPI ship) {
		return -1;
	}

	private static float getSystemEngineScale(ShipEngineControllerAPI.ShipEngineAPI engine, float direction) {
		float engAngle = engine.getEngineSlot().getAngle();
		if (Math.abs(MathUtils.getShortestRotation(engAngle, direction)) > 100f) {
			return 1f;
		} else {
			return 0f;
		}
	}
}

