package org.selkie.kol.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.MoteControlScript;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.hullmods.SparkleHullMod;

import java.awt.*;

public class SparkleOnHitEffect implements OnHitEffectPlugin {

//	public static float ANTI_FIGHTER_DAMAGE = 500;
//	public static float ANTI_FIGHTER_DAMAGE_HF = 1000;

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		boolean withEMP = false;
		ShipAPI source = projectile.getSource();
		if (target instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) target;
			if (!ship.isFighter() && source.hasTag(SparkleControlScript.HFCOOLDOWNTAG)
					|| !ship.isFighter() && source.getCustomData().containsKey("HF_SPARKLE_BOSS")) {
				float pierceChance = 1f;
				pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
				boolean piercedShield = shieldHit && (float) Math.random() < pierceChance;
				
				if (!shieldHit || piercedShield) {
					float emp = projectile.getEmpAmount();
					float dam = 1f;
					if (source.getCustomData().containsKey("HF_SPARKLE") && source.getCustomData().get("HF_SPARKLE").equals(true)
							|| source.getCustomData().containsKey("HF_SPARKLE_BOSS") && source.getCustomData().get("HF_SPARKLE_BOSS").equals(true)) {
						dam = SparkleHullMod.shipDamageHF; // 300
					} else {
						dam = SparkleHullMod.shipDamageReg; // 1
					}
					engine.spawnEmpArcPierceShields(source, point, target, target,
									   projectile.getDamageType(), 
									   dam,
									   emp,
									   100000f, // max range 
									   "mote_attractor_impact_emp_arc",
									   20f, // thickness
									   //new Color(100,165,255,255),
									   SparkleControlScript.getEMPColor(source),
									   new Color(255,255,255,255)
									   );
					withEMP = true;
				}
				
				//ship.getFluxTracker().increaseFlux(FLUX_RAISE_AMOUNT, shieldHit);
				
			} else {
//				float damage = ANTI_FIGHTER_DAMAGE;
//				if (MoteControlScript.isHighFrequency(projectile.getSource())) {
//					damage = ANTI_FIGHTER_DAMAGE_HF;
//				}
				float damage = SparkleControlScript.getAntiFighterDamage(source);
				Global.getCombatEngine().applyDamage(projectile, ship, point, 
						damage, DamageType.ENERGY, 0f, false, false, projectile.getSource(), true);
			}
		} else if (target instanceof MissileAPI) {
			float damage = SparkleControlScript.getAntiFighterDamage(source);
			Global.getCombatEngine().applyDamage(projectile, target, point, 
					damage, DamageType.ENERGY, 0f, false, false, projectile.getSource(), true);
		}
		
		//if (!withEMP) {
			String impactSoundId = SparkleControlScript.getImpactSoundId(projectile.getSource());
			Global.getSoundPlayer().playSound(impactSoundId, 1f, 1f, point, new Vector2f());
			//Global.getSoundPlayer().playSound("hit_glancing_energy", 1f, 1f, point, new Vector2f());
		//}
	}
}



