package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class HellfireCannonSubmunitionScript extends BaseEveryFrameCombatPlugin {
    private final DamagingProjectileAPI infernoCanister;
    private final float explosionTime;
    private final Color explosionColor;

    /**
     * @param proj           inferno cannon shot
     */
    public HellfireCannonSubmunitionScript(DamagingProjectileAPI proj, float explosionTime, Color explosionColor) {
        this.infernoCanister = proj;
        this.explosionTime = explosionTime;
        this.explosionColor = explosionColor;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        if (infernoCanister.isExpired() || !Global.getCombatEngine().isEntityInPlay(infernoCanister)) {
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        infernoCanister.getVelocity().scale(0.993f);

        if (explosionTime - Global.getCombatEngine().getTotalElapsedTime(false) <= 0) {
            DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                    0.25f,
                    100f,
                    30f,
                    1500f,
                    750f,
                    CollisionClass.PROJECTILE_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    3f,
                    3f,
                    0.4f,
                    100,
                    new Color(255, 200, 30, 100),
                    explosionColor
            );
            Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, infernoCanister.getSource(), infernoCanister.getLocation(), false);

            Global.getCombatEngine().spawnExplosion(
                    infernoCanister.getLocation(),
                    MathUtils.getPoint(
                            new Vector2f(),
                            30,
                            0f
                    ),
                    Color.DARK_GRAY,
                    200f,
                    1f
            );

            Global.getSoundPlayer().playSound("system_canister_flak_explosion", 1f, 1f, infernoCanister.getLocation(), new Vector2f());

            Global.getCombatEngine().removeEntity(infernoCanister);
            Global.getCombatEngine().removePlugin(this);
        }
    }
}