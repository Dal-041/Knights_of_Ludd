package org.selkie.kol.campaign.tech

import com.fs.starfarer.api.Global
import org.json.JSONObject
import org.selkie.kol.helpers.KolStaticStrings.KolTech

/**
 * Saved state of the Technology situation: scrip earned over the whole game (drives the progress bar and never
 * falls), the spendable balance, and which unique ships were announced or bought.
 */
class KolTechData {
    var lifetimeScrip = 0
    var balance = 0
    val uniquesUnlocked = HashSet<Int>()
    val uniquesBought = HashSet<Int>()
    var officersHired = 0

    /** Credits a handover: both totals rise. */
    fun earn(scrip: Int) {
        lifetimeScrip += scrip
        balance += scrip
    }

    fun canAfford(price: Int) = balance >= price

    fun spend(price: Int): Boolean {
        if (!canAfford(price)) return false
        balance -= price
        return true
    }

    companion object {
        @JvmStatic
        fun get(): KolTechData {
            val data = Global.getSector().persistentData
            return data[KolTech.DATA_KEY] as? KolTechData ?: KolTechData().also { data[KolTech.DATA_KEY] = it }
        }
    }
}

/** The `kol_tech` block of settings.json, read fresh on each use so edits apply without a rebuild. */
object KolTechSettings {
    private val json: JSONObject get() = Global.getSettings().getJSONObject(KolTech.SETTINGS_KEY)

    val scripPerProgress get() = json.getInt("scripPerProgress")
    val stageThresholds get() = json.getJSONArray("stageThresholds").let { a -> List(a.length()) { a.getInt(it) } }
    val maxScrip get() = json.getInt("maxScrip")
    val acceptedManufacturers get() = json.getJSONArray("acceptedManufacturers").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
    val forbiddenManufacturers get() = json.getJSONArray("forbiddenManufacturers").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
    val baseValue get() = json.getDouble("baseValue").toFloat()
    val sizeSteps get() = json.getJSONArray("sizeSteps").let { a -> List(a.length()) { a.getDouble(it).toFloat() } }
    val tierSteps get() = json.getJSONArray("tierSteps").let { a -> List(a.length()) { a.getDouble(it).toFloat() } }
    val coreValues get() = json.getJSONObject("coreValues").toIntMap()
    val hullBaseValue get() = json.getDouble("hullBaseValue").toFloat()
    val fixedValues get() = json.getJSONObject("fixedValues").toIntMap()
    val storeRates get() = json.getJSONArray("storeRates").let { a -> List(a.length()) { a.getDouble(it).toFloat() } }
    val officerCreditValue get() = json.getDouble("officerCreditValue").toFloat()
    val officerRates get() = json.getJSONArray("officerRates").let { a -> List(a.length()) { a.getDouble(it).toFloat() } }
    val officerPriceVariance get() = json.getDouble("officerPriceVariance").toFloat()
    val officersPerMilestone get() = json.getInt("officersPerMilestone")
    val officerLevel get() = json.getInt("officerLevel")
    val excommunicationRep get() = json.getString("excommunicationRep")

    data class UniqueSlot(val slot: Int, val stage: Int, val variant: String, val price: Int)

    val uniques: List<UniqueSlot>
        get() = json.getJSONArray("uniques").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                UniqueSlot(i + 1, o.getInt("stage"), o.getString("variant"), o.getInt("price"))
            }
        }

    private fun JSONObject.toIntMap(): Map<String, Int> {
        val out = LinkedHashMap<String, Int>()
        val keys = keys()
        while (keys.hasNext()) {
            val k = keys.next() as String
            out[k] = getInt(k)
        }
        return out
    }
}
