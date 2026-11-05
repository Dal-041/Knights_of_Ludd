package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class HellfireStats extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS = 0.4f;
	private static final String RELOADED_KEY = "KOL_hellfireReloadedAlready";

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float mult = 1f + DAMAGE_BONUS * effectLevel;

		stats.getBallisticWeaponDamageMult().modifyMult(id, mult);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship.getCustomData().get(RELOADED_KEY) == null) {
			for (WeaponAPI wpn : ship.getAllWeapons()) {
				if (wpn.getType() != WeaponAPI.WeaponType.BALLISTIC) {
					continue;
				}

				wpn.setRemainingCooldownTo(0);
				AmmoTrackerAPI ammo = wpn.getAmmoTracker();
				if (ammo.usesAmmo()) {
					ammo.resetAmmo();
				}
			}
			ship.setCustomData(RELOADED_KEY, true);
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponDamageMult().unmodify(id);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.removeCustomData(RELOADED_KEY);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + DAMAGE_BONUS * effectLevel;
		float bonusPercent1 = ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("ballistics damage bonus +" + Math.round(bonusPercent1) + "%", false);
		}
		return null;
	}
}