package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.impl.combat.activators.NianFlaresActivator;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.awt.*;
import java.util.EnumSet;

public class NianBoss extends BaseHullMod {
    public static class NianBossEnragedScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        private static final float ENRAGED_TRANSITION_TIME  = 5f;
        private static final float ENRAGED_FIRERATE_MULT = 2f;
        private static final float ENRAGED_FLUX_COST_MULT = 0.5f;
        private static final float ENRAGED_ENGINE_DAMAGE_MULT = 0.1f;
        public static final String ENRAGED_ID = "boss_enraged_modifier";

        public boolean enraged = false;
        public float enragedTransitionTime = 0f;
        public CombatEngineAPI engine;
        public ShipAPI ship;
        public NianBossEnragedScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if ((ship.getHitpoints() - damageAmount) <= ship.getMaxHitpoints() * 0.5f && !enraged) {
                enraged = true;

                ship.getMutableStats().getBallisticRoFMult().modifyMult(ENRAGED_ID, ENRAGED_FIRERATE_MULT);
                ship.getMutableStats().getEnergyRoFMult().modifyMult(ENRAGED_ID, ENRAGED_FIRERATE_MULT);
                ship.getMutableStats().getMissileRoFMult().modifyMult(ENRAGED_ID, ENRAGED_FIRERATE_MULT);

                ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(ENRAGED_ID, ENRAGED_FLUX_COST_MULT);
                ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(ENRAGED_ID, ENRAGED_FLUX_COST_MULT);
                ship.getMutableStats().getMissileWeaponFluxCostMod().modifyMult(ENRAGED_ID, ENRAGED_FLUX_COST_MULT);

                ship.getMutableStats().getEngineDamageTakenMult().modifyMult(ENRAGED_ID, ENRAGED_ENGINE_DAMAGE_MULT);

                ship.getMutableStats().getHullDamageTakenMult().modifyMult(ENRAGED_ID, 0f);
                ship.setWeaponGlow(1f, Color.RED, EnumSet.allOf(WeaponAPI.WeaponType.class));
                ship.getMutableStats().getPeakCRDuration().modifyFlat(ENRAGED_ID, ship.getHullSpec().getNoCRLossSeconds());

                ship.setCustomData(ENRAGED_ID, true);
                return true;
            }

            if (enraged) {
                float hullMult = (1f - ship.getHullLevel() / 0.5f); //larger as health below half gets lower
                ship.getMutableStats().getMinArmorFraction().modifyPercent(ENRAGED_ID, hullMult * 400f);
            }
            return false;
        }

        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();

            if(enraged){
                if (enragedTransitionTime <= ENRAGED_TRANSITION_TIME) {
                    enragedTransitionTime += amount;
                    if (enragedTransitionTime > ENRAGED_TRANSITION_TIME) {
                        ship.getMutableStats().getHullDamageTakenMult().unmodify(ENRAGED_ID);
                        return;
                    }

                    StarficzAIUtils.stayStill(ship);
                    if (ship.getShield() != null) {
                        ship.getShield().toggleOff();
                        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                    }

                } else {
                    ship.setJitterUnder(ENRAGED_ID, new Color(125, 255, 65), 1f, 5, 10f);
                }
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MagicSubsystemsManager.addSubsystemToShip(ship, new NianFlaresActivator(ship));
        boolean isBoss = ship.getVariant().hasTag(ZeaUtils.BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().contains(ZeaUtils.BOSS_TAG))));

        if(isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            if(!ship.hasListenerOfClass(NianBossEnragedScript.class)) ship.addListener(new NianBossEnragedScript(ship));
        }
    }
}
