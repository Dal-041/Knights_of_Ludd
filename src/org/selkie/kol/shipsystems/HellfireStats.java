package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class HellfireStats extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS = 0.4f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float mult = 1f + DAMAGE_BONUS * effectLevel;

		stats.getBallisticWeaponDamageMult().modifyMult(id, mult);

		ShipAPI ship = (ShipAPI) stats.getEntity();
		for (WeaponAPI wpn : ship.getAllWeapons()) {
			// Only the gap between shots of a burst that is already firing is skipped. Weapons that are
			// reloading keep their remaining cooldown, so the system never hands out a free reload.
			if (wpn.getType() == WeaponAPI.WeaponType.BALLISTIC && wpn.isInBurst()) {
				wpn.setRemainingCooldownTo(0);
			}
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponDamageMult().unmodify(id);
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