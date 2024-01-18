package org.selkie.kol.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.combat.MoteControlScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class SparkleControlScript extends MoteControlScript {

    private static Color baseColor = new Color(100,165,255,175);
    private static Color baseEMPColor = new Color(100,165,255,255);

    private static Color hfColor = new Color(220,80,100,225);
    private static Color hfEMPColor = new Color(255,80,100,255);

    public static final String NINAYA = "zea_boss_ninaya";
    public static final String NINMAH = "zea_boss_ninmah";
    public static final String HFCOOLDOWNTAG = "HF_SPARKLE";

    public boolean useAttractor = false;

    private final int maxDrones = 8;

    private final float antiFighterDamage = 100f;

    public static float MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD = 600f;
    public static float MAX_DIST_FROM_ATTRACTOR_TO_ENGAGE_AS_PD = 600f;

    protected IntervalUtil launchInterval = new IntervalUtil(1.25f, 1.75f); //(0.75f, 1.25f)
    protected IntervalUtil attractorParticleInterval = new IntervalUtil(0.05f, 0.1f);
    protected IntervalUtil HFGraceInterval = new IntervalUtil(1f, 1f);
    protected WeightedRandomPicker<WeaponSlotAPI> launchSlots = new WeightedRandomPicker<WeaponSlotAPI>();
    protected WeaponSlotAPI lock = null;

    public static float ANTI_FIGHTER_DAMAGE = 100f; //half
    public static float ANTI_FIGHTER_DAMAGE_HF = 1000f;
    public static float ANTI_SHIP_DAMAGE = 1f;
    public static float ANTI_SHIP_DAMAGE_HF = 300f;
    public static float EMP_DAMAGE = 300f;
    public static float EMP_DAMAGE_HF = 300f;

    public static Map<String, MoteData> MOTE_DATA = new HashMap<String, MoteData>();

    public static boolean isHighFrequency(ShipAPI ship) {
        //if (true) return true;
        return ship != null && ship.getVariant().hasHullMod(HullMods.HIGH_FREQUENCY_ATTRACTOR); //override with some fancy conditions
    }

    static {
        MoteData normal = new MoteData();
        normal.jitterColor = baseColor;
        normal.empColor = baseEMPColor;
        normal.maxMotes = MAX_MOTES;
        normal.antiFighterDamage = ANTI_FIGHTER_DAMAGE;
        normal.impactSound = "mote_attractor_impact_normal";
        normal.loopSound = "mote_attractor_loop";

        MOTE_DATA.put(MOTELAUNCHER, normal);

        MoteData hf = new MoteData();
        hf.jitterColor = hfColor;
        hf.empColor = hfEMPColor;
        hf.maxMotes = MAX_MOTES_HF;
        hf.antiFighterDamage = ANTI_FIGHTER_DAMAGE_HF;
        hf.impactSound = "mote_attractor_impact_damage";
        hf.loopSound = "mote_attractor_loop_dark";

        MOTE_DATA.put(MOTELAUNCHER_HF, hf);
    }

    public static float getAntiFighterDamage(ShipAPI ship) {
        if (useHFdrone(ship)) return ANTI_FIGHTER_DAMAGE_HF;
        return ANTI_FIGHTER_DAMAGE;
    }
    public static String getImpactSoundId(ShipAPI ship) {
        if (useHFdrone(ship)) return "mote_attractor_impact_damage";
        return "mote_attractor_impact_normal";
    }
    public static Color getJitterColor(ShipAPI ship) {
        if (useHFdrone(ship)) return hfColor;
        return hfEMPColor;
    }
    public static Color getEMPColor(ShipAPI ship) {
        if (useHFdrone(ship)) return baseEMPColor;
        return hfEMPColor;
    }
    public static float getAntiShipDamage(ShipAPI ship) {
        if (useHFdrone(ship)) return ANTI_SHIP_DAMAGE_HF;
        return ANTI_SHIP_DAMAGE;
    }
    public static float getAntiShipEMP(ShipAPI ship) {
        if (useHFdrone(ship)) return EMP_DAMAGE_HF;
        return EMP_DAMAGE;
    }

    public static boolean useHFdrone(ShipAPI ship) {
        if ((ship.getHullSpec().getBaseHullId().equals(NINAYA) && ship.getSystem().isCoolingDown())
        || (ship.getHullSpec().getBaseHullId().equals(NINMAH) && ship.getSystem().isCoolingDown())) return true;
        return false;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        float amount = Global.getCombatEngine().getElapsedInLastFrame();


        SparkleControlScript.SharedSparkleAIData data = getDroneSharedData(ship);
        data.elapsed += amount;

        if (data.lockRemaining > 0) {
            data.lockRemaining -= amount;
            if (data.lockRemaining <= 0 ||
                    (data.targetLock != null && !data.targetLock.isAlive()) ||
                    data.drones.isEmpty()) {
                data.droneTarget = null;
                data.targetLock = null;
                data.lockRemaining = 0;
            }
        }
        if (effectLevel <= 0) {
            findNewTargetOnUse = true;
        }

        CombatEngineAPI engine = Global.getCombatEngine();

        attractorParticleInterval.advance(amount);
        if (attractorParticleInterval.intervalElapsed()) {
            //spawnAttractorParticles(ship);
        }

        launchInterval.advance(amount * 5f);
        if (launchInterval.intervalElapsed()) {
            Iterator<MissileAPI> iter = data.drones.iterator();
            while (iter.hasNext()) {
                if (!engine.isMissileAlive(iter.next())) {
                    iter.remove();
                }
            }
            /*
            if (ship.isHulk()) {
                for (MissileAPI mote : data.drones) {
                    mote.flameOut();
                }
                data.drones.clear();
                return;
            }
            */

            if (data.drones.size() < maxDrones && data.targetLock == null &&// false &&
                    !ship.getFluxTracker().isOverloadedOrVenting() &&
                    !ship.isPhased()) {
                findDroneSlots(ship);

                WeaponSlotAPI slot = launchSlots.pick();

                Vector2f loc = slot.computePosition(ship);
                float dir = slot.computeMidArcAngle(ship);
                float arc = slot.getArc();
                dir += arc * (float) Math.random() - arc /2f;

                String weaponId = "motelauncher";
                MissileAPI drone = (MissileAPI) engine.spawnProjectile(ship, null,
                        weaponId,
                        loc, dir, null);
                drone.setWeaponSpec(weaponId);
                drone.setMissileAI(new SparkleAIScript(drone));
                drone.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
                // if they could flame out/be affected by emp, that'd be bad since they don't expire for a
                // very long time so they'd be stuck disabled permanently, for practical purposes
                // thus: total emp resistance (which can't target them anyway, but if it did.)
                drone.setEmpResistance(10000);
                data.drones.add(drone);

                engine.spawnMuzzleFlashOrSmoke(ship, slot, drone.getWeaponSpec(), 0, dir);

                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 0.25f, loc, new Vector2f());
            }
        }

        float fraction = data.drones.size() / (Math.max(1f, maxDrones));
        float volume = fraction * 3f;
        if (volume > 1f) volume = 1f;
        if (data.drones.size() > 3) {
            Vector2f com = new Vector2f();
            for (MissileAPI mote : data.drones) {
                Vector2f.add(com, mote.getLocation(), com);
            }
            com.scale(1f / data.drones.size());
        }


        if (effectLevel > 0 && findNewTargetOnUse) {
            calculateDroneTargetData(ship);
            findNewTargetOnUse = false;
        }
        Color temp = new Color(100,165,255,125);
        Color temp2 = new Color(100,165,255,255);

        if (effectLevel == 1) {
            // possible if system is reused immediately w/ no time to cool down, I think
            if (data.droneTarget == null) {
                calculateDroneTargetData(ship);
            }
            findDroneSlots(ship);

            Vector2f slotLoc = lock.computePosition(ship);

            CombatEntityAPI asteroid = engine.spawnAsteroid(0, data.droneTarget.x, data.droneTarget.y, 0, 0);
            asteroid.setCollisionClass(CollisionClass.NONE);
            CombatEntityAPI target = asteroid;
            if (data.targetLock != null) {
                target = data.targetLock;
            }

            float emp = 0;
            float dam = 0;
            EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArc(ship, slotLoc, ship, target,
                    DamageType.ENERGY,
                    dam,
                    emp, // emp
                    100000f, // max range
                    "mote_attractor_targeted_ship",
                    40f, // thickness
                    //new Color(100,165,255,255),
                    baseEMPColor,
                    new Color(255,255,255,255)
            );
            if (data.targetLock != null) {
                arc.setTargetToShipCenter(slotLoc, data.targetLock);
            }
            arc.setCoreWidthOverride(30f);

            engine.removeEntity(asteroid);
        }

        /*
        // HIGH FREQUENCY SPARKLES
        for (int i = 0; i < data.drones.size(); i++) {
            MissileAPI sparkle = data.drones.get(i);
            if (useHFdrone(ship)) {
                HFGraceInterval.advance(amount);
                sparkle.setNoGlowTime(0.1f);
                sparkle.setJitter(this, getJitterColor(ship), 1f, 1, sparkle.getGlowRadius());
                sparkle.getEngineController().fadeToOtherColor(this,getJitterColor(ship),getJitterColor(ship),1f,0.75f);
            }
            if (HFGraceInterval.intervalElapsed()) { //Reset
                HFGraceInterval.forceCurrInterval(0f);
                sparkle.setNoGlowTime(0.01f); //One frame,ish
                sparkle.setJitter(this, getJitterColor(ship), 1f, 1, sparkle.getGlowRadius());
                sparkle.getEngineController().fadeToOtherColor(this,getJitterColor(ship),getJitterColor(ship),1f,0.75f);
            }
        }
         */
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    protected void findDroneSlots(ShipAPI ship) {
        if (lock != null) return;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot.isSystemSlot()) {
                if (slot.getSlotSize() == WeaponSize.SMALL) {
                    launchSlots.add(slot);
                }
                if (slot.getSlotSize() == WeaponSize.MEDIUM) {
                    lock = slot;

                }
            }
        }
    }

    public static SparkleControlScript.SharedSparkleAIData getDroneSharedData(ShipAPI source) {
        String key = source + "_drone_AI_shared";
        SparkleControlScript.SharedSparkleAIData data = (SparkleControlScript.SharedSparkleAIData)Global.getCombatEngine().getCustomData().get(key);
        if (data == null) {
            data = new SparkleControlScript.SharedSparkleAIData();
            Global.getCombatEngine().getCustomData().put(key, data);
        }
        return data;
    }

    public static class SharedSparkleAIData {
        public float elapsed = 0f;

        public List<MissileAPI> drones = new ArrayList<MissileAPI>();

        public float lockRemaining = 0f;

        public Vector2f droneTarget = null;

        public ShipAPI targetLock = null;
    }

    public void calculateDroneTargetData(ShipAPI ship) {
        SharedSparkleAIData data = getDroneSharedData(ship);
        Vector2f targetLoc = getDroneTargetLoc(ship);
        //System.out.println(getTargetedLocation(ship));
        data.targetLock = getDroneLockTarget(ship, targetLoc);

        data.lockRemaining = 10f;
        if (data.targetLock != null) {
            targetLoc = new Vector2f(data.targetLock.getLocation());
            data.lockRemaining = 20f;
        }
        data.droneTarget = targetLoc;

    }

    public Vector2f getDroneTargetedLocation(ShipAPI from) {
        Vector2f loc = from.getSystem().getTargetLoc();
        if (loc == null) {
            loc = new Vector2f(from.getMouseTarget());
        }
        return loc;
    }

    public Vector2f getDroneTargetLoc(ShipAPI from) {
        findDroneSlots(from);

        Vector2f slotLoc = lock.computePosition(from);
        Vector2f targetLoc = new Vector2f(getDroneTargetedLocation(from));
        float dist = Misc.getDistance(slotLoc, targetLoc);
        if (dist > getRangeR(from)) {
            targetLoc = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(slotLoc, targetLoc));
            targetLoc.scale(getRangeR(from));
            Vector2f.add(targetLoc, slotLoc, targetLoc);
        }
        return targetLoc;
    }

    public ShipAPI getDroneLockTarget(ShipAPI from, Vector2f loc) {
        Vector2f slotLoc = lock.computePosition(from);
        for (ShipAPI other : Global.getCombatEngine().getShips()) {
            if (other.isFighter()) continue;
            if (other.getOwner() == from.getOwner()) continue;
            if (other.isHulk()) continue;

            float dist = Misc.getDistance(slotLoc, other.getLocation());
            if (dist > getRangeR(from)) continue;

            dist = Misc.getDistance(loc, other.getLocation());
            if (dist < other.getCollisionRadius() + 50f) {
                return other;
            }
        }
        return null;
    }

    public static float getRangeR(ShipAPI ship) {
        if (ship == null) return 10000f;
        return ship.getMutableStats().getSystemRangeBonus().computeEffective(10000f);
    }
}
