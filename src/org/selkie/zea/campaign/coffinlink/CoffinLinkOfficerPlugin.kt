package org.selkie.zea.campaign.coffinlink

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.util.Random

/**
 * Officer plugin for the hidden COFFIN Link commodity. The automated-ship captain picker lists every
 * cargo commodity that has an AI core officer plugin and assigns the person it creates, so returning
 * the player person puts the player in command.
 */
class CoffinLinkOfficerPlugin : BaseAICoreOfficerPluginImpl() {

    override fun createPerson(aiCoreId: String?, factionId: String?, random: Random?): PersonAPI {
        return Global.getSector().playerPerson
    }

    // The base implementation looks up the person's AI core commodity, which the player doesn't have
    override fun createPersonalitySection(person: PersonAPI, tooltip: TooltipMakerAPI) {
        val faction = Global.getSector().playerFaction
        tooltip.addSectionHeading("COFFIN Link", faction.baseUIColor, faction.darkUIColor, Alignment.MID, 20f)
        tooltip.addPara("You take direct command of this ship through the COFFIN Link.", 10f)
    }
}
