package org.selkie.kol.impl.shipsystems;

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
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lazywizard.lazylib.combat.WeaponUtils.getEnemiesInArc;

public class SupernovaStats extends BaseShipSystemScript {
    protected static ShipSystemSpecAPI entry = Global.getSettings().getShipSystemSpec(ZeaUtils.systemIDSupernova);
    private static final float IN_STATE_DURATION = entry.getIn(); //2f;
    private static final float ACTIVE_STATE_DURATION = entry.getActive(); //4f;
    private static final String SUPERNOVA = "SUPERNOVA";
    private static final String FIRED_INFERNO_CANNON_IDS = "FIRED_INFERNO_CANNON_IDS";
    private static final List<String> INFERNO_CANNON_IDS = new ArrayList<>();

    //private static final float assessmentArc = 35f; //Determined by weapon mount
    //private static final float assessmentRange = 1500f;
    private static final float assessmentThreshold = 5f;
    public static HashMap<ShipAPI.HullSize, Float> scoresHull = new HashMap<>();
    static {
        scoresHull.put(ShipAPI.HullSize.FIGHTER, 1f);
        scoresHull.put(ShipAPI.HullSize.FRIGATE, 3f);
        scoresHull.put(ShipAPI.HullSize.DESTROYER, 5f);
        scoresHull.put(ShipAPI.HullSize.CRUISER, 8f);
        scoresHull.put(ShipAPI.HullSize.CAPITAL_SHIP, 100f);
        scoresHull.put(ShipAPI.HullSize.DEFAULT, 0f);
    }

    static {
        INFERNO_CANNON_IDS.add("zea_nian_maingun_l");
        INFERNO_CANNON_IDS.add("zea_nian_maingun_r");
    }

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        ShipAPI ship = (ShipAPI) stats.getEntity();
        SupernovaData data = getSupernovaData(ship);

        if (data == null) {
            data = new SupernovaData();
            ship.setCustomData(SUPERNOVA, data);
        } else if (state == State.IN && data.finished) {
            data.finished = false;
            data.superNova = false;
        }

        if (!data.finished) {
            if (!data.superNova) {
                if (state == State.IN) {
                    if (effectLevel >= 0.25f) {
                        float scorePotential = 0f;
                        WeaponAPI lidarWep = null;
                        for (WeaponAPI wep : ship.getAllWeapons()) {
                            if (wep.getSpec().getWeaponId().equals("zea_dawn_targeting_beam_wpn")) lidarWep = wep;
                        }
                        if (lidarWep == null) {
                            data.superNova = true;
                            return;
                        }
                        ship.getMutableStats().getBeamWeaponRangeBonus().modifyMult("nian_gun_assessment", 2f);
                        for (ShipAPI enemy: getEnemiesInArc(lidarWep)) {
                            scorePotential += scoresHull.get(enemy.getHullSize());
                        }
                        ship.getMutableStats().getBeamWeaponRangeBonus().unmodify("nian_gun_assessment");
                        if (scorePotential < assessmentThreshold) {
                            data.superNova = true;
                            return;
                        }
                    }

                    for (WeaponAPI lidar : getLidars(ship)) {
                        lidar.setForceFireOneFrame(true);
                    }
                }

                if (state == State.ACTIVE) {
                    WeaponAPI infernoCannon = getInfernoCannon(ship, data);
                    infernoCannon.setForceNoFireOneFrame(false);
                    infernoCannon.setForceFireOneFrame(true);

                    data.firedWeapons.add(infernoCannon);
                    data.finished = true;
                }
            } else {
                if (state == State.IN || state == State.ACTIVE) {
                    data.supernovaInActiveTime += amount;
                    float jitterLevel = data.supernovaInActiveTime / (IN_STATE_DURATION + ACTIVE_STATE_DURATION);
                    ship.setJitter(id, new Color(1f, 1f, 0f, MathUtils.clamp(jitterLevel, 0f, 1f)), effectLevel, 4, 5f + 10f * jitterLevel);
                    ship.setJitterUnder(id, new Color(1f, 0f, 0f, MathUtils.clamp(jitterLevel, 0f, 1f)), effectLevel, 20, 6f + 20f * jitterLevel);

                    data.targetingAngle += amount * (5f + jitterLevel * 20f);
                    MagicRender.singleframe(
                            Global.getSettings().getSprite("fx", "zea_nian_targetingRing"),
                            ship.getLocation(), //location
                            new Vector2f(1200, 1200), //size
                            data.targetingAngle, //angle
                            new Color(255, 100, 0, 128),
                            true, //additive
                            CombatEngineLayers.UNDER_SHIPS_LAYER
                    );
                }

                if (state == State.OUT) {
                    //boom
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
                            1000f,
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
                    Global.getSoundPlayer().playSound("explosion_from_damage", 1f, 1f, ship.getLocation(), new Vector2f());
                    Global.getSoundPlayer().playSound("system_orion_device_explosion", 1f, 1f, ship.getLocation(), new Vector2f());

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

                    data.finished = true;
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.getCustomData().remove(id + "drone");
    }

    public static WeaponAPI getInfernoCannon(ShipAPI ship, SupernovaData data) {
        List<WeaponAPI> cannons = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (INFERNO_CANNON_IDS.contains(weapon.getId())) {
                cannons.add(weapon);
            }
        }

        WeaponAPI cannonToFire = cannons.get(0);
        List<WeaponAPI> firedCannons = data.firedWeapons;
        if (firedCannons.size() == cannons.size()) {
            firedCannons.clear();
        } else {
            cannons.removeAll(firedCannons);
            cannonToFire = cannons.get(0);
        }

        return cannonToFire;
    }

    public static List<WeaponAPI> getLidars(ShipAPI ship) {
        List<WeaponAPI> lidars = new ArrayList<>();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
                lidars.add(w);
            }
        }
        return lidars;
    }

    public static SupernovaData getSupernovaData(ShipAPI ship) {
        if (ship.getCustomData().containsKey(SUPERNOVA)) {
            return ((SupernovaData) ship.getCustomData().get(SUPERNOVA));
        }
        return null;
    }

    public class SupernovaData {
        private boolean finished = false;
        private boolean superNova = false;
        private float targetingAngle = 0f;
        private float supernovaInActiveTime = 0f;
        private List<WeaponAPI> firedWeapons = new ArrayList<>();

        private SupernovaData() {
        }
    }
}
