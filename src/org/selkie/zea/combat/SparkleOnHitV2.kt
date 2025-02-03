package org.selkie.zea.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.lwjgl.util.vector.Vector2f
import org.selkie.zea.hullmods.DuskBuiltin
import java.awt.Color

class SparkleOnHitV2 : OnHitEffectPlugin {
    override fun onHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI?, point: Vector2f?,
                       shieldHit: Boolean, damageResult: ApplyDamageResultAPI?, engine: CombatEngineAPI) {
        val isHF = projectile.source?.hasTag(DuskBuiltin.HF_TAG) == true || ((projectile as? MissileAPI)?.unwrappedMissileAI as? SparkleAIV2)?.hfOverride == true
        val shipDamage = if(isHF) 300f else 1f
        val shipEMPDamage = 1500f
        val fighterDamage = if(isHF) 1000f else 200f
        val empColor = if(isHF) SparkleAIV2.hfEMPColor else SparkleAIV2.baseEMPColor
        val impactSoundId = if(isHF) "mote_attractor_impact_damage" else "mote_attractor_impact_normal"

        if (target is ShipAPI) {
            if(!target.isFighter){
                val pierceChance = (target.hardFluxLevel - 0.1f) * (projectile.source?.mutableStats?.dynamic?.getValue(Stats.SHIELD_PIERCED_MULT) ?: 0f)
                val piercedShield = shieldHit && Math.random().toFloat() < pierceChance
                if (!shieldHit || piercedShield) {
                    engine.spawnEmpArcPierceShields(projectile.source, point, target, target,
                        projectile.damageType,
                        shipDamage,
                        shipEMPDamage,
                        100000f,  // max range
                        "mote_attractor_impact_emp_arc",
                        20f,
                        empColor,
                        Color(255, 255, 255, 255)
                    )
                }
            } else{
                engine.applyDamage(projectile, target, point, fighterDamage, DamageType.ENERGY, 0f, false, false, projectile.source, true)
            }
        } else if (target is MissileAPI) {
            engine.applyDamage(projectile, target, point, fighterDamage, DamageType.ENERGY, 0f, false, false, projectile.source, true)
        }

        Global.getSoundPlayer().playSound(impactSoundId, 1f, 1f, point, Vector2f())
    }
}