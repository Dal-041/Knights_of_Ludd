package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI

/**
 * Shared identities of the Knights story: the faction, its places and its people. The only constants shared across
 * features; a feature's own keys live in its `<Feature>Keys` object.
 */
object KolIds {
    const val FACTION = "knights_of_selkie"

    // markets (the stations' market ids)
    const val CYGNUS = "kol_cygnus"
    const val LYRA = "kol_lyra"

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
