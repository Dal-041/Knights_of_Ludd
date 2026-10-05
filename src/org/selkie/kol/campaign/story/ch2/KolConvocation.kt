package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolAssembly
import org.selkie.kol.campaign.story.KolPeople
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import org.selkie.kol.helpers.KolStaticStrings.KolPrelude

/**
 * The convocation's supporting state (rules: kolConv_*).
 * - **The bench:** the voices in the hall besides the principals. Regulars are generated once and remembered for later
 *   assemblies (`kol_bench_<slot>`); fresh voices are new for each scene. Each is published for the text as
 *   `$global.kolBench_<slot>_<name|last|title|heOrShe|HeOrShe|hisOrHer|HisOrHer|himOrHer>`.
 * - **World state** the scene branches on, published as `$global.kolConv_*` when the scene opens.
 * - **Effects:** the player's replies are recorded as `$global.kolConv_said_<topic>` and settled together at the
 *   close ([effects]); only a lie under oath takes effect at once ([oath]).
 */
object KolConvocation {
    private val memory get() = Global.getSector().memoryWithoutUpdate
    private val playerMemory get() = Global.getSector().playerMemoryWithoutUpdate

    enum class Slot(val regular: Boolean) { LECTOR(true), CAPTAIN(true), KEEPER(true), CHAPLAIN(true), YOUNG(false), DELEGATE(false) }

    private val fresh = HashMap<Slot, PersonAPI>() // transient: new voices for each scene

    private fun create(slot: Slot): PersonAPI {
        val church = slot == Slot.CHAPLAIN || slot == Slot.DELEGATE
        val faction = Global.getSector().getFaction(if (church) Factions.LUDDIC_CHURCH else KolStaticStrings.kolFactionID)
        return faction.createRandomPerson().apply {
            rankId = when {
                slot == Slot.CAPTAIN -> Ranks.KNIGHT_CAPTAIN
                church -> if (isMale) Ranks.FATHER else Ranks.MOTHER
                else -> if (isMale) Ranks.BROTHER else Ranks.SISTER
            }
            postId = when (slot) {
                Slot.LECTOR -> Ranks.POST_ADMINISTRATOR
                Slot.CAPTAIN -> Ranks.POST_FLEET_COMMANDER
                Slot.KEEPER -> Ranks.POST_SUPPLY_MANAGER
                Slot.CHAPLAIN -> Ranks.POST_CURATE
                Slot.YOUNG -> Ranks.POST_INITIATE
                Slot.DELEGATE -> Ranks.POST_SYNOD_SUBCURATE
            }
        }
    }

    fun bench(slot: Slot): PersonAPI = if (slot.regular) {
        KolPeople.remembered(KolCh2.BENCH_PREFIX + slot.name.lowercase(), KolAssembly.lyra) { create(slot) }
    } else {
        fresh.getOrPut(slot) { create(slot) }
    }

    fun bench(name: String): PersonAPI? = Slot.values().firstOrNull { it.name.equals(name, true) }?.let { bench(it) }

    /** Opens the scene: new fresh voices, everyone's tokens, and the world state the rules branch on. */
    fun open() {
        fresh.clear()
        for (slot in Slot.values()) publish(slot, bench(slot))
        for (key in listOf("attrition", "funding", "powers", "threats")) memory.unset("\$kolConv_said_$key")
        memory.unset("\$kolConv_ownDismissed")
        memory.set("\$kolConv_dossierItems", dossierItems())
        memory.set("\$kolConv_dossierDeep", dossierDeep())
        memory.set("\$kolConv_fought", hasFought())
        val diplomacy = diplomacy()
        memory.set("\$kolConv_diplomat", diplomacy.isNotEmpty())
        memory.set("\$kolConv_diploFought", "fought" in diplomacy)
        memory.set("\$kolConv_diploChurch", "church" in diplomacy)
        memory.set("\$kolConv_diploDealt", "dealt" in diplomacy)
        val doors = KolCh2Story.openDoors()
        memory.set("\$kolConv_hasDoors", doors.isNotEmpty())
        memory.set("\$kolConv_doors", doorsText(doors.map { it.id }))
        memory.set("\$kolConv_canOfferOwn", KolCh2Story.canOfferOwn())
    }

    private fun publish(slot: Slot, person: PersonAPI) = publishPerson("\$kolBench_${slot.name.lowercase()}_", person)

    /** Publishes [person]'s name, title and pronouns for rule text as `<prefix>name`, `<prefix>last`, `<prefix>heOrShe`... */
    fun publishPerson(p: String, person: PersonAPI) {
        val male = person.isMale
        memory.set(p + "name", person.nameString)
        memory.set(p + "last", person.name.last)
        memory.set(p + "title", when (person.rankId) {
            Ranks.KNIGHT_CAPTAIN -> "Knight-Captain"
            Ranks.FATHER -> "Father"
            Ranks.MOTHER -> "Mother"
            Ranks.BROTHER -> "Brother"
            else -> "Sister"
        })
        memory.set(p + "heOrShe", if (male) "he" else "she")
        memory.set(p + "HeOrShe", if (male) "He" else "She")
        memory.set(p + "hisOrHer", if (male) "his" else "her")
        memory.set(p + "HisOrHer", if (male) "His" else "Her")
        memory.set(p + "himOrHer", if (male) "him" else "her")
    }

    // --- world state ---------------------------------------------------------------------------------------------

    private fun chron(fact: String) = memory.getBoolean(KolStaticStrings.KolStory.CHRON_PREFIX + fact)
    private fun grade(thread: String) = memory.getInt(KolStaticStrings.KolStory.CHRON_PREFIX + thread + "_grade")

    /** What Greenflight's dossier can speak to: Elysian, Dusk and Dawn traces, Ninaya beaten, Ozymandias visited. */
    fun dossierItems(): Int = listOf(grade("elysian") >= 1, grade("dusk") >= 1, grade("dawn") >= 1,
        chron("ninayaDefeated"), chron("ozymandiasVisited")).count { it }

    /** More than the dossier will say aloud: a great machine beaten, or a thread taken past a visit. */
    fun dossierDeep(): Boolean = memory.getInt(KolStaticStrings.KolStory.CHRON_POWERS_DEFEATED) > 0 ||
            listOf("elysian", "dusk", "dawn").any { grade(it) >= 3 }

    /** The player has fought the machines (not only seen them). */
    fun hasFought(): Boolean = memory.getBoolean(KolCh1.FOUGHT_DUSK) || chron("ninayaDefeated") ||
            memory.getInt(KolStaticStrings.KolStory.CHRON_POWERS_DEFEATED) > 0 ||
            memory.getInt(KolStaticStrings.KolStory.CHRON_TT_SITES_CLEARED) > 0 ||
            listOf("elysian", "dusk", "dawn", "tritach").any { grade(it) >= 3 }

    private fun flag(key: String) = playerMemory.getBoolean(key) || memory.getBoolean(key)

    /**
     * The player's dealings with great powers: `fought` (turned back a power's force in a colony crisis), `church`
     * (that power was the Church), `dealt` (a crisis deal, League membership, the Yaribays, Bornanew's errands, or a
     * major power's commission).
     */
    fun diplomacy(): Set<String> {
        val kinds = HashSet<String>()
        val church = flag("\$defeatedLuddicChurchExpedition") || flag("\$brokeLuddicChurchDeal")
        if (church || listOf("\$defeatedHegemony", "\$defeatedLeagueBlockade", "\$defeatedDiktatAttack",
                "\$defeatedTTMercAttack", "\$counterRaidedTriTach").any { flag(it) }) kinds.add("fought")
        if (church) kinds.add("church")
        val commission = Misc.getCommissionFactionId()
        if (listOf("\$makeDiktatDeal", "\$madeImmigrationDealWithLuddicChurch", "\$bribedTTMercAttack", "\$isLeagueMember",
                "\$metHorusYaribay", "\$metMenesYaribay", "\$metBornanew", "\$lke_completed").any { flag(it) } ||
            commission in listOf(Factions.HEGEMONY, Factions.PERSEAN, Factions.DIKTAT, Factions.TRITACHYON, Factions.LUDDIC_CHURCH)
        ) kinds.add("dealt")
        return kinds
    }

    private val DOOR_TEXT = mapOf(
        "mazalot" to "the faithful on Mazalot, under the Persean League",
        "jangala" to "on Jangala, under the Hegemony",
        "volturn" to "on Volturn, under the Diktat")

    private fun doorsText(ids: List<String>): String {
        val parts = ids.mapNotNull { DOOR_TEXT[it] }.toMutableList()
        if (parts.isEmpty()) return ""
        if (!parts[0].startsWith("the faithful")) parts[0] = "the faithful " + parts[0]
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] + "; and " + parts[1]
            else -> parts.dropLast(1).joinToString("; ") + "; and " + parts.last()
        }
    }

    // --- effects -------------------------------------------------------------------------------------------------

    private fun person(id: String) = Global.getSector().importantPeople.getPerson(id)

    private fun adjust(person: PersonAPI?, amount: Float, dialog: InteractionDialogAPI) {
        person ?: return
        person.relToPlayer.adjustRelationship(amount, null)
        val name = "${person.rank} ${person.name.last}"
        if (amount > 0) dialog.textPanel.addPara("Relations with $name improved", Misc.getPositiveHighlightColor())
        else dialog.textPanel.addPara("Relations with $name worsened", Misc.getNegativeHighlightColor())
    }

    /** The oath for offering the player's own forces. A lie costs Greenflight's regard at once (she was watching). */
    fun oath(lie: Boolean, dialog: InteractionDialogAPI) {
        memory.set(KolCh2.CONV_OATH_SWORN, true)
        if (lie) {
            memory.set(KolCh2.CONV_OATH_LIE, true)
            adjust(person(KolCh1.GREENFLIGHT_ID), -0.15f, dialog)
        }
    }

    /** Settles what the player said, at the close. */
    fun effects(dialog: InteractionDialogAPI) {
        fun said(topic: String) = memory.getString("\$kolConv_said_$topic")
        var helensis = 0f
        var enarms = 0f
        var greenflight = 0f
        when (said("attrition")) { "offense" -> helensis += 0.05f; "defense" -> enarms += 0.05f; "pragmatic" -> greenflight += 0.05f }
        when (said("funding")) { "coin" -> helensis += 0.05f; "loyal" -> enarms += 0.05f }
        when (said("powers")) { "outside", "allies" -> greenflight += 0.05f; "diplomat" -> greenflight += 0.08f }
        if (said("threats") == "fought") helensis += 0.05f
        if (helensis > 0f) adjust(person(KolCh1.HELENSIS_ID), helensis, dialog)
        if (enarms > 0f) adjust(person(KolPrelude.ENARMS_ID), enarms, dialog)
        if (greenflight > 0f) adjust(person(KolCh1.GREENFLIGHT_ID), greenflight, dialog)
    }
}
