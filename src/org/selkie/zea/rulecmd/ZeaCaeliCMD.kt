package org.selkie.zea.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.campaign.ZeaCaeli

/** Caeli's guardian (ZeaCaeli): `engage` starts the fight; `reward` gives the filler loot once. */
class ZeaCaeliCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "engage" -> ZeaCaeli.engage(dialog, map)
            "reward" -> { ZeaCaeli.reward(dialog); true }
            else -> false
        }
    }
}
