package org.selkie.kol.impl.hullmods;

import activators.ActivatorManager;
import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.Utils;
import org.selkie.kol.impl.combat.StarficzAIUtils;
import org.selkie.kol.impl.combat.activators.ShachihokoDroneActivator;
import org.selkie.kol.impl.combat.activators.ShachihokoTideActivator;

import java.awt.*;
import java.util.Map;

public class CorruptingHeartBoss extends BaseHullMod {
    public static class CorruptingHeartPhaseTwoScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public boolean phaseTwo = false;
        public CombatEngineAPI engine;
        public float phaseTwoTimer = 0f;
        public ShipAPI ship;
        public String id = "boss_phase_two_modifier";

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
                    ((Map<Class<?>, CombatActivator>) ship.getCustomData().get("combatActivators")).remove(ShachihokoDroneActivator.class);
                    ActivatorManager.addActivator(ship, new ShachihokoTideActivator(ship));
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
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasTag("kol_boss") || StarficzAIUtils.DEBUG_ENABLED || true) {
            ship.addListener(new CorruptingHeartPhaseTwoScript(ship));
            ActivatorManager.addActivator(ship, new ShachihokoTideActivator(ship));

            String key = "phaseAnchor_canDive";
            Global.getCombatEngine().getCustomData().put(key, true); // disable phase dive, as listener conflicts with phase two script
        }
    }
}
