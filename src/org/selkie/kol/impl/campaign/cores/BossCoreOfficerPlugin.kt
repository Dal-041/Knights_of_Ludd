package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.impl.helpers.ZeaStaticStrings
import org.selkie.kol.impl.skills.cores.BaseCoreOfficerSkill
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill
import java.util.*

abstract class BossCoreOfficerPlugin : AICoreOfficerPlugin {
    var automatedPointsMult = 4f
    override fun createPerson(aiCoreId: String?, factionId: String?, random: Random?): PersonAPI {
        val spec = Global.getSettings().getCommoditySpec(aiCoreId)
        val core = Global.getFactory().createPerson()

        core.id = Misc.genUID()
        core.setFaction(factionId)
        core.aiCoreId = aiCoreId
        core.name = FullName(spec.name, "", FullName.Gender.ANY)

        core.stats.isSkipRefresh = false
        core.stats.level = 8
        core.setPersonality(Personalities.RECKLESS)
        core.rankId = Ranks.SPACE_CAPTAIN

        core.stats.setSkillLevel(Skills.HELMSMANSHIP, 2F)
        core.stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2F)
        core.stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2F)
        core.stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2F)
        core.stats.setSkillLevel(Skills.FIELD_MODULATION, 2F)
        core.stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2F)
        core.stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2F)
        core.stats.setSkillLevel(exclusiveSkill.skillID, 2f)

        core.portraitSprite = Global.getSettings().getSpriteName("characters", portraitSpriteName)

        core.memoryWithoutUpdate.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, automatedPointsMult)
        return core
    }

    override fun createPersonalitySection(person: PersonAPI, tooltip: TooltipMakerAPI) {
        val opad = 10f
        val text = person.faction.baseUIColor
        val bg = person.faction.darkUIColor
        val spec = Global.getSettings().getCommoditySpec(person.aiCoreId)
        val skill = Global.getSettings().getSkillSpec(exclusiveSkill.skillID)
        val pointsMult = automatedPointsMult.toInt()

        tooltip.addSpacer(opad)
        tooltip.addPara("Automated Points Multiplier: ${pointsMult}${Strings.X}", 0f, Misc.getTextColor(), Misc.getHighlightColor(),  "Automated Points Multiplier")
        tooltip.addSpacer(opad)

        tooltip.addSectionHeading("Signature Skill: ${skill.name}", Alignment.MID, 0f)
        tooltip.addSpacer(opad)

        val skillImg = tooltip.beginImageWithText(skill.spriteName, 48f)

        exclusiveSkill.createCustomDescription(null, null, skillImg, tooltip.widthSoFar)

        tooltip.addImageWithText(0f)

        if (person.personalityAPI.id == Personalities.RECKLESS)
        {
            tooltip.addSectionHeading("Personality: Fearless", text, bg, Alignment.MID, opad)
            tooltip.addPara("In combat, the " + spec.name + " is single-minded and determined. " + "In a human captain, its traits might be considered reckless. In a machine, they're terrifying.",
                opad)
        }
    }

    protected abstract val exclusiveSkill: BaseCoreOfficerSkill
    protected abstract val coreItemID: String
    protected abstract val portraitSpriteName: String
}