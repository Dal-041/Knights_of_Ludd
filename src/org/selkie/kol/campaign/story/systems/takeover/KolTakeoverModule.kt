package org.selkie.kol.campaign.story.systems.takeover

import org.selkie.kol.campaign.story.systems.KolStoryModule

/** The Knights' reinforcement of vanilla's takeover expedition (holds no saved state; registered transiently). */
object KolTakeoverModule : KolStoryModule {
    override val id = "takeover"
    override fun onLoad() {
        // purge saved copies duplicated by older versions, then register transiently
        com.fs.starfarer.api.Global.getSector().removeScriptsOfClass(KnightsTakeoverOverride::class.java)
        com.fs.starfarer.api.Global.getSector().addTransientScript(KnightsTakeoverOverride())
    }
}
