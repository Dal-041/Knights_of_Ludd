package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill
import java.util.*



class DawnBossCoreOfficerPlugin : AICoreOfficerPlugin {

    var automatedPointsMult = 4f
    var uniqueSkillID = "zea_dawn_boss_core_skill"
    var portraitID = "zea_dawn_boss_core"

    override fun createPerson(aiCoreId: String?, factionId: String?, random: Random?): PersonAPI {
        var core = AICoreUtil.createCorePerson(aiCoreId, factionId)
        core.stats.level = 8
        core.setPersonality(Personalities.RECKLESS)
        core.setRankId(Ranks.SPACE_CAPTAIN)

        core.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitID))

        core.stats.setSkillLevel(Skills.HELMSMANSHIP, 2F)
        core.stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2F)
        core.stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2F)
        core.stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2F)
        core.stats.setSkillLevel(Skills.FIELD_MODULATION, 2F)
        core.stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2F)
        core.stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2F)
        core.stats.setSkillLevel(uniqueSkillID, 2f)

        core.memoryWithoutUpdate.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, automatedPointsMult)
        return core
    }

    override fun createPersonalitySection(person: PersonAPI, tooltip: TooltipMakerAPI) {
        val opad = 10f
        val text = person!!.faction.baseUIColor
        val bg = person.faction.darkUIColor
        val spec = Global.getSettings().getCommoditySpec(person.aiCoreId)

        var skill = Global.getSettings().getSkillSpec(uniqueSkillID)
        var pointsMult = automatedPointsMult.toInt()

        AICoreUtil.addTooltip(person, tooltip, pointsMult, skill, AICoreUtil.BossCore.Dawn)
    }

}
