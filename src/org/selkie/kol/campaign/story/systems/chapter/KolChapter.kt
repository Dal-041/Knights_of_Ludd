package org.selkie.kol.campaign.story.systems.chapter

import com.fs.starfarer.api.Global
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.chapters.ch1.Ch1Keys

/** The Knights story chapter: `$global.kol_chapter`. 1 until Chapter 1 is done; then 2, advanced by chapter assemblies. */
object KolChapter {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    fun get(): Int = if (memory.contains(ChapterKeys.CHAPTER)) memory.getInt(ChapterKeys.CHAPTER) else 1

    fun set(chapter: Int) = memory.set(ChapterKeys.CHAPTER, chapter)

    /** Chapter 2 begins once Chapter 1 is done (called on load and by the story scripts). */
    fun ensureChapterTwo() {
        if (memory.getBoolean(Ch1Keys.CH1_DONE) && get() < 2) set(2)
    }
}

/** The chapter counter: Chapter 2 begins once Chapter 1 is done. */
object KolChapterModule : KolStoryModule {
    override val id = "chapter"
    override fun onLoad() = KolChapter.ensureChapterTwo()
    override fun pulse() = KolChapter.ensureChapterTwo()
    override fun severedKeys() = listOf("\$kol_chapter")
}
