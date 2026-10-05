package org.selkie.kol.campaign.story

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolStory
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys
import org.selkie.zea.intel.ZeaLoreIntel
import org.selkie.zea.intel.ZeaMechanicIntel

/**
 * Turns exploration states (openspec/notes/exploration-states.md) into permanent, rules-readable facts.
 * Checks every few days; each fact, once true, is recorded forever as `$global.kolChron_<fact>`, with
 * `_day` (days since sector start) and `_chapter`. Per thread it keeps a 0-4 grade (unaware, encountered, visited,
 * defeated, claimed) that only rises, plus aggregates and per-source lore counts. Transient script; facts live in
 * sector memory, so nothing extra is saved.
 */
class KolChronicle : EveryFrameScript {
    private val interval = IntervalUtil(1f, 1f)

    override fun isDone() = false
    override fun runWhilePaused() = false

    override fun advance(amount: Float) {
        interval.advance(Global.getSector().clock.convertToDays(amount))
        if (interval.intervalElapsed()) {
            interval.setInterval(KolStorySettings.chronicleIntervalDays, KolStorySettings.chronicleIntervalDays)
            check()
        }
    }

    companion object {
        const val ELYSIAN = "elysian"
        const val DUSK = "dusk"
        const val DAWN = "dawn"
        const val TRITACH = "tritach"
        const val PATH = "path"
        val THREADS = listOf(ELYSIAN, DUSK, DAWN, TRITACH, PATH)

        /** Lore sources (ZeaLoreIntel by faction crest). */
        val LORE_SOURCES = linkedMapOf(
            ELYSIAN to ZeaStaticStrings.elysianID, DAWN to ZeaStaticStrings.dawnID, DUSK to ZeaStaticStrings.duskID,
            TRITACH to Factions.TRITACHYON, "hegemony" to Factions.HEGEMONY,
        )

        private val memory get() = Global.getSector().memoryWithoutUpdate

        fun key(fact: String) = KolStory.CHRON_PREFIX + fact
        fun has(fact: String) = memory.getBoolean(key(fact))
        fun grade(thread: String) = memory.getInt(key("${thread}_grade"))
        fun loreCount(source: String) = memory.getInt(key("lore_$source"))

        /** Visited Abyss systems that have jump points (Elysia, the Luna Sea, Ozymandias, black neutron stars). */
        fun visitedSealable(): List<String> = KolChronicleData.get().visitedSealable.toList()

        /** Records a fact the first time it's seen. Returns true if it was new. */
        fun record(fact: String): Boolean {
            if (has(fact)) return false
            memory.set(key(fact), true)
            memory.set(key("${fact}_day"), today())
            memory.set(key("${fact}_chapter"), KolChapter.get())
            KolAssembly.report("chron_$fact")
            KolAssembly.report("chronFacts")
            return true
        }

        fun setGrade(thread: String, grade: Int) {
            if (grade > grade(thread)) memory.set(key("${thread}_grade"), grade)
        }

        private fun today(): Int = Global.getSector().clock.getElapsedDaysSince(0L).toInt()

        // --- sources -----------------------------------------------------------------------------------------

        private fun system(name: String): StarSystemAPI? = Global.getSector().getStarSystem(name)
        private fun entered(name: String) = system(name)?.isEnteredByPlayer == true
        private fun flag(key: String) = memory.getBoolean(key)

        private fun neutronStars(): List<StarSystemAPI> = Global.getSector().starSystems.filter {
            it.star?.typeId == ZeaStaticStrings.ZeaStarTypes.ZEA_STAR_BLACK_NEUTRON
        }

        private fun hasAbility(id: String) = Global.getSector().characterData.abilities.contains(id)

        private fun playerHasSkill(id: String) = Global.getSector().playerStats.hasSkill(id)

        private fun officerHasSkill(id: String) =
            Global.getSector().playerFleet?.fleetData?.officersCopy?.any { it.person.stats.hasSkill(id) } == true ||
                    Global.getSector().playerFleet?.fleetData?.membersListCopy?.any { it.captain?.stats?.hasSkill(id) == true } == true

        private fun coreInstalled(coreId: String) =
            Global.getSector().playerFleet?.fleetData?.membersListCopy?.any { it.captain?.aiCoreId == coreId } == true

        private fun hullHeld(prefix: String) =
            Global.getSector().playerFleet?.fleetData?.membersListCopy?.any { it.hullSpec.baseHullId.startsWith(prefix) } == true

        private fun crestCount(intel: Class<*>, factionId: String): Int {
            val crest = Global.getSector().getFaction(factionId)?.crest ?: return 0
            return Global.getSector().intelManager.getIntel(intel).count { it.icon == crest }
        }

        /** Runs every check now (also from the console and on load). */
        fun check() {
            if (Global.getSector()?.playerFleet == null) return
            val data = KolChronicleData.get()

            // places
            if (entered(ZeaStaticStrings.elysiaSysName)) record("elysiaVisited")
            if (entered(ZeaStaticStrings.nullspaceSysName)) record("nullspaceVisited")
            if (entered(ZeaStaticStrings.lunaSeaSysName)) record("lunaSeaVisited")
            if (entered(ZeaStaticStrings.ozymandiasSysName)) record("ozymandiasVisited")
            if (entered("Delta Site")) record("deltaSiteVisited")
            if (entered("Unknown Location")) record("alphaSiteVisited")
            if (Global.getSector().starSystems.any { it.memoryWithoutUpdate.contains(ZeaMemKeys.ZEA_TT_2_SYSTEM) && it.isEnteredByPlayer })
                record("ninmahSystemVisited")
            val neutrons = neutronStars().filter { it.isEnteredByPlayer }
            if (neutrons.isNotEmpty()) record("neutronVisited")

            // sealable systems visited (have jump points)
            val sealable = listOfNotNull(system(ZeaStaticStrings.elysiaSysName), system(ZeaStaticStrings.lunaSeaSysName),
                system(ZeaStaticStrings.ozymandiasSysName)) + neutronStars()
            for (sys in sealable) {
                if (sys.isEnteredByPlayer && data.visitedSealable.add(sys.id)) record("visited_${sys.id}")
            }

            // bosses
            if (flag(ZeaMemKeys.ZEA_ELYSIAN_BOSS_1_DONE)) record("amaterasuDefeated")
            if (flag(ZeaMemKeys.ZEA_ELYSIAN_BOSS_2_DONE)) record("heartDefeated")
            if (flag(ZeaMemKeys.ZEA_DAWN_BOSS_DONE)) record("nianDefeated")
            if (flag(ZeaMemKeys.ZEA_DUSK_BOSS_DONE)) record("yukionnaDefeated")
            if (flag(ZeaMemKeys.ZEA_TT_NINAYA_DONE)) record("ninayaDefeated")
            if (flag(ZeaMemKeys.ZEA_TT_NINMAH_DONE)) record("ninmahDefeated")
            if (flag(ZeaMemKeys.ZEA_TT_NINEVEH_DONE)) record("ninevehDefeated")
            if (flag(KolStaticStrings.KolMemKeys.KOL_LP_INVICTUS_DONE)) record("invictusLpDefeated")
            if (flag(KolStaticStrings.KolMemKeys.KOL_LP_RETRIBUTION_DONE)) record("retributionLpDefeated")

            // rewards and their use
            if (hasAbility(ZeaStaticStrings.abilityJumpElysia)) record("jumpElysia")
            if (hasAbility(ZeaStaticStrings.abilityJumpDawn)) record("jumpLunaSea")
            if (hasAbility(ZeaStaticStrings.abilityJumpDusk)) record("jumpNeutron")
            if (playerHasSkill(ZeaStaticStrings.CoffinLink.SKILL_ID)) record("coffinLink")
            if (officerHasSkill(ZeaStaticStrings.BossCore.DUSK_CORE.exclusiveSkillID)) record("neuralLink")
            if (Global.getSector().getEntityById(ZeaStaticStrings.ZeaEntities.ZEA_EDF_CORONAL_TAP)?.memoryWithoutUpdate?.contains("\$usable") == true)
                record("hypershuntRestored")
            if (coreInstalled(ZeaStaticStrings.BossCore.ELYSIAN_CORE.itemID)) record("elysianCoreUsed")
            if (coreInstalled(ZeaStaticStrings.BossCore.DAWN_CORE.itemID)) record("dawnCoreUsed")
            if (coreInstalled(ZeaStaticStrings.BossCore.DUSK_CORE.itemID)) record("duskCoreUsed")
            if (hullHeld(ZeaStaticStrings.ZEA_BOSS_AMATERASU) || hullHeld(ZeaStaticStrings.ZEA_BOSS_CORRUPTINGHEART)) record("elysianHullClaimed")
            if (hullHeld(ZeaStaticStrings.ZEA_BOSS_NIAN)) record("dawnHullClaimed")
            if (hullHeld(ZeaStaticStrings.ZEA_BOSS_YUKIONNA)) record("duskHullClaimed")
            if (hullHeld(ZeaStaticStrings.ZEA_BOSS_NINAYA) || hullHeld(ZeaStaticStrings.ZEA_BOSS_NINMAH) || hullHeld(ZeaStaticStrings.ZEA_BOSS_NINEVENH))
                record("tritachHullClaimed")
            if (hullHeld(KolStaticStrings.KOL_INVICTUS_LP) || hullHeld(KolStaticStrings.KOL_BOSS_RET_LP)) record("pathHullClaimed")

            // lore counts per source (only rise) and mechanic intel
            for ((source, factionId) in LORE_SOURCES) {
                val count = crestCount(ZeaLoreIntel::class.java, factionId)
                if (count > loreCount(source)) memory.set(key("lore_$source"), count)
                if (count > 0) record("lore_${source}_any")
            }
            for ((source, factionId) in LORE_SOURCES) {
                if (crestCount(ZeaMechanicIntel::class.java, factionId) > 0) record("mechanics_$source")
            }

            // grades
            fun lore(s: String) = has("lore_${s}_any") || has("mechanics_$s")
            setGrade(ELYSIAN, when {
                has("coffinLink") || has("hypershuntRestored") || has("elysianCoreUsed") || has("elysianHullClaimed") -> 4
                has("amaterasuDefeated") || has("heartDefeated") -> 3
                has("elysiaVisited") -> 2
                lore(ELYSIAN) || lore("hegemony") -> 1
                else -> 0
            })
            setGrade(DUSK, when {
                has("neuralLink") || has("duskCoreUsed") || has("duskHullClaimed") -> 4
                has("yukionnaDefeated") -> 3
                has("nullspaceVisited") || has("neutronVisited") -> 2
                lore(DUSK) -> 1
                else -> 0
            })
            setGrade(DAWN, when {
                has("dawnCoreUsed") || has("dawnHullClaimed") -> 4
                has("nianDefeated") -> 3
                has("lunaSeaVisited") || has("ozymandiasVisited") -> 2
                lore(DAWN) -> 1
                else -> 0
            })
            setGrade(TRITACH, when {
                has("tritachHullClaimed") -> 4
                has("ninayaDefeated") || has("ninmahDefeated") || has("ninevehDefeated") -> 3
                has("alphaSiteVisited") || has("deltaSiteVisited") || has("ninmahSystemVisited") -> 2
                lore(TRITACH) -> 1
                else -> 0
            })
            setGrade(PATH, when {
                has("pathHullClaimed") -> 4
                has("invictusLpDefeated") || has("retributionLpDefeated") -> 3
                else -> 0
            })

            // aggregates
            memory.set(KolStory.CHRON_POWERS_DEFEATED,
                listOf("amaterasuDefeated", "heartDefeated", "nianDefeated", "yukionnaDefeated").count { has(it) })
            memory.set(KolStory.CHRON_TT_SITES_CLEARED,
                listOf("ninayaDefeated", "ninmahDefeated", "ninevehDefeated").count { has(it) })
        }
    }
}

/** Saved chronicle data that isn't a flag: the list of visited sealable systems. */
class KolChronicleData {
    val visitedSealable = LinkedHashSet<String>()

    companion object {
        fun get(): KolChronicleData {
            val data = Global.getSector().persistentData
            return data[KolStory.CHRON_DATA_KEY] as? KolChronicleData ?: KolChronicleData().also { data[KolStory.CHRON_DATA_KEY] = it }
        }
    }
}
