package org.selkie.kol.campaign.story.chapters.ch2.patron

/** The patron quest: the mission, the record (permanent) and the parley keys. */
object PatronKeys {
    /** The player was turned away from the brief (below Welcoming with the Church): the next step says so. */
    const val BRIEF_REFUSED = "\$kolPatron_briefRefused"

    // Missions (person_missions.csv ids) and their global references / in-progress flags
    const val PATRON_ID = "kolCh2Patron"
    const val PATRON_REF = "\$kolPatron_ref"
    const val PATRON_ACTIVE = "\$kolPatron_active"
    const val PATRON = "\$kolPatron_signed" // faction id, or "player"
    const val PATRON_SECURED = "\$kolPatron_secured" // boolean twin for rules
    // Leads the player has learned: + league / hegemony / diktat / charter / tritach / pirate / path
    const val LEAD_PREFIX = "\$kolPatron_lead_"
    // Per-power parley keys: PREFIX + power key + STAGE_SUFFIX (lead ... signed) or PRICE_SUFFIX (the price on the table)
    const val PREFIX = "\$kolPatron_"
    const val STAGE_SUFFIX = "_stage"
    const val PRICE_SUFFIX = "_price"
    const val DELEGATION = "\$kolPatron_delegation" // the Order's delegation is aboard (one party, collected once)
    // The record (permanent)
    const val LEVER = "\$kolPatron_lever"
    const val PRICE = "\$kolPatron_price" // the price key paid
    const val CHARTER_SOURCE = "\$kolPatron_charterSource" // independent / tritach
    const val CHARTER_SCUTTLED = "\$kolPatron_charterScuttled"
    const val CHARTER_ACCEPTED = "\$kolPatron_charterAccepted"
    const val TRIED_PREFIX = "\$kolPatron_tried_" // + pirate / path
    const val OWN_COMMITMENT = "\$kolPatron_ownCommitment" // the forces committed at the Lyra meeting
    const val VOLTURN_FILES_GIVEN = "\$kolPatron_volturnFilesGiven" // the Diktat's price: the faith's files on Volturn
    const val MET_FIXER = "\$kolPatron_metFixer" // the player met the Tri-Tachyon fixer (vanilla records nothing)
    const val FIXER_VISIT_SEEN = "\$kolPatron_fixerVisitSeen"
    // People
    const val STANDIN_PREFIX = "kol_patron_standin_" // + the vanilla id a remembered stand-in replaces
    const val PERSON_PREFIX = "kol_patron_" // + role: the parleys' remembered people
}
