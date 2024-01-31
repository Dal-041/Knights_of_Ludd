package org.selkie.kol.impl.weapons;

import org.lazywizard.lazylib.MathUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;

public class RailgunChargeBeamWeaponScript implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin  {

	private String TagWeapon = "targetinglaser2"; // the id of the weapon to use for the targeting laser beam.
	
	private boolean init = false;
	private boolean fired = true;
	
    protected ShipAPI demDrone;
    
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		fired = true;
	}
	
	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		
		if (engine.isPaused()) {
			return;
		}
		
		if (!weapon.getShip().isAlive()) {
			if (demDrone != null) {
				engine.removeEntity(demDrone);
			}
			return;
		}
		
		if (!init) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec("dem_drone");
			ShipVariantAPI v = Global.getSettings().createEmptyVariant("dem_drone", spec);
			v.addWeapon("WS 000", TagWeapon);
			WeaponGroupSpec g = new WeaponGroupSpec(WeaponGroupType.LINKED);
			g.addSlot("WS 000");
			v.addWeaponGroup(g);
			
			demDrone = Global.getCombatEngine().createFXDrone(v);
			demDrone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
			demDrone.setOwner(weapon.getShip().getOriginalOwner());
			demDrone.getMutableStats().getBeamWeaponRangeBonus().modifyFlat("dem", weapon.getRange());
			demDrone.getMutableStats().getHullDamageTakenMult().modifyMult("dem", 0f); // so it's non-targetable
			demDrone.setDrone(true);
			demDrone.getAIFlags().setFlag(AIFlags.DRONE_MOTHERSHIP, 100000f, weapon.getShip());
			demDrone.getMutableStats().getEnergyWeaponDamageMult().applyMods(weapon.getShip().getMutableStats().getBallisticWeaponDamageMult());
			demDrone.setCollisionClass(CollisionClass.NONE);
			demDrone.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
			Global.getCombatEngine().addEntity(demDrone);
			
			init = true;
		}
		
		demDrone.getLocation().set(weapon.getFirePoint(0));
		demDrone.setFacing(weapon.getCurrAngle());
		demDrone.getVelocity().set(weapon.getShip().getVelocity());
		
		WeaponAPI tLaser = demDrone.getWeaponGroupsCopy().get(0).getWeaponsCopy().get(0);
		tLaser.setFacing(weapon.getCurrAngle());
		tLaser.setKeepBeamTargetWhileChargingDown(true);
		tLaser.setScaleBeamGlowBasedOnDamageEffectiveness(false);
		tLaser.updateBeamFromPoints();
		
		if (weapon.getChargeLevel() > 0.05f && !fired) {
			demDrone.giveCommand(ShipCommand.FIRE, MathUtils.getPointOnCircumference(weapon.getFirePoint(0), 100f, weapon.getCurrAngle()), 0);
		}
		
		if (fired && weapon.getChargeLevel() < 0.05f) {
			fired = false;
		}
		
	}
  }