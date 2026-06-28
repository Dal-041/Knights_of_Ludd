package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.selkie.kol.ReflectionUtilsV2;

import java.util.Objects;

public class HellfireStats extends BaseShipSystemScript {

	public static final float PROJECTILE_SPEED_BONUS = 0.3f;
	public static final float DAMAGE_BONUS = 0.4f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float mult = 1f + DAMAGE_BONUS * effectLevel;
		float mult2 = 1f + PROJECTILE_SPEED_BONUS * effectLevel;

		stats.getBallisticWeaponDamageMult().modifyMult(id, mult);
		stats.getBallisticProjectileSpeedMult().modifyMult(id, mult2);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship.getCustomData().get("KOL_hellfireReloadedAlready") == null) {
			for (WeaponAPI wpn : ship.getAllWeapons()) {
				if (wpn.getType() != WeaponAPI.WeaponType.BALLISTIC) {
					continue;
				}
                if (wpn.isInBurst()) {
                    continue;
                }

				wpn.setRemainingCooldownTo(0); // this instantly finishes their burst and makes them instantly fire the rest of their salvo
			}
			ship.setCustomData("KOL_hellfireReloadedAlready", true);
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getBallisticProjectileSpeedMult().unmodify(id);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.removeCustomData("KOL_hellfireReloadedAlready");
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + DAMAGE_BONUS * effectLevel;
		float bonusPercent1 = ((mult - 1f) * 100f);
		float mult2 = 1f + PROJECTILE_SPEED_BONUS * effectLevel;
		float bonusPercent2 = ((mult2 - 1f) * 100f);
		if (index == 0) {
			return new StatusData("ballistics damage bonus +" + Math.round(bonusPercent1) + "%", false);
		}
		if (index == 1) {
			return new StatusData("ballistics projectile speed +" + Math.round(bonusPercent2) + "%", false);
		}
		return null;
	}
}