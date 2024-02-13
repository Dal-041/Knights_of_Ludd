package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.combat.AIUtils

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

    @JvmStatic
    fun addPersonalityTooltip(person: PersonAPI?, tooltip: TooltipMakerAPI?)
    {
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