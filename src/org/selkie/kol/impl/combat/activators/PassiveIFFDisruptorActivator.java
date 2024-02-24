package org.selkie.kol.impl.combat.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.selkie.kol.impl.hullmods.CoronalCapacitor;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassiveIFFDisruptorActivator extends MagicSubsystem {
    private static final float REFLECT_RANGE = 300f; // added onto ship collision radius
    private static final float ROTATION_SPEED = 420f; // 420f how fast missiles get rotated in degrees per second
    private float chargeRechargeMult = 1f;
    private Map<MissileAPI, MissileTracker> missileMap = new HashMap<>();
    private CombatEntityAPI target = null;

    public PassiveIFFDisruptorActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    public boolean canAssignKey() {
        return false;
    }

    @Override
    public boolean hasCharges() {
        return true;
    }

    @Override
    public int getMaxCharges() {
        return 10;
    }

    @Override
    public String getDisplayText() {
        return "Passive IFF Disruptor";
    }

    @Override
    public float getBaseInDuration() {
        return 0f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0f;
    }

    @Override
    public float getBaseOutDuration() {
        return 0f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 0.1f;
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        return 3f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return false;
    }

    @Override
    public boolean canActivate() {
        return ship.getCustomData().containsKey(CoronalCapacitor.CAPACITY_FACTOR_KEY) && ((Float) ship.getCustomData().get(CoronalCapacitor.CAPACITY_FACTOR_KEY) > 20f);
    }

    private float getHackingRange() {
        return REFLECT_RANGE + ship.getCollisionRadius();
    }

    private EmpArcEntityAPI shootEmpArcVisual(CombatEntityAPI ent) {
        Global.getSoundPlayer().playSound("shock_repeater_emp_impact", 1f, 1f, ent.getLocation(), ent.getVelocity());
        return Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(), ship, ent.getLocation(), ent, MathUtils.getRandomNumberInRange(2f, 5f), new Color(255, 0, 0), new Color(255, 200, 200));
    }

    private EmpArcEntityAPI shootEmpArc(CombatEntityAPI ent) {
        return Global.getCombatEngine().spawnEmpArc(
                ship,
                ship.getLocation(),
                ship,
                ent,
                DamageType.ENERGY,
                0f,
                250f,
                getHackingRange(),
                "shock_repeater_emp_impact",
                MathUtils.getRandomNumberInRange(2f, 5f),
                new Color(255, 0, 0),
                new Color(255, 200, 200));
    }

    private float getThreatValue(MissileAPI missile) {
        return missile.getDamageAmount() / 100f;
    }

    private float getThreatValue(ShipAPI fighter) {
        return fighter.getHullSpec().getOrdnancePoints(null);
    }

    @Override
    public void onActivate() {
        if (target != null) {
            if (target instanceof MissileAPI) {
                shootEmpArcVisual(target);
                missileMap.put((MissileAPI) target, new MissileTracker((MissileAPI) target));
            } else if (target instanceof ShipAPI) {
                shootEmpArc(target);
            }

            chargeRechargeMult = Math.max(1f, chargeRechargeMult * 0.5f);
        }
        target = null;
    }


    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        if (ship.getFluxTracker().isOverloaded()) {
            chargeRechargeMult = 1f;
        } else {
            chargeRechargeMult += amount * (0.5f + 2f * (Float) ship.getCustomData().get(CoronalCapacitor.CAPACITY_FACTOR_KEY));
        }

        if (!chargeInterval.intervalElapsed()) {
            chargeInterval.advance(amount * (chargeRechargeMult - 1f));
        }

        List<MissileAPI> missilesInRange = CombatUtils.getMissilesWithinRange(ship.getLocation(), getHackingRange());
        if (state == State.READY && charges > 0 && !ship.getFluxTracker().isOverloadedOrVenting()) {
            CombatEntityAPI entityToHack = null;
            float biggestThreat = 0f;
            for (MissileAPI missile : missilesInRange) {
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher"))
                    continue;
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher_hf"))
                    continue;

                if (!missileMap.containsKey(missile) && missile.getOwner() != ship.getOwner()) {
                    float threat = getThreatValue(missile);
                    if (threat > biggestThreat) {
                        entityToHack = missile;
                        biggestThreat = threat;
                    }
                }
            }

            List<ShipAPI> fightersInRange = AIUtils.getNearbyEnemies(ship, getHackingRange());
            for (ShipAPI fighter : fightersInRange) {
                if (fighter.isFighter() && getThreatValue(fighter) > biggestThreat) {
                    entityToHack = fighter;
                    biggestThreat = getThreatValue(fighter);
                }
            }

            target = entityToHack;
            if (entityToHack != null) {
                activate();
            }
        }

        List<MissileAPI> toRemove = new ArrayList<>();
        for (MissileAPI missile : missileMap.keySet()) {
            if (missile == null || missile.isFading()) {
                toRemove.add(missile);
                continue;
            }

            if (missile.isMine()) {
                toRemove.add(missile);
                missile.fadeOutThenIn(amount);
                continue;
            }

            try {
                if (missile.getBehaviorSpecParams().get("behavior").equals("PROXIMITY_FUSE")) {
                    toRemove.add(missile);
                    continue;
                }
            } catch (Exception ex) {
            }

            MissileTracker tracker = missileMap.get(missile);
            if (missile.getOwner() != ship.getOwner()) {
                if (missile.getMissileAI() instanceof GuidedMissileAI) {
                    ((GuidedMissileAI) missile.getMissileAI()).setTarget(missile.getSource());
                } else if (missile.isGuided()) {
                    missile.setMissileAI(new DummyMissileAI(missile, missile.getSource()));
                } //Salamander sourced missiles

                missile.setOwner(ship.getOwner());
                missile.setSource(ship);
            }

            if (!tracker.isFacingOrigin()) {
                if (tracker.shouldTurnLeft()) {
                    VectorUtils.rotate(missile.getVelocity(), ROTATION_SPEED * amount);
                    missile.setFacing(missile.getFacing() + ROTATION_SPEED * amount);
                } else {
                    VectorUtils.rotate(missile.getVelocity(), -ROTATION_SPEED * amount);
                    missile.setFacing(missile.getFacing() - ROTATION_SPEED * amount);
                }
            }

            if (!missilesInRange.contains(missile))
                toRemove.add(missile);
        }

        for (MissileAPI missile : toRemove) {
            missileMap.remove(missile);
        }
    }

    private static class MissileTracker {
        private final MissileAPI missile;
        private final Vector2f source;
        private final float initialFacing;

        private MissileTracker(MissileAPI missile) {
            if (missile.getSource() != null) {
                this.source = missile.getSource().getLocation();
            } else {
                this.source = MathUtils.getRandomPointInCone(missile.getLocation(), 800f, missile.getFacing() - 150, missile.getFacing() - 210);
            }
            this.missile = missile;
            this.initialFacing = missile.getFacing();
        }

        public boolean isFacingOrigin() {
            if (source == null)
                return false;
            return Math.abs(MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), source))) < 10;
        }

        public boolean shouldTurnLeft() {
            if (source == null)
                return false;
            float delta = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), source));
            return delta > 0;
        }

        public float getTotalRotation() {
            return Math.abs((missile.getFacing() - initialFacing) % 360);
        }
    }

    private static class DummyMissileAI implements MissileAIPlugin, GuidedMissileAI {
        CombatEntityAPI target;
        MissileAPI missile;

        private DummyMissileAI(MissileAPI missile, CombatEntityAPI target) {
            setTarget(target);
            this.missile = missile;
        }

        @Override
        public void advance(float amount) {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        @Override
        public void setTarget(CombatEntityAPI target) {
            this.target = target;
        }

        @Override
        public CombatEntityAPI getTarget() {
            return target;
        }
    }
}
