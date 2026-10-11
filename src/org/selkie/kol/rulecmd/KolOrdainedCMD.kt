package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.core.KolMissions
import org.selkie.kol.campaign.story.situations.tech.milestones.ordained.KolOrdainedEarly
import org.selkie.kol.campaign.story.core.KolIds

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
            "begin" -> KolMissions.start(KolOrdainedEarly.ID, KolIds.HELENSIS, dialog) != null
            else -> false
        }
    }
}
