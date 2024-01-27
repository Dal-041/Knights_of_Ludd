package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.magiclib.util.*;
import org.selkie.kol.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CoronalCapacitor extends BaseHullMod {
    public static class CoronalCapacitorListener implements AdvanceableListener {

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
        public static final float MAX_CHARGEFLUX = 1f; // multiplier on ships max flux
        public static final float FLUX_CHARGERATE = 0.05f; // percentage of bar gained per second

        public static final float SPEED_BOOST = 0.3f;
        public static final float DAMAGE_BOOST = 0.3f;
        public static final float ROF_BOOST = 0.3f;

        private float capacitorLevel = 0f;
        private float chargeTime = 0f;
        private List<Pair<EngineSlotAPI, Pair<Color, Color>>> engines;
        protected Object STATUSKEY1 = new Object();
        protected Object STATUSKEY2 = new Object();
        protected Object STATUSKEY3 = new Object();
        IntervalUtil lightInterval = new IntervalUtil(1.5f, 1.5f);
        StandardLight glow = null;

        CoronalCapacitorListener(ShipAPI ship){
            this.ship = ship;
            engine = Global.getCombatEngine();
            for(WeaponAPI weapon : ship.getAllWeapons()){
                if(!weapon.isDecorative()){
                    if (weapon.isBeam() && !weapon.isBurstBeam()){
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
            float fluxPerSecond = 0f;
            float flatFlux = 0f;

            boolean charging = true;

            if (engines == null){
                engines = new ArrayList<>();
                for(ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()){
                    engines.add(new Pair<>(engine.getEngineSlot(), new Pair<>(engine.getEngineSlot().getColor(), engine.getEngineSlot().getGlowAlternateColor())));
                }
            }


            for(WeaponAPI weapon : beams){
                if(weapon.isFiring()) {
                    fluxPerSecond += weapon.getFluxCostToFire();
                    if(!weapon.hasAIHint(WeaponAPI.AIHints.PD))
                        charging = false;
                }
            }
            for(WeaponAPI weapon : fluxCounted.keySet()){
                if(weapon.isFiring()){
                    if(!fluxCounted.get(weapon)){
                        fluxCounted.put(weapon, true);
                        flatFlux += weapon.getFluxCostToFire();
                        if(!weapon.hasAIHint(WeaponAPI.AIHints.PD))
                            charging = false;
                    }
                } else if(!weapon.isInBurst()){
                    fluxCounted.put(weapon, false);
                }
            }

            if(USING_FLUX){
                MagicUI.drawInterfaceStatusBar(ship, capacitorLevel, Misc.getPositiveHighlightColor(), null, 0, "BOOST", Math.round(capacitorLevel*100));
                float fluxUsed = flatFlux + fluxPerSecond*amount;
                capacitorLevel = capacitorLevel - fluxUsed/(MAX_CHARGEFLUX*ship.getMaxFlux()) + FLUX_CHARGERATE*amount;
            } else{
                MagicUI.drawInterfaceStatusBar(ship, capacitorLevel, Misc.getPositiveHighlightColor(), null, 0, "BOOST", Math.round(capacitorLevel*MAX_CHARGETIME));
                if(charging){
                    chargeTime += amount;
                    if(chargeTime >= CHARGE_DELAY)
                        capacitorLevel += amount/MAX_CHARGETIME*TIME_CHARGERATE;
                } else{
                    chargeTime = 0;
                    capacitorLevel -= amount/MAX_CHARGETIME;
                }

            }
            capacitorLevel = Math.max(0, Math.min(1, capacitorLevel));
            for(Pair<EngineSlotAPI, Pair<Color, Color>> engineData : engines){
                engineData.one.setColor(Utils.OKLabInterpolateColor(engineData.two.one,new Color(235, 185,20, 200), capacitorLevel));
                if (engineData.two.two != null) engineData.one.setGlowAlternateColor(Utils.OKLabInterpolateColor(engineData.two.two, new Color(215, 175,55, 100), capacitorLevel));
            }

            ship.setJitterUnder("coronal_cap" + ship.getId(), new Color(235, 185,20, 200),
                    Utils.linMap(0,0.6f,0.5f,1, capacitorLevel), 3, 0f, 20);


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


            String id = "coronal_cap_modifier";
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getMaxSpeed().modifyPercent(id, 100*SPEED_BOOST*capacitorLevel);
            stats.getAcceleration().modifyPercent(id, 100*SPEED_BOOST*capacitorLevel);
            stats.getDeceleration().modifyPercent(id, 100*SPEED_BOOST*capacitorLevel);
            stats.getMaxTurnRate().modifyPercent(id, 100*SPEED_BOOST*capacitorLevel);
            stats.getTurnAcceleration().modifyPercent(id, 100*SPEED_BOOST*capacitorLevel);

            stats.getEnergyWeaponDamageMult().modifyMult(id, 1+DAMAGE_BOOST*capacitorLevel);
            stats.getBallisticRoFMult().modifyMult(id, 1+ROF_BOOST*capacitorLevel);
            stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1/(1+ROF_BOOST*capacitorLevel));

            if(engine.getPlayerShip() == ship){
                engine.maintainStatusForPlayerShip(STATUSKEY1, Global.getSettings().getSpriteName("icons", "coronal_cap_bottom"),
                        "+" + Math.round(100*SPEED_BOOST*capacitorLevel) + "% top speed", "improved maneuverability", false);
                engine.maintainStatusForPlayerShip(STATUSKEY2,Global.getSettings().getSpriteName("icons", "coronal_cap_middle"),
                        "+" + Math.round(100*(ROF_BOOST*capacitorLevel)) + "% ballistic rate of fire",
                        "-" + Math.round(100*(1-1/(1+ROF_BOOST*capacitorLevel))) + "% ballistic flux use", false);
                engine.maintainStatusForPlayerShip(STATUSKEY3,Global.getSettings().getSpriteName("icons", "coronal_cap_top"),"Coronal Capacitor",
                        "+" + Math.round(100*(DAMAGE_BOOST*capacitorLevel)) + "% energy weapon damage" , false);

            }
        }
    }


    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new CoronalCapacitor.CoronalCapacitorListener(ship));
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
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        tooltip.addPara("Utilizes coronal energy from the local system to supercharge weapons and engines. This energy is represented in units of flux, with a maximum charge equal to the ships flux capacity.", opad, h, "supercharge weapons and engines");
        tooltip.addPara("Coronal energy charges at a rate equal to %s of the ships flux capacity per second in a typical starsystem.", opad, h, "5%");
        tooltip.addPara("Coronal energy depletes at a rate equal to the flux cost of any weapon upon firing.", opad);
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
