package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import java.awt.Color;
import java.util.EnumSet;

public class CascadeTargetingProtocol extends BaseHullMod {

    //Should be synced with ZeaMechnicIntel
    public static final float RANGE_BONUS = 15f; // 200su
    public static final float ENMITY_BONUS_ROF_RELOAD = 20f;
    public static final float ENMITY_BONUS_FLUX_REDUCTION = 10f;
    //smooth instead of flat
    //public static final float HULL_PERCENTAGE = 0.5f;
    private final String ID;
    public CascadeTargetingProtocol() {
        this.ID = "CascadeTargetingProtocol";
    }    
    
    @Override
    public void advanceInCombat(final ShipAPI ship, final float amount) {

        if (!ship.isAlive()){
            return;
        }
        final CombatEngineAPI engine = Global.getCombatEngine();
        final float HPLeftRatio = ship.getHitpoints() / ship.getMaxHitpoints();

        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(this.ID, RANGE_BONUS * HPLeftRatio);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(this.ID, RANGE_BONUS * HPLeftRatio);
        ship.getMutableStats().getMissileWeaponRangeBonus().modifyPercent(this.ID, RANGE_BONUS * HPLeftRatio);
        ship.getMutableStats().getBallisticRoFMult().modifyPercent(this.ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio));
        ship.getMutableStats().getEnergyRoFMult().modifyPercent(this.ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio));
        ship.getMutableStats().getBallisticAmmoRegenMult().modifyPercent(this.ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio));
        ship.getMutableStats().getEnergyAmmoRegenMult().modifyPercent(this.ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio));
        ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyPercent(this.ID, -ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio));
        ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyPercent(this.ID, -ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio));
        // fuckin glow
        final EnumSet<WeaponAPI.WeaponType> WEAPON_TYPES = EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY);
        ship.setWeaponGlow(1f, new Color(235,20,25,250), WEAPON_TYPES);

        if (engine.getPlayerShip() != ship) {
            return;
        }

        engine.maintainStatusForPlayerShip((Object)(String.valueOf(this.ID) + "_TOOLTIP_SNEEDS"), "graphics/icons/campaign/sensor_strength.png", "Cascade Targeting Protocol", "+" + Math.round(RANGE_BONUS * HPLeftRatio) + "% weapon range", false);
        engine.maintainStatusForPlayerShip((Object)(String.valueOf(this.ID) + "_TOOLTIP_FEED"), "graphics/icons/campaign/sensor_strength.png", "Cascade Targeting Protocol", "+" + Math.round(ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio)) + "% weapon ROF and recharge rate", false);
        engine.maintainStatusForPlayerShip((Object)(String.valueOf(this.ID) + "_TOOLTIP_SEED"), "graphics/icons/campaign/sensor_strength.png", "Cascade Targeting Protocol", "-" + Math.round(ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio)) + "% weapon flux cost", false);
    }

    @Override
    public String getDescriptionParam(final int index, final ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0:
                return "200 su";
            case 1:
                return "50%";
            case 2:
                return "below";
            case 3:
                return "50%";
            case 4:
                return "30%";
            case 5:
                return "15%";
            default:
                break;
        }
        return null;
    }
        
}