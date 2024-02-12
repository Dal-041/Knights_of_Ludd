package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.*;
import org.selkie.kol.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.selkie.kol.impl.hullmods.CoronalCapacitor.CoronalCapacitorListener.getSystemStellarIntensity;

public class CoronalCapacitor extends BaseHullMod {
    public static final String CAPACITY_FACTOR_KEY = "$cc_cap_modifier";
    public static final String CHARGE_CHECK_KEY = "$cc_check_time";
    public static class CoronalCapacitorListener implements AdvanceableListener {

        private FleetMemberAPI member;
        private ShipAPI ship;
        private CombatEngineAPI engine;
        private HashMap<WeaponAPI, Boolean> fluxCounted = new HashMap<>();
        private List<WeaponAPI> beams = new ArrayList<>();

        // flux or charge based
        private static final boolean USING_FLUX = true;

        //Time based
        public static final float MAX_CHARGETIME = 10f; // in seconds
        public static final float TIME_CHARGERATE = 0.5f; // seconds gained per second
        public static final float CHARGE_DELAY = 1f; // seconds before charge after ship has stopped firing

        //Flux based
        public static final float MAX_CHARGEFLUX = 1.0f; // multiplier on ships max flux
        public static final float FLUX_CHARGERATE = 0.05f; // percentage of bar gained per second

        public static final float MIN_CHARGEMULT = 0.2f; // multiplier of chargerate in no-light conditions
        public static final float MAX_CHARGEMULT = 4f; // multiplier of chargerate in full-light

        public static final float SPEED_BOOST = 0.3f;
        public static final float DAMAGE_BOOST = 0.3f;
        public static final float ROF_BOOST = 0.3f;

        public static HashMap<String,Float> starLux;
        static {
            starLux = new HashMap<>();
            starLux.put("star_orange_giant", 3f);
            starLux.put("star_red_giant", 2.5f);
            starLux.put("star_red_supergiant", 2.5f);
            starLux.put("star_red_dwarf", 2.5f);
            starLux.put("star_browndwarf", 0.25f);
            starLux.put("star_orange", 3f);
            starLux.put("star_yellow", 3f);
            starLux.put("star_white", 2f);
            starLux.put("star_blue_giant", 10f);
            starLux.put("star_blue_supergiant", 10f);
            starLux.put("black_hole", 0.1f);
            starLux.put("star_neutron", 25f);
            starLux.put("nebula_center_old", 0f);
            starLux.put("nebula_center_average", 0f);
            starLux.put("nebula_center_young", 0f);
            starLux.put("zea_star_black_neutron", 0f);
            starLux.put("zea_white_hole", 100f);
            starLux.put("zea_red_hole", 3f);
            starLux.put("US_star_blue_giant", 10f);
            starLux.put("US_star_yellow", 3f);
            starLux.put("US_star_orange_giant", 3f);
            starLux.put("US_star_red_giant", 2.5f);
            starLux.put("US_star_white", 2f);
            starLux.put("US_star_browndwarf", 0.25f);
            starLux.put("tiandong_shaanxi", 2f);
            starLux.put("star_brstar", 3f);
            starLux.put("star_yellow_supergiant", 3f);
            starLux.put("quasar", 8f);
        }

        private static boolean inited = false;
        public static float actual_chargemult = 1f; // resultant charge rate
        private float capacitorFactor = 1f; // 0-1
        private float capacitorAmount = 0f; // Gets verified
        private float chargeTime = 0f;

        private List<Pair<EngineSlotAPI, Pair<Color, Color>>> engines;
        protected Object STATUSKEY1 = new Object();
        protected Object STATUSKEY2 = new Object();
        protected Object STATUSKEY3 = new Object();
        protected Object STATUSKEY4 = new Object();
        IntervalUtil lightInterval = new IntervalUtil(1.5f, 1.5f);
        StandardLight glow = null;

        CoronalCapacitorListener(ShipAPI ship) {
            this.ship = ship;
            engine = Global.getCombatEngine();
            for(WeaponAPI weapon : ship.getAllWeapons()) {
                if(!weapon.isDecorative()) {
                    if (weapon.isBeam() && !weapon.isBurstBeam()) {
                        beams.add(weapon);
                    }
                    else{
                        fluxCounted.put(weapon, true);
                    }
                }
            }
        }

        @Override
        public void advance(float amount) {
            float fluxPerSecond = 0;
            float flatFlux = 0f;
            boolean charging = true;

            if (!ship.isAlive() || ship.isHulk() || engine.isPaused()) return;

            if (engines == null) {
                engines = new ArrayList<>();
                for(ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()){
                    engines.add(new Pair<>(engine.getEngineSlot(), new Pair<>(engine.getEngineSlot().getColor(), engine.getEngineSlot().getGlowAlternateColor())));
                }
            }

            for(WeaponAPI weapon : beams) {
                if(weapon.getChargeLevel() >= 0.2f) { //firing
                    fluxPerSecond += weapon.getFluxCostToFire();
                    if(!weapon.hasAIHint(WeaponAPI.AIHints.PD))
                        charging = false;
                }
            }
            for(WeaponAPI weapon : fluxCounted.keySet()) {
                if(weapon.getChargeLevel() >= 1f) { //firing
                    if(!fluxCounted.get(weapon)) {
                        fluxCounted.put(weapon, true);
                        flatFlux += weapon.getFluxCostToFire();
                        if(!weapon.hasAIHint(WeaponAPI.AIHints.PD))
                            charging = false;
                    }
                } else if(!weapon.isInBurst()) {
                    fluxCounted.put(weapon, false);
                }
            }

            float effectiveChargeRate = actual_chargemult * amount; // TODO BAD CODE STENCH
            if (ship.getFluxTracker().isVenting()) {
                effectiveChargeRate *= ship.getMutableStats().getVentRateMult().getModifiedValue()*2;
            }
            if(!charging) effectiveChargeRate = 0f;

            if(USING_FLUX) {
                float fluxPool = MAX_CHARGEFLUX*ship.getMaxFlux();
                float fluxUsed = flatFlux + (fluxPerSecond*amount);
                capacitorAmount += (fluxUsed*-1) + (fluxPool*effectiveChargeRate*amount);
                capacitorAmount = Math.max(0, (Math.min(capacitorAmount, fluxPool)));
                capacitorFactor = capacitorAmount/fluxPool;
                MagicUI.drawInterfaceStatusBar(ship, capacitorFactor, Misc.getPositiveHighlightColor(), null, 0, "BOOST", Math.round(capacitorFactor*100));
            } else {
                MagicUI.drawInterfaceStatusBar(ship, capacitorFactor, Misc.getPositiveHighlightColor(), null, 0, "BOOST", Math.round(capacitorFactor*MAX_CHARGETIME));
                if(charging) {
                    chargeTime += amount;
                    if(chargeTime >= CHARGE_DELAY)
                        capacitorFactor += amount/MAX_CHARGETIME*TIME_CHARGERATE;
                } else {
                    chargeTime = 0;
                    capacitorFactor -= amount/MAX_CHARGETIME;
                }
            }

            capacitorFactor = Math.max(0, Math.min(1, capacitorFactor));
            for(Pair<EngineSlotAPI, Pair<Color, Color>> engineData : engines){
                engineData.one.setColor(Utils.OKLabInterpolateColor(engineData.two.one,new Color(235, 165,20, 150), capacitorFactor));
                if (engineData.two.two != null) engineData.one.setGlowAlternateColor(Utils.OKLabInterpolateColor(engineData.two.two, new Color(215, 155,55, 100), capacitorFactor));
            }

            ship.setJitterUnder("coronal_cap" + ship.getId(), new Color(235, 165,20, 100),
                    Utils.linMap(0,0.6f,0.5f,1, capacitorFactor), 3, 0f, 15);



            /*
            lightInterval.advance(amount);

            if(lightInterval.intervalElapsed()){
                glow = new StandardLight(ship.getLocation(), new Vector2f(0,0), new Vector2f(0,0), ship);
                glow.setSize(50f);
                glow.setIntensity(5000f);
                glow.setColor(new Color(235, 185,20, 255));
                glow.setLifetime(10f);
                glow.fadeIn(0.1f);
                glow.fadeOut(0.1f);
                glow.setHeight(100f);
                LightShader.addLight(glow);
            } else{
                if(glow != null) glow.advance(amount);
            }*/


            ship.setCustomData(CAPACITY_FACTOR_KEY, capacitorFactor);
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getMaxSpeed().modifyPercent(CAPACITY_FACTOR_KEY, 100*SPEED_BOOST*capacitorFactor);
            stats.getAcceleration().modifyPercent(CAPACITY_FACTOR_KEY, 100*SPEED_BOOST*capacitorFactor);
            stats.getDeceleration().modifyPercent(CAPACITY_FACTOR_KEY, 100*SPEED_BOOST*capacitorFactor);
            stats.getMaxTurnRate().modifyPercent(CAPACITY_FACTOR_KEY, 100*SPEED_BOOST*capacitorFactor);
            stats.getTurnAcceleration().modifyPercent(CAPACITY_FACTOR_KEY, 100*SPEED_BOOST*capacitorFactor);

            stats.getEnergyWeaponDamageMult().modifyMult(CAPACITY_FACTOR_KEY, 1+DAMAGE_BOOST*capacitorFactor);
            stats.getBallisticRoFMult().modifyMult(CAPACITY_FACTOR_KEY, 1+ROF_BOOST*capacitorFactor);
            stats.getBallisticWeaponFluxCostMod().modifyMult(CAPACITY_FACTOR_KEY, 1/(1+ROF_BOOST*capacitorFactor));

            if(engine.getPlayerShip() == ship) {
                engine.maintainStatusForPlayerShip(STATUSKEY1, Global.getSettings().getSpriteName("icons", "coronal_cap_bottom"),
                        "+" + Math.round(100*SPEED_BOOST*capacitorFactor) + "% top speed", "improved maneuverability", false);
                engine.maintainStatusForPlayerShip(STATUSKEY2,Global.getSettings().getSpriteName("icons", "coronal_cap_middle"),
                        "+" + Math.round(100*(ROF_BOOST*capacitorFactor)) + "% ballistic rate of fire",
                        "-" + Math.round(100*(1-1/(1+ROF_BOOST*capacitorFactor))) + "% ballistic flux use", false);
                engine.maintainStatusForPlayerShip(STATUSKEY3,Global.getSettings().getSpriteName("icons", "coronal_cap_top"),"Coronal Capacitor",
                        "+" + Math.round(100*(DAMAGE_BOOST*capacitorFactor)) + "% energy weapon damage" , false);
            }
        }

        public static float getSystemStellarIntensity(ShipAPI ship) {
            if (Global.getSector() == null || Global.getSector().getCurrentLocation() == null) return 1f;
            if (Global.getSector().getCurrentLocation().isHyperspace()) return MIN_CHARGEMULT;
            StarSystemAPI system = (StarSystemAPI) Global.getSector().getCurrentLocation();
            if (system.getStar() == null) return MIN_CHARGEMULT;

            if (ship.getFleetMember() == null || ship.getFleetMember().getFleetData() == null || ship.getFleetMember().getFleetData().getFleet() == null) return 1f;

            CampaignFleetAPI fleet = ship.getFleetMember().getFleetData().getFleet();
            Vector2f loc = fleet.getLocation();

            float lux = 0f;
            PlanetAPI primary = system.getStar();
            lux += getStarIntensity(primary, loc);
            PlanetAPI secondary;
            PlanetAPI tertiary;

            if (system.getSecondary() != null) {
                lux += getStarIntensity(system.getSecondary(), loc);
            }
            if (system.getTertiary() != null) {
                lux += getStarIntensity(system.getTertiary(), loc);
            }

            return Math.max(MIN_CHARGEMULT, Math.min(lux, MAX_CHARGEMULT));
        }

        public static float getSystemStellarIntensity(FleetMemberAPI member) {
            if (Global.getSector() == null || Global.getSector().getCurrentLocation() == null) return 1f;
            if (Global.getSector().getCurrentLocation().isHyperspace()) return MIN_CHARGEMULT;
            StarSystemAPI system = (StarSystemAPI) Global.getSector().getCurrentLocation();
            if (system.getStar() == null) return MIN_CHARGEMULT;

            if (member == null || member.getFleetData() == null || member.getFleetData().getFleet() == null) return 1f;

            CampaignFleetAPI fleet = member.getFleetData().getFleet();
            Vector2f loc = fleet.getLocation();

            float lux = 0f;
            PlanetAPI primary = system.getStar();
            lux += getStarIntensity(primary, loc);
            PlanetAPI secondary;
            PlanetAPI tertiary;

            if (system.getSecondary() != null) {
                lux += getStarIntensity(system.getSecondary(), loc);
            }
            if (system.getTertiary() != null) {
                lux += getStarIntensity(system.getTertiary(), loc);
            }

            return Math.max(MIN_CHARGEMULT, Math.min(lux, MAX_CHARGEMULT));
        }

        public static float getStarIntensity(PlanetAPI star, Vector2f fleetLoc) {
            float baseDistance = star.getRadius() * 1f; //Intensity values are fairly high, so tweaking this down
            float lux = 0f;
            for (Map.Entry entry : starLux.entrySet()) {
                if (entry.getKey().equals(star.getTypeId())) {
                    lux = (float) entry.getValue();
                }
            }
            if (lux == 0f) lux = 1f; // Unknown star type
            if (Misc.getDistance(star.getLocation(), fleetLoc) < (star.getRadius() + 500)) { // Very close, inside corona for most stars
                return lux * 5f; // Almost always full rate
            }
            lux *= baseDistance/Misc.getDistance(star.getLocation(), fleetLoc);

            return lux;
        }

        public float getActualRecharge() {
            return FLUX_CHARGERATE * getSystemStellarIntensity(ship);
        }

        public float getActualRecharge(FleetMemberAPI member) {
            return FLUX_CHARGERATE * getSystemStellarIntensity(member);
        }
    }


    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getListeners(CoronalCapacitorListener.class).isEmpty()) ship.addListener(new CoronalCapacitor.CoronalCapacitorListener(ship));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        //if (stats.getListeners(CoronalCapacitorListener.class).isEmpty() && stats.getFleetMember() != null) stats.addListener(new CoronalCapacitorListener(stats.getFleetMember()));
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (member != null &&
                member.getFleetData() != null
                && member.getFleetData().getFleet() != null
                && (!member.getFleetData().getFleet().getCustomData().containsKey(CHARGE_CHECK_KEY)
                || (long) member.getFleetData().getFleet().getCustomData().get(CHARGE_CHECK_KEY) >= Global.getSector().getClock().getTimestamp() + 1000)) {
            CoronalCapacitorListener.actual_chargemult = getSystemStellarIntensity(member);
            member.getFleetData().getFleet().getCustomData().put(CHARGE_CHECK_KEY, Global.getSector().getClock().getTimestamp());
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        CoronalCapacitorListener listener = ship.getListeners(CoronalCapacitorListener.class).get(0);
        float rate = listener.getActualRecharge();

        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        tooltip.addPara("Utilizes energy from local stars to supercharge weapons and engines.", opad, h, "supercharge weapons and engines");
        tooltip.addPara("Stellar energy charges at a rate equal to %s in this location. Active venting increases this rate by %s.",
                opad, h, String.format("%.2f", rate*100)+"% per second", String.format("%.2f", ship.getMutableStats().getVentRateMult().getModifiedValue()*2)+"x");
        tooltip.addPara("Firing weapons deletes energy at a rate proportional to their flux cost.", opad);
        tooltip.addPara("The boost from a full charge results in a %s to:", opad, h, "30% increase");
        tooltip.setBulletedListMode(" - ");
        tooltip.addPara("Energy Weapon Damage", pad, h, "Energy Weapon Damage");
        tooltip.addPara("Ballistic Rate of Fire", pad, h, "Ballistic Rate of Fire");
        tooltip.addPara("Top Speed", pad, h, "Top Speed");
        tooltip.addPara("Acceleration", pad, h, "Acceleration");
        tooltip.setBulletedListMode(null);
    }

    @Override
    public float getTooltipWidth() {
        return 349f;
    }

}
