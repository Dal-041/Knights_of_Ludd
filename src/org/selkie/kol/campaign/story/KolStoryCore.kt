package org.selkie.kol.campaign.story

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import org.json.JSONObject
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import org.selkie.kol.helpers.KolStaticStrings.KolStory

/** The `kol_story` block in settings.json, read on each use so edits apply on the next launch without a rebuild. */
object KolStorySettings {
    private val json: JSONObject get() = Global.getSettings().getJSONObject(KolStory.SETTINGS_KEY)

    val chronicleIntervalDays get() = json.getDouble("chronicleIntervalDays").toFloat()
    val assemblyIntervalDays get() = json.getDouble("assemblyIntervalDays").toFloat()
    val assemblyNoticeDays get() = json.getDouble("assemblyNoticeDays").toFloat()
    val assemblyWindowDays get() = json.getDouble("assemblyWindowDays").toFloat()
    val firstAssemblyDays get() = json.optDouble("firstAssemblyDays", assemblyIntervalDays.toDouble()).toFloat()
    val patronOwnFleetPoints get() = json.optInt("patronOwnFleetPoints", 150)
    val jointOpFleetPoints get() = json.optDouble("jointOpFleetPoints", 120.0).toFloat()
    val alreadyDoneBountyCredits get() = json.optInt("alreadyDoneBountyCredits", 150000)
    val inquestModerateScore get() = json.optDouble("inquestModerateScore", 8.0).toFloat()
    val inquestHeavyScore get() = json.optDouble("inquestHeavyScore", 20.0).toFloat()
    val readingAppends get() = json.optBoolean("readingAppends", true)
    val inquestWatchedScore get() = json.optDouble("inquestWatchedScore", 4.0).toFloat()
    val inquestLieWeight get() = json.optDouble("inquestLieWeight", 3.0).toFloat()
    fun inquestAnswerWeight(i: Int): Float = json.optJSONArray("inquestAnswerWeights")?.optDouble(i, 0.0)?.toFloat() ?: listOf(0f, 2f, 5f)[i]
    val chapterAdvanceEnabled get() = json.optBoolean("chapterAdvanceEnabled", false)
    val inquestValidFraction get() = json.getDouble("inquestValidFraction").toFloat()
    val desertionBase get() = json.getDouble("desertionBase").toFloat()
    val desertionK get() = json.getDouble("desertionK").toFloat()
    val desertionScale get() = json.getDouble("desertionScale").toFloat()
    val desertionCap get() = json.getDouble("desertionCap").toFloat()
    val desertionMinFleetPoints get() = json.getInt("desertionMinFleetPoints")
    val desertionPerFleetCap get() = json.getInt("desertionPerFleetCap")
    val desertionOrdainedShare get() = json.getDouble("desertionOrdainedShare").toFloat()

    // patron (add-knights-patron-lobbying)
    val patronCharterCost get() = json.optInt("patronCharterCost", 1500000)
    val patronCharterCompanyName: String get() = json.optString("patronCharterCompanyName", "Independent Security Combine")
    val patronPirateDeal get() = json.optInt("patronPirateDeal", 800000)
    val patronPirateAudienceBribe get() = json.optInt("patronPirateAudienceBribe", 25000)
    val patronPirateAgreementBribe get() = json.optInt("patronPirateAgreementBribe", 50000)
    val patronPirateNominalFee get() = json.optInt("patronPirateNominalFee", 50000)
    val patronPirateCut get() = json.optInt("patronPirateCut", 250000)
    val patronSwarmFleetPoints get() = json.optDouble("patronSwarmFleetPoints", 2000.0).toFloat()
    val patronSwarmKnightsMult get() = json.optDouble("patronSwarmKnightsMult", 2.0).toFloat()
    val patronSwarmLaunchRange get() = json.optDouble("patronSwarmLaunchRange", 2500.0).toFloat()
    val patronDiktatReplyDays get() = json.optDouble("patronDiktatReplyDays", 14.0).toFloat()
    val patronFleetCount get() = json.optInt("patronFleetCount", 2)
    val patronFleetPoints get() = json.optDouble("patronFleetPoints", 150.0).toFloat()
    val patronFleetReplenishDays get() = json.optDouble("patronFleetReplenishDays", 30.0).toFloat()
    val patronOwnFleetPenalty get() = json.optDouble("patronOwnFleetPenalty", 0.4).toFloat()
    val patronCharterOffenseRep get() = json.optDouble("patronCharterOffenseRep", 0.1).toFloat()
    val patronPathAttemptRep get() = json.optDouble("patronPathAttemptRep", 0.05).toFloat()
    val patronPirateBribe get() = json.optInt("patronPirateBribe", 20000)
    val patronOffStagePeople: Set<String> get() = json.optJSONArray("patronOffStagePeople")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()

    /** The rival factions penalized when the Church signs with the power keyed [powerKey], with the reputation lost. */
    fun patronRivalRep(powerKey: String): Map<String, Float> {
        val o = json.optJSONObject("patronRivalRep")?.optJSONObject(powerKey) ?: return emptyMap()
        return o.keys().asSequence().map { it as String }.associateWith { o.optDouble(it, 0.0).toFloat() }
    }

    /** The Knights level for a severance [reason], or null to use the excommunication level. */
    fun severanceRepLevel(reason: String): String? = json.optJSONObject("severanceRepLevels")?.optString(reason, null)

    /** The standing a story [moment] needs (a RepLevel name); the default gate when the moment has none of its own. */
    fun standingGate(moment: String): String {
        val gates = json.optJSONObject("standingGates")
        return gates?.optString(moment, null) ?: gates?.optString("default", null) ?: "NEUTRAL"
    }
}

/** The Knights story chapter: `$global.kol_chapter`. 1 until Chapter 1 is done; then 2, advanced by chapter assemblies. */
object KolChapter {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    fun get(): Int = if (memory.contains(KolStory.CHAPTER)) memory.getInt(KolStory.CHAPTER) else 1

    fun set(chapter: Int) = memory.set(KolStory.CHAPTER, chapter)

    /** Chapter 2 begins once Chapter 1 is done (called on load and by the story scripts). */
    fun ensureChapterTwo() {
        if (memory.getBoolean(KolCh1.CH1_DONE) && get() < 2) set(2)
    }
}

/** People created once on first need and remembered by id (board people, Inquisitors, agents). */
object KolPeople {
    /**
     * The important person with this id, created with [factory] and registered the first time it's asked for.
     * [market] is the person's home market; [listed] also adds them to that market's people.
     */
    fun remembered(id: String, market: MarketAPI? = null, listed: Boolean = false, factory: () -> PersonAPI): PersonAPI {
        val important = Global.getSector().importantPeople
        important.getPerson(id)?.let { return it }
        val person = factory()
        person.id = id
        important.addPerson(person)
        if (market != null) {
            person.market = market
            if (listed) market.addPerson(person)
        }
        return person
    }
}
