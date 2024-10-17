package org.selkie.zea.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import org.selkie.kol.plugins.KOL_ModPlugin;
import org.selkie.zea.helpers.ZeaStaticStrings;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SupernovaStats extends BaseShipSystemScript {
    public static final float EXPLOSION_RADIUS = 1000f;
    protected static final ShipSystemSpecAPI entry = Global.getSettings().getShipSystemSpec(ZeaStaticStrings.systemIDSupernova);
    private static final float IN_STATE_DURATION = entry.getIn(); //2f;
    private static final float ACTIVE_STATE_DURATION = entry.getActive(); //4f;
    private static boolean doBoom = true;

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        ShipAPI ship = null;
        ship = (ShipAPI)stats.getEntity();
        if (ship == null) return;
        float supernovaInActiveTime = 1f;

        // targeting ring
        float jitterLevel = supernovaInActiveTime / (IN_STATE_DURATION + ACTIVE_STATE_DURATION);
        ship.setJitter(id, new Color(1f, 1f, 0f, MathUtils.clamp(jitterLevel, 0f, 1f)), effectLevel, 4, 5f + 10f * jitterLevel);
        ship.setJitterUnder(id, new Color(1f, 0f, 0f, MathUtils.clamp(jitterLevel, 0f, 1f)), effectLevel, 20, 6f + 20f * jitterLevel);

        if (state.equals(State.IN)) {
            MagicRender.singleframe(
                    Global.getSettings().getSprite("fx", "zea_nian_targetingRing"),
                    ship.getLocation(), //location
                    new Vector2f(1200, 1200), //size
                    ship.getFacing(), //angle
                    new Color(255, 100, 0, 128),
                    true, //additive
                    CombatEngineLayers.UNDER_SHIPS_LAYER
            );
        }
        if (doBoom && state.equals(State.ACTIVE)) {
            //explosion
            Global.getCombatEngine().spawnExplosion(
                    ship.getLocation(),
                    new Vector2f(),
                    Color.DARK_GRAY,
                    2000f,
                    5f
            );
            Global.getCombatEngine().spawnExplosion(
                    ship.getLocation(),
                    new Vector2f(),
                    Color.RED,
                    1400f,
                    1.66f
            );
            DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                    1.66f,
                    EXPLOSION_RADIUS,
                    500f,
                    4000f,
                    750f,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    7f,
                    14f,
                    1.66f,
                    250,
                    new Color(255, 200, 30, 100),
                    new Color(255, 30, 30, 0)
            );
            Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, ship.getLocation(), false);
            Global.getSoundPlayer().playSound("explosion_from_damage", 1f, 1f, ship.getLocation(), ship.getVelocity());
            Global.getSoundPlayer().playSound("system_orion_device_explosion", 1f, 1f, ship.getLocation(), ship.getVelocity());

            if (KOL_ModPlugin.hasGraphicsLib) {
                RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f());
                ripple.setSize(700f);
                ripple.setIntensity(100f);
                ripple.setFrameRate(60f);
                ripple.fadeInSize(0.2f);
                ripple.fadeOutIntensity(1.5f);
                DistortionShader.addDistortion(ripple);

                StandardLight light = new StandardLight(ship.getLocation(), new Vector2f(), new Vector2f(), null);
                light.setSize(800f);
                light.setIntensity(16f);
                light.setLifetime(0.66f);
                light.setAutoFadeOutTime(0.5f);
                light.setColor(new Color(255, 125, 25, 255));
                LightShader.addLight(light);
            }
            doBoom = false;
        }
        if (state.equals(State.OUT) || state.equals(State.IDLE)) doBoom = true;
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.getCustomData().remove(id + "drone");
    }
}
