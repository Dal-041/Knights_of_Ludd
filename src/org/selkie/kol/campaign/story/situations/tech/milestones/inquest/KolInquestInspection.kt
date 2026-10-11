package org.selkie.kol.campaign.story.situations.tech.milestones.inquest

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.situations.tech.KolTechSettings
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.core.KolMissions
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.situations.tech.TechKeys
import org.selkie.kol.campaign.story.systems.chapter.KolChapter
import org.selkie.kol.campaign.story.systems.chapter.KolChapterRequirements
import org.selkie.kol.campaign.story.systems.severance.KolSeverance

/**
 * The Inquisition's inspection of the player's fleet during the inquest. Counts what an Order of Luddites would call
 * abominations: Remnant and zea_ ships, weapons, fighters and hullmods (fitted or in cargo), zea_ cores (installed or
 * carried), and vanilla AI cores (any AI officer; alpha and beta cores in cargo). The weighted score is one part
 * of the inquest's total (KolCh2Story). Surrendering removes all of it, the flagship included.
 */
object KolInquestInspection {
    private const val REMNANT = "Remnant"

    class Report {
        var ships = 0
        var weapons = 0
        var hullmods = 0
        var aiOfficers = 0
        var cores = 0
        var score = 0f
    }

    private fun isAbominationHull(member: FleetMemberAPI) =
        member.hullSpec.baseHullId.startsWith("zea_") || member.hullSpec.manufacturer == REMNANT

    private fun isAbominationWeapon(id: String?): Boolean {
        if (id == null) return false
        if (id.startsWith("zea_")) return true
        val spec: WeaponSpecAPI? = runCatching { Global.getSettings().getWeaponSpec(id) }.getOrNull()
        return spec?.manufacturer == REMNANT
    }

    private fun isAbominationWing(id: String?): Boolean {
        if (id == null) return false
        if (id.startsWith("zea_")) return true
        val spec = runCatching { Global.getSettings().getFighterWingSpec(id) }.getOrNull()
        return spec?.variant?.hullSpec?.manufacturer == REMNANT
    }

    private fun isZeaCore(id: String?) = id != null && id.startsWith("zea_") && id.contains("core")

    private fun hullWeight(size: HullSize) = when (size) {
        HullSize.FRIGATE -> 1f
        HullSize.DESTROYER -> 2f
        HullSize.CRUISER -> 3f
        HullSize.CAPITAL_SHIP -> 4f
        else -> 1f
    }

    private fun weaponWeight(id: String) = when (runCatching { Global.getSettings().getWeaponSpec(id).size }.getOrNull()) {
        WeaponAPI.WeaponSize.LARGE -> 1.5f
        WeaponAPI.WeaponSize.MEDIUM -> 1f
        else -> 0.5f
    }

    private fun coreWeight(id: String, installed: Boolean): Float = when {
        isZeaCore(id) -> if (installed) 5f else 4f
        id == Commodities.ALPHA_CORE -> if (installed) 3f else 2f
        id == Commodities.BETA_CORE -> if (installed) 2f else 1f
        id == Commodities.GAMMA_CORE -> if (installed) 1f else 0f   // gamma cores count only as officers
        else -> if (installed) 2f else 0f                            // other AI officers (mods' cores)
    }

    fun inspect(): Report {
        val report = Report()
        val fleet = Global.getSector().playerFleet ?: return report
        val cargo = fleet.cargo
        for (member in fleet.fleetData.membersListCopy) {
            if (isAbominationHull(member)) {
                report.ships++
                report.score += hullWeight(member.hullSpec.hullSize) * if (member.hullSpec.baseHullId.startsWith("zea_boss")) 2f else 1f
            }
            val variant = member.variant
            for (slot in variant.nonBuiltInWeaponSlots) {
                val id = variant.getWeaponId(slot)
                if (isAbominationWeapon(id)) { report.weapons++; report.score += weaponWeight(id!!) }
            }
            for (wing in variant.nonBuiltInWings) {
                if (isAbominationWing(wing)) { report.weapons++; report.score += 1f }
            }
            for (mod in variant.nonBuiltInHullmods) {
                if (mod.startsWith("zea_")) { report.hullmods++; report.score += 1f }
            }
            val captain = member.captain
            if (captain != null && captain.isAICore && captain.aiCoreId != null) {
                report.aiOfficers++
                report.score += coreWeight(captain.aiCoreId, true)
            }
        }
        for (stack in cargo.stacksCopy) {
            val size = stack.size
            when {
                stack.isWeaponStack && isAbominationWeapon(stack.weaponSpecIfWeapon?.weaponId) -> {
                    report.weapons += size.toInt(); report.score += weaponWeight(stack.weaponSpecIfWeapon.weaponId) * size
                }
                stack.isFighterWingStack && isAbominationWing(stack.fighterWingSpecIfWing?.id) -> {
                    report.weapons += size.toInt(); report.score += size
                }
                stack.isCommodityStack -> {
                    val id = stack.commodityId
                    val weight = coreWeight(id, false)
                    if (weight > 0f && (isZeaCore(id) || id == Commodities.ALPHA_CORE || id == Commodities.BETA_CORE)) {
                        report.cores += size.toInt(); report.score += weight * size
                    }
                }
            }
        }
        return report
    }

    /**
     * Hands everything counted over to the Inquisition, the flagship included. If no clean ship would be left to
     * carry the player, a Mercury shuttle is provided first. Returns true if the shuttle was given.
     */
    fun surrender(): Boolean {
        val fleet = Global.getSector().playerFleet ?: return false
        val cargo: CargoAPI = fleet.cargo
        var shuttle = false
        val flagship = fleet.flagship
        if (flagship != null && isAbominationHull(flagship)) {
            val newFlag = fleet.fleetData.membersListCopy.filter { !isAbominationHull(it) && !it.isFighterWing }
                .maxByOrNull { it.fleetPointCost }
                ?: Global.getFactory().createFleetMember(FleetMemberType.SHIP, "mercury_Standard").also {
                    fleet.fleetData.addFleetMember(it)
                    it.repairTracker.cr = it.repairTracker.maxCR
                    shuttle = true
                }
            fleet.fleetData.setFlagship(newFlag)
            newFlag.captain = Global.getSector().playerPerson
        }
        for (member in fleet.fleetData.membersListCopy) {
            if (isAbominationHull(member)) {
                fleet.fleetData.removeFleetMember(member)
                continue
            }
            val variant = member.variant
            for (slot in ArrayList(variant.nonBuiltInWeaponSlots)) {
                if (isAbominationWeapon(variant.getWeaponId(slot))) variant.clearSlot(slot)
            }
            for (wing in ArrayList(variant.nonBuiltInWings)) {
                if (isAbominationWing(wing)) variant.setWingId(variant.wings.indexOf(wing), null)
            }
            for (mod in ArrayList(variant.nonBuiltInHullmods)) {
                if (mod.startsWith("zea_")) { variant.removePermaMod(mod); variant.removeMod(mod) }
            }
            if (member.captain?.isAICore == true) member.captain = null
        }
        for (stack in cargo.stacksCopy) {
            val remove = when {
                stack.isWeaponStack -> isAbominationWeapon(stack.weaponSpecIfWeapon?.weaponId)
                stack.isFighterWingStack -> isAbominationWing(stack.fighterWingSpecIfWing?.id)
                stack.isCommodityStack -> isZeaCore(stack.commodityId) || stack.commodityId == Commodities.ALPHA_CORE ||
                        stack.commodityId == Commodities.BETA_CORE
                else -> false
            }
            if (remove) cargo.removeStack(stack)
        }
        fleet.fleetData.setSyncNeeded()
        fleet.fleetData.syncIfNeeded()
        return shuttle
    }

    /** As the Shrouded handover does: the Knights and the Church turn on the player. */
    fun excommunicate() = KolSeverance.excommunicate("inquest",
        runCatching { RepLevel.valueOf(KolTechSettings.excommunicationRep) }.getOrDefault(RepLevel.VENGEFUL))
}

/** The inquest into the player, from TRUSTED (a Technology milestone). */
object KolInquestModule : KolStoryModule {
    override val id = "inquest"
    private val memory get() = Global.getSector().memoryWithoutUpdate

    override fun onLoad() = KolPeople.resolver("inquisitor") { KolInquest.inquisitor(it.ifEmpty { "lead" }) }

    override fun requirements() = listOf(KolStoryModule.Requirement(2, "inquestPlayer",
        { KolChapterRequirements.inquestValid(KolInquest.trustedThreshold()) },
        { memory.getBoolean(InquestKeys.INQUEST_PLAYER_DONE) }))

    override fun pulse() {
        if (KolChapter.get() < 2) return
        if (memory.getBoolean(TechKeys.STAGE_FLAGS[2]) && !memory.getBoolean(InquestKeys.INQUEST_PLAYER_DONE) &&
            !memory.getBoolean(InquestKeys.INQUEST_ACTIVE)) {
            KolMissions.start(InquestKeys.INQUEST_ID, KolIds.GREENFLIGHT)
        }
    }

    override fun severedKeys() = listOf("\$kolInq")
}
