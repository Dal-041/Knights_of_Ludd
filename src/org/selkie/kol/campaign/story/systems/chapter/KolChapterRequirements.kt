package org.selkie.kol.campaign.story.systems.chapter

import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.kol.campaign.story.situations.tech.KolTechData

/**
 * What each chapter needs before its chapter assembly. Entries are registered by key at each load (transient):
 * re-registering replaces, so features can register in any order. A chapter is met when every valid entry is done.
 */
object KolChapterRequirements {
    class Entry(val valid: () -> Boolean, val done: () -> Boolean)

    private val chapters = HashMap<Int, LinkedHashMap<String, Entry>>()

    /** Testing: chapters vetoed from the console never count as met. */
    val vetoed = HashSet<Int>()

    fun register(chapter: Int, key: String, valid: () -> Boolean, done: () -> Boolean) {
        chapters.getOrPut(chapter) { LinkedHashMap() }[key] = Entry(valid, done)
    }

    fun entries(chapter: Int): Map<String, Entry> = chapters[chapter] ?: emptyMap()

    fun isMet(chapter: Int): Boolean = chapter !in vetoed && entries(chapter).values.all { !it.valid() || it.done() }

    /** True when every valid entry except [key] is done (for content offered last). */
    fun isMetExcept(chapter: Int, key: String): Boolean =
        chapter !in vetoed && entries(chapter).filterKeys { it != key }.values.all { !it.valid() || it.done() }

    /** Inquest validity: the player's lifetime scrip is within the configured share of the milestone that unlocks it. */
    fun inquestValid(milestoneScrip: Int): Boolean =
        KolTechData.get().lifetimeScrip >= milestoneScrip * KolStorySettings.inquestValidFraction
}
