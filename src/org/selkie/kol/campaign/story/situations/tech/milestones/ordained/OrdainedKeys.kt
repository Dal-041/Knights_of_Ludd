package org.selkie.kol.campaign.story.situations.tech.milestones.ordained

/** The Ordained rollouts' outcome flags (read by desertion, the spawn modifier and the schism). */
object OrdainedKeys {
    // Ordained rollouts (add-knights-tech-milestones): permanent flags
    const val ORDAINED_EARLY = "\$kolOrdained_early" // the early rollout happened
    const val ORDAINED_FULL = "\$kolOrdained_full" // the full rollout happened
    const val ORDAINED_EARLY_STANCE = "\$kolOrdained_earlyStance" // helensis / enarms / silent
    const val ORDAINED_EARLY_SHAKEDOWN = "\$kolOrdained_earlyShakedown" // joined / absent
    const val ORDAINED_EARLY_LOST = "\$kolOrdained_earlyLost" // the squadron was destroyed
}
