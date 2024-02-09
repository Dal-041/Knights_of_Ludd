package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import com.fs.starfarer.rpg.Person;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
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

    public static final int FLUX_CAP_PER_OP = 25;
    public static final int FLUX_DISS_PER_OP = 5;
    private static final Logger log = Logger.getLogger(KnightRefit.class);

    private final String knightRefitID = "knightRefit";
    private final float SPEED_BONUS = 0.25f;
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

        PersonAPI captain = ship.getOriginalCaptain();
        MutableCharacterStatsAPI stats = captain == null ? null : captain.getFleetCommanderStats();
        float capBonus = 0;
        float dissBonus = 0;
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if (weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.COMPOSITE ){
                int opCost = (int) weapon.getSpec().getOrdnancePointCost(stats, ship.getMutableStats());
                capBonus += opCost* FLUX_CAP_PER_OP;
                dissBonus += opCost* FLUX_DISS_PER_OP;
            }
        }
        ship.getMutableStats().getFluxCapacity().modifyFlat(id, capBonus);
        ship.getMutableStats().getFluxDissipation().modifyFlat(id, dissBonus);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    }

    @Override
    public float getTooltipWidth() {
        return 400f;
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 64f;
        float headingPad = 20f;
        float underHeadingPad = 5f;
        float listPad = 3f;
        Color YELLOW = new Color(241, 199, 0);

        boolean hasComposite = false;
        for(WeaponSlotAPI slot : ship.getVariant().getHullSpec().getAllWeaponSlotsCopy()){
            if (slot.getWeaponType() == WeaponAPI.WeaponType.COMPOSITE){
                hasComposite = true;
                break;
            }
        }

        if(hasComposite){
            tooltip.addSectionHeading("Integrated Ballistics", YELLOW, Global.getSettings().getColor("buttonBgDark"), Alignment.MID, headingPad);
            TooltipMakerAPI integratedBallistics = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "kol_composite"), HEIGHT);
            //integratedBallistics.addTitle("Integrated Ballistics", YELLOW);
            integratedBallistics.setBulletedListMode(" • ");
            integratedBallistics.addPara("Every ordnance point spent on ballistic weapons installed into composite slots increases Flux Capacity by %s, and Flux Dissipation by %s.",
                    listPad, Misc.getPositiveHighlightColor(), ""+FLUX_CAP_PER_OP, ""+FLUX_DISS_PER_OP);
            tooltip.addImageWithText(underHeadingPad);
        }

        if(ship.getShield() != null){
            tooltip.addSectionHeading("Primitive Capacitor Shields", YELLOW, Global.getSettings().getColor("buttonBgDark"), Alignment.MID, headingPad);
            TooltipMakerAPI capacitorShields = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "kol_primshield"), HEIGHT);
            //capacitorShields.addTitle("Primitive Capacitor Shields", YELLOW);
            capacitorShields.setBulletedListMode(" • ");
            capacitorShields.addPara("Shields rely on a charge and can only stay online for a max of %s at a time.",listPad, Misc.getPositiveHighlightColor(), "10 seconds");
            capacitorShields.addPara("Shield charge regenerates while shields are offline.",listPad);
            capacitorShields.addPara("Shield emitters undergo a forced shutdown when charge reaches %s, and can only be reactivated once recharged to %s.",listPad, YELLOW, "0%", "100%");
            tooltip.addImageWithText(underHeadingPad);
        }


        ShipVariantAPI variant = ship.getVariant();

        if (variant == null) {
            // default to base variant if the ship doesn't have a proper one (when it is bought)
            variant = Global.getSettings().getVariant(ship.getId() + "_Standard");
        }

        if (variant == null) return;
        if (ship.getVariant().getStationModules().isEmpty()) return;

        // title
        tooltip.addSectionHeading("Modular Armor", YELLOW, Global.getSettings().getColor("buttonBgDark"), Alignment.MID, headingPad);
        TooltipMakerAPI modularArmor = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "kol_modules"), HEIGHT);
        //modularArmor.addTitle("Modular Armor", YELLOW);
        modularArmor.setBulletedListMode(" • ");
        modularArmor.addPara("Increases top speed and maneuverability by up to %s as armor panels are destroyed.", listPad, Misc.getPositiveHighlightColor(), "25%");

        modularArmor.beginTable(
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(),
                20f,
                true,
                true,
                new Object[]{"Armor Plate Name", width - 80f * 2 - HEIGHT - 25f, "Hull", 80f, "Armor", 80f});

        for (String module : ship.getVariant().getStationModules().values()) {
            // for some insane reason, the hullspec can return null
            if (Global.getSettings().getVariant(module) == null
                    || Global.getSettings().getVariant(module).getHullSpec() == null) continue;
            ShipHullSpecAPI hull = Global.getSettings().getVariant(module).getHullSpec();
            modularArmor.addRow(
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
        modularArmor.addTable("-", 0, 4f);

        modularArmor.addPara("The following hullmods affects your modules:", 10);
        for (String hullmodId : variant.getHullMods()) {
            if (hullmodEffects.containsKey(hullmodId)) {
                modularArmor.addPara(Global.getSettings().getHullModSpec(hullmodId).getDisplayName(), 4f);
                hullmodEffects.get(hullmodId).addTooltipText(modularArmor, ship);
            }
        }
        tooltip.addImageWithText(underHeadingPad);
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
