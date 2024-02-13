package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwinShieldAI implements ShipSystemAIScript {
    private ShipAPI ship;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {


        int enemiesInFront = 0;
        int enemiesTotal = 0;
        List<Float> enemyAngles = new ArrayList<>();
        for(ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 2000)){
            if(enemy.getHullSize() != ShipAPI.HullSize.FIGHTER && !enemy.isDrone()){
                float enemyRange = 0f;
                for(WeaponAPI weapon : enemy.getAllWeapons()){
                    if(weapon.getType() != WeaponAPI.WeaponType.MISSILE && !weapon.isDecorative()){
                        enemyRange = Math.max(weapon.getRange(), enemyRange);
                    }
                }
                if(MathUtils.getDistanceSquared(ship.getLocation(), enemy.getLocation()) < enemyRange*enemyRange){
                    float enemyAngle = MathUtils.getShortestRotation(VectorUtils.getAngle(enemy.getLocation(), ship.getLocation()), ship.getFacing());
                    if(Math.abs(enemyAngle) > ship.getShield().getArc()/2){
                        enemyAngles.add(enemyAngle);
                        enemiesInFront += 1;
                    }
                    enemiesTotal += 1;
                }
            }
        }

        int missilesInFront = 0;
        int missilesTotal = 0;
        for(MissileAPI missile : AIUtils.getNearbyEnemyMissiles(ship, 1000)){
            if(Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(missile.getLocation(), ship.getLocation()), ship.getFacing())) > ship.getShield().getArc()/2){
                missilesInFront += 1;
            }
            missilesTotal += 1;
        }

        boolean wantToTwinShield = false;
        if(enemiesTotal > 0){
            Collections.sort(enemyAngles);
            if(enemyAngles.size() > 0 && enemyAngles.get(enemyAngles.size()-1)-enemyAngles.get(0) < ship.getShield().getArc()/2) wantToTwinShield = true;
            if((float) enemiesInFront /enemiesTotal < 0.6f ) wantToTwinShield = true;
        }

        if(missilesTotal > 0){
            if((float) missilesInFront /missilesTotal < 0.6f ) wantToTwinShield = true;
        }


        if(wantToTwinShield ^ ship.getSystem().isOn()){
            ship.useSystem();
        }
    }
}
