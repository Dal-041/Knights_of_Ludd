package org.selkie.kol.campaign.story.systems.chronicle

/** The chronicle: fact prefix, counters, saved data. */
object ChronicleKeys {
    // Chronicle: $global.kolChron_<fact>, _day, _chapter; $global.kolChron_<thread>_grade
    const val CHRON_PREFIX = "\$kolChron_"
    const val CHRON_POWERS_DEFEATED = "\$kolChron_powersDefeated"
    const val CHRON_TT_SITES_CLEARED = "\$kolChron_ttSitesCleared"
    const val CHRON_DATA_KEY = "kol_chronicleData"
}
