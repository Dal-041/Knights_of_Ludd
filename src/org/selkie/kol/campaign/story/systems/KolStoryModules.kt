package org.selkie.kol.campaign.story.systems

/**
 * Every story module, in load and pulse order: the one list to edit when a feature is added. Chapter first (others
 * read it), the cue check last (it reads what the others set this pulse).
 */
object KolStoryModules {
    val all: List<KolStoryModule> by lazy {
        listOf(
            org.selkie.kol.campaign.story.systems.chapter.KolChapterModule,
            org.selkie.kol.campaign.story.chapters.ch2.patron.KolPatronModule,
            org.selkie.kol.campaign.story.systems.assembly.KolAssemblyModule,
            org.selkie.kol.campaign.story.chapters.ch2.KolCh2Module,
            org.selkie.kol.campaign.story.chapters.ch2.convocation.KolConvocationModule,
            org.selkie.kol.campaign.story.chapters.ch2.jointops.KolJointOpsModule,
            org.selkie.kol.campaign.story.chapters.ch2.ozymandias.KolOzymandiasModule,
            org.selkie.kol.campaign.story.chapters.ch2.agent.KolAgentModule,
            org.selkie.kol.campaign.story.situations.tech.milestones.inquest.KolInquestModule,
            org.selkie.kol.campaign.story.situations.tech.milestones.ordained.KolOrdainedModule,
            org.selkie.kol.campaign.story.chapters.prelude.KolPreludeModule,
            org.selkie.kol.campaign.story.chapters.ch1.KolCh1Module,
            org.selkie.kol.campaign.story.systems.chronicle.KolChronicleModule,
            org.selkie.kol.campaign.story.systems.dock.KolDockModule,
            org.selkie.kol.campaign.story.systems.duties.KolDutiesModule,
            org.selkie.kol.campaign.story.systems.desertion.KolDesertionModule,
            org.selkie.kol.campaign.story.systems.severance.KolSeveranceModule,
            org.selkie.kol.campaign.story.systems.takeover.KolTakeoverModule,
            org.selkie.kol.campaign.story.systems.cue.KolCueModule,
        )
    }
}
