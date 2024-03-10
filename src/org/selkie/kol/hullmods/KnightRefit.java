package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;
import org.selkie.kol.combat.ShipExplosionListener;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.helpers.KOLStaticStrings;

import java.awt.*;
import java.util.List;
import java.util.*;
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
    public static final float SPEED_BONUS = 0.25f;
    protected Object SPEED_STATUS_KEY = new Object();
    public static final String KNIGHT_REFIT_STATMOD_ID = "knightRefit";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod(HullMods.ACCELERATED_SHIELDS)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.ACCELERATED_SHIELDS, KOLStaticStrings.KNIGHT_REFIT);
        if (ship.getVariant().hasHullMod(HullMods.OMNI_SHIELD_CONVERSION)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.OMNI_SHIELD_CONVERSION, KOLStaticStrings.KNIGHT_REFIT);
        if (ship.getVariant().hasHullMod(HullMods.FRONT_SHIELD_CONVERSION)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.FRONT_SHIELD_CONVERSION, KOLStaticStrings.KNIGHT_REFIT);
        if (ship.getVariant().hasHullMod(HullMods.EXTENDED_SHIELDS)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.EXTENDED_SHIELDS, KOLStaticStrings.KNIGHT_REFIT);

        PersonAPI captain = ship.getOriginalCaptain();
        MutableCharacterStatsAPI stats = captain == null ? null : captain.getFleetCommanderStats();
        float capBonus = 0;
        float dissBonus = 0;
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if (weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.COMPOSITE && weapon.getType() == WeaponAPI.WeaponType.BALLISTIC){
                int opCost = (int) weapon.getSpec().getOrdnancePointCost(stats, ship.getMutableStats());
                capBonus += opCost * FLUX_CAP_PER_OP;
                dissBonus += opCost * FLUX_DISS_PER_OP;
            }
        }
        ship.getMutableStats().getFluxCapacity().modifyFlat(id, capBonus);
        ship.getMutableStats().getFluxDissipation().modifyFlat(id, dissBonus);

        if(!ship.hasListenerOfClass(ModuleUnhulker.class)) ship.addListener(new ModuleUnhulker());
        if(!ship.hasListenerOfClass(ShipExplosionListener.class)) ship.addListener(new ShipExplosionListener());
        if(!ship.hasListenerOfClass(ExplosionOcclusionRaycast.class)) ship.addListener(new ExplosionOcclusionRaycast());
    }

    public static class ModuleUnhulker implements HullDamageAboutToBeTakenListener {
        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if(ship.getHitpoints() <= damageAmount) {
                for(ShipAPI module: ship.getChildModulesCopy()){
                    if(!module.hasTag(KnightModule.KOL_MODULE_DEAD)){
                        module.setHulk(false);
                        module.addTag(KnightModule.KOL_MODULE_DEAD);
                    }
                }
            }
            return false;
        }
    }

    public static class ExplosionOcclusionRaycast implements DamageTakenModifier {
        public static final int NUM_RAYCASTS = 36;
        public static final String RAYCAST_KEY = "kol_module_explosion_raycast";
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            ShipAPI ship = (ShipAPI) target;
            ShipAPI parent = ship.getParentStation() == null ? ship : ship.getParentStation();

            if (param instanceof DamagingExplosion || param instanceof MissileAPI) {
                DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;


                HashMap<DamagingProjectileAPI, HashMap<String, Float>> explosionMap = (HashMap<DamagingProjectileAPI, HashMap<String, Float>>) parent.getCustomData().get(RAYCAST_KEY);

                // if this is the explosion from the missile, look up the cached result and use that
                if(projectile instanceof DamagingExplosion && explosionMap != null){
                    for(DamagingProjectileAPI pastProjectile : explosionMap.keySet()){
                        if (pastProjectile instanceof MissileAPI && pastProjectile.getSource() == projectile.getSource() &&
                                MathUtils.getDistanceSquared(pastProjectile.getLocation(), projectile.getSpawnLocation()) < 1f){
                            projectile = pastProjectile;
                            break;
                        }
                    }
                }

                generateExplosionRayhitMap(projectile, damage, parent);
                explosionMap = (HashMap<DamagingProjectileAPI, HashMap<String, Float>>) parent.getCustomData().get(RAYCAST_KEY);

                if(!explosionMap.containsKey(projectile) || !explosionMap.get(projectile).containsKey(ship.getId())){
                    return null;
                }

                damage.getModifier().modifyMult(this.getClass().getName(), explosionMap.get(projectile).get(ship.getId()));
                return this.getClass().getName();
            }
            return null;
        }

        public void generateExplosionRayhitMap(DamagingProjectileAPI projectile, DamageAPI damage, ShipAPI parent){

            if(!parent.getCustomData().containsKey(RAYCAST_KEY) || !(parent.getCustomData().get(RAYCAST_KEY) instanceof HashMap)){
                parent.setCustomData(RAYCAST_KEY, new HashMap<DamagingProjectileAPI, HashMap<String, Float>>());
            }
            HashMap<DamagingProjectileAPI, HashMap<String, Float>> explosionMap = (HashMap<DamagingProjectileAPI, HashMap<String, Float>>) parent.getCustomData().get(RAYCAST_KEY);
            if(explosionMap.containsKey(projectile)){
                return;
            }

            HashMap<String, Float> damageReductionMap = new HashMap<>();
            explosionMap.put(projectile, damageReductionMap);
            damageReductionMap.put("RemovalTime", Global.getCombatEngine().getTotalElapsedTime(false) + 1f);

            // remove old cached explosions
            for(Iterator<Map.Entry<DamagingProjectileAPI, HashMap<String, Float>>> pastProjectileIterator = explosionMap.entrySet().iterator(); pastProjectileIterator.hasNext();){
                Map.Entry<DamagingProjectileAPI, HashMap<String, Float>> pastProjectile = pastProjectileIterator.next();
                if (pastProjectile.getValue().get("RemovalTime") < Global.getCombatEngine().getTotalElapsedTime(false)) pastProjectileIterator.remove();
            }

            // init all occlusions
            List<ShipAPI> potentialOcclusions = parent.getChildModulesCopy();
            potentialOcclusions.add(parent);
            HashMap<String, Integer> hitsMap = new HashMap<>();
            for(ShipAPI occlusion: potentialOcclusions){
                damageReductionMap.put(occlusion.getId(), 1f);
                hitsMap.put(occlusion.getId(), 0);
            }

            // skip if not an explosion
            Vector2f explosionLocation;
            List<CombatEntityAPI> damagedAlready = new ArrayList<>();
            float radius;
            if (projectile instanceof MissileAPI || projectile instanceof DamagingExplosion) {
                if(projectile instanceof MissileAPI ){
                    MissileAPI missile = (MissileAPI) projectile;
                    if(missile.getDamagedAlready() != null) damagedAlready = missile.getDamagedAlready();
                    explosionLocation = missile.getLocation();
                    radius = missile.getSpec().getExplosionRadius();
                } else{
                    DamagingExplosion explosion = (DamagingExplosion) projectile;
                    if(explosion.getDamagedAlready() != null) damagedAlready = explosion.getDamagedAlready();
                    explosionLocation = explosion.getLocation();
                    radius = explosion.getCollisionRadius();
                }
            } else{
                return;
            }


            // remove out of range occlusions
            for (Iterator<ShipAPI> occlusionIter = potentialOcclusions.iterator(); occlusionIter.hasNext();){
                ShipAPI occlusion = occlusionIter.next();
                float explosionDistance = Misc.getTargetingRadius(explosionLocation, occlusion, false) + radius;
                float moduleDistance = MathUtils.getDistanceSquared(explosionLocation, occlusion.getLocation());
                if(moduleDistance > (explosionDistance * explosionDistance)){
                    occlusionIter.remove();
                }
            }

            // skip if there is 0 or 1 ship in range
            if(potentialOcclusions.isEmpty() || potentialOcclusions.size() == 1){
                return;
            }

            // if more then 1 thing is in range, then raycast to check for explosion mults
            int totalRayHits = 0;

            List<Vector2f> rayEndpoints = MathUtils.getPointsAlongCircumference(explosionLocation, radius, NUM_RAYCASTS, 0f);
            for(Vector2f endpoint : rayEndpoints){
                float closestDistanceSquared = radius * radius;
                String occlusionID = null;
                for(ShipAPI occlusion : potentialOcclusions){ //  for each ray loop past all occlusions
                    Vector2f pointOnModuleBounds = CollisionUtils.getCollisionPoint(explosionLocation, endpoint, occlusion);

                    if(pointOnModuleBounds != null){ // if one is hit
                        float occlusionDistance = MathUtils.getDistanceSquared(explosionLocation, pointOnModuleBounds);
                        if(occlusionDistance < closestDistanceSquared){ // check the distance, if its shorter remember it
                            occlusionID = occlusion.getId();
                            closestDistanceSquared = occlusionDistance;
                        }
                    }
                }
                if(occlusionID != null){ // only not null if something is hit, in that case inc TotalRayHits
                    totalRayHits++;
                    hitsMap.put(occlusionID, hitsMap.get(occlusionID) + 1);
                }
            }
            if(totalRayHits == 0) return;

            float overkillDamage = 0f;
            for(ShipAPI occlusion : potentialOcclusions){
                if(occlusion == parent) continue; // special case the parent
                // calculate and set the damage mult
                float rayHits = (float) hitsMap.get(occlusion.getId());
                float damageMult = Math.min(1f, Math.max(rayHits / totalRayHits, rayHits /((float) NUM_RAYCASTS /2)));
                damageReductionMap.put(occlusion.getId(), damageMult);

                // calculate the actual hp left over after the hit, if damage > hp, note down the overflow
                float moduleArmor = StarficzAIUtils.getCurrentArmorRating(occlusion);
                Pair<Float, Float> damageResult = StarficzAIUtils.damageAfterArmor(damage.getType(), damage.getDamage()*damageMult, damage.getDamage(), moduleArmor, occlusion);
                float hullDamage = damageResult.two;

                if(hullDamage > occlusion.getHitpoints() && !damagedAlready.contains(occlusion)){
                    overkillDamage += hullDamage - occlusion.getHitpoints();
                }
            }

            // do the same mult calc for the parent, except also subtract overkill from the reduction
            float damageMult = (float) hitsMap.get(parent.getId()) / totalRayHits;
            damageReductionMap.put(parent.getId(), ((damage.getDamage() * damageMult) + overkillDamage)/damage.getDamage());
        }
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


        ShipVariantAPI variant = Global.getSettings().getVariant(ship.getHullSpec().getBaseHullId() + "_Blank");
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

            for (String module : variant.getStationModules().values()) {
                Matcher matcher = kolPattern.matcher(module);

                if(matcher.find()){
                    ShipHullSpecAPI hull = Global.getSettings().getHullSpec(matcher.group());
                    float hullMult = getTotalHullMult(ship.getVariant(), hull.getHitpoints());
                    float armorMult = getTotalArmorMult(ship.getVariant(), hull.getArmorRating());

                    Color hullTextColor = hullMult < 0.99f ? Misc.getPositiveHighlightColor() : (hullMult > 1.01f ? Misc.getNegativeHighlightColor() : Misc.getTextColor());
                    Color armorTextColor = armorMult < 0.99f ? Misc.getPositiveHighlightColor() : (armorMult > 1.01f ? Misc.getNegativeHighlightColor() : Misc.getTextColor());

                    modularArmor.addRow(Alignment.MID, Misc.getTextColor(), hull.getHullName(),
                            Alignment.MID, hullTextColor, String.valueOf(Math.round(hull.getHitpoints() / hullMult)),
                            Alignment.MID, armorTextColor, String.valueOf(Math.round(hull.getArmorRating() / armorMult)));
                }
            }
            modularArmor.addTable("-", 0, 4f);

            modularArmor.setBulletedListMode("");
            modularArmor.setBulletWidth(0f);

            modularArmor.addPara("Hold 1 to highlight armor locations, 2 to revert.", Misc.getGrayColor(), 10);
            Color fadeAwayColor = Keyboard.isKeyDown(2) ? new Color(200,200,255, 80) : Color.white;
            if (Keyboard.isKeyDown(2) || Keyboard.isKeyDown(3)) {
                ship.getSpriteAPI().setColor(fadeAwayColor);
                for(ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()){
                    EngineSlotAPI engineSlot = engine.getEngineSlot();
                    engineSlot.setColor(Misc.setAlpha(engineSlot.getColor(), Keyboard.isKeyDown(2) ? 20 : 255));
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
        //Note by Starficz, I've refactored this code so much I have no clue how much was from Tart anymore
        if (ship==null) {
            return;
        }

        if (!ship.isAlive()) {
            removeStats(ship);
            return;
        }

        if(Global.getCombatEngine().isPaused()) return;

        // apply speed boost for main ship and durability buffs to modules from main ships hullmods
        ShipVariantAPI variant = Global.getSettings().getVariant(ship.getHullSpec().getBaseHullId() + "_Blank");
        float modules = variant == null ? 0 : variant.getStationModules().size();

        float alive = 0;
        for(ShipAPI module : ship.getChildModulesCopy()){
            if (module.getHitpoints() <= 0f) continue;
            alive++;
            if(ship.getVariant() == null || module.getVariant() == null) continue;

            float hullmult =  getTotalHullMult(ship.getVariant(), module.getVariant().getHullSpec().getHitpoints());
            float armorMult =  getTotalArmorMult(ship.getVariant(), module.getVariant().getHullSpec().getArmorRating());

            module.getMutableStats().getHullDamageTakenMult().modifyMult("kol_module_parent_hullmods", hullmult);
            module.getMutableStats().getArmorDamageTakenMult().modifyMult("kol_module_parent_hullmods", armorMult);
        }

        if(modules!=0){
            //speed bonus applies linearly
            float speedRatio=1 - (alive / modules);
            applyStats(speedRatio, ship);
        }
    }

    private void removeStats(ShipAPI ship) {
        ship.getMutableStats().getMaxSpeed().unmodify(KNIGHT_REFIT_STATMOD_ID);
        ship.getMutableStats().getAcceleration().unmodify(KNIGHT_REFIT_STATMOD_ID);
        ship.getMutableStats().getDeceleration().unmodify(KNIGHT_REFIT_STATMOD_ID);
        ship.getMutableStats().getMaxTurnRate().unmodify(KNIGHT_REFIT_STATMOD_ID);
        ship.getMutableStats().getTurnAcceleration().unmodify(KNIGHT_REFIT_STATMOD_ID);
    }

    private void applyStats(float speedRatio, ShipAPI ship) {
        ship.getMutableStats().getMaxSpeed().modifyMult(KNIGHT_REFIT_STATMOD_ID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getAcceleration().modifyMult(KNIGHT_REFIT_STATMOD_ID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getDeceleration().modifyMult(KNIGHT_REFIT_STATMOD_ID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getMaxTurnRate().modifyMult(KNIGHT_REFIT_STATMOD_ID, (1 + (speedRatio * SPEED_BONUS)));
        ship.getMutableStats().getTurnAcceleration().modifyMult(KNIGHT_REFIT_STATMOD_ID, (1 + (speedRatio * SPEED_BONUS)));

        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.getPlayerShip() == ship && speedRatio > 0.01f){
            String modularIcon = Global.getSettings().getSpriteName("icons", "kol_modules");
            engine.maintainStatusForPlayerShip(SPEED_STATUS_KEY, modularIcon, "Damaged Modular Armor", "+" + Math.round((speedRatio * SPEED_BONUS * 100)) + " top speed" , false);
        }
    }

    public float getTotalArmorMult(ShipVariantAPI variant, float baseArmor){
        if(variant == null) return 1f;
        Map<String, ArmorEffect> effects = HULLMOD_EFFECTS.get(variant.getHullSize());
        float totalFlat = 0;
        float totalPercent = 0;
        boolean isAblative = true; //TODO: fix this hack
        for(String hullmodID : variant.getHullMods()){
            if(effects.containsKey(hullmodID)){
                totalFlat += effects.get(hullmodID).armorFlat;
                totalPercent += effects.get(hullmodID).armorPercent;
            }
            if(Objects.equals(hullmodID, "ablative_armor")) isAblative = true;
        }
        baseArmor = baseArmor * (isAblative ? 0.1f :1f);
        return baseArmor / (baseArmor + totalFlat + (baseArmor * totalPercent));
    }

    public float getTotalHullMult(ShipVariantAPI variant, float baseHull){
        if(variant == null) return 1f;
        Map<String, ArmorEffect> effects = HULLMOD_EFFECTS.get(variant.getHullSize());
        float totalFlat = 0;
        float totalPercent = 0;
        for(String hullmodID : variant.getHullMods()){
            if(effects.containsKey(hullmodID)){
                totalFlat += effects.get(hullmodID).hullFlat;
                totalPercent += effects.get(hullmodID).hullPercent;
            }
        }
        return baseHull / (baseHull + totalFlat + (baseHull * totalPercent));
    }

    private static final Map<ShipAPI.HullSize, Map<String, ArmorEffect>> HULLMOD_EFFECTS = new HashMap<>();

    static {
        Map<String, ArmorEffect> hullmodMap = new HashMap<>();
        hullmodMap.put(HullMods.REINFORCEDHULL, new ArmorEffect(0,0,0,0.4f));
        hullmodMap.put(HullMods.BLAST_DOORS, new ArmorEffect(0,0,0,0.2f));
        hullmodMap.put(HullMods.INSULATEDENGINE, new ArmorEffect(0,0,0,0.1f));
        hullmodMap.put(HullMods.ARMOREDWEAPONS, new ArmorEffect(0,0.1f,0,0));
        hullmodMap.put(HullMods.COMP_HULL, new ArmorEffect(0,0f,0,-0.2f));
        hullmodMap.put(HullMods.COMP_ARMOR, new ArmorEffect(0,-0.2f,0,0));
        hullmodMap.put(HullMods.COMP_STRUCTURE, new ArmorEffect(0,-0.2f,0,-0.2f));

        Map<String, ArmorEffect> capitalHullmodMap = new HashMap<>(hullmodMap);
        capitalHullmodMap.put(HullMods.HEAVYARMOR, new ArmorEffect(500,0,0,0));
        HULLMOD_EFFECTS.put(ShipAPI.HullSize.CAPITAL_SHIP, capitalHullmodMap);

        Map<String, ArmorEffect> cruiserHullmodMap = new HashMap<>(hullmodMap);
        cruiserHullmodMap.put(HullMods.HEAVYARMOR, new ArmorEffect(400,0,0,0));
        HULLMOD_EFFECTS.put(ShipAPI.HullSize.CRUISER, cruiserHullmodMap);

        Map<String, ArmorEffect> destroyerHullmodMap = new HashMap<>(hullmodMap);
        destroyerHullmodMap.put(HullMods.HEAVYARMOR, new ArmorEffect(300,0,0,0));
        HULLMOD_EFFECTS.put(ShipAPI.HullSize.DESTROYER, destroyerHullmodMap);

        Map<String, ArmorEffect> frigateHullmodMap = new HashMap<>(hullmodMap);
        frigateHullmodMap.put(HullMods.HEAVYARMOR, new ArmorEffect(150,0,0,0));
        HULLMOD_EFFECTS.put(ShipAPI.HullSize.FRIGATE, frigateHullmodMap);

        Map<String, ArmorEffect> fighterHullmodMap = new HashMap<>(hullmodMap);
        fighterHullmodMap.put(HullMods.HEAVYARMOR, new ArmorEffect(75,0,0,0));
        HULLMOD_EFFECTS.put(ShipAPI.HullSize.FIGHTER, fighterHullmodMap);
    }

    public static class ArmorEffect {
        public float armorFlat, armorPercent, hullFlat, hullPercent;
        ArmorEffect(float aF, float aP, float hF, float hP){
            armorFlat = aF; armorPercent = aP; hullFlat = hF; hullPercent = hP;
        }
    }
}
