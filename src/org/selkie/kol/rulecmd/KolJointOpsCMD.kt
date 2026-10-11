package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.chapters.ch2.jointops.JointOpsKeys
import org.selkie.kol.campaign.story.chapters.ch2.jointops.KolJointOps
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.core.KolMissions

/**
 * Rule commands for joint operations (kolNinaya_* rules):
 * - `KolJointOpsCMD startNinaya`: starts the Ninaya operation from the dialog (Greenflight its contact).
 * - `KolJointOpsCMD ninayaBounty`: Ninaya was beaten before the operation; pay the bounty, carry half to the next one.
 */
class KolJointOpsCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "startNinaya" -> KolMissions.start(JointOpsKeys.NINAYA_ID, KolIds.GREENFLIGHT, dialog) != null
            "ninayaBounty" -> { KolJointOps.alreadyDoneBounty(JointOpsKeys.NINAYA_OP_DONE, dialog); true }
            else -> false
        }
    }
}
