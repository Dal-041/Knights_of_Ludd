package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.combat.StarficzAIUtils;

import java.util.ArrayList;
import java.util.List;

import static org.selkie.kol.combat.StarficzAIUtils.*;

public class FortressFluxConverterAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShieldAPI shield;
    private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.3f); //Seconds
    public float lastUpdatedTime = 0f;
    public List<StarficzAIUtils.FutureHit> incomingProjectiles = new ArrayList<>();
    public List<StarficzAIUtils.FutureHit> predictedWeaponHits = new ArrayList<>();
    public List<StarficzAIUtils.FutureHit> combinedHits = new ArrayList<>();
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.shield = ship.getShield();
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

        if (!AIUtils.canUseSystemThisFrame(ship)) {return;}

        // use system to convert softflux
        if (ship.getFluxLevel() - ship.getHardFluxLevel() > 0.25f){
            ship.useSystem(); return;
        }

        ShipSystemAPI system = ship.getSystem();
        float systemTime = system.getChargeUpDur() + system.getChargeActiveDur() + system.getChargeDownDur();

        tracker.advance(amount);
        if (tracker.intervalElapsed()) {
            lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
            incomingProjectiles = incomingProjectileHits(ship, ship.getLocation());
            predictedWeaponHits = generatePredictedWeaponHits(ship, ship.getLocation(), systemTime + tracker.getMaxInterval() + 0.1f);
            combinedHits = new ArrayList<>();
            combinedHits.addAll(incomingProjectiles);
            combinedHits.addAll(predictedWeaponHits);
            float BUFFER_ARC = 10f;

            // prefilter expensive one time functions
            if(shield.getType() == ShieldAPI.ShieldType.FRONT){
                List<FutureHit> filteredHits = new ArrayList<>();
                for(FutureHit hit : combinedHits){
                    if(Misc.isInArc(ship.getFacing(), shield.getArc() + BUFFER_ARC, hit.angle)){
                        filteredHits.add(hit);
                    }
                }
                combinedHits = filteredHits;
            }
        }

        // calculate how much hardflux would be saved if the system activated
        float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
        float timeElapsed = currentTime - lastUpdatedTime;
        float unfoldTime = shield.getUnfoldTime();
        float hardFluxSaved = 0;

        for(FutureHit hit : combinedHits){
            float offAngle = Math.abs(MathUtils.getShortestRotation(ship.getShield().getFacing(), hit.angle)) - (shield.isOn() ? shield.getActiveArc()/2 : 0);
            if(offAngle < 0) offAngle = 0;
            float timeToBlock = (offAngle/(shield.getArc()/2)) * unfoldTime;
            float timeToHit = (hit.timeToHit - timeElapsed);
            if(timeToHit < -0.1f) continue; // skip hits that have already happened
            if (timeToHit > timeToBlock && !hit.softFlux){
                float hitHardflux = fluxToShield(hit.damageType, hit.damage, ship);

                if (timeToHit < system.getChargeUpDur()){
                    hardFluxSaved += hitHardflux * Misc.interpolate(0f,0.9f, timeToHit/system.getChargeUpDur());
                } else if (timeToHit < systemTime - system.getChargeDownDur()) {
                    hardFluxSaved += hitHardflux * 0.9f;
                } else if (timeToHit < systemTime){
                    hardFluxSaved += hitHardflux * Misc.interpolate(0.9f,0f, (systemTime - timeToHit)/system.getChargeDownDur());
                }
            }
        }

        // use system to tank damage
        if(hardFluxSaved > ship.getMaxFlux() * 0.2f && system.getAmmo() > 1){
            ship.useSystem();
        }
        else if(hardFluxSaved > ship.getMaxFlux() * 0.3f){
            ship.useSystem();
        }
    }
}
