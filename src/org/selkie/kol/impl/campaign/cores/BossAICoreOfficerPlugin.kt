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
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaGfxCat;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaStarTypes;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import java.util.*

class BossAICoreOfficerPlugin : AICoreOfficerPlugin {
    companion object{
        const val AUTOMATED_POINTS_MULT = 4f
        const val AUTOMATED_POINTS_MULT_SAME_FACTION = 2.5f
    }

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
        core.stats.setSkillLevel(BossCore.getCore(aiCoreId).exclusiveSkillID, 2f)

        core.portraitSprite = Global.getSettings().getSpriteName(ZeaGfxCat.CHARACTERS, BossCore.getCore(aiCoreId).portraitID)

        core.memoryWithoutUpdate.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, AUTOMATED_POINTS_MULT)
        return core
    }

    override fun createPersonalitySection(person: PersonAPI, tooltip: TooltipMakerAPI) {
        val opad = 10f
        val text = person.faction.baseUIColor
        val bg = person.faction.darkUIColor
        val spec = Global.getSettings().getCommoditySpec(person.aiCoreId)

        val skill = Global.getSettings().getSkillSpec(BossCore.getCore(person.aiCoreId).exclusiveSkillID)
        val pointsMult = AUTOMATED_POINTS_MULT.toInt()
        val pointsMultSameFaction = String.format("%.1f",AUTOMATED_POINTS_MULT_SAME_FACTION)

        tooltip.addSpacer(opad)
        tooltip.addPara("Automated Points Multiplier: ${pointsMult}${Strings.X} (${pointsMultSameFaction}${Strings.X} on same faction ships)",
            0f, Misc.getTextColor(), Misc.getHighlightColor(),  "Automated Points Multiplier:")
        tooltip.addSpacer(opad)

        tooltip.addSectionHeading("Signature Skill: ${skill.name}", Alignment.MID, 0f)
        tooltip.addSpacer(opad)

        val skillImg = tooltip.beginImageWithText(skill.spriteName, 48f)

        BossCore.getCore(person.aiCoreId).exclusiveSkill.newInstance().createCustomDescription(null, null, skillImg, tooltip.widthSoFar)

        tooltip.addImageWithText(0f)

        if (person.personalityAPI.id == Personalities.RECKLESS)
        {
            tooltip.addSectionHeading("Personality: Fearless", text, bg, Alignment.MID, opad)
            tooltip.addPara("In combat, the " + spec.name + " is single-minded and determined. " +
                    "In a human captain, its traits might be considered reckless. In a machine, they're terrifying.", opad)
        }
    }
}