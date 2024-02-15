package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill

object AICoreUtil
{

    @JvmStatic
    fun createCorePerson(aiCoreId: String?, factionId: String?) : PersonAPI
    {
        val spec = Global.getSettings().getCommoditySpec(aiCoreId)
        var core = Global.getFactory().createPerson()

        core.id = Misc.genUID()
        core.setFaction(factionId)
        core.setAICoreId(aiCoreId)
        core.setName(FullName(spec.getName(), "", FullName.Gender.ANY))

        core.setPortraitSprite(spec.iconName)

        core.stats.isSkipRefresh = false
        return core
    }


    enum class BossCore {
        Dusk, Dawn, Elysia
    }

    @JvmStatic
    fun addTooltip(person: PersonAPI, tooltip: TooltipMakerAPI, pointsMult: Int, skill: SkillSpecAPI, core: BossCore)
    {
        tooltip.addSpacer(10f)
        tooltip.addPara("Automated Points Multiplier: ${pointsMult}${Strings.X}", 0f, Misc.getTextColor(), Misc.getHighlightColor(),  "Automated Points Multiplier")
        tooltip.addSpacer(10f)

        tooltip.addSectionHeading("Signature Skill: ${skill.name}", Alignment.MID, 0f)
        tooltip.addSpacer(10f)

        var skillImg = tooltip.beginImageWithText(skill.spriteName, 48f)

        when (core) {
            BossCore.Dawn -> DawnBossCoreSkill().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)
            BossCore.Dusk -> DuskBossCoreSkill().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)
            BossCore.Elysia -> ElysiaBossCoreSkill().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)
        }


        tooltip.addImageWithText(0f)

        if (core == BossCore.Dusk) {
            tooltip.addSpacer(10f)
            tooltip.addSectionHeading("Transcendence", Alignment.MID, 0f)
            tooltip.addSpacer(10f)

            tooltip.addPara("This core has the unique property of being installable on to crewed ships. This can be done over the \"Additional Options\" menu in refit.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(),
                "crewed", "Additional Options")
        }

        val opad = 10f
        val text = person!!.faction.baseUIColor
        val bg = person.faction.darkUIColor
        val spec = Global.getSettings().getCommoditySpec(person.aiCoreId)

        if (person.personalityAPI.id == Personalities.RECKLESS)
        {
            tooltip!!.addSectionHeading("Personality: Fearless", text, bg, Alignment.MID, 10f)
            tooltip.addPara("In combat, the " + spec.name + " is single-minded and determined. " + "In a human captain, its traits might be considered reckless. In a machine, they're terrifying.",
                opad)
        }
    }
}