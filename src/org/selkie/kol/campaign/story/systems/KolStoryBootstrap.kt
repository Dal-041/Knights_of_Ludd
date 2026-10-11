package org.selkie.kol.campaign.story.systems

import com.fs.starfarer.api.Global
import org.selkie.kol.campaign.story.systems.chapter.KolChapterRequirements
import org.selkie.kol.campaign.story.systems.cue.KolStoryCue
import org.selkie.kol.campaign.story.systems.spawn.KolFleetSpawnHook

/**
 * Registers the story at each load (called from KOL_ModPlugin.onGameLoad). Everything is transient: every module's
 * cues, fleet-spawn modifiers and chapter requirements first, then each module's onLoad in order, then the shared
 * fleet-spawn listener and the story pulse.
 */
object KolStoryBootstrap {
    @JvmStatic
    fun onGameLoad() {
        val sector = Global.getSector()
        val modules = KolStoryModules.all
        for (module in modules) {
            for ((key, cue) in module.cues()) KolStoryCue.register(key, cue)
            for ((key, modifier) in module.spawnModifiers()) KolFleetSpawnHook.register(key, modifier)
            for (r in module.requirements()) KolChapterRequirements.register(r.chapter, r.key, r.valid, r.done)
        }
        for (module in modules) module.onLoad()
        sector.addTransientListener(KolFleetSpawnHook())
        sector.addTransientScript(KolStoryPulse())
    }
}
