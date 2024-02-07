package org.selkie.kol.impl.combat.activators;

import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IFFOverrideActivator extends CombatActivator {
    private static final float REFLECT_RANGE = 300f; // added onto ship collision radius
    private static final float ROTATION_SPEED = 420f; // 420f how fast missiles get rotated in degrees per second
    IntervalUtil aiInterval = new IntervalUtil(0.5f, 1f);
    private Map<MissileAPI, MissileTracker> missileMap = new HashMap<>();

    public IFFOverrideActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    public String getDisplayText() {
        return "IFF Override";
    }

    @Override
    public float getBaseInDuration() {
        return 0.2f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 1.5f;
    }

    @Override
    public float getBaseOutDuration() {
        return 0f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 10f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        aiInterval.advance(amount);
        if (aiInterval.intervalElapsed()) {
            boolean wantToReflect = false;

            float totalShieldDamage = 0f;
            int mirvMissiles = 0;
            for (MissileAPI missile : AIUtils.getNearbyEnemyMissiles(ship, REFLECT_RANGE + ship.getCollisionRadius())) {
                float damage = missile.getDamageAmount();
                if (missile.getDamageType() == DamageType.KINETIC) {
                    damage *= 2;
                } else if (missile.getDamageType() == DamageType.FRAGMENTATION) {
                    damage *= 0.25f;
                }
                totalShieldDamage += damage;

                if (missile.isMirv()) {
                    mirvMissiles++;
                }
            }

            if (mirvMissiles > 0) {
                wantToReflect = true;
            } else if (totalShieldDamage > 0) {
                if (ship.getFluxTracker().getCurrFlux() + (totalShieldDamage * ship.getShield().getFluxPerPointOfDamage()) >= ship.getFluxTracker().getMaxFlux()) {
                    wantToReflect = true;
                } else if (totalShieldDamage >= 750f) {
                    wantToReflect = true;
                }
            }

            return canActivate() && wantToReflect;
        } else {
            return false;
        }
    }

    @Override
    public void advance(float amount) {
        List<MissileAPI> missilesInRange = CombatUtils.getMissilesWithinRange(ship.getLocation(), REFLECT_RANGE + ship.getCollisionRadius());

        if (state == State.ACTIVE) {
            for (MissileAPI missile : missilesInRange) {
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher"))
                    continue;
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher_hf"))
                    continue;

                if (!missileMap.containsKey(missile) && missile.getOwner() != ship.getOwner()) {
                    missileMap.put(missile, new MissileTracker(missile));
                    Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(), ship, missile.getLocation(), missile, MathUtils.getRandomNumberInRange(2f, 5f), new Color(255, 0, 0), new Color(255, 200, 200));
                }
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
