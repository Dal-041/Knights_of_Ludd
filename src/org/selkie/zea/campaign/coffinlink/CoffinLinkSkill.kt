package org.selkie.zea.campaign.coffinlink

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.helpers.ZeaStaticStrings.CoffinLink

/** Effects of the COFFIN Link skill (zea_elysia_COFFIN_link_skill.skill). */
object CoffinLinkSkill {

    const val MAX_CR_BONUS = 0.15f

    /** Description only: piloting itself is enabled by CoffinLinkScript and CoffinLinkOfficerPlugin. */
    class Pilot : ShipSkillEffect {
        override fun getEffectDescription(level: Float): String = "Allows you to personally command automated ships"

        override fun getEffectPerLevelDescription(): String? = null

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription = LevelBasedEffect.ScopeDescription.CUSTOM

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {}

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {}
    }

    /** Automated ships you pilot get extra maximum CR, standing in for the missing AI core. */
    class MaxCR : BaseSkillEffectDescription(), ShipSkillEffect {
        override fun getEffectPerLevelDescription(): String? = null

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription = LevelBasedEffect.ScopeDescription.CUSTOM

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            if (Misc.isAutomated(stats)) {
                val skillName = Global.getSettings().getSkillSpec(CoffinLink.SKILL_ID).name
                stats.maxCombatReadiness.modifyFlat(id, MAX_CR_BONUS, "$skillName skill")
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String) {
            stats.maxCombatReadiness.unmodify(id)
        }

        override fun createCustomDescription(stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI, width: Float) {
            val h = Misc.getHighlightColor()
            info.addPara("+%s maximum combat readiness for piloted automated ships", 0f, h, "${(MAX_CR_BONUS * 100f).toInt()}%")
        }
    }
}
