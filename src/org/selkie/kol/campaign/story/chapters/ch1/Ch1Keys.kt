package org.selkie.kol.campaign.story.chapters.ch1

/** Chapter 1's keys: missions, references, flags and what the player said. */
object Ch1Keys {
    // mission ids (person_missions.csv)
    const val SCOUT_ID = "kolCh1Scout"
    const val BOUNTY_ID = "kolCh1Bounty"
    const val LYRA_ID = "kolCh1Lyra"
    const val RETURN_ID = "kolCh1Return"
    // mission references (setGlobalReference)
    const val SCOUT_REF = "\$kolCh1Scout_ref"
    const val BOUNTY_REF = "\$kolCh1Bounty_ref"
    const val LYRA_REF = "\$kolCh1Lyra_ref"
    const val RETURN_REF = "\$kolCh1Return_ref"
    // Mission-owned flags: auto-unset by BaseHubMission when the mission ends ("during" states only)
    const val SCOUT_ACTIVE = "\$kolCh1Scout_active"
    const val SCOUT_SCOUTED = "\$kolCh1Scout_scouted" // stage trigger: -> REPORT
    const val SCOUT_DONE = "\$kolCh1Scout_done" // stage trigger: -> COMPLETED
    const val BOUNTY_ACTIVE = "\$kolCh1Bounty_active"
    const val LYRA_ACTIVE = "\$kolCh1Lyra_active"
    const val GREENFLIGHT_MET = "\$kolCh1Lyra_met" // stage trigger: -> COMPLETED
    const val RETURN_ACTIVE = "\$kolCh1Return_active"
    const val RETURN_DONE = "\$kolCh1Return_done" // stage trigger: -> COMPLETED (set when the council opens)
    // Permanent flags: chapter progress
    const val QUARTERS_DONE = "\$kolCh1_quartersDone" // stances given, oath asked
    const val OATH_DEFERRED = "\$kolCh1_oathDeferred"
    const val SCOUT_COMPLETE = "\$kolCh1_scoutComplete"
    const val BOUNTY_COMPLETE = "\$kolCh1_bountyComplete"
    const val COUNCIL_DONE = "\$kolCh1_councilDone"
    const val CH1_DONE = "\$kolCh1_done"
    // Permanent flags: what the player said and did (for later chapters to quote)
    const val DECLINE = "\$kolCh1_decline" // grieve / indifferent / glad / defer
    const val DECLINE_LIE = "\$kolCh1_declineLie"
    const val TECH = "\$kolCh1_tech" // oppose / embrace / defer
    const val TECH_LIE = "\$kolCh1_techLie"
    const val OATH_SWORN = "\$kolCh1_oathSworn"
    const val OATH_LIE = "\$kolCh1_oathLie"
    const val FOUGHT_DUSK = "\$kolCh1_foughtDusk"
    const val PULLED_THROUGH = "\$kolCh1_pulledThrough"
    const val DOUBTED_LINK = "\$kolCh1_doubtedLink"
}
