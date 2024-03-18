package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicUI;
import org.selkie.kol.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.selkie.kol.combat.StarficzAIUtils.*;

//Concept and impl by Tartiflette, AI by Starficz

public class ShieldEffect implements EveryFrameWeaponEffectPlugin {
    private static final Logger log = Global.getLogger(ShieldEffect.class);
    private final float MAX_SHIELD=10;
    private boolean runOnce = false, disabled=false;
    private float shieldT=0,aiT=0;
    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShieldAPI shield;
    private Color baseColorRing;
    private Color baseColorInner;
    private Color depletedColorRing;
    private Color depletedColorInner;
    private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f); //Seconds
    public float lastUpdatedTime = 0f;
    public List<FutureHit> incomingProjectiles = new ArrayList<>();
    public List<FutureHit> predictedWeaponHits = new ArrayList<>();
    public List<FutureHit> combinedHits = new ArrayList<>();
    public List<Float> omniShieldDirections = new ArrayList<>();
    public float lastShieldOnTime = 0f;
    public long debugTime = 0;

    private final IntervalUtil debugTracker = new IntervalUtil(10f, 10f); //Seconds
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        debugTracker.advance(amount);
        if(debugTracker.intervalElapsed()){
            log.debug("nanoseconds " + weapon.getShip().getHullSpec().getHullId() + ":"+ weapon.getShip().getName() + " spent in shield ai in the last " +
                    debugTracker.getMaxInterval() + " seconds: " + debugTime + " (" + debugTime*0.000000001 + " seconds)");
            debugTime = 0;
        }

        if (!runOnce){
            runOnce=true;
            ship=weapon.getShip();
            shield=ship.getShield();
            if (shield!= null) {
                baseColorRing = shield.getRingColor();
                baseColorInner = shield.getInnerColor();
                depletedColorRing = new Color(255, 80, 80, baseColorRing.getAlpha());
                depletedColorInner = new Color(255, 45, 45, baseColorInner.getAlpha());
            }
            this.engine=engine;
        }

        if(shield == null) return;

        //UI
        float shieldTLevel = shieldT/MAX_SHIELD;
        Color barColor = disabled ? Misc.getNegativeHighlightColor() : Utils.OKLabInterpolateColor(Misc.getPositiveHighlightColor(), Misc.getNegativeHighlightColor(), shieldTLevel);
        MagicUI.drawInterfaceStatusBar(ship, 1-shieldTLevel, barColor, null, 0, "SHIELD", (int) (100-100*shieldTLevel));
        
        if (engine.isPaused() || !ship.isAlive()) {return;}

        if(!disabled){
            if(shield.isOn()){
                shieldT=Math.min(MAX_SHIELD, shieldT+amount);
            } else {
                shieldT=Math.max(0, shieldT-(amount*2));
            }

            //FX
            shield.setInnerColor(Utils.OKLabInterpolateColor(baseColorInner, depletedColorInner, shieldTLevel));
            shield.setRingColor(Utils.OKLabInterpolateColor(baseColorRing, depletedColorRing, shieldTLevel));

            //AI

            //If debug is turned on, weapons that can hit the ship will be blue, and projectiles that can hit the ship will be magenta
            if (!disabled && (!engine.isUIAutopilotOn() || engine.getPlayerShip() != ship)) {
                // lockout from spamming shields
                boolean lockoutOver = shield.isOff() || shield.getActiveArc()/shield.getArc() > 0.5f;

                if ((shield.isOn() ^ wantToShield(amount)) && lockoutOver)
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                else
                    ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
            }

            // Overloaded Shields
            if(shieldT==MAX_SHIELD){
                shield.toggleOff();
                ship.getFluxTracker().showOverloadFloatyIfNeeded("Shield offline", Misc.getNegativeHighlightColor(), 2, true);
                ship.getFluxTracker().beginOverloadWithTotalBaseDuration(0.05f);
                disabled=true;
            }

        } else {
            shield.toggleOff();
            shieldT=Math.max(0, shieldT-(amount/2));

            if(shieldT==0){
                disabled=false;
                ship.getFluxTracker().showOverloadFloatyIfNeeded("Shield online", Misc.getPositiveHighlightColor(), 2, true);
            }
        }
    }

    public boolean wantToShield(float amount){
        long startTime = System.nanoTime();
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {
            lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
            incomingProjectiles = incomingProjectileHits(ship, ship.getLocation());
            predictedWeaponHits = generatePredictedWeaponHits(ship, ship.getLocation(), shield.getUnfoldTime()*2);
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
            else{
                List<Float> candidateShieldDirections = new ArrayList<>();
                float FUZZY_RANGE = 1f;
                for(FutureHit hit : combinedHits){
                    boolean tooCLose = false;
                    for(float candidateDirection : candidateShieldDirections){
                        if (Math.abs(hit.angle - candidateDirection) < FUZZY_RANGE) { tooCLose = true; break; }
                    }
                    if(!tooCLose) candidateShieldDirections.add(hit.angle);
                }
                if (candidateShieldDirections.isEmpty()) candidateShieldDirections.add(ship.getFacing());
                omniShieldDirections = candidateShieldDirections;
            }
        }


        // calculate how much damage the ship would take if shields went down
        float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
        float timeElapsed = currentTime - lastUpdatedTime;
        float armor = getCurrentArmorRating(ship);

        float unfoldTime = shield.getUnfoldTime();
        float bufferTime = unfoldTime/4; //TODO: get real shield activate delay, unfoldTime/4 is: "looks about right"
        if(shield.isOn()) lastShieldOnTime = currentTime;
        float delayTime = Math.max(0.5f - (currentTime - lastShieldOnTime), 0f); //TODO: get real shield deactivate time

        float shieldHardFluxSavedIfNoShield = 0f;
        float armorDamageIfNoShield = Float.POSITIVE_INFINITY;
        float hullDamageIfNoShield = Float.POSITIVE_INFINITY;
        float empDamageIfNoShield = 0f;

        List<Float> shieldDirections = new ArrayList<>();
        if(shield.getType() == ShieldAPI.ShieldType.FRONT) shieldDirections.add(ship.getFacing());
        else shieldDirections.addAll(omniShieldDirections);

        for(float shieldDirection : shieldDirections){
            float currentHullDamage = 0f;
            float currentArmor = armor;
            float currentShieldHardFluxSaved = 0f;
            float currentEMPDamage = 0f;
            float currentBufferTime = Float.POSITIVE_INFINITY;
            float bestBufferTime = 0f;
            for(FutureHit hit : combinedHits){
                float offAngle = Math.abs(MathUtils.getShortestRotation(shieldDirection, hit.angle));
                float timeToBlock = (offAngle/(shield.getArc()/2)) * unfoldTime + delayTime;
                float timeToHit = (hit.timeToHit - timeElapsed);
                if(timeToHit < -0.1f) continue; // skip hits that have already happened
                if (timeToHit < (timeToBlock + bufferTime)){
                    if (!hit.softFlux) currentShieldHardFluxSaved += fluxToShield(hit.damageType, hit.damage, ship);
                    Pair<Float, Float> trueDamage = damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, currentArmor, ship);
                    currentArmor = Math.max(currentArmor - trueDamage.one, 0f);
                    currentHullDamage += trueDamage.two;
                    currentEMPDamage += hit.empDamage;
                }else{
                    currentBufferTime = Math.min(timeToHit - timeToBlock, currentBufferTime);
                }
            }

            boolean betterDirectionFound = false;
            if((armor - currentArmor) < armorDamageIfNoShield) betterDirectionFound = true;
            else if((armor - currentArmor) == armorDamageIfNoShield){
                if(currentHullDamage < hullDamageIfNoShield) betterDirectionFound = true;
                if(currentHullDamage == hullDamageIfNoShield && currentBufferTime > bestBufferTime) betterDirectionFound = true;
            }

            if (betterDirectionFound){
                shieldHardFluxSavedIfNoShield = currentShieldHardFluxSaved;
                armorDamageIfNoShield = (armor - currentArmor);
                hullDamageIfNoShield = currentHullDamage;
                empDamageIfNoShield = currentEMPDamage;
            }
        }


        // consider the max of shield time or flux level to decide when to shield
        float alpha = Math.max(ship.getFluxLevel(), shieldT / MAX_SHIELD);
        float lowDamage = 0.001f; // 0.1% hull damage
        float highDamage = 0.020f; // 2.0% hull damage

        // shield based on the current flux level and shield timer
        boolean wantToShield = (hullDamageIfNoShield + armorDamageIfNoShield + empDamageIfNoShield/3) > ship.getHitpoints() * Misc.interpolate(lowDamage, highDamage, alpha);

        // if the damage is high enough to shield, but flux is high/ shield timer is low, armor tank KE
        if (wantToShield && shieldHardFluxSavedIfNoShield/(hullDamageIfNoShield + armorDamageIfNoShield)  > Misc.interpolate(7f,2f, alpha))
            wantToShield = false;

        float emergencyArcSaved = Math.min(ship.getShield().getUnfoldTime() / (ship.getShield().getArc() / 80), ship.getShield().getUnfoldTime());
        if (shieldT + emergencyArcSaved > MAX_SHIELD) // Save enough time for 1 emergency unfold
            wantToShield = false;

        if ((shieldHardFluxSavedIfNoShield + ship.getCurrFlux())/ship.getMaxFlux() > 1) // Prevent overloads...
            wantToShield = false;

        if ((hullDamageIfNoShield + armorDamageIfNoShield) > 4000f) // Unless a reaper is on the way, in that case try and block it no matter what
            wantToShield = true;

        long endTime = System.nanoTime();
        debugTime += (endTime - startTime);
        return wantToShield;
    }

}
