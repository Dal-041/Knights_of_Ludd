package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
- flavor description tl;dr-ing both the limited-charge shielding, and ablative armor modules, reduced main ship hull/armor

- Passives
 - Reduces hull and armor by [orangetext] 40% [/o]. [this is done via shipdata.csv, not hullmod]
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

 - When a module is destroyed, the corresponding deco cover on the main hull is hidden
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
    private float topArmorAvg = 0f, topHullAvg = 0f, middleArmorAvg = 0f, middleHullAvg = 0f, rearArmorAvg = 0f, rearHullAvg = 0f;

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
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int) flux_cap_per_op;
        if (index == 1) return "" + (int) flux_diss_per_op;
        if (index == 2) return "" + (int) 100 * SPEED_BONUS;
        return null;
    }

    @Override
    public void addPostDescriptionSection(
            TooltipMakerAPI tooltip,
            ShipAPI.HullSize hullSize,
            ShipAPI ship,
            float width,
            boolean isForModSpec) {

        ShipVariantAPI variant = ship.getVariant();

        if (variant == null) {
            // default to base variant if the ship doesn't have a proper one (when it is bought)
            variant = Global.getSettings().getVariant(ship.getId() + "_Standard");
        }

        if (variant == null) return;
        if (ship.getVariant().getStationModules().isEmpty()) return;

        // title
        tooltip.addSectionHeading("Modular Armor Stats", Alignment.MID, 15);

        tooltip.beginTable(
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(),
                20f,
                true,
                true,
                new Object[]{"Name", width - 80f * 2 - 8f, "Hull", 80f, "Armor", 80f});

        System.out.println("KOL Refit modules:");
        for (String module : ship.getVariant().getStationModules().values()) {
            System.out.printf("module [%s] variant not null [%s] hullspec not null if variant not null [%s]%n",
                    module, Global.getSettings().getVariant(module) != null, Global.getSettings().getVariant(module) != null && Global.getSettings().getVariant(module).getHullSpec() != null);
            // for some insane reason, the hullspec can return null
            if (Global.getSettings().getVariant(module) == null
                    || Global.getSettings().getVariant(module).getHullSpec() == null) continue;
            ShipHullSpecAPI hull = Global.getSettings().getVariant(module).getHullSpec();
            tooltip.addRow(
                    Alignment.LMID,
                    Misc.getTextColor(),
                    hull.getHullName(),
                    Alignment.MID,
                    Misc.getTextColor(),
                    String.valueOf(Math.round(hull.getHitpoints())),
                    Alignment.MID,
                    Misc.getTextColor(),
                    String.valueOf(Math.round(hull.getArmorRating())));
        }
        tooltip.addTable("-", 0, 4f);

        tooltip.addPara("These hullmods also apply to your modules:", 10);
        for (String hullmodId : variant.getHullMods()) {
            if (hullmodEffects.containsKey(hullmodId)) {
                tooltip.addPara(Global.getSettings().getHullModSpec(hullmodId).getDisplayName(), 4f);
                hullmodEffects.get(hullmodId).addTooltipText(tooltip, ship);
            }
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

    private static final Map<String, ArmorEffect> hullmodEffects = new HashMap<>();

    static {
        hullmodEffects.put("heavyarmor", new ArmorEffect(150, 1, 1, 1, 1, 1));
        hullmodEffects.put("reinforcedhull", new ArmorEffect(0, 1, 0.72f, 1, 1, 1));
        //hullmodEffects.put("TADA_lightArmor", new ArmorEffect(0, 2, 1, 1, 1, 1));
        //hullmodEffects.put("TADA_reactiveArmor", new ArmorEffect(0, 1, 1, 1.25f, 1.25f, 0.66f));
    }

    public static void applyHullmodModificationsToStats(MutableShipStatsAPI stats, ShipHullSpecAPI moduleSpec, ShipVariantAPI parentVariant) {
        for (String hullmodId : parentVariant.getHullMods()) {
            if (hullmodEffects.containsKey(hullmodId)) {
                hullmodEffects.get(hullmodId).applyToStats(hullmodId, stats, moduleSpec);
            }
        }
    }

    protected static class ArmorEffect {
        public float armorDamageTakenModifier;
        public float armorDamageTakenMult;
        public float hullDamageTakenMult;
        public float energyDamageTakenMult;
        public float kineticDamageTakenMult;
        public float heDamageTakenMult;

        public ArmorEffect(float armorDamageTakenModifier, float armorDamageTakenMult, float hullDamageTakenMult, float energyDamageTakenMult, float kineticDamageTakenMult, float heDamageTakenMult) {
            this.armorDamageTakenModifier = armorDamageTakenModifier;
            this.armorDamageTakenMult = armorDamageTakenMult;
            this.hullDamageTakenMult = hullDamageTakenMult;
            this.energyDamageTakenMult = energyDamageTakenMult;
            this.kineticDamageTakenMult = kineticDamageTakenMult;
            this.heDamageTakenMult = heDamageTakenMult;
        }

        public float calcArmorDamageMult(float baseArmor) {
            return baseArmor / (baseArmor + armorDamageTakenModifier) * armorDamageTakenMult;
        }

        public void applyToStats(String buffId, MutableShipStatsAPI stats, ShipHullSpecAPI spec) {
            stats.getArmorDamageTakenMult().modifyMult(buffId, calcArmorDamageMult(spec.getArmorRating()));
            stats.getHullDamageTakenMult().modifyMult(buffId, hullDamageTakenMult);
            stats.getEnergyDamageTakenMult().modifyMult(buffId, energyDamageTakenMult);
            stats.getKineticDamageTakenMult().modifyMult(buffId, kineticDamageTakenMult);
            stats.getHighExplosiveDamageTakenMult().modifyMult(buffId, heDamageTakenMult);
        }

        public void addTooltipText(TooltipMakerAPI tooltip, ShipAPI ship) {
            tooltip.setBulletedListMode("- ");
            float armorDamageTaken = calcArmorDamageMult(ship.getHullSpec().getArmorRating());
            if (armorDamageTaken != 1) {
                String text = Misc.getRoundedValue(armorDamageTaken);
                tooltip.addPara(text, 4f, Misc.getHighlightColor(), text);
            }

            if (hullDamageTakenMult != 1) {
                String text = Misc.getRoundedValue(hullDamageTakenMult);
                tooltip.addPara(text, 4f, Misc.getHighlightColor(), text);
            }

            if (energyDamageTakenMult != 1) {
                String text = Misc.getRoundedValue(energyDamageTakenMult);
                tooltip.addPara(text, 4f, Misc.getHighlightColor(), text);
            }

            if (kineticDamageTakenMult != 1) {
                String text = Misc.getRoundedValue(kineticDamageTakenMult);
                tooltip.addPara(text, 4f, Misc.getHighlightColor(), text);
            }

            if (heDamageTakenMult != 1) {
                String text = Misc.getRoundedValue(heDamageTakenMult);
                tooltip.addPara(text, 4f, Misc.getHighlightColor(), text);
            }

            tooltip.setBulletedListMode(null);
        }
    }
}
