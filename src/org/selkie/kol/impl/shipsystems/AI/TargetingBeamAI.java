package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;

public class TargetingBeamAI implements ShipSystemAIScript {

    private ShipAPI ship;
    private float lidarRange = 1500f;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {

        this.ship = ship;

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {

                lidarRange = w.getRange() - 100f;
            }
        }

    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        ShipAPI bestTarget = null;
        float bestScore = 0f;
        if(!ship.isAlive() || !AIUtils.canUseSystemThisFrame(ship)) return;
        for(ShipAPI other : CombatUtils.getShipsWithinRange(ship.getLocation(), lidarRange)){
            float currentScore = 0f;
            if (other.getOwner() == ship.getOwner() || other.getHullSize() == ShipAPI.HullSize.FIGHTER) continue;
            switch (other.getHullSize()){
                case CAPITAL_SHIP:
                    currentScore += 100; break;
                case CRUISER:
                    currentScore += 70; break;
                case DESTROYER:
                    currentScore += 40; break;
                case FRIGATE:
                    currentScore += 20; break;
            }

            currentScore += currentScore * other.getFluxTracker().getFluxLevel();
            currentScore -= 10 * MathUtils.getDistance(other, ship)/lidarRange;
            if (currentScore < bestScore) continue;

            boolean occluded = false;
            for(CombatEntityAPI occlusion : CombatUtils.getEntitiesWithinRange(ship.getLocation(), lidarRange)){
                if (occlusion == other || occlusion == ship || occlusion instanceof DamagingProjectileAPI) continue;
                Vector2f closestPoint = MathUtils.getNearestPointOnLine(occlusion.getLocation(), ship.getLocation(), other.getLocation());
                float occlusionDistance = Misc.getTargetingRadius(closestPoint, occlusion, other.getShield() != null && other.getShield().isOn());
                if (MathUtils.getDistanceSquared(closestPoint, occlusion.getLocation()) < occlusionDistance*occlusionDistance){
                    occluded = true;
                    break;
                }
            }
            if(occluded) continue;

            bestScore = currentScore;
            bestTarget = other;
        }
        if (bestTarget != null){
            float bestFacing = VectorUtils.getAngle(ship.getLocation(), bestTarget.getLocation());
            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.FACING_OVERRIDE_FOR_MOVE_AND_ESCORT_MANEUVERS, 4f, bestFacing);
            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.TARGET_FOR_SHIP_SYSTEM, 4f, bestTarget);
            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.SYSTEM_TARGET_COORDS, 4f, bestTarget.getLocation());
            double diff = Math.abs((ship.getFacing() % 360 + 360) % 360 - (bestFacing % 360 + 360) % 360);

            if(Math.min(diff, 360 - diff) <= 5f ){
                ship.useSystem();
            }
        }
    }
}