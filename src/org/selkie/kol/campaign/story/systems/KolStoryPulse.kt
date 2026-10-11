package org.selkie.kol.campaign.story.systems

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.core.KolLog

/** The story heartbeat: twice a day, each module's `pulse()` in module order (transient; registered at each load). */
class KolStoryPulse : EveryFrameScript {
    private val interval = IntervalUtil(0.5f, 0.5f)

    override fun isDone() = false
    override fun runWhilePaused() = false

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        for (module in KolStoryModules.all) {
            try {
                module.pulse()
            } catch (e: Exception) {
                KolLog.error(module.id, "pulse failed", e)
            }
        }
    }
}
