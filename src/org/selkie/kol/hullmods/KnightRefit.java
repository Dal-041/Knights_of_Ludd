package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.ArrayList;
import java.util.List;

/*
  - When ballistic weaponry is installed in composite slots :
    - Increases flux capacity by #/#/#.
    - Increases flux dissipation by #/#/#.

 - Front Armor
     - Rating:
     - Effectiveness:
 - Flank Armor
     - Rating:
     - Effectiveness:
 - Rear Armor
     - Rating:
     - Effectiveness:

 - Ship gains increased speed and maneuverability as armor panels are destroyed.

    */

public class KnightRefit extends BaseHullMod {

    public static final int flux_cap_smol  = 10;
    public static final int flux_cap_med  = 25;
    public static final int flux_cap_lorge  = 50;
    public static final int flux_cap_per_op  = 25;
    public static final int flux_diss_smol  = 5;
    public static final int flux_diss_med  = 5;
    public static final int flux_diss_lorge  = 10;
    public static final int flux_diss_per_op  = 5;

    private final String knightRefitID = "knightRefit";
    private final float SPEED_BONUS=0.25f;

    @Override
    public void init(HullModSpecAPI spec) {

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("advancedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "advancedshieldemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("adaptiveshields")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "adaptiveshields", "kol_refit");
        if (ship.getVariant().hasHullMod("frontemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "frontemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("extendedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "extendedshieldemitter", "kol_refit");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        float capBonus = 0;
        float dissBonus = 0;

        for (String weaponSlot:stats.getVariant().getFittedWeaponSlots()) {
            WeaponSpecAPI wep = stats.getVariant().getWeaponSpec(weaponSlot);
            if (wep.getType() == WeaponAPI.WeaponType.BALLISTIC && stats.getVariant().getSlot(weaponSlot).getWeaponType() == WeaponAPI.WeaponType.COMPOSITE) {
                /*
                //Bonus based on size
                switch (wep.getSize()) {
                    case LARGE:
                        capBonus += wep.getOrdnancePointCost(cStats) * flux_cap_lorge;
                    case MEDIUM:
                        capBonus += wep.getOrdnancePointCost(cStats) * flux_cap_med;
                    case SMALL:
                        capBonus += wep.getOrdnancePointCost(cStats) * flux_cap_smol;
                }
                 */
                // bonus based on OP
                int opCost = (int)(wep.getOrdnancePointCost(null)+0.1f); //I hope so much this is paranoia
                capBonus += opCost*flux_cap_per_op;
                dissBonus += opCost*flux_diss_per_op;
            }
        }

        stats.getFluxCapacity().modifyFlat(id, capBonus);
        stats.getFluxDissipation().modifyFlat(id, dissBonus);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        //I have no idea what I'm doing

        List<Float> topHull = new ArrayList<>(), topArmor = new ArrayList<>(), middleHull = new ArrayList<>(), middleArmor = new ArrayList<>(), rearHull = new ArrayList<>(), rearArmor = new ArrayList<>();
        float topArmorAvg = 0f, topHullAvg = 0f, middleArmorAvg = 0f, middleHullAvg = 0f, rearArmorAvg = 0f, rearHullAvg = 0f;

        if (ship.getChildModulesCopy() != null) {
            for(ShipAPI module : ship.getChildModulesCopy()) {
                String ID = module.getVariant().getHullVariantId();
                if (ID.contains("_tl") || ID.contains("_tr")) {
                    topArmor.add(module.getArmorGrid().getArmorRating()/module.getHullSpec().getArmorRating() * 100f);
                    topHull.add(module.getHullLevel()/module.getMaxHitpoints() * 100f);
                }
                if (ID.contains("_ml") || ID.contains("_mr")) {
                    middleArmor.add(module.getArmorGrid().getArmorRating()/module.getHullSpec().getArmorRating() * 100f);
                    middleHull.add(module.getHullLevel()/module.getMaxHitpoints() * 100f * 100f);
                }
                if (ID.contains("_ll") || ID.contains("_lr")) {
                    rearArmor.add(module.getArmorGrid().getArmorRating()/module.getHullSpec().getArmorRating() * 100f);
                    rearHull.add(module.getHullLevel()/module.getMaxHitpoints() * 100f * 100f);
                }
            }
        }

        for (int i = 0; i < topArmor.size(); i++) {
            topArmorAvg += topArmor.get(i);
            topHullAvg += topHull.get(i);
            if (i == topArmor.size() - 1) {
                topArmorAvg = topArmorAvg / (i + 1);
                topHullAvg = topHullAvg / (i + 1);
            }
        }
        for (int i = 0; i < middleArmor.size(); i++) {
            middleArmorAvg += middleArmor.get(i);
            middleHullAvg += middleHull.get(i);
            if (i == middleArmor.size() - 1) {
                middleArmorAvg = middleArmorAvg / (i + 1);
                middleHullAvg = middleHullAvg / (i + 1);
            }
        }
        for (int i = 0; i < rearArmor.size(); i++) {
            rearArmorAvg += rearArmor.get(i);
            rearHullAvg += rearHull.get(i);
            if (i == rearArmor.size() - 1) {
                rearArmorAvg = rearArmorAvg / (i + 1);
                rearHullAvg = rearHullAvg / (i + 1);
            }
        }

        tooltip.addSectionHeading("Module status", Alignment.MID, 10);
        if(topHullAvg != 0f) {
            tooltip.addPara("Fore Armor: " + Math.round(topArmorAvg), 0f);
            tooltip.addPara("Fore Hull: " + Math.round(topHullAvg), 0f);
        }
        if(middleHullAvg != 0f) {
            tooltip.addPara("Middle Armor: " + Math.round(middleArmorAvg), 0f);
            tooltip.addPara("Middle Hull: " + Math.round(middleHullAvg), 0f);
        }
        if(rearHullAvg != 0f) {
            tooltip.addPara("Rear Armor: " + Math.round(rearArmorAvg), 0f);
            tooltip.addPara("Rear Hull: " + Math.round(rearHullAvg), 0f);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Yoinked whole-cloth from SCY. <3 ya Tarty
        if (Global.getCombatEngine().isPaused() || ship==null || ship.getOriginalOwner() == -1) {
            return;
        }

        if (!ship.isAlive()) {
            removeStats(ship);
            return;
        }

        float modules =0;
        float alive =0;
        for(ShipAPI s : ship.getChildModulesCopy()){
            modules++;
            if(s.isAlive()){
                alive++;
            }
        }

        if(modules!=0){
            //speed bonus applies linearly
            float speedRatio=1 - (alive / modules);
            applyStats(speedRatio, ship);
        }
    }

    private void removeStats(ShipAPI ship) {
        ship.getMutableStats().getMaxSpeed().unmodify(knightRefitID);
        ship.getMutableStats().getAcceleration().unmodify(knightRefitID);
        ship.getMutableStats().getDeceleration().unmodify(knightRefitID);
        ship.getMutableStats().getMaxTurnRate().unmodify(knightRefitID);
        ship.getMutableStats().getTurnAcceleration().unmodify(knightRefitID);
    }

    private void applyStats(float speedRatio, ShipAPI ship) {
        ship.getMutableStats().getMaxSpeed().modifyMult(knightRefitID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getAcceleration().modifyMult(knightRefitID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getDeceleration().modifyMult(knightRefitID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getMaxTurnRate().modifyMult(knightRefitID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getTurnAcceleration().modifyMult(knightRefitID, (1 + (speedRatio * SPEED_BONUS)));
    }
}
