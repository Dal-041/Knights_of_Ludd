package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.Utils;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.combat.StarficzAIUtils.*;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NinmahBoss extends BaseHullMod {

    public static class NinmahBossPhaseTwoScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public boolean phaseTwo = false;
        public CombatEngineAPI engine;
        public float phaseTwoTimer = 0f;
        public static final float MAX_TIME = 8f;
        public ShipAPI ship;
        public ShipAPI escortA = null, escortB = null, escortC = null;
        public String id = "boss_phase_two_modifier";
        public Utils.FogSpawner escortAFog, escortBFog, escortCFog;
        public NinmahBossPhaseTwoScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            float hull = ship.getHitpoints();
            if (damageAmount >= hull && !phaseTwo) {
                phaseTwo = true;
                escortAFog = new Utils.FogSpawner();
                escortBFog = new Utils.FogSpawner();
                escortCFog = new Utils.FogSpawner();
                ship.setHitpoints(1f);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.000001f);
                if (!ship.isPhased()) {
                    Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                }
                ship.getMutableStats().getPeakCRDuration().modifyFlat(id, ship.getHullSpec().getNoCRLossSeconds());
                Utils.shipSpawnExplosion(ship.getShieldRadiusEvenIfNoShield(), ship.getLocation());
                float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),"<REQUESTING REINFORCEMENTS>", NeuralLinkScript.getFloatySize(ship), Color.magenta,
                        ship, 16f * timeMult, 3.2f/timeMult, 4f/timeMult, 0f, 0f,1f);
                return true;
            } else if(phaseTwo && phaseTwoTimer < MAX_TIME){
                return true;
            }
            return false;
        }


        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();
            float armorRegen = 0.8f;
            float hpRegen = 0.6f;


            if(phaseTwo && phaseTwoTimer < MAX_TIME){

                phaseTwoTimer += amount;

                if (phaseTwoTimer > MAX_TIME){
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    return;
                }

                // force phase, mitigate damage, regen hp/armor, vent flux, reset ppt/ cr

                if(ship.getPhaseCloak() != null)
                    ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                else{
                    ship.setPhased(true);
                }

                ship.getFluxTracker().setHardFlux(0f);
                ship.setHitpoints(Misc.interpolate(1f, ship.getMaxHitpoints()*hpRegen, phaseTwoTimer/MAX_TIME));
                ArmorGridAPI armorGrid = ship.getArmorGrid();

                for(int i = 0; i < armorGrid.getGrid().length; i++){
                    for(int j = 0; j < armorGrid.getGrid()[0].length; j++){
                        if(armorGrid.getArmorValue(i, j) < armorGrid.getMaxArmorInCell()*armorRegen)
                            armorGrid.setArmorValue(i, j, Misc.interpolate(armorGrid.getArmorValue(i, j), armorGrid.getMaxArmorInCell()*armorRegen, phaseTwoTimer/MAX_TIME));
                    }
                }

                StarficzAIUtils.stayStill(ship);

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


                String escortSpec = "zea_boss_harbinger_Strike";

                CombatFleetManagerAPI.AssignmentInfo assignmentInfo = taskManager.createAssignment(CombatAssignmentType.DEFEND, fleetManager.getDeployedFleetMemberEvenIfDisabled(ship), false);

                float escortFacing = ship.getFacing();
                Vector2f escortASpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 250f, escortFacing);
                Vector2f escortBSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 250f, escortFacing + 120f);
                Vector2f escortCSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 250f, escortFacing + 240f);

                escortAFog.spawnFog(amount, 25f, escortASpawn);
                escortBFog.spawnFog(amount, 25f, escortBSpawn);
                escortCFog.spawnFog(amount, 25f, escortCSpawn);

                if(phaseTwoTimer > MAX_TIME*4/7){
                    if (escortA == null) {
                        escortA = fleetManager.spawnShipOrWing(escortSpec, escortASpawn, escortFacing, 0f, captain);
                        Utils.shipSpawnExplosion(escortA.getShieldRadiusEvenIfNoShield(), escortA.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortA), assignmentInfo, false);
                    } else{
                        escortA.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortA.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortA);
                    }
                }

                if(phaseTwoTimer > MAX_TIME*5/7){
                    if (escortB == null) {
                        escortB = fleetManager.spawnShipOrWing(escortSpec, escortBSpawn, escortFacing + 120f, 0f, captain);
                        Utils.shipSpawnExplosion(escortB.getShieldRadiusEvenIfNoShield(), escortB.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortB), assignmentInfo, false);
                    } else{
                        escortB.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortB.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortB);
                    }
                }

                if(phaseTwoTimer > MAX_TIME*6/7){
                    if (escortC == null) {
                        escortC = fleetManager.spawnShipOrWing(escortSpec, escortCSpawn, escortFacing + 240f, 0f, captain);
                        Utils.shipSpawnExplosion(escortC.getShieldRadiusEvenIfNoShield(), escortC.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortC), assignmentInfo, false);
                    } else{
                        escortC.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortC.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortC);
                    }
                }
            }
        }
    }

    public static class NinmahAIScript implements AdvanceableListener {
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

        public NinmahAIScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();
            if (!ship.isAlive() || ship.getParentStation() != null || engine == null || !engine.isEntityInPlay(ship)) {
                return;
            }

            if (ship.getOwner() != 0 || StarficzAIUtils.DEBUG_ENABLED) {
                ship.getMutableStats().getPeakCRDuration().modifyFlat("phase_boss_cr", 100000);
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
                incomingProjectiles = StarficzAIUtils.incomingProjectileHits(ship, ship.getLocation());
                float timeToPredict = Math.max(ship.getFluxTracker().getTimeToVent() + damageTracker.getMaxInterval(), 3f);
                predictedWeaponHits = StarficzAIUtils.generatePredictedWeaponHits(ship, ship.getLocation(), timeToPredict);
                combinedHits = new ArrayList<>();
                combinedHits.addAll(incomingProjectiles);
                combinedHits.addAll(predictedWeaponHits);
            }
            if (StarficzAIUtils.DEBUG_ENABLED) {
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
                ship.setCaptain(captain);
            }

            // update ranges and block firing if system is active
            float minRange = Float.POSITIVE_INFINITY;

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (!weapon.isDecorative() && !weapon.hasAIHint(WeaponAPI.AIHints.PD) && weapon.getType() != WeaponAPI.WeaponType.MISSILE) {
                    float currentRange = weapon.getRange();
                    minRange = Math.min(currentRange, minRange);
                    if (ship.getSystem().isChargeup()) {
                        weapon.setForceNoFireOneFrame(true);
                    }
                }
            }
            targetRange = minRange;

            // calculate how much damage the ship would take if unphased/vent/used system
            float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
            float timeElapsed = currentTime - lastUpdatedTime;
            float bufferTime = 0.2f; // 0.2 sec of buffer time before getting hit
            float armorBase = StarficzAIUtils.getCurrentArmorRating(ship);
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

            for (FutureHit hit : combinedHits) {
                float timeToHit = (hit.timeToHit - timeElapsed);
                if (timeToHit < -0.1f) continue; // skip hits that have already happened
                if (timeToHit < phaseTime + bufferTime) {
                    Pair<Float, Float> trueDamage = StarficzAIUtils.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorUnphase, ship);
                    hullDamageIfUnphased += trueDamage.two;
                    empDamageIfUnphased += hit.empDamage;
                    armorUnphase = Math.max(armorUnphase - trueDamage.one, armorMinLevel * armorMax);
                }
                if (timeToHit < phaseTime + bufferTime && !Objects.equals(hit.enemyId, target.getId())) {
                    Pair<Float, Float> trueDamage = StarficzAIUtils.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorSystem, ship);
                    hullDamageIfSystem += trueDamage.two;
                    empDamageIfSystem += hit.empDamage;
                    armorSystem = Math.max(armorSystem - trueDamage.one, armorMinLevel * armorMax);
                }
                if (timeToHit < ship.getFluxTracker().getTimeToVent() + bufferTime) {
                    Pair<Float, Float> trueDamage = StarficzAIUtils.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armorVent, ship);
                    hullDamageIfVent += trueDamage.two;
                    empDamageIfVent += hit.empDamage;
                    armorVent = Math.max(armorVent - trueDamage.one, armorMinLevel * armorMax);
                }
            }


            float armorDamageLevel = (armorBase - armorUnphase) / armorMax;
            float hullDamageLevel = hullDamageIfUnphased / (ship.getHitpoints() * ship.getHullLevel());
            float armorDamageLevelSystem = (armorBase - armorSystem) / armorMax;
            float hullDamageLevelSystem = hullDamageIfSystem / (ship.getHitpoints() * ship.getHullLevel());
            float armorDamageLevelVent = (armorBase - armorVent) / armorMax;
            float hullDamageLevelVent = hullDamageIfVent / (ship.getHitpoints() * ship.getHullLevel());

            float mountHP = 0f;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                mountHP += weapon.getCurrHealth();
            }
            float empDamageLevel = empDamageIfUnphased / mountHP;
            float empDamageLevelSystem = empDamageIfSystem / mountHP;
            float empDamageLevelVent = empDamageIfVent / mountHP;

            Color test = Color.blue;

            if (StarficzAIUtils.DEBUG_ENABLED) {
                test = (armorDamageLevel > 0.03f || hullDamageLevel > 0.03f || empDamageLevel > 0.3f) ? Color.green : test;
                test = (armorDamageLevel > 0.05f || hullDamageLevel > 0.05f || empDamageLevel > 0.5f) ? Color.yellow : test;
                test = (armorDamageLevel > 0.07f || hullDamageLevel > 0.07f || empDamageLevel > 0.7f) ? Color.red : test;
                engine.addSmoothParticle(ship.getLocation(), ship.getVelocity(), 200f, 100f, 0.1f, test);
            }

            // Set decision flags
            float totalFlux = ship.getCurrFlux();
            float hardFlux = ship.getFluxTracker().getHardFlux();
            float maxFlux = ship.getMaxFlux();
            float softFluxLevel = (totalFlux - hardFlux) / (maxFlux - hardFlux);
            if (!ventingSoftFlux && softFluxLevel > 0.8f)
                ventingSoftFlux = true;
            if (ventingSoftFlux && softFluxLevel < 0.1f)
                ventingSoftFlux = false;

            float targetVulnerability = (float) Math.pow((Math.max(target.getFluxLevel(), (1 - target.getHullLevel()))), 3);
            targetVulnerability = target.getFluxTracker().isOverloadedOrVenting() ? 1 : targetVulnerability;
            if (!ventingHardFlux && ship.getHardFluxLevel() > Misc.interpolate(0.5f, 0.85f, targetVulnerability))
                ventingHardFlux = true;
            if (ventingHardFlux && ((ship.getFluxTracker().isVenting() && ship.getHardFluxLevel() < 0.5f) || ship.getHardFluxLevel() < 0.1f))
                ventingHardFlux = false;

            if (!rechargeCharges && StarficzAIUtils.lowestWeaponAmmoLevel(ship) < 0.1f)
                rechargeCharges = true;
            if (rechargeCharges && StarficzAIUtils.lowestWeaponAmmoLevel(ship) > 0.5f)
                rechargeCharges = false;


            // Phase Decision Tree starts here:
            boolean wantToPhase = false;
            if (ventingHardFlux) { // while retreating to vent, decide to phase based on flux and incoming damage
                if (ship.getFluxLevel() < Utils.linMap(0.4f, 1f, 0f, 0.07f, armorDamageLevel) ||
                        ship.getFluxLevel() < Utils.linMap(0.4f, 1f, 0f, 0.07f, hullDamageLevel) ||
                        ship.getFluxLevel() < Utils.linMap(0.4f, 1f, 0.5f, 0.9f, empDamageLevel)) {
                    wantToPhase = true;
                }
            } else { // otherwise, ship is attacking
                if (MathUtils.getDistance(ship.getLocation(), target.getLocation()) < targetRange + Misc.getTargetingRadius(ship.getLocation(), target, false)) {
                    // if ship is in weapon range decide to phase based on incoming damage, accounting for the reduction in damage if the system overloads an enemy
                    if (AIUtils.canUseSystemThisFrame(ship) || ship.getSystem().isActive() || target.getFluxTracker().isOverloadedOrVenting()) {
                        if ((armorDamageLevelSystem > 0.07f || hullDamageLevelSystem > 0.07f || empDamageLevelSystem > 0.7f))
                            wantToPhase = true;
                        else
                            ship.useSystem();
                    } else {
                        if ((armorDamageLevel > 0.07f || hullDamageLevel > 0.07f || empDamageLevel > 0.7f))
                            wantToPhase = true;
                    }

                    // if the ship is not in immense danger of damage, but needs to reload/vent soft flux, also phase. (unless the enemy is overloaded, to maximize dps in that window)
                    boolean maximiseDPS = target.getFluxTracker().isOverloaded() || ship.getSystem().isActive();
                    if ((ventingSoftFlux || rechargeCharges) && !maximiseDPS)
                        wantToPhase = true;

                    // phase to avoid getting nuked by enemy ship explosion
                    if(target.getHullLevel() < 0.15f && MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < Math.pow(target.getShipExplosionRadius() + ship.getCollisionRadius(),2))
                        wantToPhase = true;

                } else { // if the ship is not in range, the acceptable damage threshold is much lower,
                    if ((armorDamageLevel > 0.03f || hullDamageLevel > 0.03f || empDamageLevel > 0.3f) || ventingSoftFlux || rechargeCharges || ship.getEngineController().isFlamedOut())
                        wantToPhase = true;
                    if(ship.isPhased() && ship.getHardFluxLevel() < 0.1f)
                        wantToPhase = true;
                    if(!ship.isPhased() && ship.getHardFluxLevel() < 0.01f)
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
                if (ventingHardFlux && armorDamageLevelVent < 0.03f && hullDamageLevelVent < 0.03f && empDamageLevelVent < 0.5f) {
                    ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                } else {
                    ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                }

                // movement control
                CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOwner());
                CombatTaskManagerAPI taskManager = (fleetManager != null) ? fleetManager.getTaskManager(ship.isAlly()) : null;
                CombatFleetManagerAPI.AssignmentInfo assignmentInfo = (taskManager != null) ? taskManager.getAssignmentFor(ship) : null;
                CombatAssignmentType assignmentType = (assignmentInfo != null) ? assignmentInfo.getType() : null;

                if (shipTargetPoint != null && (assignmentType == CombatAssignmentType.SEARCH_AND_DESTROY || assignmentType == null)) {
                    Vector2f shipStrafePoint = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), shipStrafeAngle);
                    StarficzAIUtils.strafeToPoint(ship, shipStrafePoint);
                    //turnToPoint(target.getLocation());
                    ship.setShipTarget(target);
                }

                if (StarficzAIUtils.DEBUG_ENABLED) {
                    if (shipTargetPoint != null) {
                        engine.addSmoothParticle(shipTargetPoint, ship.getVelocity(), 50f, 5f, 0.1f, Color.blue);
                        Vector2f shipStrafePoint = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), shipStrafeAngle);
                        engine.addSmoothParticle(shipStrafePoint, ship.getVelocity(), 50f, 5f, 0.1f, Color.blue);
                    }
                }
            }
        }

        private void getOptimalTarget() {
            // Cache any newly detected enemies, getShipStats is expensive
            List<ShipAPI> foundEnemies = AIUtils.getNearbyEnemies(ship, 5000f);
            for (ShipAPI foundEnemy : foundEnemies) {
                if(foundEnemy.getHullSize() != ShipAPI.HullSize.FIGHTER) {
                    if (!nearbyEnemies.containsKey(foundEnemy) && foundEnemy.isAlive() && !foundEnemy.isFighter()) {
                        Map<String, Float> shipStats = StarficzAIUtils.getShipStats(foundEnemy, targetRange);
                        nearbyEnemies.put(foundEnemy, shipStats);
                    }
                }
            }

            Set<ShipAPI> deadEnemies = new HashSet<>();
            for (ShipAPI enemy : nearbyEnemies.keySet()) {
                if (!enemy.isAlive())
                    deadEnemies.add(enemy);
                if (!MathUtils.isWithinRange(enemy, ship, 3500f))
                    deadEnemies.add(enemy);
            }
            nearbyEnemies.keySet().removeAll(deadEnemies);

            // Calculate ship strafe locations
            if (!nearbyEnemies.isEmpty()) {
                if (ventingHardFlux) {
                    if (target == null || !target.isAlive())
                        target = AIUtils.getNearestEnemy(ship);
                    shipTargetPoint = StarficzAIUtils.getBackingOffStrafePoint(ship);
                } else {
                    Pair<Vector2f, ShipAPI> targetReturn = StarficzAIUtils.getLowestDangerTargetInRange(ship, nearbyEnemies, 120f, targetRange, true);
                    Vector2f targetAttackPoint = targetReturn.one;
                    target = targetReturn.two;

                    shipTargetPoint = targetAttackPoint != null ? targetAttackPoint : StarficzAIUtils.getBackingOffStrafePoint(ship);
                    if (target == null || !target.isAlive())
                        target = AIUtils.getNearestEnemy(ship);
                }
                if (shipTargetPoint != null) {

                    shipStrafeAngle = VectorUtils.getAngle(ship.getLocation(), shipTargetPoint);
                }
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        boolean isBoss = ship.getVariant().hasTag(ZeaUtils.BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().getKeys().contains(ZeaUtils.BOSS_TAG))));

        if(isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            if(!ship.hasListenerOfClass(NinmahBossPhaseTwoScript.class)) ship.addListener(new NinmahBossPhaseTwoScript(ship));
            if(!ship.hasListenerOfClass(NinmahAIScript.class)) ship.addListener(new NinmahAIScript(ship));

            String key = "phaseAnchor_canDive";
            Global.getCombatEngine().getCustomData().put(key, true); // disable phase dive, as listener conflicts with phase two script
        }
    }
}
