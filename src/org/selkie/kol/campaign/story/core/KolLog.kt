package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global

/** Story logging: every line reads `KOL: [feature]: message`, so `starsector.log` can be searched by feature. */
object KolLog {
    private val log = Global.getLogger(KolLog::class.java)

    fun info(feature: String, message: String) = log.info("KOL: [$feature]: $message")
    fun warn(feature: String, message: String, t: Throwable? = null) =
        if (t != null) log.warn("KOL: [$feature]: $message", t) else log.warn("KOL: [$feature]: $message")
    fun error(feature: String, message: String, t: Throwable? = null) =
        if (t != null) log.error("KOL: [$feature]: $message", t) else log.error("KOL: [$feature]: $message")
}
