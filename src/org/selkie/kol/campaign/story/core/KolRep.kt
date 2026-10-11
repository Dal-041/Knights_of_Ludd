package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions

/**
 * The player's standing with the Knights, the one way story code changes it. The Knights and the Church share one
 * standing (UpdateRelationships mirrors them on reputation events), so a direct change to the Knights alone would be
 * undone by the next Church event: changes go through vanilla's reputation plugin (shown and reported) and the Church
 * is set to match, unless the Order has schismed (`$kol_knights_schism`), when the two are deliberately apart.
 */
object KolRep {
    const val SCHISMED = "\$kol_knights_schism"

    fun knights(impact: CoreReputationPlugin.CustomRepImpact, text: TextPanelAPI?) {
        val sector = Global.getSector()
        sector.adjustPlayerReputation(
            CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact, null, text, true),
            KolIds.FACTION)
        mirrorChurch()
    }

    /** A plain change of [delta] (fraction of the scale, negative for a loss). */
    fun knights(delta: Float, text: TextPanelAPI?) {
        if (delta == 0f) return
        knights(CoreReputationPlugin.CustomRepImpact().apply { this.delta = delta }, text)
    }

    /** Sets the Knights and the Church to [level] outright (no message): for story endings such as excommunication. */
    fun setBoth(level: RepLevel) {
        for (id in listOf(KolIds.FACTION, Factions.LUDDIC_CHURCH)) Global.getSector().getFaction(id)?.setRelationship(Factions.PLAYER, level)
    }

    private fun mirrorChurch() {
        val sector = Global.getSector()
        if (sector.memoryWithoutUpdate.getBoolean(SCHISMED)) return
        val knights = sector.getFaction(KolIds.FACTION).getRelationship(Factions.PLAYER)
        sector.getFaction(Factions.LUDDIC_CHURCH)?.setRelationship(Factions.PLAYER, knights)
    }
}
