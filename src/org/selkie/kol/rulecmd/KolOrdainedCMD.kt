package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.ch2.KolCh2Story
import org.selkie.kol.campaign.tech.KolOrdainedEarly
import org.selkie.kol.helpers.KolStaticStrings

/**
 * Rule commands for the Ordained rollouts (kolOrdained_* rules):
 * - `KolOrdainedCMD due` (condition): the early rollout's confrontation is due (ARMORY some days past, not started).
 * - `KolOrdainedCMD begin`: starts the early rollout mission, Helensis as its contact.
 * The mission's own actions go through `Call $global.kolOrdainedEarly_ref <action>` (`confronted <stance>`, `go`).
 */
class KolOrdainedCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "due" -> KolOrdainedEarly.due()
            "begin" -> KolCh2Story.start(KolOrdainedEarly.ID, KolStaticStrings.KolCh1.HELENSIS_ID, dialog) != null
            else -> false
        }
    }
}
