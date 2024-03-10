package org.selkie.kol.impl.combat.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BallLightningActivator extends MagicSubsystem {
    private static final int MAX_TARGETS = 5;
    private static final float MAX_RANGE = 500f;
    private static final float MAX_ARC = 90f;
    private static final float DAMAGE = 50f;
    private static final float EMP_DAMAGE = 300f;
    ShipAPI ship;
    IntervalUtil lightningInterval = new IntervalUtil(0.07f, 0.15f);
    IntervalUtil lightningDamageInterval = new IntervalUtil(0.05f, 0.5f);
    IntervalUtil aiInterval = new IntervalUtil(0.5f,1f);
    public BallLightningActivator(ShipAPI ship) {
        super(ship);
        this.ship = ship;
    }
    @Override
    public String getDisplayText() {
        return "Lightning Storm";
    }
    @Override
    public float getBaseInDuration() {
        return 1f;
    }
    @Override
    public float getBaseActiveDuration() {
        return 10f;
    }
    @Override
    public float getBaseOutDuration() {
        return 1f;
    }
    @Override
    public float getBaseCooldownDuration() {
        return 10f;
    }
    @Override
    public boolean shouldActivateAI(float amount) {
        aiInterval.advance(amount);
        if(aiInterval.intervalElapsed() && canActivate()) {
            List missiles = AIUtils.getNearbyEnemyMissiles(ship, MAX_RANGE + 100f);
            List enemies = AIUtils.getNearbyEnemies(ship, MAX_RANGE + 100f);
            if (missiles.size() + enemies.size() >= 5) {
                Vector2f center = MathUtils.getPointOnCircumference(ship.getLocation(), 85, ship.getFacing());
                List<CombatEntityAPI> targets = findTargets(center, MAX_TARGETS);
                return (targets.size() >= 3);
            }
        }
        return false;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        Color glowFringe = new Color(100,100,255, ship.isPhased() ? 10 : 100);
        Color glowCenter = new Color(200,220,255, ship.isPhased() ? 20 : 200);
        lightningInterval.advance(amount);
        if(lightningInterval.intervalElapsed()){

            Vector2f leftEmitter = MathUtils.getPointOnCircumference(ship.getLocation(), 96, ship.getFacing() - 14.5f);
            Vector2f rightEmitter = MathUtils.getPointOnCircumference(ship.getLocation(), 96, ship.getFacing() + 14.5f);
            Vector2f centerTarget = MathUtils.getRandomPointInCircle(MathUtils.getPointOnCircumference(ship.getLocation(), 93, ship.getFacing()), 5f);

            EmpArcEntityAPI arcL = Global.getCombatEngine().spawnEmpArcVisual(leftEmitter, ship, centerTarget, ship, 0.01f, glowFringe, glowCenter);
            EmpArcEntityAPI arcR = Global.getCombatEngine().spawnEmpArcVisual(rightEmitter, ship, centerTarget, ship, 0.01f, glowFringe, glowCenter);
            arcL.setCoreWidthOverride(20f);
            arcR.setCoreWidthOverride(20f);

            if(getEffectLevel() > 0 && !ship.isPhased()) {
                Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1f, 1f, centerTarget, ship.getVelocity());
            }
        }

        if(getEffectLevel() > 0 && !ship.isPhased()){
            lightningDamageInterval.advance(amount);
            if(lightningDamageInterval.intervalElapsed()){
                Vector2f center = MathUtils.getPointOnCircumference(ship.getLocation(), 93, ship.getFacing());
                List<CombatEntityAPI> targets = findTargets(center, MAX_TARGETS);
                int visualArcs = MAX_TARGETS - targets.size();
                while(!targets.isEmpty()){
                    EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArc(ship, center, ship,
                            targets.remove(0),
                            DamageType.ENERGY,
                            DAMAGE,
                            EMP_DAMAGE, // emp
                            100000f, // max range
                            "realitydisruptor_emp_impact",
                            5f, // thickness
                            glowFringe,
                            glowCenter
                    );
                    arc.setCoreWidthOverride(30f);
                }
                for(int i = 0; i < visualArcs; i++) {
                    Vector2f visualArc = MathUtils.getRandomPointInCone(center, MAX_RANGE, ship.getFacing() - MAX_ARC/2, ship.getFacing() + MAX_ARC/2);
                    EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(center, ship, visualArc, ship, 5f, glowFringe, glowCenter);
                    arc.setCoreWidthOverride(30f);
                }
            }
        }
    }

    public List<CombatEntityAPI> findTargets(Vector2f from, int maxTargets) {

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,  MAX_RANGE * 2f, MAX_RANGE * 2f);
        int owner = ship.getOwner();
        List<CombatEntityAPI> targets = new ArrayList<>();
        float minScore = Float.MAX_VALUE;

        boolean ignoreFlares = false;

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) && !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;

            if (other instanceof ShipAPI) {
                ShipAPI otherShip = (ShipAPI) other;
                if (otherShip.isHulk()) continue;
                if (otherShip.isPhased()) continue;
            }

            if (other.getCollisionClass() == CollisionClass.NONE) continue;

            if (ignoreFlares && other instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) other;
                if (missile.isFlare()) continue;
            }

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > MAX_RANGE) continue;

            if (!Misc.isInArc(ship.getFacing(), MAX_ARC, from, other.getLocation())) continue;

            targets.add(other);
        }
        Collections.shuffle(targets);
        List<CombatEntityAPI> output = new ArrayList<>();
        for(int i = 0; i < Math.min(maxTargets, targets.size()); i++){
            output.add(targets.get(i));
        }
        return output;
    }

    public void onActivate() {
    }
}
