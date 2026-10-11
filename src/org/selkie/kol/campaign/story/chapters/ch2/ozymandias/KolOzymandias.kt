package org.selkie.kol.campaign.story.chapters.ch2.ozymandias

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import org.selkie.kol.campaign.story.core.KolRep
import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.zea.helpers.ZeaStaticStrings

/** Ozymandias outside its mission: the Zhi Nu's recovery. */
object KolOzymandias {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /**
     * Caeli's Zhi Nu was recovered (vanilla's PostShipRecoverySpecial rule hook): a Knights reputation hit if any of
     * the Ozymandias mission's Knights fleets is alive in the system.
     */
    fun zhiNuRecovered(dialog: InteractionDialogAPI) {
        val system = Global.getSector().getStarSystem(ZeaStaticStrings.ozymandiasSysName) ?: return
        if (system.fleets.none { it.isAlive && it.memoryWithoutUpdate.getBoolean(OzymandiasKeys.OZY_KNIGHTS) }) return
        val impact = CoreReputationPlugin.CustomRepImpact()
        impact.delta = -KolStorySettings.ozZhiNuRecoveryRep
        KolRep.knights(impact, dialog.textPanel)
    }
}
