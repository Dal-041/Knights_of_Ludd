package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import org.selkie.kol.helpers.KolStaticStrings

/**
 * Shared identities of the Knights story: the faction, its places and its people. The only constants shared across
 * features; a feature's own keys live in its `<Feature>Keys` object. Ids that KolStaticStrings already holds are
 * taken from it: it is the mod's ground truth, and never refers to story code.
 */
object KolIds {
    const val FACTION = KolStaticStrings.kolFactionID

    // markets (the stations' market ids)
    const val CYGNUS = KolStaticStrings.KOL_CYGNUS
    const val LYRA = KolStaticStrings.KOL_LYRA

    // people (ImportantPeople ids)
    const val ENARMS = "kol_knightcaptain"
    const val MARTINS = "kol_libramaster"
    const val HELENSIS = "kol_chaptermaster"
    const val GREENFLIGHT = "kol_intel_director"
    const val GRANDMASTER = "kol_grandmaster"
    const val AGENT = "kol_outside_agent"
    /** + slot (`lead`, `second`) */
    const val INQUISITOR_PREFIX = "kol_inquisitor_"
    /** + slot: the assembly's regular voices */
    const val BENCH_PREFIX = "kol_bench_"

    fun market(id: String): MarketAPI? = Global.getSector().economy.getMarket(id)
    val cygnus: MarketAPI? get() = market(CYGNUS)
    val lyra: MarketAPI? get() = market(LYRA)
}
