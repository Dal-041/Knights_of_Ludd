package org.selkie.zea.weapons;

import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.util.Misc;

public class draGUNfire implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

	private String tagWeapon = "targetinglaser3"; // the id of the weapon to use for the targeting beam.
	private String DemWeapon = "dragon_payload"; // the id of the weapon to use for the main beam.
	private float sweepVal = 7f; // how many degrees the targeting beams should start angled outwards as they sweep in. 
	
	private boolean init = false;
	private boolean fired = false;
	
    protected ShipAPI demDrone;
    protected ShipAPI tagDroneL;
    protected ShipAPI tagDroneR;
    
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		
        Vector2f proj_location = projectile.getLocation();
        float facing = weapon.getCurrAngle();
        
		demDrone.giveCommand(ShipCommand.FIRE, MathUtils.getPointOnCircumference(proj_location, 100f, facing), 0);
		
		DamagingExplosionSpec blast = new DamagingExplosionSpec(0.12f,
                75f,
                50f,
                200f, //maxDam
                100f, //minDam
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                4f,
                4f,
                1f,
                100,
                new Color(255,100,80,255),
                new Color(255,70,70,255));
        blast.setDamageType(DamageType.ENERGY);
        blast.setShowGraphic(true);
        blast.setUseDetailedExplosion(true);
        blast.setDetailedExplosionFlashColorCore(new Color(155,155,155,255));
        blast.setDetailedExplosionFlashColorFringe(new Color(255,20,50,255));
        blast.setDetailedExplosionRadius(150f);
        blast.setDetailedExplosionFlashRadius(350f);
        blast.setDetailedExplosionFlashDuration(1.25f);
        engine.spawnDamagingExplosion(blast,projectile.getSource(),proj_location,false);
        
		for (int i = 0; i < 50; i++) {
			float angleOffset = (float) Math.random();
			if (angleOffset > 0.2f) {
				angleOffset *= angleOffset;
			}
			float speedMult = 1f - angleOffset;
			speedMult = 0.5f + speedMult * 0.5f;
			angleOffset *= Math.signum((float) Math.random() - 0.5f);
			angleOffset *= 22.5f;
			float theta = (float) Math.toRadians(facing + angleOffset);
			float r = (float) (Math.random() * Math.random() * 100);
			float x = (float)Math.cos(theta) * r;
			float y = (float)Math.sin(theta) * r;
			Vector2f pLoc = new Vector2f(proj_location.x + x, proj_location.y + y);

			float speed = MathUtils.getRandomNumberInRange(50f, 250f);
			speed *= speedMult;
			
			Vector2f pVel = Misc.getUnitVectorAtDegreeAngle((float) Math.toDegrees(theta));
			pVel.scale(speed);
			
			float pSize = MathUtils.getRandomNumberInRange(50f, 70f);
			float pDur = MathUtils.getRandomNumberInRange(0.7f, 1.1f);
			float endSize = MathUtils.getRandomNumberInRange(1f, 2f);
			Global.getCombatEngine().addNebulaParticle(pLoc, pVel, pSize, endSize, 0.1f, 0.5f, pDur, new Color(255,40,40,155));
		}
		
		fired = true;
		
    	engine.removeEntity(projectile);
        
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
			if (tagDroneL != null) {
				engine.removeEntity(tagDroneL);
			}
			if (tagDroneR != null) {
				engine.removeEntity(tagDroneR);
			}
			return;
		}
		
		if (!init) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec("dem_drone");
			ShipVariantAPI v = Global.getSettings().createEmptyVariant("dem_drone", spec);
			v.addWeapon("WS 000", DemWeapon);
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
			demDrone.getMutableStats().getEnergyWeaponDamageMult().applyMods(weapon.getShip().getMutableStats().getEnergyWeaponDamageMult());
			demDrone.setCollisionClass(CollisionClass.NONE);
			demDrone.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
			Global.getCombatEngine().addEntity(demDrone);
			
			ShipHullSpecAPI specTag = Global.getSettings().getHullSpec("dem_drone");
			ShipVariantAPI vT = Global.getSettings().createEmptyVariant("dem_drone", specTag);
			vT.addWeapon("WS 000", tagWeapon);
			WeaponGroupSpec gT = new WeaponGroupSpec(WeaponGroupType.LINKED);
			gT.addSlot("WS 000");
			vT.addWeaponGroup(gT);
			
			tagDroneL = Global.getCombatEngine().createFXDrone(vT);
			tagDroneL.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
			tagDroneL.setOwner(weapon.getShip().getOriginalOwner());
			tagDroneL.getMutableStats().getBeamWeaponRangeBonus().modifyFlat("dem", weapon.getRange());
			tagDroneL.getMutableStats().getHullDamageTakenMult().modifyMult("dem", 0f); // so it's non-targetable
			tagDroneL.setDrone(true);
			tagDroneL.getAIFlags().setFlag(AIFlags.DRONE_MOTHERSHIP, 100000f, weapon.getShip());
			tagDroneL.getMutableStats().getEnergyWeaponDamageMult().applyMods(weapon.getShip().getMutableStats().getEnergyWeaponDamageMult());
			tagDroneL.setCollisionClass(CollisionClass.NONE);
			tagDroneL.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
			Global.getCombatEngine().addEntity(tagDroneL);
			
			tagDroneR = Global.getCombatEngine().createFXDrone(vT);
			tagDroneR.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
			tagDroneR.setOwner(weapon.getShip().getOriginalOwner());
			tagDroneR.getMutableStats().getBeamWeaponRangeBonus().modifyFlat("dem", weapon.getRange());
			tagDroneR.getMutableStats().getHullDamageTakenMult().modifyMult("dem", 0f); // so it's non-targetable
			tagDroneR.setDrone(true);
			tagDroneR.getAIFlags().setFlag(AIFlags.DRONE_MOTHERSHIP, 100000f, weapon.getShip());
			tagDroneR.getMutableStats().getEnergyWeaponDamageMult().applyMods(weapon.getShip().getMutableStats().getEnergyWeaponDamageMult());
			tagDroneR.setCollisionClass(CollisionClass.NONE);
			tagDroneR.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
			Global.getCombatEngine().addEntity(tagDroneR);
			
			init = true;
		}
		
		float deflect = (1f - weapon.getChargeLevel()) * sweepVal;
		Vector2f tagPointL = MathUtils.getPointOnCircumference(weapon.getFirePoint(0), 100f, weapon.getCurrAngle() - deflect);
		Vector2f tagPointR = MathUtils.getPointOnCircumference(weapon.getFirePoint(0), 100f, weapon.getCurrAngle() + deflect);
		
		if (!fired && weapon.getChargeLevel() > 0.05f) {
			tagDroneL.giveCommand(ShipCommand.FIRE, tagPointL, 0);
			tagDroneR.giveCommand(ShipCommand.FIRE, tagPointR, 0);
		}
		
		if (fired && weapon.getChargeLevel() < 0.05f) {
			fired = false;
		}
		
        float facing = weapon.getCurrAngle();
        Vector2f ship_velocity = weapon.getShip().getVelocity();
        
        demDrone.getLocation().set(weapon.getFirePoint(0));
		demDrone.setFacing(facing);
		demDrone.getVelocity().set(ship_velocity);
		WeaponAPI Laser = demDrone.getWeaponGroupsCopy().get(0).getWeaponsCopy().get(0);
		Laser.setFacing(facing);
		Laser.setKeepBeamTargetWhileChargingDown(true);
		Laser.setScaleBeamGlowBasedOnDamageEffectiveness(false);
		Laser.updateBeamFromPoints();
		
		tagDroneL.getLocation().set(weapon.getFirePoint(0));
		tagDroneL.setFacing(facing - deflect);
		tagDroneL.getVelocity().set(ship_velocity);
		WeaponAPI Laser2 = tagDroneL.getWeaponGroupsCopy().get(0).getWeaponsCopy().get(0);
		Laser2.setFacing(facing - deflect);
		Laser2.setKeepBeamTargetWhileChargingDown(true);
		Laser2.setScaleBeamGlowBasedOnDamageEffectiveness(false);
		Laser2.updateBeamFromPoints();
		
		tagDroneR.getLocation().set(weapon.getFirePoint(0));
		tagDroneR.setFacing(facing + deflect);
		tagDroneR.getVelocity().set(ship_velocity);
		WeaponAPI Laser3 = tagDroneR.getWeaponGroupsCopy().get(0).getWeaponsCopy().get(0);
		Laser3.setFacing(facing + deflect);
		Laser3.setKeepBeamTargetWhileChargingDown(true);
		Laser3.setScaleBeamGlowBasedOnDamageEffectiveness(false);
		Laser3.updateBeamFromPoints();
		
	}
	
  }