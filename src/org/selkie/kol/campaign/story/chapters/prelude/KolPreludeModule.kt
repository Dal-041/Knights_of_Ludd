package org.selkie.kol.campaign.story.chapters.prelude

import org.selkie.kol.campaign.story.systems.KolStoryModule

/** The prelude: the notice, the pirate dossier and the Libra relief run. The notice-giver's keys survive severance. */
object KolPreludeModule : KolStoryModule {
    override val id = "prelude"
    override fun severedKeys() = listOf("\$kolPrelude")
    override fun keptKeys() = listOf("\$kolPreludeHook")
}
