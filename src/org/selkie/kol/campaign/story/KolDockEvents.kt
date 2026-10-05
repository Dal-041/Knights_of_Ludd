package org.selkie.kol.campaign.story

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import org.selkie.kol.helpers.KolStaticStrings.KolStory

/**
 * Story events that play when the player docks at a Knights market (assemblies at Lyra; summonses and ceremonies at
 * Cygnus). One MarketPostDock guard (rules: kolDock_guard) plays the pending events of the docked market, highest
 * priority first; each event's scene ends with `FireBest KolDockNext` to play the next one or return to the market.
 * Owners add an event while it is pending and remove it once resolved.
 */
class KolDockEvents {
    class DockEvent(val market: String, val key: String, val priority: Int, val trigger: String)

    val events = ArrayList<DockEvent>()

    /** Events already played in the current dock dialog (not saved): each plays once per dock. */
    @Transient private var played: MutableSet<String>? = null
    @Transient private var playedDialog: InteractionDialogAPI? = null

    fun add(market: String, key: String, priority: Int, trigger: String) {
        events.removeAll { it.key == key }
        events.add(DockEvent(market, key, priority, trigger))
    }

    fun remove(key: String) = events.removeAll { it.key == key }

    fun has(key: String) = events.any { it.key == key }

    private fun playedFor(dialog: InteractionDialogAPI): MutableSet<String> {
        if (playedDialog !== dialog || played == null) {
            playedDialog = dialog
            played = HashSet()
        }
        return played!!
    }

    private fun nextFor(dialog: InteractionDialogAPI): DockEvent? {
        val marketId = dialog.interactionTarget?.market?.id ?: return null
        val done = playedFor(dialog)
        return events.filter { it.market == marketId && it.key !in done }.maxByOrNull { it.priority }
    }

    fun hasPending(dialog: InteractionDialogAPI) = nextFor(dialog) != null

    /** Plays the next pending event here, or hands the dock back to the market's own rules. */
    fun playNext(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        val next = nextFor(dialog)
        if (next == null) {
            FireBest.fire(null, dialog, memoryMap, "MarketPostDock")
            return
        }
        playedFor(dialog).add(next.key)
        FireBest.fire(null, dialog, memoryMap, next.trigger)
    }

    companion object {
        fun get(): KolDockEvents {
            val data = Global.getSector().persistentData
            return data[KolStory.DOCK_DATA_KEY] as? KolDockEvents ?: KolDockEvents().also { data[KolStory.DOCK_DATA_KEY] = it }
        }
    }
}
