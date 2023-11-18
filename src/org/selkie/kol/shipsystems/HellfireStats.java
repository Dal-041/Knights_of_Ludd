package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class HellfireStats extends BaseShipSystemScript {

	public static final float WEAPON_BONUS = 0.3f;
	//public static final float DAMAGE_BONUS = 0.3f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float mult = 1f + WEAPON_BONUS * effectLevel;
		//float mult2 = 1f + WEAPON_BONUS * effectLevel;
		stats.getBallisticWeaponDamageMult().modifyMult(id, mult);
		stats.getBallisticProjectileSpeedMult().modifyMult(id, mult);

//		ShipAPI ship = (ShipAPI)stats.getEntity();
//		ship.blockCommandForOneFrame(ShipCommand.FIRE);
//		ship.setHoldFireOneFrame(true);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getBallisticProjectileSpeedMult().unmodify(id);
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		float mult = 1f + WEAPON_BONUS * effectLevel;
		float bonusPercent1 = (int) ((mult - 1f) * 100f);
		//float mult2 = 1f + DAMAGE_BONUS * effectLevel;
		float bonusPercent2 = (int) ((mult - 1f) * 100f);
		if (index == 0) {
			return new StatusData("ballistics damage bonus +" + (int) bonusPercent1 + "%", false);
		}
		if (index == 1) {
			return new StatusData("ballistics projectile speed +" + (int) bonusPercent2 + "%", false);
		}
		return null;
	}
}