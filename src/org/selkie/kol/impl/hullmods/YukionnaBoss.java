package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.util.MagicRender;
import org.selkie.kol.Utils;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.impl.combat.activators.BallLightningActivator;
import org.selkie.kol.impl.combat.activators.BlizzardActivator;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.awt.*;
import java.util.EnumSet;

public class YukionnaBoss extends BaseHullMod {
    public static class YukionnaBossPhaseTwoScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        private static final Color PHASE_COLOR = new Color(70, 90, 240, 230);
        public static final float DAMAGE_BONUS_PERCENT = 50f;
        public boolean phaseTwo = false;
        public CombatEngineAPI engine;
        public String id = "boss_phase_two_modifier";
        public static float SHIP_ALPHA_MULT = 0.25f;
        public float phaseTwoTimer = 0f;
        public static final float MAX_TIME = 8f;
        public ShipAPI ship;

        public YukionnaBossPhaseTwoScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            float hull = ship.getHitpoints();
            if (damageAmount >= hull && !phaseTwo) {
                phaseTwo = true;
                ship.setHitpoints(1f);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.000001f);
                ship.getMutableStats().getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_PERCENT);
                ship.setCustomData("HF_SPARKLE_BOSS", true);
                for (ShipAPI other : AIUtils.getAlliesOnMap(ship)) {
                    other.setCustomData("HF_SPARKLE_BOSS", true);
                }
                ship.setWeaponGlow(1f, Misc.setAlpha(PHASE_COLOR, 255), EnumSet.of(WeaponAPI.WeaponType.ENERGY));
                if (!ship.isPhased()) {
                    Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                }
                ship.getMutableStats().getPeakCRDuration().modifyFlat(id, ship.getHullSpec().getNoCRLossSeconds());
                Utils.shipSpawnExplosion(ship.getShieldRadiusEvenIfNoShield(), ship.getLocation());
                return true;
            } else if (phaseTwo && phaseTwoTimer < MAX_TIME) {
                return true;
            }
            return false;
        }

        @Override
        public void advance(float amount) {
            engine = Global.getCombatEngine();
            float armorRegen = 0.8f;
            float hpRegen = 0.6f;

            if (phaseTwo && phaseTwoTimer < MAX_TIME) {

                phaseTwoTimer += amount;

                if (phaseTwoTimer > MAX_TIME) {
                    ship.setPhased(false);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    Global.getSoundPlayer().playSound("system_phase_cloak_deactivate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    return;
                }

                // force phase, mitigate damage, regen hp/armor, vent flux, reset ppt/ cr

                ship.setPhased(true);
                float timeIn = 0.5f;
                float timeOut = 0.5f;
                float effectLevel = phaseTwoTimer < timeIn ? (phaseTwoTimer / timeIn) : (phaseTwoTimer > (MAX_TIME - timeOut) ? (MAX_TIME - phaseTwoTimer) / timeOut : 1f);

                ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);
                ship.setApplyExtraAlphaToEngines(true);

                Color colorToUse = new Color(((float) PHASE_COLOR.getRed() / 255f), ((float) PHASE_COLOR.getGreen() / 255f), ((float) PHASE_COLOR.getBlue() / 255f), ((float) PHASE_COLOR.getAlpha() / 255f) * effectLevel);
                Vector2f jitterLocation = MathUtils.getRandomPointInCircle(ship.getLocation(), 2f + (1 - effectLevel) * 5f);
                SpriteAPI glow1 = Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow1");
                SpriteAPI glow2 = Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow2");
                MagicRender.singleframe(glow1, ship.getLocation(), new Vector2f(glow1.getWidth(), glow1.getHeight()), ship.getFacing() - 90f, colorToUse, true);
                MagicRender.singleframe(glow2, jitterLocation, new Vector2f(glow2.getWidth(), glow2.getHeight()), ship.getFacing() - 90f, colorToUse, true);

                ship.getFluxTracker().setHardFlux(0f);
                ship.setHitpoints(Misc.interpolate(1f, ship.getMaxHitpoints() * hpRegen, phaseTwoTimer / MAX_TIME));
                ArmorGridAPI armorGrid = ship.getArmorGrid();

                for (int i = 0; i < armorGrid.getGrid().length; i++) {
                    for (int j = 0; j < armorGrid.getGrid()[0].length; j++) {
                        if (armorGrid.getArmorValue(i, j) < armorGrid.getMaxArmorInCell() * armorRegen)
                            armorGrid.setArmorValue(i, j, Misc.interpolate(armorGrid.getArmorValue(i, j), armorGrid.getMaxArmorInCell() * armorRegen, phaseTwoTimer / MAX_TIME));
                    }
                }
                StarficzAIUtils.stayStill(ship);
                ship.getShield().toggleOff();
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
            }
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MagicSubsystemsManager.addSubsystemToShip(ship, new BallLightningActivator(ship));
        boolean isBoss = ship.getVariant().hasTag(ZeaUtils.BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().contains(ZeaUtils.BOSS_TAG))));

        if (isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            if (!ship.hasListenerOfClass(YukionnaBossPhaseTwoScript.class))
                ship.addListener(new YukionnaBossPhaseTwoScript(ship));
            MagicSubsystemsManager.addSubsystemToShip(ship, new BlizzardActivator(ship));
            String key = "phaseAnchor_canDive";
            Global.getCombatEngine().getCustomData().put(key, true); // disable phase dive, as listener conflicts with phase two script
            //for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) { member.getVariant().addTag(ZeaUtils.BOSS_TAG); }
        }
    }
}
