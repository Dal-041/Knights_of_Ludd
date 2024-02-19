package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;
import org.selkie.kol.impl.combat.SparkleAIScript;

import java.awt.*;
import java.util.List;
import java.util.*;

public class SparkleHullMod extends BaseHullMod {
    private Color jitterColor = new Color(100, 165, 255, 175);
    private Color droneEMPColor = new Color(100, 165, 255, 255);

    private static Color hfColor = new Color(255, 80, 100, 225);
    private static Color hfEMPColor = new Color(255, 80, 100, 255);

    public static final String NINAYA = "zea_boss_ninaya";
    public static final String NINMAH = "zea_boss_ninmah";
    public static final String NINEVEH = "zea_boss_nineveh";
    public static final String HFCOOLDOWNTAG = "HF_SPARKLE";

    public static float shipDamageReg = 1f;
    public static float shipDamageHF = 300f;

    private static final Map<ShipAPI.HullSize, Integer> maxDrones = new HashMap<>();

    static {
        maxDrones.put(ShipAPI.HullSize.FIGHTER, 2);
        maxDrones.put(ShipAPI.HullSize.FRIGATE, 6);
        maxDrones.put(ShipAPI.HullSize.DESTROYER, 9);
        maxDrones.put(ShipAPI.HullSize.CRUISER, 12);
        maxDrones.put(ShipAPI.HullSize.CAPITAL_SHIP, 20);
    }

    private static String WEAPON = "zea_dusk_sparkler_wpn";
    private static final String TEST_DATA_KEY = "_test_AI_shared";

    public static float FLUX_THRESHOLD_INCREASE_PERCENT = 150f; //Adjusts high flux target, not min flux threshold. Values > 100 allowed and effective

    private float antiFighterDamage = 100f;
    public static float MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD = 400f;
    public static float MAX_DIST_FROM_ATTRACTOR_TO_ENGAGE_AS_PD = MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD;
    private boolean findNewTargetOnUse;

    protected IntervalUtil launchInterval = new IntervalUtil(1.25f, 1.75f); //(0.75f, 1.25f)
    protected IntervalUtil HFGraceInterval = new IntervalUtil(5f, 5f);
    protected boolean madeHF = false;
    protected boolean madeNormal = false;
    protected IntervalUtil attractorParticleInterval = new IntervalUtil(0.05f, 0.1f);
    protected WeightedRandomPicker<WeaponSlotAPI> launchSlots = new WeightedRandomPicker<WeaponSlotAPI>();
    protected WeaponSlotAPI lock = null;

    public static class DroneData {
        public Color droneJitterColor;
        public Color droneEmpColor;
        public int maxDrones;
        public float test_antiFighterDamage;
        public String test_impactSound;
        public String test_loopSound;
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).modifyPercent(id, FLUX_THRESHOLD_INCREASE_PERCENT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("adaptive_coils"))
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "adaptive_coils", "kol_ea_sparkle");
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);

        ShipAPI.HullSize hullSize = ship.getHullSize();
        SharedAIData data = getSharedData(ship);
        data.elapsed += amount;

        if (!ship.isAlive()) {
            for (MissileAPI drone : data.drones) {
                drone.explode();
                Global.getCombatEngine().removeEntity(drone);
            }
            data.droneTarget = null;
            data.targetLock = null;
            data.lockRemaining = 0;
            data.drones.clear();
            return;
        }

        int maxMotes = (int) maxDrones.get(hullSize);

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

        CombatEngineAPI engine = Global.getCombatEngine();

        launchInterval.advance(amount * 3f);

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

            if (data.drones.size() < maxMotes && data.targetLock == null &&// false &&
                    !ship.getFluxTracker().isOverloadedOrVenting() &&
                    !ship.isPhased()) {
                findDroneSlots(ship);

                WeaponSlotAPI slot = launchSlots.pick();

                Vector2f loc = slot.computePosition(ship);
                float dir = slot.computeMidArcAngle(ship);
                float arc = slot.getArc();
                dir += arc * (float) Math.random() - arc / 2f;

                String weaponId = WEAPON;
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


        float fraction = data.drones.size() / (Math.max(1f, maxMotes));
        float volume = fraction * 3f;
        if (volume > 1f) volume = 1f;
        if (data.drones.size() > 3) {
            Vector2f com = new Vector2f();
            for (MissileAPI mote : data.drones) {
                Vector2f.add(com, mote.getLocation(), com);
            }
            com.scale(1f / data.drones.size());
        }

        HFGraceInterval.advance(amount);
        if (ship.getCustomData().containsKey("HF_SPARKLE_BOSS") || (!madeHF && makeHFSparkles(ship)) || (madeHF && !HFGraceInterval.intervalElapsed())) { //make mean
            for (int i = 0; i < data.drones.size(); i++) {
                MissileAPI orig = data.drones.get(i);
                // NEW PLAN: set a custom tag for the onHit to pick up
                //orig.setNoGlowTime(0.01f); //One frame,ish
                orig.setJitter(this, hfColor, (HFGraceInterval.getElapsed() / (HFGraceInterval.getMaxInterval() / 3)) * 3, 1, orig.getGlowRadius());
                orig.getEngineController().fadeToOtherColor(this, hfColor, hfColor, 2f, 0.75f);
            }
            if (makeHFSparkles(ship)) {
                HFGraceInterval.setElapsed(0f);
                ship.setCustomData("HF_SPARKLE", true);
            }
            madeHF = true;
        } else {
            ship.setCustomData("HF_SPARKLE", false);
        }
        if (madeHF && HFGraceInterval.intervalElapsed() && !ship.getCustomData().containsKey("HF_SPARKLE_BOSS")) { //make chill
            for (int i = 0; i < data.drones.size(); i++) {
                MissileAPI orig = data.drones.get(i);
                //orig.setNoGlowTime(0.01f); //One frame,ish
                //orig.setJitter(this, jitterColor, 1f, 1, orig.getGlowRadius());
                orig.getEngineController().fadeToOtherColor(this, jitterColor, hfColor, 1f, 1f);
            }
            madeHF = false;
            ship.removeCustomData("HF_SPARKLE");
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    protected boolean makeHFSparkles(ShipAPI ship) {
        return ship.getSystem().isActive()
                && (ship.getHullSpec().getBaseHullId().equals(NINMAH)
                || ship.getHullSpec().getBaseHullId().equals(NINAYA)
                || ship.getHullSpec().getBaseHullId().equals(NINEVEH));
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

    public static SharedAIData getSharedData(ShipAPI source) {
        String key = source + TEST_DATA_KEY;
        SharedAIData data = (SharedAIData) Global.getCombatEngine().getCustomData().get(key);
        if (data == null) {
            data = new SharedAIData();
            Global.getCombatEngine().getCustomData().put(key, data);
        }
        return data;
    }

    public static class SharedAIData {
        public float elapsed = 0f;

        public List<MissileAPI> drones = new ArrayList<MissileAPI>();

        public float lockRemaining = 0f;

        public Vector2f droneTarget = null;

        public ShipAPI targetLock = null;
    }

    public void calculateDroneTargetData(ShipAPI ship) {
        SharedAIData data = getSharedData(ship);
        Vector2f targetLoc = getTargetLoc(ship);
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

    public Vector2f getTargetLoc(ShipAPI from) {
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
