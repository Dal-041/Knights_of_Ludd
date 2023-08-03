package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;
import static com.fs.starfarer.api.util.Misc.isPointInBounds;

public class CreepyFog extends BaseHullMod {

    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.15f);
    private final Color rgbPos = new Color(90,160,222,60);
    private final Color rgbNeg = new Color(145,85,115,60);

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isPhased()) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        interval.advance(amount);
        if (interval.intervalElapsed()) {

            Vector2f point = new Vector2f(MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius()));
            while (!CollisionUtils.isPointWithinBounds(point, ship)) {
                point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius());
            }

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

            while (!CollisionUtils.isPointWithinBounds(point, ship)) {
                point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.75f);
            }

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
        }
    }
}
