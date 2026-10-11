package org.selkie.kol.campaign.story.chapters.ch2.convocation

/** The convocation's outcome flags. */
object ConvocationKeys {
    // Permanent flags
    const val CONVOCATION_DONE = "\$kolConv_done"
    const val CONV_OATH_SWORN = "\$kolConv_oathSworn" // the player offered their own forces under oath
    const val CONV_OATH_LIE = "\$kolConv_oathLie" // ... and lied (counts at the inquest)
}
