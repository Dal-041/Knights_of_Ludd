package org.selkie.kol.campaign.story

import com.fs.starfarer.api.Global

/** Registers the story foundations at each load (called from KOL_ModPlugin.onGameLoad). Everything is transient. */
object KolStoryBootstrap {
    @JvmStatic
    fun onGameLoad() {
        val sector = Global.getSector()
        KolChapter.ensureChapterTwo()
        org.selkie.kol.campaign.story.ch2.KolPatronParley.refreshSteps() // step markers for saves made before they existed
        KolStoryCue.check() // cues due in saves from before they existed

        sector.addTransientScript(KolChronicle())
        sector.addTransientScript(KolAssemblyScript())
        sector.addTransientScript(org.selkie.kol.campaign.story.ch2.KolPatronFleetScript())
        sector.addTransientScript(KolSeveranceScript())

        KolFleetSpawnHook.register("desertion", KolDesertion.Modifier())
        sector.addTransientListener(KolFleetSpawnHook())
        sector.listenerManager.addListener(KolDesertion.Tick(), true)

        KolDutiesBoard.sync()
        org.selkie.kol.campaign.story.ch2.KolCh2Story.register()
        KolChronicle.check() // catch up existing saves at once
    }
}
