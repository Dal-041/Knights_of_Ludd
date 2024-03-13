package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class HellfireStats extends BaseShipSystemScript {

	public static final float PROJECTILE_SPEED_BONUS = 0.3f;
	public static final float DAMAGE_BONUS = 0.4f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		float mult = 1f + DAMAGE_BONUS * effectLevel;
		float mult2 = 1f + PROJECTILE_SPEED_BONUS * effectLevel;

		stats.getBallisticWeaponDamageMult().modifyMult(id, mult);
		stats.getBallisticProjectileSpeedMult().modifyMult(id, mult2);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getBallisticProjectileSpeedMult().unmodify(id);
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