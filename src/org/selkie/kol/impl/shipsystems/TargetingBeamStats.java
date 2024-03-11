package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.EntropyAmplifierStats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetingBeamStats extends BaseShipSystemScript {
    public static String LIDAR_WINDUP = "lidar_windup";
    public WeaponAPI lidar;
    protected boolean needsUnapply = false;
    protected boolean playedWindup = false;
    protected float lidarMaxRange = 1000f;
    protected boolean inited = false;
    public static Object KEY_SHIP = new Object();
    public static Object KEY_TARGET = new Object();
    public static float DAM_MULT = 1.5f;
    public static Color TEXT_COLOR = new Color(255,55,55,255);
    public static Color JITTER_COLOR = new Color(255,50,50,75);
    public float lidarMinRange = 700f;


    public void init(ShipAPI ship) {
        if (inited) return;
        inited = true;
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
                lidar = w;
                lidarMaxRange = w.getRange();
            }
        }
    }

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        if (ship == null || ship.isHulk()) {
            if (needsUnapply) {
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (!w.isDecorative() && w.getSlot().isHardpoint() && !w.isBeam() &&
                            (w.getType() == WeaponAPI.WeaponType.BALLISTIC || w.getType() == WeaponAPI.WeaponType.ENERGY)) {
                        w.setGlowAmount(0, null);
                    }
                }
                needsUnapply = false;
            }
            return;
        }

        init(ship);

        boolean active = state == State.IN || state == State.ACTIVE || state == State.OUT;

        if (active) {
            needsUnapply = true;
        } else {
            if (needsUnapply) {
                playedWindup = false;
                for (WeaponAPI w : ship.getAllWeapons()) {
                    if (w.getSlot().isSystemSlot()) continue;
                    if (!w.isDecorative() && w.getSlot().isHardpoint() && !w.isBeam() &&
                            (w.getType() == WeaponAPI.WeaponType.BALLISTIC || w.getType() == WeaponAPI.WeaponType.ENERGY)) {
                        w.setGlowAmount(0, null);
                    }
                }
                needsUnapply = false;
            }
        }

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().isSystemSlot()) continue;
            if (w.getType() == WeaponAPI.WeaponType.MISSILE) continue;
            if (!(state == State.IN) && (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR))) {
                w.setForceNoFireOneFrame(true);
            }
        }

        // always wait a quarter of a second before starting to fire the targeting lasers
        // this is the worst-case turn time required for the dishes to face front
        // doing this to keep the timing of the lidar ping sounds consistent relative
        // to when the windup sound plays
        float fireThreshold = 0.25f / 3.25f;
        fireThreshold += 0.02f; // making sure there's only 4 lidar pings; lines up with the timing of the lidardish weapon

        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().isSystemSlot()) continue;
            if (w.getType() == WeaponAPI.WeaponType.MISSILE) continue;
            if (state == State.IN && effectLevel >= fireThreshold) {
                if ((w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR))) {
                    w.setForceFireOneFrame(true);
                }
            }
        }

        if (((state == State.IN && effectLevel > 0.67f) || state == State.ACTIVE) && !playedWindup) {
            Global.getSoundPlayer().playSound(LIDAR_WINDUP, 1f, 1f, ship.getLocation(), ship.getVelocity());
            playedWindup = true;
        }


        final String targetDataKey = ship.getId() + "_entropy_target_data";

        Object targetDataObj = Global.getCombatEngine().getCustomData().get(targetDataKey);
        if (state == State.ACTIVE && targetDataObj == null) {
            ShipAPI target = findTarget(ship);
            Global.getCombatEngine().getCustomData().put(targetDataKey, new EntropyAmplifierStats.TargetData(ship, target));
            if (target != null) {
                float targetRange = MathUtils.getDistance(ship, target);
                if (targetRange < lidarMinRange){
                    DAM_MULT = 2f;
                } else {
                    DAM_MULT = Misc.interpolate(2f,1f, (targetRange - lidarMinRange)/(lidarMaxRange - lidarMinRange));
                }

                if (target.getFluxTracker().showFloaty() ||
                        ship == Global.getCombatEngine().getPlayerShip() ||
                        target == Global.getCombatEngine().getPlayerShip()) {
                    target.getFluxTracker().showOverloadFloatyIfNeeded("Lidar Locked!", TEXT_COLOR, 4f, true);
                }
            }
        } else if (state == State.IDLE && targetDataObj != null) {
            Global.getCombatEngine().getCustomData().remove(targetDataKey);
            ((EntropyAmplifierStats.TargetData)targetDataObj).currDamMult = 1f;
            targetDataObj = null;
        }
        if (targetDataObj == null || ((EntropyAmplifierStats.TargetData) targetDataObj).target == null) return;
        final EntropyAmplifierStats.TargetData targetData = (EntropyAmplifierStats.TargetData) targetDataObj;
        targetData.currDamMult = 1f + (DAM_MULT - 1f) * effectLevel;
        if (targetData.targetEffectPlugin == null) {
            targetData.targetEffectPlugin = new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (Global.getCombatEngine().isPaused()) return;
                    if (targetData.target == Global.getCombatEngine().getPlayerShip()) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(KEY_TARGET,
                                targetData.ship.getSystem().getSpecAPI().getIconSpriteName(),
                                targetData.ship.getSystem().getDisplayName(),
                                "" + (int)((targetData.currDamMult - 1f) * 100f) + "% more damage taken", true);
                    }

                    if (targetData.currDamMult <= 1f || !targetData.ship.isAlive()) {
                        targetData.target.getMutableStats().getHullDamageTakenMult().unmodify(id);
                        targetData.target.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                        targetData.target.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                        targetData.target.getMutableStats().getEmpDamageTakenMult().unmodify(id);
                        Global.getCombatEngine().removePlugin(targetData.targetEffectPlugin);
                    } else {
                        targetData.target.getMutableStats().getHullDamageTakenMult().modifyMult(id, targetData.currDamMult);
                        targetData.target.getMutableStats().getArmorDamageTakenMult().modifyMult(id, targetData.currDamMult);
                        targetData.target.getMutableStats().getShieldDamageTakenMult().modifyMult(id, targetData.currDamMult);
                        targetData.target.getMutableStats().getEmpDamageTakenMult().modifyMult(id, targetData.currDamMult);
                    }
                }
            };
            Global.getCombatEngine().addPlugin(targetData.targetEffectPlugin);
        }

        if (effectLevel > 0) {
            if (state != State.IN) {
                targetData.elaspedAfterInState += Global.getCombatEngine().getElapsedInLastFrame();
            }
            float shipJitterLevel = 0;
            if (state != State.IN) {
                float durOut = 0.5f;
                shipJitterLevel = Math.max(0, durOut - targetData.elaspedAfterInState) / durOut;
            }
            float targetJitterLevel = effectLevel;

            float maxRangeBonus = 50f;
            float jitterRangeBonus = shipJitterLevel * maxRangeBonus;

            Color color = JITTER_COLOR;
            if (shipJitterLevel > 0) {
                ship.setJitter(KEY_SHIP, color, shipJitterLevel, 4, 0f, 0 + jitterRangeBonus * 1f);
            }

            if (targetJitterLevel > 0) {
                targetData.target.setJitter(KEY_TARGET, color, targetJitterLevel, 3, 0f, 5f);
            }
        }
    }

    protected ShipAPI findTarget(ShipAPI ship) {
        float range = lidarMaxRange;
        ArrayList<ShipAPI> targets = new ArrayList<>();
        for(ShipAPI other : AIUtils.getNearbyEnemies(ship, range)){
            Vector2f closestPoint = MathUtils.getNearestPointOnLine(other.getLocation(), ship.getLocation(), MathUtils.getPointOnCircumference(ship.getLocation(), range, ship.getFacing()));
            float otherDistance = Misc.getTargetingRadius(closestPoint, other, other.getShield() != null && other.getShield().isOn());
            if (MathUtils.getDistanceSquared(closestPoint, other.getLocation()) < otherDistance*otherDistance){
                targets.add(other);
            }
        }

        float minDistance = range;
        ShipAPI target = null;
        for(ShipAPI other : targets){
            float otherDistance = MathUtils.getDistanceSquared(other.getLocation(), ship.getLocation());
            if(otherDistance < minDistance*minDistance){
                minDistance = otherDistance;
                target = other;
            }
        }
        return target;
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        // never called due to runScriptWhileIdle:true in the .system file
    }

}
