package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.List;
import java.util.*;

/*
  - When ballistic weaponry is installed in composite slots :
    - Increases flux capacity by #/#/#.
    - Increases flux dissipation by #/#/#.

 - Front Armor
     - Hull : %
     - Armor : %
 - Flank Armor
     - Hull : %
     - Armor : %
 - Rear Armor
     - Hull : %
     - Armor %

 - Ship gains increased speed and maneuverability as armor panels are destroyed.

    desc:
 - Max duration of [yellowtext] # [/y] seconds.
 - Shield charge regenerates while shields are off.
 - Shield emitters forcibly shut down at [y] 0% [/y] charge until fully regenerated.

    [orange text] Incompatible with Shield Conversion - Omni
    [orange text] Shield arc cannot be extended by any means.
    */

public class knightRefit extends BaseHullMod {

    public static final int flux_cap_smol  = 10;
    public static final int flux_cap_med  = 25;
    public static final int flux_cap_lorge  = 50;
    public static final int flux_cap_per_op  = 25;
    public static final int flux_diss_smol  = 5;
    public static final int flux_diss_med  = 5;
    public static final int flux_diss_lorge  = 10;
    public static final int flux_diss_per_op  = 5;

    @Override
    public void init(HullModSpecAPI spec) {

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //MutableShipStatsAPI stats = ship.getMutableStats();
        //MutableCharacterStatsAPI cStats = BaseSkillEffectDescription.getCommanderStats(stats);

        if (ship.getVariant().hasHullMod("advancedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "advancedshieldemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("adaptiveshields")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "adaptiveshields", "kol_refit");
        if (ship.getVariant().hasHullMod("frontemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "frontemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("extendedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "extendedshieldemitter", "kol_refit");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        MutableCharacterStatsAPI cStats = BaseSkillEffectDescription.getCommanderStats(stats);

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

        HashMap<String, List<Float>> childHPVals = new HashMap<>();
        List<Float> moduleHP = new ArrayList<>(); //armor, hull
        List<Float> topHull = new ArrayList<>(), topArmor = new ArrayList<>(), middleHull = new ArrayList<>(), middleArmor = new ArrayList<>(), rearHull = new ArrayList<>(), rearArmor = new ArrayList<>();
        float topArmorAvg = 0f, topHullAvg = 0f, middleArmorAvg = 0f, middleHullAvg = 0f, rearArmorAvg = 0f, rearHullAvg = 0f;

        if (ship.getChildModulesCopy() != null) {
            for (ShipAPI module : ship.getChildModulesCopy()) {
                if (module.isAlive()) {
                    moduleHP.add(module.getArmorGrid().getArmorRating());
                    moduleHP.add(module.getHullLevel());
                    childHPVals.put(module.getId(), moduleHP);
                    moduleHP.clear();
                }
            }
        }

        topArmorAvg = 0f;
        topHullAvg = 0f;
        middleArmorAvg = 0f;
        middleHullAvg = 0f;
        rearArmorAvg = 0f;
        rearHullAvg = 0f;

        for (String ID : childHPVals.keySet()) {
            if (ID.contains("_tl") || ID.contains("_tr")) {
                topArmor.add(childHPVals.get(ID).get(0));
                topHull.add(childHPVals.get(ID).get(1));
            }
            if (ID.contains("_ml") || ID.contains("_mr")) {
                middleArmor.add(childHPVals.get(ID).get(0));
                middleHull.add(childHPVals.get(ID).get(1));
            }
            if (ID.contains("_ll") || ID.contains("_lr")) {
                rearArmor.add(childHPVals.get(ID).get(0));
                rearHull.add(childHPVals.get(ID).get(1));
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
        //if(topHullAvg != 0f) {
            tooltip.addPara("Fore Armor: " + Math.round(topArmorAvg), 0f);
            tooltip.addPara("Fore Hull: " + Math.round(topHullAvg), 0f);
        //}
        //if(middleHullAvg != 0f) {
            tooltip.addPara("Middle Armor: " + Math.round(middleArmorAvg), 0f);
            tooltip.addPara("Middle Hull: " + Math.round(middleHullAvg), 0f);
        //}
        //if(rearHullAvg != 0f) {
            tooltip.addPara("Rear Armor: " + Math.round(rearArmorAvg), 0f);
            tooltip.addPara("Rear Hull: " + Math.round(rearHullAvg), 0f);
        //}
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        super.advanceInCampaign(member, amount);

        /*
        //once a second..ish
        if (childHPVals == null || (amount != 0 && amount % 60 != 0)) {
            return;
        }
        topArmor.clear();
        topHull.clear();
        middleArmor.clear();
        middleHull.clear();
        rearArmor.clear();
        rearHull.clear();

        topArmorAvg = 0f;
        topHullAvg = 0f;
        middleArmorAvg = 0f;
        middleHullAvg = 0f;
        rearArmorAvg = 0f;
        rearHullAvg = 0f;

        for (String ID:childHPVals.keySet()) {
            if (ID.contains("_tl") || ID.contains("_tr")) {
                topArmor.add(childHPVals.get(ID).get(0));
                topHull.add(childHPVals.get(ID).get(1));
            }
            if (ID.contains("_ml") || ID.contains("_mr")) {
                middleArmor.add(childHPVals.get(ID).get(0));
                middleHull.add(childHPVals.get(ID).get(1));
            }
            if (ID.contains("_ll") || ID.contains("_lr")) {
                rearArmor.add(childHPVals.get(ID).get(0));
                rearHull.add(childHPVals.get(ID).get(1));
            }
        }

        for (int i = 0; i < topArmor.size(); i++) {
            topArmorAvg += topArmor.get(i);
            topHullAvg += topHull.get(i);
            if (i == topArmor.size()-1) {
                topArmorAvg = topArmorAvg/(i+1);
                topHullAvg = topHullAvg/(i+1);
            }
        }
        for (int i = 0; i < middleArmor.size(); i++) {
            middleArmorAvg += middleArmor.get(i);
            middleHullAvg += middleHull.get(i);
            if (i == middleArmor.size()-1) {
                middleArmorAvg = middleArmorAvg/(i+1);
                middleHullAvg = middleHullAvg/(i+1);
            }
        }
        for (int i = 0; i < rearArmor.size(); i++) {
            rearArmorAvg += rearArmor.get(i);
            rearHullAvg += rearHull.get(i);
            if (i == rearArmor.size()-1) {
                rearArmorAvg = rearArmorAvg/(i+1);
                rearHullAvg = rearHullAvg/(i+1);
            }
        }
        */
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
    }

}
