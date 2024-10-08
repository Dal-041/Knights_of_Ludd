package org.selkie.zea.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.selkie.zea.helpers.ZeaStaticStrings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SupernovaStats extends BaseShipSystemScript {
    public static final float EXPLOSION_RADIUS = 1000f;
    protected static final ShipSystemSpecAPI entry = Global.getSettings().getShipSystemSpec(ZeaStaticStrings.systemIDSupernova);
    private static final float IN_STATE_DURATION = entry.getIn(); //2f;
    private static final float ACTIVE_STATE_DURATION = entry.getActive(); //4f;
    private static final String SUPERNOVA = "SUPERNOVA";
    private static final List<String> INFERNO_CANNON_IDS = new ArrayList<>();

    //private static final float assessmentArc = 35f; //Determined by weapon mount
    //private static final float assessmentRange = 1500f;
    private static final float assessmentThreshold = 5f;
    public static final HashMap<ShipAPI.HullSize, Float> scoresHull = new HashMap<>();
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
    }

    /*
    public void tempHolding(){
        ShipAPI ship = null;
        String id = "temp";
        float supernovaInActiveTime = 1f;
        float effectLevel = 0.5f;

        // lidar force fire
        for (WeaponAPI lidar : getLidars(ship)) {
            lidar.setForceFireOneFrame(true);
        }

        // targeting ring
        float jitterLevel = supernovaInActiveTime / (IN_STATE_DURATION + ACTIVE_STATE_DURATION);
        ship.setJitter(id, new Color(1f, 1f, 0f, MathUtils.clamp(jitterLevel, 0f, 1f)), effectLevel, 4, 5f + 10f * jitterLevel);
        ship.setJitterUnder(id, new Color(1f, 0f, 0f, MathUtils.clamp(jitterLevel, 0f, 1f)), effectLevel, 20, 6f + 20f * jitterLevel);

        MagicRender.singleframe(
                Global.getSettings().getSprite("fx", "zea_nian_targetingRing"),
                ship.getLocation(), //location
                new Vector2f(1200, 1200), //size
                ship.getFacing(), //angle
                new Color(255, 100, 0, 128),
                true, //additive
                CombatEngineLayers.UNDER_SHIPS_LAYER
        );

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
    }
    */

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

    public static class SupernovaData {
        private boolean finished = false;
        private boolean superNova = false;
        private float targetingAngle = 0f;
        private float supernovaInActiveTime = 0f;
        private final List<WeaponAPI> firedWeapons = new ArrayList<>();

        private SupernovaData() {
        }
    }
}
