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
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.Utils;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.awt.*;

public class NinevehBoss extends BaseHullMod {
    public static class NinevehBossPhaseTwoScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public boolean phaseTwo = false;
        public CombatEngineAPI engine;
        public float phaseTwoTimer = 0f;
        public static final float MAX_TIME = 8f;
        public ShipAPI ship;
        public ShipAPI escortHyperionA = null, escortHyperionB = null, escortHarbingerA = null, escortHarbingerB = null, escortDoomA = null, escortDoomB = null;
        public String id = "boss_phase_two_modifier";
        public Utils.FogSpawner escortHyperionAFog, escortHyperionBFog, escortHarbingerAFog, escortHarbingerBFog, escortDoomAFog, escortDoomBFog;
        public NinevehBossPhaseTwoScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            float hull = ship.getHitpoints();
            if (damageAmount >= hull && !phaseTwo) {
                phaseTwo = true;
                escortHyperionAFog = new Utils.FogSpawner();
                escortHyperionBFog = new Utils.FogSpawner();
                escortHarbingerAFog = new Utils.FogSpawner();
                escortHarbingerBFog = new Utils.FogSpawner();
                escortDoomAFog = new Utils.FogSpawner();
                escortDoomBFog = new Utils.FogSpawner();
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

                if (phaseTwoTimer > MAX_TIME) {
                    StarficzAIUtils.unapplyDamper(escortHyperionA, id);
                    StarficzAIUtils.unapplyDamper(escortHyperionB, id);
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

                // specially tuned for doom ships
                PersonAPI doomCaptain = Global.getSettings().createPerson();
                doomCaptain.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
                doomCaptain.setFaction(Factions.REMNANTS);
                doomCaptain.setAICoreId(Commodities.ALPHA_CORE);
                doomCaptain.getStats().setSkipRefresh(true);
                doomCaptain.getStats().setLevel(7);
                //captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                //captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                doomCaptain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
                //doomCaptain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                doomCaptain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                //captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                doomCaptain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                //captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                doomCaptain.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                doomCaptain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                //captain.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                doomCaptain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                doomCaptain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                doomCaptain.getStats().setSkipRefresh(false);

                // specially tuned for harbinger ships
                PersonAPI harbingerCaptain = Global.getSettings().createPerson();
                harbingerCaptain.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
                harbingerCaptain.setFaction(Factions.REMNANTS);
                harbingerCaptain.setAICoreId(Commodities.ALPHA_CORE);
                doomCaptain.getStats().setSkipRefresh(true);
                harbingerCaptain.getStats().setLevel(7);
                //captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                //captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                //captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                //captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                //captain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                //captain.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                harbingerCaptain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                doomCaptain.getStats().setSkipRefresh(false);

                // specially tuned for hyperion ships
                PersonAPI hyperionCaptain = Global.getSettings().createPerson();
                hyperionCaptain.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
                hyperionCaptain.setFaction(Factions.REMNANTS);
                hyperionCaptain.setAICoreId(Commodities.ALPHA_CORE);
                doomCaptain.getStats().setSkipRefresh(true);
                hyperionCaptain.getStats().setLevel(7);
                hyperionCaptain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                hyperionCaptain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                //captain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
                //captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                hyperionCaptain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                //captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                hyperionCaptain.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                //captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                hyperionCaptain.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                //captain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                hyperionCaptain.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
                hyperionCaptain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                //captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                doomCaptain.getStats().setSkipRefresh(false);


                CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOriginalOwner());
                CombatTaskManagerAPI taskManager = (fleetManager != null) ? fleetManager.getTaskManager(ship.isAlly()) : null;

                if (fleetManager == null) return;


                CombatFleetManagerAPI.AssignmentInfo assignmentInfo = taskManager.createAssignment(CombatAssignmentType.DEFEND, fleetManager.getDeployedFleetMemberEvenIfDisabled(ship), false);

                float escortFacing = ship.getFacing();
                Vector2f escortDoomASpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 300f, escortFacing);
                Vector2f escortHyperionASpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 300f, escortFacing + 60f);
                Vector2f escortHarbingerASpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 300f, escortFacing + 120f);
                Vector2f escortDoomBSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 300f, escortFacing + 180f);
                Vector2f escortHyperionBSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 300f, escortFacing + 240f);
                Vector2f escortHarbingerBSpawn = MathUtils.getPointOnCircumference(ship.getLocation(), 300f, escortFacing + 300f);

                escortHyperionAFog.spawnFog(amount, 25f, escortDoomASpawn);
                escortHyperionBFog.spawnFog(amount, 25f, escortHyperionASpawn);
                escortHarbingerAFog.spawnFog(amount, 25f, escortHarbingerASpawn);
                escortHarbingerBFog.spawnFog(amount, 25f, escortDoomBSpawn);
                escortDoomAFog.spawnFog(amount, 25f, escortHyperionBSpawn);
                escortDoomBFog.spawnFog(amount, 25f, escortHarbingerBSpawn);

                DamagingExplosionSpec spec = new DamagingExplosionSpec(
                        1f,
                        ship.getCollisionRadius(),
                        70f,
                        100000f,
                        50000f,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        0f,
                        0f,
                        0f,
                        0,
                        new Color(0,0,0,0),
                        new Color(0,0,0,0)
                );


                if(phaseTwoTimer > MAX_TIME*4/7){
                    if (escortHyperionA == null) {
                        escortHyperionA = fleetManager.spawnShipOrWing("zea_boss_hyperion_Strike", escortHyperionASpawn, escortFacing + 60f, 0f, hyperionCaptain);
                        Utils.shipSpawnExplosion(escortHyperionA.getShieldRadiusEvenIfNoShield(), escortHyperionA.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortHyperionA), assignmentInfo, false);

                        escortHyperionB = fleetManager.spawnShipOrWing("zea_boss_hyperion_Strike", escortHyperionBSpawn, escortFacing + 240f, 0f, hyperionCaptain);
                        Utils.shipSpawnExplosion(escortHyperionB.getShieldRadiusEvenIfNoShield(), escortHyperionB.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortHyperionB), assignmentInfo, false);
                    } else{
                        ship.getFluxTracker().setHardFlux(0f);
                        escortHyperionA.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.applyDamper(escortHyperionA, id, Utils.linMap(1,0, MAX_TIME*2/3, MAX_TIME, phaseTwoTimer));
                        StarficzAIUtils.stayStill(escortHyperionA);
                        StarficzAIUtils.holdFire(escortHyperionA);

                        ship.getFluxTracker().setHardFlux(0f);
                        escortHyperionB.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.applyDamper(escortHyperionB, id, Utils.linMap(1,0, MAX_TIME*2/3, MAX_TIME, phaseTwoTimer));
                        StarficzAIUtils.stayStill(escortHyperionB);
                        StarficzAIUtils.holdFire(escortHyperionB);
                    }
                }
                if(phaseTwoTimer > MAX_TIME*5/7){
                    if (escortHarbingerA == null || escortHarbingerB == null) {
                        escortHarbingerA = fleetManager.spawnShipOrWing("zea_boss_harbinger_Strike", escortHarbingerASpawn, escortFacing + 120f, 0f, harbingerCaptain);
                        Utils.shipSpawnExplosion(escortHarbingerA.getShieldRadiusEvenIfNoShield(), escortHarbingerA.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortHarbingerA), assignmentInfo, false);

                        escortHarbingerB = fleetManager.spawnShipOrWing("zea_boss_harbinger_Strike", escortHarbingerBSpawn, escortFacing + 300f, 0f, harbingerCaptain);
                        Utils.shipSpawnExplosion(escortHarbingerB.getShieldRadiusEvenIfNoShield(), escortHarbingerB.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortHarbingerB), assignmentInfo, false);
                    } else{
                        escortHarbingerA.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortHarbingerA.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortHarbingerA);

                        escortHarbingerB.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortHarbingerB.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortHarbingerB);
                    }
                }
                if(phaseTwoTimer > MAX_TIME*6/7){
                    if (escortDoomA == null || escortDoomB == null) {
                        escortDoomA = fleetManager.spawnShipOrWing("zea_boss_doom_Strike", escortDoomASpawn, escortFacing, 0f, doomCaptain);
                        Utils.shipSpawnExplosion(escortDoomA.getShieldRadiusEvenIfNoShield(), escortDoomA.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortDoomA), assignmentInfo, false);

                        escortDoomB = fleetManager.spawnShipOrWing("zea_boss_doom_Strike", escortDoomBSpawn, escortFacing + 180f, 0f, doomCaptain);
                        Utils.shipSpawnExplosion(escortDoomB.getShieldRadiusEvenIfNoShield(), escortDoomB.getLocation());
                        taskManager.giveAssignment(fleetManager.getDeployedFleetMemberEvenIfDisabled(escortDoomB), assignmentInfo, false);
                    } else{
                        escortDoomA.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortDoomA.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortDoomA);

                        escortDoomB.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                        ship.getFluxTracker().setHardFlux(0f);
                        escortDoomB.giveCommand(ShipCommand.DECELERATE, null, 0);
                        StarficzAIUtils.stayStill(escortDoomB);
                    }
                }
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        boolean isBoss = ship.getVariant().hasTag(ZeaUtils.BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().getKeys().contains(ZeaUtils.BOSS_TAG))));

        if(isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            if(!ship.hasListenerOfClass(NinevehBossPhaseTwoScript.class)) ship.addListener(new NinevehBossPhaseTwoScript(ship));

            String key = "phaseAnchor_canDive";
            Global.getCombatEngine().getCustomData().put(key, true); // disable phase dive, as listener conflicts with phase two script
        }
    }
}
