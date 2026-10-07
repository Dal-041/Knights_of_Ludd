package org.selkie.kol.campaign.story

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.tech.KolTechData
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolStory
import java.awt.Color

/**
 * The Order's assemblies at Star Keep Lyra: a fixed calendar from Chapter 2, one every `assemblyIntervalDays`, each
 * sitting for `assemblyWindowDays`. The player attends through a market option at Lyra while one sits. Any system adds
 * to the next assembly's reckoning with [report].
 * - **Routine:** missed, it is held anyway (summary intel).
 * - **Important** (the convocation until held, or a chapter assembly): an attend mission marks Lyra from the call;
 *   missed, it does not sit without the player, the mission ends, and the next assembly is important again.
 * - **Tracker:** a permanent intel lists the next two assemblies' dates.
 */
object KolAssembly {
    const val EVENT_KEY = "kol_assembly" // legacy dock event, removed from old saves
    private const val MS_PER_DAY = 86_400_000L

    fun data(): KolAssemblyData {
        val data = Global.getSector().persistentData
        return data[KolStory.ASSEMBLY_DATA_KEY] as? KolAssemblyData ?: KolAssemblyData().also { data[KolStory.ASSEMBLY_DATA_KEY] = it }
    }

    /** Adds [value] to [key] in the next assembly's reckoning (published as `$global.kolAssembly_<key>`). */
    fun report(key: String, value: Int = 1) {
        val pending = data().pending
        pending[key] = (pending[key] ?: 0) + value
    }

    val lyra: MarketAPI? get() = Global.getSector().economy.getMarket(KolStaticStrings.KOL_LYRA)

    fun now(): Long = Global.getSector().clock.timestamp
    fun daysSince(timestamp: Long): Float = Global.getSector().clock.getElapsedDaysSince(timestamp)
    fun plusDays(timestamp: Long, days: Float): Long = timestamp + (days * MS_PER_DAY).toLong()
    fun dateOf(timestamp: Long): String = Global.getSector().clock.createClock(timestamp).dateString

    /** When the assembly [index] places ahead sits (0 = the next or current one). */
    fun sitTime(index: Int = 0): Long = plusDays(data().nextSit, index * KolStorySettings.assemblyIntervalDays)
    fun closeTime(index: Int = 0): Long = plusDays(sitTime(index), KolStorySettings.assemblyWindowDays)

    /** Whether the current chapter's conditions are met, so the next assembly advances it. */
    fun chapterReady(): Boolean =
        KolStorySettings.chapterAdvanceEnabled && KolChapterRequirements.isMet(KolChapter.get())

    const val LIBRA_TOPIC_PENDING = "\$kolLibra_assemblyPending"
    const val TOPIC_KEY = "\$kolAssembly_topic"

    /**
     * The important topic the next assembly takes up, by priority: the convocation (Chapter 1 to 2), a patron to
     * announce, Libra's restoration, chapter advancement; null for a routine assembly. One topic per assembly; the
     * others wait for the next, and each topic's effect applies only at the assembly that takes it up.
     */
    fun pickTopic(chapterForced: Boolean = false): String? {
        val memory = Global.getSector().memoryWithoutUpdate
        return when {
            !memory.getBoolean(KolStaticStrings.KolCh2.CONVOCATION_DONE) -> "convocation"
            memory.getBoolean(org.selkie.kol.campaign.story.ch2.KolPatronFlags.ANNOUNCE) -> "patron"
            memory.getBoolean(LIBRA_TOPIC_PENDING) -> "libra"
            chapterForced || chapterReady() -> "chapter"
            else -> null
        }
    }

    /** Some important topic waits for the next assembly. */
    fun importantNext(): Boolean = pickTopic() != null

    /** Publishes the assembly's topic for its scene (`$global.kolAssembly_topic`: a topic, or `routine`). */
    fun publishTopic(topic: String?) = Global.getSector().memoryWithoutUpdate.set(TOPIC_KEY, topic ?: "routine")

    /** An assembly is sitting and the player has not attended it yet (the market option and notice at Lyra). */
    fun isOpen(): Boolean = data().let { it.state == KolAssemblyData.State.SITTING && !it.attended } &&
            !org.selkie.kol.campaign.story.ch2.KolPatronParley.gatePending() // the patron's pre-assembly scenes play first

    /** Publishes the reckoning for the scene (`$global.kolAssembly_<key>`) and clears it. */
    fun publishReckoning() {
        val memory = Global.getSector().memoryWithoutUpdate
        val data = data()
        for (key in data.published) memory.unset(KolStory.ASSEMBLY_PREFIX + key)
        data.published.clear()
        for ((key, value) in data.pending) {
            memory.set(KolStory.ASSEMBLY_PREFIX + key, value)
            data.published.add(key)
        }
        data.pending.clear()
    }

    /** Called from the scene (KolStoryCMD assemblySit): the player attended the assembly in session. */
    fun attended() {
        val data = data()
        val memory = Global.getSector().memoryWithoutUpdate
        memory.set(KolStory.ASSEMBLY_HELD, memory.getInt(KolStory.ASSEMBLY_HELD) + 1)
        // a patron waiting to be announced (reported first), only at the assembly that takes up that topic
        if (data.topic == "patron") org.selkie.kol.campaign.story.ch2.KolPatron.onAssemblyAttended()
        publishReckoning()
        memory.set("\$kolAssembly_unique", data.unique)
        memory.set("\$kolAssembly_nextChapter", KolChapter.get() + 1)
        data.attended = true
        if (memory.getBoolean(KolStory.ATTEND_ACTIVE)) memory.set(KolStory.ATTEND_DONE, true)
    }

    /** Testing: the next assembly sits now (optionally as the chapter assembly). */
    fun forceNow(chapter: Boolean) {
        val data = data()
        val script = KolAssemblyScript()
        if (data.state == KolAssemblyData.State.IDLE) {
            data.nextSit = now()
            script.announce(data, chapter)
        } else {
            data.nextSit = minOf(data.nextSit, now())
            if (chapter) { data.unique = true; data.important = true; data.topic = "chapter"; publishTopic("chapter") }
        }
        script.tick()
    }

    /** Called from the chapter scene (KolStoryCMD assemblyAdvance). */
    fun advanceChapter() {
        KolChapter.set(KolChapter.get() + 1)
        data().unique = false
        Global.getSector().memoryWithoutUpdate.set("\$kolAssembly_unique", false)
    }
}

/** Saved state of the assembly calendar and the open report log. */
class KolAssemblyData {
    var cycleStart = 0L        // legacy (pre-calendar saves): converted into nextSit
    var offsetDays = 0f        // legacy
    var nextSit = 0L           // when the next (or current) assembly sits
    var state = State.IDLE
    var unique = false          // this assembly advances the chapter
    var important = false       // an important topic: attend mission, not held without the player
    var topic: String? = null   // the important topic this assembly takes up (KolAssembly.pickTopic), or null
    var attended = false
    var tracker: KolAssemblyTracker? = null
    val pending = LinkedHashMap<String, Int>()
    val published = ArrayList<String>()

    enum class State { IDLE, ANNOUNCED, SITTING }
}

/** Drives the calendar (transient; registered at each load, state in KolAssemblyData). */
class KolAssemblyScript : EveryFrameScript {
    private val interval = IntervalUtil(0.5f, 0.5f)

    override fun isDone() = false
    override fun runWhilePaused() = false

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        KolChapter.ensureChapterTwo()
        org.selkie.kol.campaign.story.ch2.KolPatron.watchFixerVisit()
        if (KolChapter.get() < 2 || KolAssembly.lyra == null) return
        tick()
        org.selkie.kol.campaign.story.ch2.KolCh2Story.pulse()
    }

    fun tick() {
        val data = KolAssembly.data()
        if (data.nextSit == 0L) {
            data.nextSit = if (data.cycleStart != 0L) {
                KolAssembly.plusDays(data.cycleStart, KolStorySettings.assemblyIntervalDays - data.offsetDays)
            } else {
                // the first assembly of Chapter 2 comes sooner, so the convocation does
                KolAssembly.plusDays(KolAssembly.now(), KolStorySettings.firstAssemblyDays)
            }
            KolDockEvents.get().remove(KolAssembly.EVENT_KEY)
        }
        KolAssemblyTracker.ensure()
        // saves from before the topic queue: an assembly already called takes up the topic it was called for
        if (data.state != KolAssemblyData.State.IDLE && data.important && data.topic == null) {
            data.topic = if (data.unique) "chapter" else KolAssembly.pickTopic()
            KolAssembly.publishTopic(data.topic)
        }

        val untilSit = -KolAssembly.daysSince(data.nextSit)
        when (data.state) {
            KolAssemblyData.State.IDLE -> if (untilSit <= KolStorySettings.assemblyNoticeDays) announce(data, false)
            KolAssemblyData.State.ANNOUNCED -> if (untilSit <= 0f) {
                data.state = KolAssemblyData.State.SITTING
                if (data.important) Global.getSector().memoryWithoutUpdate.set(KolStory.ATTEND_SITTING, true)
                data.tracker?.update(KolAssemblyTracker.UPDATE_SITTING)
            }
            KolAssemblyData.State.SITTING ->
                if (data.attended || untilSit + KolStorySettings.assemblyWindowDays <= 0f) close(data)
        }
    }

    fun announce(data: KolAssemblyData, chapter: Boolean) {
        data.topic = KolAssembly.pickTopic(chapter)
        data.unique = data.topic == "chapter"
        data.important = data.topic != null
        KolAssembly.publishTopic(data.topic)
        data.attended = false
        data.state = KolAssemblyData.State.ANNOUNCED
        if (data.important) KolAssemblyAttend.start()
        data.tracker?.update(KolAssemblyTracker.UPDATE_CALLED)
    }

    private fun close(data: KolAssemblyData) {
        val memory = Global.getSector().memoryWithoutUpdate
        if (!data.attended) {
            memory.set(KolStory.ASSEMBLY_MISSED, memory.getInt(KolStory.ASSEMBLY_MISSED) + 1)
            if (data.important) {
                // it does not sit without the player; the reckoning waits for the next one
                if (memory.getBoolean(KolStory.ATTEND_ACTIVE)) memory.set(KolStory.ATTEND_MISSED, true)
            } else {
                // held without the player: the reckoning goes into the summary
                val entries = LinkedHashMap(data.pending)
                data.pending.clear()
                memory.set(KolStory.ASSEMBLY_HELD, memory.getInt(KolStory.ASSEMBLY_HELD) + 1)
                KolAssemblyIntel(entries).post()
            }
        }
        data.state = KolAssemblyData.State.IDLE
        data.attended = false
        data.important = false
        data.topic = null
        KolAssembly.publishTopic(null)
        // the calendar is fixed: the next assembly sits one interval after this one, whenever this one closed
        do {
            data.nextSit = KolAssembly.plusDays(data.nextSit, KolStorySettings.assemblyIntervalDays)
        } while (KolAssembly.daysSince(KolAssembly.closeTime()) > 0f)
        data.tracker?.update(null)
    }
}

/** The standing tracker: the next two assemblies at Lyra, with their dates. Never ends. */
class KolAssemblyTracker : BaseIntelPlugin() {
    companion object {
        const val UPDATE_CALLED = "called"
        const val UPDATE_SITTING = "sitting"

        fun ensure() {
            val data = KolAssembly.data()
            if (data.tracker != null) return
            data.tracker = KolAssemblyTracker().also { Global.getSector().intelManager.addIntel(it) }
        }
    }

    fun update(param: Any?) {
        if (param == null) return
        sendUpdateIfPlayerHasIntel(param, false)
    }

    override fun shouldRemoveIntel(): Boolean = false

    override fun getName(): String = "Knights: Assemblies at Lyra"

    private fun range(index: Int) = KolAssembly.dateOf(KolAssembly.sitTime(index)) + " to " + KolAssembly.dateOf(KolAssembly.closeTime(index))

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode, isUpdate: Boolean, tc: Color, initPad: Float) {
        val h = Misc.getHighlightColor()
        val data = KolAssembly.data()
        bullet(info)
        if (isUpdate) {
            when (listInfoParam) {
                UPDATE_CALLED -> info.addPara("Sits from %s", initPad, tc, h, KolAssembly.dateOf(KolAssembly.sitTime()))
                UPDATE_SITTING -> info.addPara("In session until %s", initPad, tc, h, KolAssembly.dateOf(KolAssembly.closeTime()))
            }
        } else {
            if (data.state == KolAssemblyData.State.SITTING && !data.attended) {
                info.addPara("In session until %s", initPad, tc, h, KolAssembly.dateOf(KolAssembly.closeTime()))
            } else {
                val days = (-KolAssembly.daysSince(KolAssembly.sitTime())).toInt()
                info.addPara("Next: %s (in %s)", initPad, tc, h, range(0), Misc.getStringForDays(days))
            }
            info.addPara("Then: %s", 0f, tc, h, range(1))
        }
        unindent(info)
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        val data = KolAssembly.data()
        info.addPara("[PLACEHOLDER] The Order assembles at Star Keep Lyra every season. Captains in its service are expected to attend.", 0f)
        val sitting = data.state == KolAssemblyData.State.SITTING && !data.attended
        if (sitting) {
            info.addPara("The Order is in assembly now, until %s.", 10f, h, KolAssembly.dateOf(KolAssembly.closeTime()))
        } else {
            info.addPara("The next assembly sits from %s to %s.", 10f, h,
                KolAssembly.dateOf(KolAssembly.sitTime()), KolAssembly.dateOf(KolAssembly.closeTime()))
        }
        info.addPara("The one after sits from %s to %s.", 3f, h,
            KolAssembly.dateOf(KolAssembly.sitTime(1)), KolAssembly.dateOf(KolAssembly.closeTime(1)))
        if (data.important && data.state != KolAssemblyData.State.IDLE) {
            info.addPara("[PLACEHOLDER] Word from Lyra is that matters of importance will be raised, and that the principals of Cygnus will attend.", 10f)
        }
        val memory = Global.getSector().memoryWithoutUpdate
        info.addPara("Assemblies held: %s. Missed: %s.", 10f, Misc.getGrayColor(), h,
            memory.getInt(KolStory.ASSEMBLY_HELD).toString(), memory.getInt(KolStory.ASSEMBLY_MISSED).toString())
    }

    override fun getIcon(): String = Global.getSector().getFaction(KolStaticStrings.kolFactionID).crest

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(KolStaticStrings.kolFactionID)
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? = KolAssembly.lyra?.primaryEntity
}

/** An important assembly is called: attend it at Star Keep Lyra. Missed, it ends without penalty. */
class KolAssemblyAttend : HubMissionWithSearch() {
    enum class Stage { CALLED, SITTING, ATTENDED, MISSED }

    companion object {
        fun start() {
            val memory = Global.getSector().memoryWithoutUpdate
            if (memory.getBoolean(KolStory.ATTEND_ACTIVE)) return
            val lyra = KolAssembly.lyra ?: return
            val mission = Global.getSettings().getMissionSpec(KolStory.ATTEND_ID)?.createMission() ?: return
            Global.getSector().importantPeople.getPerson(KolStaticStrings.KolCh2.GRANDMASTER_ID)?.let { mission.setPersonOverride(it) }
            mission.createAndAbortIfFailed(lyra, false)
            if (!mission.isMissionCreationAborted) mission.accept(null, null)
        }
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        val lyra = KolAssembly.lyra ?: return false
        val memory = Global.getSector().memoryWithoutUpdate
        for (flag in listOf(KolStory.ATTEND_SITTING, KolStory.ATTEND_DONE, KolStory.ATTEND_MISSED)) memory.unset(flag)
        if (!setGlobalReference(KolStory.ATTEND_REF, KolStory.ATTEND_ACTIVE)) return false
        setStartingStage(Stage.CALLED)
        addSuccessStages(Stage.ATTENDED)
        addNoPenaltyFailureStages(Stage.MISSED)
        setStoryMission()
        setNoRepChanges()
        makeImportant(lyra.primaryEntity, "\$kolAssemblyAttend_lyra", Stage.CALLED, Stage.SITTING)
        setStageOnGlobalFlag(Stage.SITTING, KolStory.ATTEND_SITTING)
        setStageOnGlobalFlag(Stage.ATTENDED, KolStory.ATTEND_DONE)
        setStageOnGlobalFlag(Stage.MISSED, KolStory.ATTEND_MISSED)
        return true
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "update") {
            checkStageChangesAndTriggers(dialog, memoryMap)
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        val text = if (KolAssembly.data().unique)
            "[PLACEHOLDER] The Grandmaster calls the Order to a great assembly at Star Keep Lyra. Matters of moment are to be decided, and your presence is expected."
        else
            "[PLACEHOLDER] The Order assembles at Star Keep Lyra. Word from Lyra is that matters of importance will be raised, that the principals of Cygnus will attend, and that your presence is expected."
        info.addPara(text, 10f)
        if (currentStage == Stage.CALLED) {
            info.addPara("It sits from %s to %s.", 10f, h, KolAssembly.dateOf(KolAssembly.sitTime()), KolAssembly.dateOf(KolAssembly.closeTime()))
        } else {
            info.addPara("It is in session until %s. It will not sit without you.", 10f, h, KolAssembly.dateOf(KolAssembly.closeTime()))
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        val h = Misc.getHighlightColor()
        when (currentStage) {
            Stage.CALLED -> info.addPara("Attend at Star Keep Lyra from %s", pad, tc, h, KolAssembly.dateOf(KolAssembly.sitTime()))
            Stage.SITTING -> info.addPara("Attend at Star Keep Lyra before %s", pad, tc, h, KolAssembly.dateOf(KolAssembly.closeTime()))
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = if (KolAssembly.data().unique) "The Great Assembly" else "Called to Assembly"
}

/** Summary of a routine assembly held without the player (placeholder text). */
class KolAssemblyIntel(private val entries: Map<String, Int> = emptyMap()) : BaseIntelPlugin() {
    fun post() {
        Global.getSector().intelManager.addIntel(this)
        endAfterDelay(30f)
    }

    override fun getName(): String = "Knights: Assembly Held"

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode, isUpdate: Boolean, tc: Color, initPad: Float) {
        bullet(info)
        info.addPara("Held without you", initPad, tc)
        unindent(info)
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        info.addPara("[PLACEHOLDER] The Order assembled at Star Keep Lyra without you. A summary of what was reckoned reached your fleet afterward.", 0f)
        if (entries.isNotEmpty()) {
            info.addPara("Reckoned:", 10f)
            for ((key, value) in entries) info.addPara("  %s: %s", 3f, Misc.getHighlightColor(), key, value.toString())
        }
    }

    override fun getIcon(): String = Global.getSector().getFaction(KolStaticStrings.kolFactionID).crest

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(KolStaticStrings.kolFactionID)
        tags.add(Tags.INTEL_FLEET_LOG)
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? = KolAssembly.lyra?.primaryEntity
}

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
