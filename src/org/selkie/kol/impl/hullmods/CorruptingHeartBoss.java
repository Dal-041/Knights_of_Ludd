package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.selkie.kol.Utils;
import org.selkie.kol.combat.ParticleController;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.impl.combat.activators.ShachihokoDroneActivator;
import org.selkie.kol.impl.combat.activators.ShachihokoTideActivator;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.shipsystems.CorruptionJetsStats;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorruptingHeartBoss extends BaseHullMod {
    private static final float BALLISTIC_DAMAGE_BUFF = 25f;
    private static final float ENERGY_ROF_BUFF = 25f;
    private static final float ENERGY_FLUX_PER_SHOT_BUFF = -25f;

    public static class CorruptingHeartPhaseTwoScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public boolean phaseTwo = false;
        public CombatEngineAPI engine;
        public float phaseTwoTimer = 0f;
        public ShipAPI ship;
        public String id = "boss_phase_two_modifier";
        public String corruptionBuffId = "corrupting_heart_buff";
        public Map<ShipAPI, ShipAPI> droneTargetMap = new HashMap<>();

        public CorruptingHeartPhaseTwoScript(ShipAPI ship) {
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
                Utils.shipSpawnExplosion(ship.getShieldRadiusEvenIfNoShield(), ship.getLocation());
                float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(), "<$F%&a2#(#D=!@$^>", NeuralLinkScript.getFloatySize(ship), Color.magenta,
                        ship, 16f * timeMult, 3.2f / timeMult, 4f / timeMult, 0f, 0f, 1f);
                return true;
            }
            return false;
        }

        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();
            float armorRegen = 0.8f;
            float hpRegen = 0.6f;
            float maxTime = 8f;

            if (phaseTwo && phaseTwoTimer < maxTime) {
                phaseTwoTimer += amount;

                if (phaseTwoTimer > maxTime) {
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.setPhased(false);
                    MagicSubsystemsManager.removeSubsystemFromShip(ship, ShachihokoDroneActivator.class);
                    MagicSubsystemsManager.addSubsystemToShip(ship, new ShachihokoTideActivator(ship));
                    return;
                }

                // force phase, mitigate damage, regen hp/armor, vent flux, reset ppt/cr
                if (ship.getPhaseCloak() != null)
                    ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
                else {
                    ship.setPhased(true);
                }

                ship.getFluxTracker().setHardFlux(0f);
                ship.setHitpoints(Misc.interpolate(1f, ship.getMaxHitpoints() * hpRegen, phaseTwoTimer / maxTime));
                ArmorGridAPI armorGrid = ship.getArmorGrid();

                for (int i = 0; i < armorGrid.getGrid().length; i++) {
                    for (int j = 0; j < armorGrid.getGrid()[0].length; j++) {
                        if (armorGrid.getArmorValue(i, j) < armorGrid.getMaxArmorInCell() * armorRegen)
                            armorGrid.setArmorValue(i, j, Misc.interpolate(armorGrid.getArmorValue(i, j), armorGrid.getMaxArmorInCell() * armorRegen, phaseTwoTimer / maxTime));
                    }
                }

                StarficzAIUtils.stayStill(ship);
            }

            List<ShipAPI> validDrones = new ArrayList<>();
            for (MagicSubsystem activator : MagicSubsystemsManager.getSubsystemsForShipCopy(ship)) {
                if (activator instanceof MagicDroneSubsystem) {
                    List<ShipAPI> wings = new ArrayList<>(((MagicDroneSubsystem) activator).getActiveWings().keySet());
                    for (ShipAPI fighter : wings) {
                        validDrones.add(fighter);

                        boolean doEffect = false;
                        ShipAPI target = Utils.getDroneShieldTarget(fighter);
                        if (target != null) {
                            boolean applyBuff = !droneTargetMap.containsKey(fighter);
                            if (!applyBuff) {
                                //remove buffs from old target if needed to, and target new target
                                ShipAPI oldTarget = droneTargetMap.get(fighter);
                                if (oldTarget != target) {
                                    oldTarget.getMutableStats().getBallisticWeaponDamageMult().unmodify(corruptionBuffId);
                                    oldTarget.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(corruptionBuffId);
                                    oldTarget.getMutableStats().getEnergyRoFMult().unmodify(corruptionBuffId);
                                    applyBuff = true;
                                }
                            }

                            if (applyBuff) {
                                droneTargetMap.put(fighter, target);

                                target.getMutableStats().getBallisticWeaponDamageMult().modifyPercent(corruptionBuffId, BALLISTIC_DAMAGE_BUFF);
                                target.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(corruptionBuffId, ENERGY_FLUX_PER_SHOT_BUFF);
                                target.getMutableStats().getEnergyRoFMult().modifyPercent(corruptionBuffId, ENERGY_FLUX_PER_SHOT_BUFF);
                            }

                            doEffect = MathUtils.getRandomNumberInRange(0f, 1f) < 0.01f;
                        } else {
                            doEffect = MathUtils.getRandomNumberInRange(0f, 3f) < 0.01f;
                        }

                        if (doEffect) {
                            ShipAPI effectTarget = target != null ? target : ship;
                            ParticleController.Companion.addParticle(CorruptionJetsStats.Companion.getRandomParticleData(effectTarget));
                            Global.getCombatEngine().spawnEmpArcVisual(fighter.getLocation(), fighter, effectTarget.getLocation(), effectTarget, 3f, Color.RED, Color.RED.brighter().brighter());
                        }
                    }
                }
            }

            for (Map.Entry<ShipAPI, ShipAPI> droneTarget : new ArrayList<>(droneTargetMap.entrySet())) {
                ShipAPI drone = droneTarget.getKey();
                if (!validDrones.contains(drone)) {
                    ShipAPI target = droneTarget.getValue();

                    target.getMutableStats().getBallisticWeaponDamageMult().unmodify(corruptionBuffId);
                    target.getMutableStats().getEnergyWeaponFluxCostMod().unmodify(corruptionBuffId);
                    target.getMutableStats().getEnergyRoFMult().unmodify(corruptionBuffId);

                    droneTargetMap.remove(drone);
                }
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        boolean isBoss = ship.getVariant().hasTag(ZeaUtils.BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().contains(ZeaUtils.BOSS_TAG))));

        if(isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            ship.addListener(new CorruptingHeartPhaseTwoScript(ship));
            MagicSubsystemsManager.addSubsystemToShip(ship, new ShachihokoDroneActivator(ship));

            String key = "phaseAnchor_canDive";
            Global.getCombatEngine().getCustomData().put(key, true); // disable phase dive, as listener conflicts with phase two script

            ship.getMutableStats().getShieldDamageTakenMult().modifyMult("kol_boss_buff", 0.8f);
            ship.getMutableStats().getFluxCapacity().modifyMult("kol_boss_buff", 1.5f);
        }
    }
}
