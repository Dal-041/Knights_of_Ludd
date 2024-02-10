package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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


        Color activeTextColor = Misc.getTextColor();
        Color activePositiveColor = Misc.getPositiveHighlightColor();
        Color activeNegativeColor = Misc.getNegativeHighlightColor();
        Color activeHeaderBannerColor = Misc.getDarkPlayerColor();
        Color activeHighlightColor = Misc.getHighlightColor();

        Color inactiveTextColor = Misc.getGrayColor().darker();
        Color inactivePositiveColor = Misc.getGrayColor().darker();
        Color inactiveNegativeColor = Misc.getGrayColor().darker();
        Color inactiveHeaderBannerColor = Misc.getDarkPlayerColor().darker().darker();
        Color inactiveHighlightColor = Misc.getGrayColor().darker();

        boolean hasComposite = false;
        for(WeaponSlotAPI slot : ship.getVariant().getHullSpec().getAllWeaponSlotsCopy()){
            if (slot.getWeaponType() == WeaponAPI.WeaponType.COMPOSITE){
                hasComposite = true;
                break;
            }
        }
        tooltip.addSectionHeading("Integrated Ballistics", hasComposite ? activeHighlightColor : inactiveHighlightColor,
                hasComposite ? activeHeaderBannerColor : inactiveHeaderBannerColor, Alignment.MID, headingPad);
        TooltipMakerAPI integratedBallistics = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", hasComposite ? "kol_composite" : "kol_composite_grey"), HEIGHT);
        integratedBallistics.setBulletedListMode("•");
        integratedBallistics.setBulletWidth(15f);
        integratedBallistics.addPara("Every ordnance point spent on ballistic weapons installed into composite slots increases Flux Capacity by %s, and Flux Dissipation by %s.",
                listPad, hasComposite ? activeTextColor : inactiveTextColor, hasComposite ? activePositiveColor: inactivePositiveColor, ""+FLUX_CAP_PER_OP, ""+FLUX_DISS_PER_OP);
        tooltip.addImageWithText(underHeadingPad);


        boolean hasShield = ship.getShield() != null;
        tooltip.addSectionHeading("Primitive Capacitor Shields", hasShield ? activeHighlightColor : inactiveHighlightColor,
                hasShield ? activeHeaderBannerColor : inactiveHeaderBannerColor, Alignment.MID, headingPad);
        TooltipMakerAPI capacitorShields = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", hasShield ? "kol_primshield" : "kol_primshield_grey"), HEIGHT);
        capacitorShields.setBulletedListMode("•");
        capacitorShields.setBulletWidth(15f);
        capacitorShields.addPara("Shields rely on a charge and can only stay online for a max of %s at a time.",
                listPad, hasShield ? activeTextColor : inactiveTextColor, hasShield ? activePositiveColor: inactivePositiveColor, "10 seconds");
        capacitorShields.addPara("Shield charge regenerates while shields are offline.", hasShield ? activeTextColor : inactiveTextColor, listPad);
        if(hasShield){
            capacitorShields.addPara("Shield emitters undergo a forced shutdown when charge reaches %s, and can only be reactivated once recharged to %s.",
                    listPad, new Color[] {activeNegativeColor, activePositiveColor}, "0%", "100%");
        } else{
            capacitorShields.addPara("Shield emitters undergo a forced shutdown when charge reaches %s, and can only be reactivated once recharged to %s.",
                    listPad, inactiveTextColor, inactivePositiveColor, "0%", "100%");
        }

        tooltip.addImageWithText(underHeadingPad);


        ShipVariantAPI variant = ship.getVariant() != null ? ship.getVariant() : Global.getSettings().getVariant(ship.getHullSpec().getBaseHullId() + "_Standard");
        boolean hasModules = variant != null && !variant.getStationModules().isEmpty();
        tooltip.addSectionHeading("Modular Armor", hasModules ? activeHighlightColor : inactiveHighlightColor,
                hasModules ? activeHeaderBannerColor : inactiveHeaderBannerColor, Alignment.MID, headingPad);
        TooltipMakerAPI modularArmor = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", hasModules ? "kol_modules" : "kol_modules_grey"), HEIGHT);
        modularArmor.setBulletedListMode("•");
        modularArmor.setBulletWidth(15f);
        modularArmor.addPara("Increases top speed and maneuverability by up to %s as armor panels are destroyed.",
                listPad, hasModules ? activeTextColor : inactiveTextColor, hasModules ? activePositiveColor: inactivePositiveColor, "25%");
        modularArmor.addPara("Armor panels have ablative armor, reducing the effective armor strength for damage reduction calculations to %s of its actual value.",
                listPad, hasModules ? activeTextColor : inactiveTextColor, hasModules ? activeNegativeColor: inactiveNegativeColor, "10%");

        if(hasModules){
            modularArmor.beginTable(
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Misc.getBrightPlayerColor(),
                    20f,
                    true,
                    true,
                    new Object[]{"Armor Location", width - 80f * 2 - HEIGHT - 25f, "Hull", 80f, "Armor", 80f});

            // getting the stats of child modules in refit shouldn't have to be this hard
            Pattern kolPattern = Pattern.compile("kol_.+?_[tml][lr]", Pattern.CASE_INSENSITIVE);

            for (String module : ship.getVariant().getStationModules().values()) {
                Matcher matcher = kolPattern.matcher(module);

                if(matcher.find()){
                    ShipHullSpecAPI hull = Global.getSettings().getHullSpec(matcher.group());

                    modularArmor.addRow(Alignment.MID, Misc.getTextColor(), hull.getHullName(),
                            Alignment.MID, Misc.getTextColor(), String.valueOf(Math.round(hull.getHitpoints())),
                            Alignment.MID, Misc.getTextColor(), String.valueOf(Math.round(hull.getArmorRating())));
                }
            }
            modularArmor.addTable("-", 0, 4f);

            modularArmor.setBulletedListMode("");
            modularArmor.setBulletWidth(0f);

            boolean addedHeading = false;
            for (String hullmodId : variant.getHullMods()) {
                if (hullmodEffects.containsKey(hullmodId)) {
                    if(!addedHeading){
                        modularArmor.addPara("Modules are receiving the following bonuses from your hullmods:", 10);
                        addedHeading = true;
                    }
                    modularArmor.addPara(Global.getSettings().getHullModSpec(hullmodId).getDisplayName(), 4f);
                    hullmodEffects.get(hullmodId).addTooltipText(modularArmor, ship);
                }
            }

            modularArmor.addPara("Hold 1 to highlight armor locations, 2 to revert.", Misc.getGrayColor(), 10);
            Color fadeAwayColor = Keyboard.isKeyDown(2) ? new Color(200,200,255, 80) : Color.white;
            if (Keyboard.isKeyDown(2) || Keyboard.isKeyDown(3)) {
                ship.getSpriteAPI().setColor(fadeAwayColor);
                for(ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()){
                    EngineSlotAPI engineSlot = engine.getEngineSlot();
                    Color originalCOlor = engineSlot.getColor();
                    engineSlot.setColor(new Color(originalCOlor.getRed(), originalCOlor.getGreen(), originalCOlor.getBlue(), 20));
                }
                for(WeaponAPI weapon : ship.getAllWeapons()){
                    if(Objects.equals(weapon.getSlot().getId(), "PUSHER_PLATE")){
                        weapon.getSprite().setColor(fadeAwayColor);
                    }
                    else if(weapon.getSprite() != null && weapon.isDecorative()){
                        weapon.getSprite().setColor(Keyboard.isKeyDown(2) ? new Color(255,255,255, 0) : Color.white);
                    }
                    else if(weapon.getSprite() != null){
                        weapon.getSprite().setColor(fadeAwayColor);
                        if(weapon.getBarrelSpriteAPI() != null)
                            weapon.getBarrelSpriteAPI().setColor(fadeAwayColor);
                    }
                }
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
            if(!s.isAlive()) continue;
            alive++;
            if(ship.getVariant() == null) continue;
            for(String hullmodId : ship.getVariant().getHullMods()){
                if (hullmodEffects.containsKey(hullmodId)) {
                    hullmodEffects.get(hullmodId).applyToStats("kol_modular_buff", s.getMutableStats(), ship.getHullSpec());
                }
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
