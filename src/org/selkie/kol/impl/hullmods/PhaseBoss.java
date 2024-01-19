package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.hullmods.PhaseAnchor;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.combat.DamagePredictor;
import org.selkie.kol.impl.combat.DamagePredictor.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class PhaseBoss extends BaseHullMod {

    public static class PhaseBossPhaseTwoScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public boolean phaseTwo = false;
        CombatEngineAPI engine;
        public float phaseTwoTimer = 0f;
        public ShipAPI ship;
        ShipAPI escortA = null, escortB = null, escortC = null;
        String id = "phase_boss_phase_two_modifier";
        public PhaseBossPhaseTwoScript(ShipAPI ship) {
            this.ship = ship;
        }


        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            float hull = ship.getHitpoints();
            if (damageAmount >= hull && !phaseTwo) {
                phaseTwo = true;
                ship.setHitpoints(1f);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
                if (!ship.isPhased()) {
                    Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                }
                ship.getMutableStats().getPeakCRDuration().modifyFlat(id, ship.getHullSpec().getNoCRLossSeconds());
                shipSpawnExplosion(ship.getShieldRadiusEvenIfNoShield(), ship.getLocation());
                float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),"<CALLING REINFORCEMENTS>", NeuralLinkScript.getFloatySize(ship), ship.getPhaseCloak().getSpecAPI().getEffectColor2(),
                        ship, 16f * timeMult, 3.2f/timeMult, 4f/timeMult, 0f, 0f,1f);
                return true;
            }

            return false;
        }

        public void stayStill(ShipAPI ship){
            ship.giveCommand(ShipCommand.DECELERATE, null, 0);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
            ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);

        }

        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();
            float maxTime = 8f;

            if(phaseTwo && phaseTwoTimer < maxTime){

                phaseTwoTimer += amount;

                // force phase, mitigate damage, regen hp/armor, vent flux, reset ppt/ cr
                if (phaseTwoTimer > maxTime) ship.getMutableStats().getHullDamageTakenMult().unmodify(id);

                ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                ship.getFluxTracker().setHardFlux(0f);
                ship.setHitpoints(Misc.interpolate(1f, ship.getMaxHitpoints()/1.8f, phaseTwoTimer/maxTime));
                ArmorGridAPI armorGrid = ship.getArmorGrid();

                for(int i = 0; i < armorGrid.getGrid().length; i++){
                    for(int j = 0; j < armorGrid.getGrid()[0].length; j++){
                        armorGrid.setArmorValue(i, j, Misc.interpolate(armorGrid.getArmorValue(i, j), armorGrid.getMaxArmorInCell()/1.3f, phaseTwoTimer/maxTime));
                    }
                }

                stayStill(ship);

                // specially tuned for phase ships
                PersonAPI captain = Global.getSettings().createPerson();
                captain.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
                captain.setFaction(Factions.REMNANTS);
                captain.setAICoreId(Commodities.ALPHA_CORE);
                captain.getStats().setLevel(7);
                //captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                //captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                captain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
                captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                captain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                //captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                captain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                //captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                captain.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                //captain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                //captain.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                captain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);


                CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOriginalOwner());
                CombatTaskManagerAPI taskManager = (fleetManager != null) ? fleetManager.getTaskManager(ship.isAlly()) : null;

                if (fleetManager == null) return;


                String escortSpec = "zea_boss_harbinger_super_strike";

                CombatFleetManagerAPI.AssignmentInfo assignmentInfo = taskManager.createAssignment(CombatAssignmentType.DEFEND, fleetManager.getDeployedFleetMemberEvenIfDisabled(ship), false);

                float escortFacing = ship.getFacing();
                Vector2f escortASpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 250f, escortFacing);
                Vector2f escortBSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 250f, escortFacing + 120f);
                Vector2f escortCSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 250f, escortFacing + 240f);
                spawnFog(amount, 50f, escortASpawn);
                spawnFog(amount, 50f, escortBSpawn);
                spawnFog(amount, 50f, escortCSpawn);

                if(phaseTwoTimer > maxTime/4){
                    if (escortA == null) {
                        escortA = fleetManager.spawnShipOrWing(escortSpec, escortASpawn, escortFacing, 0f, captain);
                        shipSpawnExplosion(escortA.getShieldRadiusEvenIfNoShield(), escortA.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortA), assignmentInfo, false);
                    } else{
                        escortA.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortA.giveCommand(ShipCommand.DECELERATE, null, 0);
                        stayStill(escortA);
                    }
                }

                if(phaseTwoTimer > maxTime*2/4){
                    if (escortB == null) {
                        escortB = fleetManager.spawnShipOrWing(escortSpec, escortBSpawn, escortFacing + 120f, 0f, captain);
                        shipSpawnExplosion(escortB.getShieldRadiusEvenIfNoShield(), escortB.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortB), assignmentInfo, false);
                    } else{
                        escortB.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortB.giveCommand(ShipCommand.DECELERATE, null, 0);
                        stayStill(escortB);
                    }
                }

                if(phaseTwoTimer > maxTime*3/4){
                    if (escortC == null) {
                        escortC = fleetManager.spawnShipOrWing(escortSpec, escortCSpawn, escortFacing + 240f, 0f, captain);
                        shipSpawnExplosion(escortC.getShieldRadiusEvenIfNoShield(), escortC.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortC), assignmentInfo, false);
                    } else{
                        escortC.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortC.giveCommand(ShipCommand.DECELERATE, null, 0);
                        stayStill(escortC);
                    }
                }
            }
        }

        public void shipSpawnExplosion(float size, Vector2f location){
            NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(new Color(200,125,255,155), size);
            p.fadeOut = 0.15f;
            p.hitGlowSizeMult = 0.25f;
            p.underglow = new Color(255,175,255, 50);
            p.withHitGlow = false;
            p.noiseMag = 1.25f;
            CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(new NegativeExplosionVisual(p));
            e.getLocation().set(location);
        }

        boolean useNormal = true;
        public void spawnFog(float amount, float radius, Vector2f location){
            final IntervalUtil interval = new IntervalUtil(0.1f, 0.15f);
            final IntervalUtil interval2 = new IntervalUtil(0.35f, 0.6f);
            final Color rgbPos = new Color(90,160,222,60);
            final Color rgbNeg = new Color(145,85,115,60);
            final Color specialOne = new Color(215, 30, 19, 60);
            final Color specialTwo = new Color(14, 220, 200, 60);


            CombatEngineAPI engine = Global.getCombatEngine();
            interval.advance(amount);
            if (interval.intervalElapsed()) {

                Vector2f point = MathUtils.getRandomPointInCircle(location, radius);

                engine.addNebulaParticle(
                        point,
                        MathUtils.getRandomPointInCircle(ZERO, 50f),
                        MathUtils.getRandomNumberInRange(150f, 300f),
                        0.3f,
                        0.5f,
                        0.5f,
                        MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                        rgbPos
                );

                point = MathUtils.getRandomPointInCircle(location, radius * 0.75f);

                if (useNormal && MathUtils.getRandom().nextInt() % 64 != 0) {
                    engine.addNegativeNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 50f),
                            MathUtils.getRandomNumberInRange(150f, 300f),
                            0.3f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                            rgbNeg
                    );
                } else {
                    useNormal = false;
                    interval2.advance(amount);
                    if (interval2.intervalElapsed()) {
                        useNormal = true;
                    }
                    engine.addNegativeNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 35f),
                            MathUtils.getRandomNumberInRange(75f, 200f),
                            0.1f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                            specialTwo
                    );
                    engine.addNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 35f),
                            MathUtils.getRandomNumberInRange(75f, 200f),
                            0.1f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                            specialOne
                    );
                }
            }
        }
    }

    public static class PhaseBossAIScript implements AdvanceableListener {
        private static final boolean DEBUG_ENABLED = false;
        public IntervalUtil enemyTracker = new IntervalUtil(0.8F, 1F);
        public IntervalUtil damageTracker = new IntervalUtil(0.2F, 0.3F);
        public CombatEngineAPI engine;
        public ShipAPI ship;
        public ShipAPI target;
        public Map<ShipAPI, Map<String, Float>> nearbyEnemies = new HashMap<>();
        public float targetRange;
        public Vector2f shipTargetPoint;
        public float shipStrafeAngle;
        public boolean ventingHardFlux, ventingSoftFlux, rechargeCharges;
        public float lastUpdatedTime = 0f;
        public List<FutureHit> incomingProjectiles = new ArrayList<>();
        public List<FutureHit> predictedWeaponHits = new ArrayList<>();
        public List<FutureHit> combinedHits = new ArrayList<>();
        public PhaseBossAIScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();
            if(!ship.isAlive() || ship.getParentStation() != null || engine == null || !engine.isEntityInPlay(ship)){
                return;
            }

            // Calculate Decision Flags
            enemyTracker.advance(amount);
            if (enemyTracker.intervalElapsed() || target == null || !target.isAlive()) {
                getOptimalTarget();
                if (target == null || !target.isAlive()) return;
            }

            damageTracker.advance(amount);
            if (damageTracker.intervalElapsed()) {
                lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
                incomingProjectiles = DamagePredictor.incomingProjectileHits(ship, ship.getLocation());
                float timeToPredict = Math.max(ship.getFluxTracker().getTimeToVent() + damageTracker.getMaxInterval(), 3f);
                predictedWeaponHits = DamagePredictor.generatePredictedWeaponHits(ship, ship.getLocation(), timeToPredict);
                combinedHits = new ArrayList<>();
                combinedHits.addAll(incomingProjectiles);
                combinedHits.addAll(predictedWeaponHits);
            }

            // update ranges and block firing if system is active
            float minRange = Float.POSITIVE_INFINITY;

            for (WeaponAPI weapon: ship.getAllWeapons()){
                if(!weapon.isDecorative() && !weapon.hasAIHint(WeaponAPI.AIHints.PD) && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                    float currentRange = weapon.getRange();
                    minRange = Math.min(currentRange, minRange);
                    if(ship.getSystem().isChargeup()){
                        weapon.setForceNoFireOneFrame(true);
                    }
                }
            }
            targetRange = minRange;

            // calculate how much damage the ship would take if unphased/vent/used system
            float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
            float timeElapsed = currentTime - lastUpdatedTime;
            float bufferTime = 0.2f; // 0.2 sec of buffer time before getting hit
            float armorBase = DamagePredictor.getWeakestTotalArmor(ship);
            float armorMax = ship.getArmorGrid().getArmorRating();
            float armorMinLevel = ship.getMutableStats().getMinArmorFraction().getModifiedValue();
            float armorUnphase = armorBase;
            float armorSystem = armorBase;
            float armorVent = armorBase;
            float phaseTime = ship.isPhased() ? ship.getPhaseCloak().getChargeDownDur() : 0f;

            float hullDamageIfUnphased = 0f;
            float empDamageIfUnphased = 0f;

            float hullDamageIfSystem = 0f;
            float empDamageIfSystem = 0f;

            float hullDamageIfVent = 0f;
            float empDamageIfVent = 0f;

            for(FutureHit hit : combinedHits){
                float timeToHit = (hit.timeToHit - timeElapsed);
                if (timeToHit < -0.1f) continue; // skip hits that have already happened
                if (timeToHit < phaseTime + bufferTime){
                    Pair<Float, Float> trueDamage = DamagePredictor.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorUnphase, ship);
                    hullDamageIfUnphased += trueDamage.two;
                    empDamageIfUnphased += hit.empDamage;
                    armorUnphase = Math.max(armorUnphase - trueDamage.one, armorMinLevel * armorMax);
                }
                if (timeToHit < phaseTime + bufferTime && !Objects.equals(hit.enemyId, target.getId())){
                    Pair<Float, Float> trueDamage = DamagePredictor.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorSystem, ship);
                    hullDamageIfSystem += trueDamage.two;
                    empDamageIfSystem += hit.empDamage;
                    armorSystem = Math.max(armorSystem - trueDamage.one, armorMinLevel * armorMax);
                }
                if (timeToHit < ship.getFluxTracker().getTimeToVent() + bufferTime){
                    Pair<Float, Float> trueDamage = DamagePredictor.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorVent, ship);
                    hullDamageIfVent += trueDamage.two;
                    empDamageIfVent += hit.empDamage;
                    armorVent = Math.max(armorVent - trueDamage.one, armorMinLevel * armorMax);
                }
            }


            float armorDamageLevel = (armorBase - armorUnphase)/armorMax;
            float hullDamageLevel = hullDamageIfUnphased/(ship.getHitpoints()*ship.getHullLevel());
            float armorDamageLevelSystem = (armorBase - armorSystem)/armorMax;
            float hullDamageLevelSystem = hullDamageIfSystem/(ship.getHitpoints()*ship.getHullLevel());
            float armorDamageLevelVent = (armorBase - armorVent)/armorMax;
            float hullDamageLevelVent = hullDamageIfVent/(ship.getHitpoints()*ship.getHullLevel());

            float mountHP = 0f;
            for(WeaponAPI weapon : ship.getAllWeapons()){
                mountHP += weapon.getCurrHealth();
            }
            float empDamageLevel = empDamageIfUnphased/mountHP;
            float empDamageLevelSystem = empDamageIfSystem/mountHP;
            float empDamageLevelVent = empDamageIfVent/mountHP;

            Color test = Color.blue;

            if(DEBUG_ENABLED) {
                test = (armorDamageLevel > 0.03f || hullDamageLevel > 0.03f || empDamageLevel > 0.3f) ? Color.green : test;
                test = (armorDamageLevel > 0.05f || hullDamageLevel > 0.05f || empDamageLevel > 0.5f) ? Color.yellow : test;
                test = (armorDamageLevel > 0.07f || hullDamageLevel > 0.07f || empDamageLevel > 0.7f) ? Color.red : test;
                engine.addSmoothParticle(ship.getLocation(), ship.getVelocity(), 200f, 100f, 0.1f, test);
            }

            // Set decision flags
            float totalFlux = ship.getCurrFlux();
            float hardFlux = ship.getFluxTracker().getHardFlux();
            float maxFlux = ship.getMaxFlux();
            float softFluxLevel = (totalFlux-hardFlux)/(maxFlux-hardFlux);
            if (!ventingSoftFlux && softFluxLevel > 0.8f)
                ventingSoftFlux = true;
            if (ventingSoftFlux && softFluxLevel < 0.1f)
                ventingSoftFlux = false;

            float targetVulnerability = (float) Math.pow((Math.max(target.getFluxLevel(), (1-target.getHullLevel()))), 3);
            targetVulnerability = target.getFluxTracker().isOverloadedOrVenting() ? 1 : targetVulnerability;
            if (!ventingHardFlux && ship.getHardFluxLevel() > Misc.interpolate(0.5f, 0.85f, targetVulnerability))
                ventingHardFlux = true;
            if (ventingHardFlux && (ship.getFluxTracker().isVenting() || ship.getHardFluxLevel() < 0.1f))
                ventingHardFlux = false;

            if (!rechargeCharges && lowestWeaponAmmoLevel(ship) < 0.1f)
                rechargeCharges = true;
            if (rechargeCharges && lowestWeaponAmmoLevel(ship) > 0.5f)
                rechargeCharges = false;


            // Phase Decision Tree starts here:
            boolean wantToPhase = false;
            if(ventingHardFlux){ // while retreating to vent, decide to phase based on flux and incoming damage
                if(ship.getFluxLevel() < linMap(0.4f, 1f, 0f, 0.07f, armorDamageLevel) ||
                        ship.getFluxLevel() < linMap(0.4f, 1f, 0f, 0.07f, hullDamageLevel) ||
                        ship.getFluxLevel() < linMap(0.4f, 1f, 0.5f, 0.9f, empDamageLevel)){
                    wantToPhase = true;
                }
            } else{ // otherwise, ship is attacking
                if(MathUtils.getDistance(ship.getLocation(), target.getLocation()) < targetRange + Misc.getTargetingRadius(ship.getLocation(), target, false)) {
                    // if ship is in weapon range decide to phase based on incoming damage, accounting for the reduction in damage if the system overloads an enemy
                    if (AIUtils.canUseSystemThisFrame(ship)){
                        if((armorDamageLevelSystem > 0.07f || hullDamageLevelSystem > 0.07f || empDamageLevelSystem > 0.7f))
                            wantToPhase = true;
                        else
                            ship.useSystem();
                    } else{
                        if ((armorDamageLevel > 0.07f || hullDamageLevel > 0.07f || empDamageLevel > 0.7f))
                            wantToPhase = true;
                    }
                    // if the ship is not in immense danger of damage, but needs to reload/vent soft flux, also phase. (unless the enemy is overloaded, to maximize dps in that window)
                    boolean maximiseDPS = target.getFluxTracker().isOverloaded() || ship.getSystem().isActive();
                    if ((ventingSoftFlux || rechargeCharges) && !maximiseDPS)
                        wantToPhase = true;

                } else{ // if the ship is not in range, the acceptable damage threshold is much lower,
                    if ((armorDamageLevel > 0.03f || hullDamageLevel > 0.03f || empDamageLevel > 0.3f) || ventingSoftFlux || rechargeCharges || ship.getHardFluxLevel() < 0.1f || ship.getEngineController().isFlamedOut())
                        wantToPhase = true;
                }
            }


            // only implement AI if not under player control
            if (!engine.isUIAutopilotOn() || engine.getPlayerShip() != ship) {
                // phase control
                if (ship.isPhased() ^ wantToPhase)
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                else
                    ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

                // vent control
                if(ventingHardFlux && armorDamageLevelVent < 0.03f && hullDamageLevelVent < 0.03f && empDamageLevelVent < 0.5f){
                    ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                }else{
                    ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                }

                // movement control
                CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOwner());
                CombatTaskManagerAPI taskManager = (fleetManager != null) ? fleetManager.getTaskManager(ship.isAlly()) : null;
                CombatFleetManagerAPI.AssignmentInfo assignmentInfo = (taskManager != null) ? taskManager.getAssignmentFor(ship) : null;
                CombatAssignmentType assignmentType = (assignmentInfo != null) ? assignmentInfo.getType() : null;

                if (shipTargetPoint != null && (assignmentType == CombatAssignmentType.SEARCH_AND_DESTROY || assignmentType == null)){
                    Vector2f shipStrafePoint = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), shipStrafeAngle);
                    strafeToPoint(shipStrafePoint);
                    //turnToPoint(target.getLocation());
                    ship.setShipTarget(target);
                }

                if(DEBUG_ENABLED){
                    if (shipTargetPoint != null) {
                        engine.addSmoothParticle(shipTargetPoint, ship.getVelocity(), 50f, 5f, 0.1f, Color.blue);
                        Vector2f shipStrafePoint = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), shipStrafeAngle);
                        engine.addSmoothParticle(shipStrafePoint, ship.getVelocity(), 50f, 5f, 0.1f, Color.blue);
                    }
                }
            }
        }

        private void getOptimalTarget(){
            // Cache any newly detected enemies
            List<ShipAPI> foundEnemies = AIUtils.getNearbyEnemies(ship, 3000f);
            for(ShipAPI foundEnemy: foundEnemies){
                if(!nearbyEnemies.containsKey(foundEnemy) && foundEnemy.isAlive() && !foundEnemy.isFighter()){
                    Map<String, Float> shipStats = getShipStats(foundEnemy, targetRange);
                    nearbyEnemies.put(foundEnemy, shipStats);
                }
            }

            Set<ShipAPI> deadEnemies = new HashSet<>();
            for (ShipAPI enemy : nearbyEnemies.keySet()){
                if (!enemy.isAlive())
                    deadEnemies.add(enemy);
                if (!MathUtils.isWithinRange(enemy, ship, 3000f))
                    deadEnemies.add(enemy);
            }
            nearbyEnemies.keySet().removeAll(deadEnemies);

            // Calculate ship strafe locations
            if (!nearbyEnemies.isEmpty()) {
                if(ventingHardFlux) {
                    if(target == null || !target.isAlive())
                        target = AIUtils.getNearestEnemy(ship);
                    shipTargetPoint = getBackingOffStrafePoint();
                } else {
                    Vector2f offensiveStrafePoint = getOffensiveStrafePoint();
                    shipTargetPoint = offensiveStrafePoint != null ? offensiveStrafePoint : getBackingOffStrafePoint();
                    if(target == null || !target.isAlive())
                        target = AIUtils.getNearestEnemy(ship);
                }
                if (shipTargetPoint != null) {

                    shipStrafeAngle = VectorUtils.getAngle(ship.getLocation(), shipTargetPoint);
                }
            }
        }

        private Vector2f getBackingOffStrafePoint(){

            float degreeDelta = 5f;

            List<Vector2f> potentialPoints = MathUtils.getPointsAlongCircumference(ship.getLocation(), ship.getCollisionRadius()*2, (int) (360f/degreeDelta), 0);
            Vector2f safestPoint = null;
            float furthestPointSumDistance = 0;
            for (Vector2f potentialPoint : potentialPoints) {
                float currentPointSumDistance = getSumDistance(potentialPoint);
                if(currentPointSumDistance > furthestPointSumDistance){
                    furthestPointSumDistance = currentPointSumDistance;
                    safestPoint = potentialPoint;
                }
            }

            return safestPoint;
        }

        private float getSumDistance(Vector2f potentialPoint){
            float currentPointSumDistance = 0;
            for(ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 2000f)){
                currentPointSumDistance += MathUtils.getDistance(enemy, potentialPoint);
            }
            return currentPointSumDistance;
        }

        private Vector2f getOffensiveStrafePoint(){

            float maxAngle = 120f;

            float degreeDelta = 10f;

            //get the all the "outside" ships
            List<ShipAPI> enemyShipsOnConvexHull = getConvexHull(new ArrayList<>(nearbyEnemies.keySet()));

            Map<ShipAPI, List<Vector2f>> targetEnemys = new HashMap<>();

            // add all the target points from enemies on the "outside edges"
            for(ShipAPI enemy : enemyShipsOnConvexHull){
                float optimalRange = targetRange + Misc.getTargetingRadius(ship.getLocation(), enemy, false);

                List<Vector2f> potentialPoints = new ArrayList<>();
                float enemyAngle = VectorUtils.getAngle(enemy.getLocation(), ship.getLocation());
                float currentAngle = enemyAngle - maxAngle;
                while(currentAngle < enemyAngle + maxAngle){
                    potentialPoints.add(MathUtils.getPointOnCircumference(enemy.getLocation(), optimalRange, currentAngle));
                    currentAngle += degreeDelta;
                }
                targetEnemys.put(enemy, potentialPoints);
            }

            // remove the points that requires ship to fly through other ships
            for(ShipAPI enemy : Global.getCombatEngine().getShips()){
                if (!MathUtils.isWithinRange(enemy, ship, 3000f) || enemy.isFighter() || enemy == ship || !enemy.isAlive())
                    continue;
                for(Map.Entry<ShipAPI, List<Vector2f>> targetEnemy : targetEnemys.entrySet()){
                    List<Vector2f> pointsToRemove = new ArrayList<>();
                    for(Vector2f potentialPoint : targetEnemy.getValue()){
                        float optimalRange = targetRange + Misc.getTargetingRadius(ship.getLocation(), enemy, false);
                        float minKeepoutRadius = (enemy.getHullLevel() < 0.20f ? enemy.getShipExplosionRadius() : enemy.getCollisionRadius()) + ship.getCollisionRadius() + 50f;
                        float keepoutRadius = MathUtils.getDistance(ship.getLocation(), enemy.getLocation()) > optimalRange ? (float) (optimalRange * 0.8) : minKeepoutRadius;
                        if(CollisionUtils.getCollides(potentialPoint, ship.getLocation(), enemy.getLocation(), keepoutRadius)){
                            pointsToRemove.add(potentialPoint);
                        }
                    }
                    targetEnemy.getValue().removeAll(pointsToRemove);
                }
            }

            // from the points left, find the safest target to attack and where to strafe to
            Vector2f optimalStrafePoint = null;
            float currentDanger = Float.POSITIVE_INFINITY;

            for(Map.Entry<ShipAPI, List<Vector2f>> targetEnemy : targetEnemys.entrySet()) {
                for (Vector2f potentialPoint : targetEnemy.getValue()) {
                    float pointDanger = getPointDanger(potentialPoint);


                    if(DEBUG_ENABLED){
                        engine.addFloatingText(potentialPoint, String.valueOf(pointDanger), 10, Color.white, null, 0, 0);
                    }

                    if (pointDanger < currentDanger) {
                        optimalStrafePoint = potentialPoint;
                        target = targetEnemy.getKey();
                        currentDanger = pointDanger;
                    }
                }
            }
            return optimalStrafePoint;
        }

        public float getPointDanger(Vector2f testPoint){
            float currentPointDanger = 0f;
            if (testPoint == null)
                return 0f;

            for (ShipAPI enemy: nearbyEnemies.keySet()) {

                float currentTargetBias = (enemy == target && MathUtils.getDistance(ship.getLocation(), enemy.getLocation()) < targetRange * 1.2) ? 0.1f : 1f;


                float highestDPSAngle = enemy.getFacing() + nearbyEnemies.get(enemy).get("HighestDPSAngle");
                float alpha = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(enemy.getLocation(), testPoint), highestDPSAngle))/180f;

                float DPSDanger = (1f - alpha) * nearbyEnemies.get(enemy).get("HighestDPS") + alpha * nearbyEnemies.get(enemy).get("LowestDPS");

                float shipDistance = MathUtils.getDistance(enemy.getLocation(), testPoint);
                float currentlyOccluded = 1;
                for (ShipAPI otherEnemy: nearbyEnemies.keySet()) {
                    if (otherEnemy != enemy && CollisionUtils.getCollides(enemy.getLocation(), testPoint, otherEnemy.getLocation(), otherEnemy.getCollisionRadius())){
                        currentlyOccluded = 0;
                    }
                }
                if(shipDistance < nearbyEnemies.get(enemy).get("MaxRange")){
                    currentPointDanger += (DPSDanger - shipDistance/100) * (1-enemy.getFluxLevel()) * enemy.getHullLevel() * currentTargetBias * currentlyOccluded;
                }
            }
            return currentPointDanger;
        }

        private Map<String, Float> getShipStats(ShipAPI newEnemy, float defaultRange){

            float deltaAngle = 1f;
            float currentRelativeAngle = 0f;
            Map<String, Float> highLowDPS = new HashMap<>();
            float highestDPS = 0;
            float lowestDPS = Float.POSITIVE_INFINITY;
            float highestDPSAngle = 0;
            float maxRange = 0;

            while(currentRelativeAngle <= 360){
                float potentialDPS = 0;
                for (WeaponAPI weapon: newEnemy.getAllWeapons()){
                    if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                        if((Math.abs(weapon.getArcFacing() - currentRelativeAngle) < weapon.getArc()/2) && (defaultRange < weapon.getRange())){
                            potentialDPS += Math.max(weapon.getDerivedStats().getDps(), weapon.getDerivedStats().getBurstDamage());
                        }
                        maxRange = Math.max(weapon.getRange(), maxRange);
                    }
                }
                if(potentialDPS > highestDPS){
                    highestDPS = potentialDPS;
                }
                if(potentialDPS < lowestDPS){
                    lowestDPS = potentialDPS;
                }

                currentRelativeAngle += deltaAngle;
            }

            currentRelativeAngle = -1f;

            float lastDPS = highestDPS;
            float highZoneStart = 0;
            float highZoneEnd = 0;

            while(currentRelativeAngle <= 360){
                float potentialDPS = 0;
                for (WeaponAPI weapon: newEnemy.getAllWeapons()){
                    if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                        if((Math.abs(weapon.getArcFacing() - currentRelativeAngle) < weapon.getArc()/2) && (defaultRange < weapon.getRange())){
                            potentialDPS += Math.max(weapon.getDerivedStats().getDps(), weapon.getDerivedStats().getBurstDamage());
                        }
                        maxRange = Math.max(weapon.getRange(), maxRange);
                    }
                }
                if(potentialDPS > highestDPS*0.8f && lastDPS < highestDPS*0.8f){
                    highZoneStart = currentRelativeAngle;
                }
                if(potentialDPS < highestDPS*0.8f && lastDPS > highestDPS*0.8f){
                    highZoneEnd = currentRelativeAngle;
                }
                lastDPS = potentialDPS;
                currentRelativeAngle += deltaAngle;
            }

            highestDPSAngle = highZoneEnd - (highZoneEnd - highZoneStart)/2;

            highLowDPS.put("HighestDPS", highestDPS);
            highLowDPS.put("LowestDPS", lowestDPS);
            highLowDPS.put("HighestDPSAngle", highestDPSAngle);
            highLowDPS.put("MaxRange", maxRange);
            return highLowDPS;
        }

        private void strafeToPoint(Vector2f strafePoint){
            float strafeAngle = VectorUtils.getAngle(ship.getLocation(), strafePoint);
            float rotAngle = MathUtils.getShortestRotation(ship.getFacing(), strafeAngle);

            if (rotAngle < 67.5f && rotAngle > -67.5f) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
                if(DEBUG_ENABLED) {
                    Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing());
                    engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
                }
            } else{
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            }

            if (rotAngle > 112.5f || rotAngle < -112.5f){
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null,0);
                if(DEBUG_ENABLED) {
                    Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing()+180f);
                    engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
                }
            }else{
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
            }

            if (rotAngle > 22.5f && rotAngle < 157.5f){
                ship.giveCommand(ShipCommand.STRAFE_LEFT, null,0);
                if(DEBUG_ENABLED) {
                    Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing()+90f);
                    engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
                }
            }else{
                ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            }

            if (rotAngle < -22.5f && rotAngle > -157.5f){
                ship.giveCommand(ShipCommand.STRAFE_RIGHT, null,0);
                if(DEBUG_ENABLED) {
                    Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing()-90f);
                    engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
                }
            }else{
                ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            }
        }

        private void turnToPoint(Vector2f turnPoint){
            float turnAngle = VectorUtils.getAngle(ship.getLocation(), turnPoint);
            float rotAngle = MathUtils.getShortestRotation(ship.getFacing(), turnAngle);

            if (rotAngle > 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
                ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
            } else{
                ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
                ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
            }
        }

        public static float lowestWeaponAmmoLevel(ShipAPI ship){
            float lowestAmmoLevel = 1f;
            for (WeaponAPI weapon: ship.getAllWeapons()){
                if(!weapon.isDecorative() && !weapon.hasAIHint(WeaponAPI.AIHints.PD) && (weapon.getType() != WeaponAPI.WeaponType.MISSILE) && weapon.usesAmmo()){
                    float currentAmmoLevel = (float) weapon.getAmmo() / weapon.getMaxAmmo();
                    if (currentAmmoLevel < lowestAmmoLevel){
                        lowestAmmoLevel = currentAmmoLevel;
                    }
                }
            }
            return lowestAmmoLevel;
        }

        public static int orientation(Vector2f p, Vector2f q, Vector2f r) {
            float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
            if (val == 0)
                return 0; // collinear
            return (val > 0) ? 1 : 2; // clock or counterclockwise
        }

        public static List<ShipAPI> getConvexHull(List<ShipAPI> ships) {
            int n = ships.size();
            if (n < 3)
                return ships;

            List<ShipAPI> hull = new ArrayList<>();

            // Find the leftmost point
            int leftmost = 0;
            for (int i = 1; i < n; i++) {
                if (ships.get(i).getLocation().x < ships.get(leftmost).getLocation().x)
                    leftmost = i;
            }

            int p = leftmost, q;
            do {
                hull.add(ships.get(p));
                q = (p + 1) % n;
                for (int i = 0; i < n; i++) {
                    if (orientation(ships.get(p).getLocation(), ships.get(i).getLocation(), ships.get(q).getLocation()) == 2)
                        q = i;
                }
                p = q;
            } while (p != leftmost);

            return hull;
        }

        public static float linMap(float minOut,float maxOut,float minIn,float maxIn,float input){
            if(input > maxIn) input = maxIn;
            if(input < minIn) input = minIn;
            return minOut+(input-minIn)*(maxOut-minOut)/(maxIn-minIn);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new PhaseBoss.PhaseBossPhaseTwoScript(ship));
        ship.addListener(new PhaseBoss.PhaseBossAIScript(ship));
        String key = "phaseAnchor_canDive";
        Global.getCombatEngine().getCustomData().put(key, true); // disable phase dive, as listener conflicts with phase two script
    }
}
