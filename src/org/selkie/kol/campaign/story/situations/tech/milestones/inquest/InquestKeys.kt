package org.selkie.kol.campaign.story.situations.tech.milestones.inquest

/** The inquest into the player: the mission, its reference and flags, the outcome, the dock event. */
object InquestKeys {
    const val INQUEST_ID = "kolCh2Inquest"
    const val INQUEST_REF = "\$kolInquest_ref"
    const val INQUEST_ACTIVE = "\$kolInquest_active"
    const val INQUEST_PLAYER = "\$kolInquest_player" // cleared / watched / censured
    const val INQUEST_PLAYER_DONE = "\$kolInquest_playerDone"
    // Rule triggers
    const val TRIGGER_INQUEST = "KolInquestPlayer"
    const val INQUEST_EVENT_KEY = "kol_inquestPlayer"
}
