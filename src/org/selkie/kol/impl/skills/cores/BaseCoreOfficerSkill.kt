package org.selkie.kol.impl.skills.cores

import com.fs.starfarer.api.characters.*
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI

abstract class BaseCoreOfficerSkill : ShipSkillEffect, CustomSkillDescription {
    final override fun getEffectDescription(level: Float): String {
        return ""
    }

    final override fun getEffectPerLevelDescription(): String {
        return ""
    }

    abstract override fun getScopeDescription(): LevelBasedEffect.ScopeDescription

    override fun hasCustomDescription(): Boolean {
        return true
    }

    abstract override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI?,  width: Float)

    abstract override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float)

    abstract override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?)
}