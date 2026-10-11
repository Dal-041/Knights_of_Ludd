package org.selkie.kol.campaign.story.chapters.prelude

/** The prelude's keys: missions, references, flags and its rule trigger. */
object PreludeKeys {
    // mission ids (person_missions.csv / bar_events.csv)
    const val HOOK_ID = "kolPreludeHook"
    const val PIRATE_ID = "kolPreludePirate"
    const val LIBRA_ID = "kolPreludeLibra"
    // mission references (setGlobalReference)
    const val HOOK_REF = "\$kolPreludeHook_ref"
    const val PIRATE_REF = "\$kolPreludePirate_ref"
    const val LIBRA_REF = "\$kolPreludeLibra_ref"
    // Permanent flags (never registered with a mission, so they survive mission end)
    const val HOOK_SEEN = "\$kolPreludeHook_seen"
    const val ENARMS_MET = "\$kolPrelude_metEnarms"
    const val PIRATE_COMPLETE = "\$kolPrelude_pirateComplete"
    const val LIBRA_COMPLETE = "\$kolPrelude_libraComplete"
    const val PRELUDE_DONE = "\$kolPrelude_done"
    // Mission-owned flags: set while a mission runs and auto-unset by BaseHubMission when it ends.
    // Rules may gate "during" states on these, never "after" states.
    const val PIRATE_ACTIVE = "\$kolPreludePirate_active" // setGlobalReference in-progress flag
    const val LIBRA_ACTIVE = "\$kolPreludeLibra_active"
    const val HOOK_TALKED = "\$kolPreludeHook_talked" // stage trigger: hook -> COMPLETED
    const val PIRATE_BEATEN = "\$kolPreludePirate_beaten" // stage trigger: -> REPORT
    const val PIRATE_DONE = "\$kolPreludePirate_done" // stage trigger: -> COMPLETED
    const val LIBRA_DELIVERED = "\$kolPreludeLibra_delivered" // stage trigger: -> REPORT
    const val LIBRA_DONE = "\$kolPreludeLibra_done" // stage trigger: -> COMPLETED
    // rules.csv trigger fired when the pirate fleet is beaten
    const val PIRATE_DEFEAT_TRIGGER = "KolPreludePirateDefeated"
}
