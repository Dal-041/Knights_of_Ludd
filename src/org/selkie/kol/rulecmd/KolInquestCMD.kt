package org.selkie.kol.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.chapters.ch2.convocation.KolConvocation
import org.selkie.kol.campaign.story.situations.tech.milestones.inquest.KolInquest

/**
 * Rule commands for the inquest into the player (kolInquest_* rules):
 * - `KolInquestCMD open`: publishes the lead Inquisitor's name tokens (`$global.kolPerson_inquisitor_*`).
 * - `KolInquestCMD inspect`: the inspection of the player's fleet (publishes `$global.kolInquest_*`).
 * - `KolInquestCMD answer <candid|guarded|defiant>`: records the interview, then the verdict scene (`KolInquestVerdict`).
 * - `KolInquestCMD resolve <none|surrender|refuse>`: settles the inquest, then the outcome scene (`KolInquestOutcome`).
 */
class KolInquestCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "open" -> {
                KolConvocation.publishPerson("\$kolPerson_inquisitor_", KolInquest.inquisitor("lead"))
                Global.getSector().memoryWithoutUpdate.unset("\$kolInquest_readCount")
                true
            }
            "inspect" -> { KolInquest.inspect(); true }
            "answer" -> {
                KolInquest.answerInquest(params.getOrNull(1)?.getString(map) ?: "guarded")
                FireBest.fire(null, dialog, map, "KolInquestVerdict")
                true
            }
            "resolve" -> {
                KolInquest.resolveInquest(params.getOrNull(1)?.getString(map) ?: "none", dialog)
                FireBest.fire(null, dialog, map, "KolInquestOutcome")
                true
            }
            else -> false
        }
    }
}
