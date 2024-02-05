package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.awt.*;
import java.util.List;

public class SupernovaSubmunitionScript extends BaseEveryFrameCombatPlugin {
    private final DamagingProjectileAPI infernoCanister;
    private final float explosionTime;
    private final Color explosionColor;

    /**
     * @param proj           inferno cannon shot
     */
    public SupernovaSubmunitionScript(DamagingProjectileAPI proj, float explosionTime, Color explosionColor) {
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
                    2.3f
            );

            Global.getSoundPlayer().playSound("system_canister_flak_explosion", 1f, 1f, infernoCanister.getLocation(), new Vector2f());

            if (KOL_ModPlugin.hasGraphicsLib) {
                RippleDistortion ripple = new RippleDistortion(infernoCanister.getLocation(), new Vector2f());
                ripple.setSize(200f);
                ripple.setIntensity(25f);
                ripple.setFrameRate(60f);
                ripple.fadeInSize(0.1f);
                ripple.fadeOutIntensity(0.6f);
                DistortionShader.addDistortion(ripple);

                StandardLight light = new StandardLight(infernoCanister.getLocation(), new Vector2f(), new Vector2f(), null);
                light.setSize(200f);
                light.setIntensity(8f);
                light.setLifetime(0.45f);
                light.setAutoFadeOutTime(0.3f);
                light.setColor(new Color(255, 125, 25, 255));
                LightShader.addLight(light);
            }

            Global.getCombatEngine().removeEntity(infernoCanister);
            Global.getCombatEngine().removePlugin(this);
        }
    }
}