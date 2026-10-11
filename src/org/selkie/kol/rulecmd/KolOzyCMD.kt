package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.chapters.ch2.ozymandias.KolOzymandias
import org.selkie.kol.campaign.story.chapters.ch2.ozymandias.OzymandiasKeys
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.core.KolMissions

/**
 * Rule commands for Ozymandias (kolOzy_* rules; Caeli's encounter keeps ZeaCaeliCMD):
 * - `KolOzyCMD start`: starts the Ozymandias mission from the dialog (Enarms its contact).
 * - `KolOzyCMD zhiNuRecovered`: the Zhi Nu was recovered (vanilla's recovery hook): the Knights' reaction.
 */
class KolOzyCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "start" -> KolMissions.start(OzymandiasKeys.OZY_ID, KolIds.ENARMS, dialog) != null
            "zhiNuRecovered" -> { KolOzymandias.zhiNuRecovered(dialog); true }
            else -> false
        }
    }
}
