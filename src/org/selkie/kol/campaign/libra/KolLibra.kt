package org.selkie.kol.campaign.libra

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.json.JSONObject
import org.selkie.kol.campaign.situations.KolLibraSituationIntel
import org.selkie.kol.world.GenerateKnights

/** The `kol_libra` block of settings.json, read fresh on each use so edits apply without a rebuild. */
object KolLibraSettings {
    private val json: JSONObject get() = Global.getSettings().getJSONObject("kol_libra")

    private fun JSONObject.strings(key: String) = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()

    val stageThresholds get() = json.optJSONArray("stageThresholds")?.let { a -> List(a.length()) { a.getInt(it) } } ?: listOf(50, 500, 1500, 3600)
    val maxProgress get() = json.optInt("maxProgress", 5000)
    val acceptedCommodities get() = json.strings("acceptedCommodities").toSet()
    val commodityMult get() = json.optDouble("commodityMult", 0.001).toFloat()
    val donationManufacturers get() = json.strings("donationManufacturers").toSet()
    val donationMult get() = json.optDouble("donationMult", 0.001).toFloat()
    val deployCap get() = json.optInt("deployCap", 8)
    val lineMonthlyProgress get() = json.optInt("lineMonthlyProgress", 15)
    val supplyStanding: String get() = json.optString("supplyStanding", "FAVORABLE")
    val crewStanding: String get() = json.optString("crewStanding", "WELCOMING")
    val supplyPrice get() = json.optInt("supplyPrice", 100000)
    val crewPrice get() = json.optInt("crewPrice", 250000)
    val raiderTaskChance get() = json.optDouble("raiderTaskChance", 0.33).toFloat()
    val raiderFleetPoints get() = json.optDouble("raiderFleetPoints", 60.0).toFloat()
    val minSupply get() = json.optInt("minSupply", 5)
    val beaconCooldownDays get() = json.optDouble("beaconCooldownDays", 30.0).toFloat()
    val wavePlayerFactor get() = json.optDouble("wavePlayerFactor", 0.6).toFloat()
    val waveStageFP get() = json.optDouble("waveStageFP", 30.0).toFloat()
    val waveVariance get() = json.optDouble("waveVariance", 0.3).toFloat()
    val waveFleets get() = json.optInt("waveFleets", 3)
    val drillWaveMult get() = json.optDouble("drillWaveMult", 1.5).toFloat()
    val gunneryMult get() = json.optDouble("gunneryMult", 0.1).toFloat()
    val moduleLossProgress get() = json.optInt("moduleLossProgress", 10)
    val responderChance get() = json.optDouble("responderChance", 1.0 / 6.0).toFloat()
    val responderRep get() = json.optDouble("responderRep", 0.05).toFloat()
    val convoyFleetPoints get() = json.optDouble("convoyFleetPoints", 40.0).toFloat()
    val convoyRaidFleetPoints get() = json.optDouble("convoyRaidFleetPoints", 80.0).toFloat()
}

/** Saved state of the Libra situation (sector persistent data). Severance leaves it: what was restored remains. */
class KolLibraData {
    val milestonesDone = HashSet<String>()
    /** Penned supply lines: line key -> supplier market id. */
    val lines = LinkedHashMap<String, String>()
    var crewSupplier: String? = null
    val vouches = LinkedHashSet<String>()
    /** Libra has a crew complement of its own (the crew contract). */
    var complement = false
    /** The assembly took up Libra's restoration: no longer hidden. */
    var legitimized = false
    /** Donated hull ids, oldest first: Libra's escorts after activation (Chapter 4; not told to the player). */
    val roster = ArrayList<String>()
    var lastBeacon = 0L

    companion object {
        private const val DATA_KEY = "kol_libraData"

        @JvmStatic
        fun get(): KolLibraData {
            val data = Global.getSector().persistentData
            return data[DATA_KEY] as? KolLibraData ?: KolLibraData().also { data[DATA_KEY] = it }
        }
    }
}

/** Libra's four milestone events, in order; each needs the previous one done and its bar threshold. */
enum class KolLibraMilestone(val key: String) {
    SUPPLY("supply"), CREW("crew"), DRILL("drill"), RESTORATION("restoration");

    val previous: KolLibraMilestone? get() = values().getOrNull(ordinal - 1)
}

/** The Libra situation's shared logic: the milestone gates, contributions and their valuation. */
object KolLibra {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    fun data() = KolLibraData.get()

    /** Battlestar Libra's market, or null if this sector has none. */
    fun market(): MarketAPI? = GenerateKnights.getLibraMarket()

    /** The situation is running (opened at the end of Chapter 1; ended by severance). */
    fun active(): Boolean = KolLibraSituationIntel.get() != null

    fun done(m: KolLibraMilestone): Boolean = m.key in data().milestonesDone

    /** The milestone's bar threshold is reached. */
    fun reached(m: KolLibraMilestone): Boolean = KolLibraSituationIntel.get()?.reached(m) ?: false

    /** Offered: its threshold reached, the previous milestone done, and not done itself. */
    fun open(m: KolLibraMilestone): Boolean = active() && !done(m) && reached(m) && (m.previous?.let { done(it) } ?: true)

    /** A milestone completes: recorded, its global flag set, and its effects on Libra applied. */
    fun complete(m: KolLibraMilestone, dialog: InteractionDialogAPI?) {
        if (!data().milestonesDone.add(m.key)) return
        memory.set("\$kolLibra_${m.key}Done", true)
        if (m == KolLibraMilestone.CREW) data().complement = true
        KolLibraEffects.apply()
        KolLibraSituationIntel.get()?.milestoneCompleted(m, dialog?.textPanel)
    }

    /** A one-time contribution (or loss, if negative) to the bar, listed under its reason. */
    fun contribute(progress: Int, dialog: InteractionDialogAPI?, reason: String) {
        if (progress == 0) return
        KolLibraSituationIntel.get()?.addFactor(org.selkie.kol.campaign.situations.KolLibraFactor(progress, reason), dialog)
    }

    // --- valuation -------------------------------------------------------------------------------------------

    fun acceptsCommodity(id: String?) = id != null && id in KolLibraSettings.acceptedCommodities

    /** Progress for handed-in goods; sum before rounding. */
    fun commodityValue(id: String, qty: Float): Float =
        Global.getSettings().getCommoditySpec(id).basePrice * qty * KolLibraSettings.commodityMult

    /** Donations open after the crew contract. */
    fun donationsOpen(): Boolean = active() && done(KolLibraMilestone.CREW)

    fun acceptsShip(member: FleetMemberAPI): Boolean =
        !member.isFlagship && !member.isStation && member.hullSpec.manufacturer in KolLibraSettings.donationManufacturers

    /** Progress for a donated ship; sum before rounding. */
    fun shipValue(member: FleetMemberAPI): Float = member.baseValue * KolLibraSettings.donationMult

    /** A donated hull joins the roster; the oldest entries drop past three times the deployment cap. */
    fun addToRoster(hullId: String) {
        val roster = data().roster
        roster.add(hullId)
        val max = 3 * KolLibraSettings.deployCap
        while (roster.size > max) roster.removeAt(0)
    }
}
