package org.selkie.kol.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.chapters.ch2.convocation.ConvocationKeys
import org.selkie.kol.campaign.story.chapters.ch2.convocation.KolConvocation
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly

/**
 * Rule commands for the convocation (kolConv_* rules):
 * - `KolConvCMD open`: opens the convocation (bench voices and world state, `$global.kolConv_*`).
 * - `KolConvCMD oath <sworn|lie>`: the oath for offering the player's own forces.
 * - `KolConvCMD effects`: settles what the player said.
 * - `KolConvCMD done`: the convocation is over.
 */
class KolConvCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "open" -> { KolConvocation.open(); true }
            "oath" -> { KolConvocation.oath(params.getOrNull(1)?.getString(map) == "lie", dialog); true }
            "effects" -> { KolConvocation.effects(dialog); true }
            "done" -> {
                Global.getSector().memoryWithoutUpdate.set(ConvocationKeys.CONVOCATION_DONE, true)
                KolAssembly.report("convocation")
                true
            }
            else -> false
        }
    }
}
