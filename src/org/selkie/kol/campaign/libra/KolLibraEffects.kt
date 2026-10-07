package org.selkie.kol.campaign.libra

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

/**
 * What the restoration does to Battlestar Libra (design, decision 4). Derived from KolLibraData each time, so it's
 * idempotent: called on the situation's first sync, after each milestone, and periodically (the station's fleet is
 * re-created by its industry, so its penalty is reapplied).
 * - before the supply contract: its own economy group, the station badly damaged;
 * - the supply contract: the main economy; the damage reduced;
 * - the crew contract: crewed (no fleet-size penalty, so patrols fly), accessibility, size 4;
 * - the drill: better officers;
 * - the final restoration: the damage gone; the battlestation upgraded to Battlestar Libra;
 * - legitimized (the assembly): no longer hidden.
 * Libra's market stats come from one condition per step (damaged, repairing, under-crewed, crewed, drilled).
 */
object KolLibraEffects {
    // Libra's state, one market condition per step (distinct icons): present or absent by KolLibraData
    const val DAMAGED = "kol_libra_damaged"          // before the supply contract
    const val REPAIRING = "kol_libra_repairing"      // the supply contract until the final restoration
    const val UNDERCREWED = "kol_libra_undercrewed"  // before the crew contract
    const val CREWED = "kol_libra_crewed"            // after the crew contract
    const val DRILLED = "kol_libra_drilled"          // after the drill
    const val SUPPLY_CONDITION = "kol_libra_supply"  // on suppliers, until the drill
    const val STATION_OLD = "kol_battlestation_libra"
    const val STATION_NEW = "kol_battlestar_libra"
    private const val MOD_ID = "kol_libra_restoration"

    /** The station's damage: 2 (badly damaged), 1 (repairs under way), 0 (restored). */
    fun damageLevel(): Int = when {
        KolLibra.done(KolLibraMilestone.RESTORATION) -> 0
        KolLibra.done(KolLibraMilestone.SUPPLY) -> 1
        else -> 2
    }

    private fun conditions(): Map<String, Boolean> = linkedMapOf(
        DAMAGED to (damageLevel() == 2),
        REPAIRING to (damageLevel() == 1),
        UNDERCREWED to !KolLibra.done(KolLibraMilestone.CREW),
        CREWED to KolLibra.done(KolLibraMilestone.CREW),
        DRILLED to KolLibra.done(KolLibraMilestone.DRILL),
    )

    fun apply() {
        val market = KolLibra.market() ?: return
        for ((id, wanted) in conditions()) {
            if (wanted && !market.hasCondition(id)) market.addCondition(id)
            else if (!wanted && market.hasCondition(id)) market.removeCondition(id)
        }
        if (KolLibra.done(KolLibraMilestone.SUPPLY) && market.econGroup != null) market.econGroup = null
        if (KolLibra.done(KolLibraMilestone.CREW) && market.size < 4) {
            market.removeCondition(Conditions.POPULATION_3)
            market.size = 4
            market.addCondition(Conditions.POPULATION_4)
        }
        if (KolLibra.done(KolLibraMilestone.RESTORATION) && market.hasIndustry(STATION_OLD)) {
            market.removeIndustry(STATION_OLD, null, false)
            market.addIndustry(STATION_NEW)
        }
        if (KolLibra.data().legitimized && market.isHidden) {
            market.isHidden = false
            market.memoryWithoutUpdate.unset(MemFlags.HIDDEN_BASE_MEM_FLAG)
        }
        if (KolLibra.done(KolLibraMilestone.DRILL)) KolLibraAgreements.liftSupplierConditions()
        market.reapplyConditions()
        market.reapplyIndustries()
        stationPenalty()
    }

    /** Max CR penalty on the station by damage level 0..2 (placeholders). */
    val STATION_CR_PENALTY = floatArrayOf(0f, 0.2f, 0.4f)
    val GROUND_DEFENSE_MULT = floatArrayOf(1f, 0.75f, 0.5f)

    /** The station's damage as a cap on its combat readiness (reapplied: its fleet is re-created by the industry). */
    fun stationPenalty() {
        val market = KolLibra.market() ?: return
        val station = market.industries.firstOrNull { it is OrbitalStation } as? OrbitalStation ?: return
        val fleet = station.stationFleet ?: return
        val penalty = STATION_CR_PENALTY[damageLevel()]
        for (member in fleet.fleetData.membersListCopy) {
            if (penalty > 0f) member.stats.maxCombatReadiness.modifyFlat(MOD_ID, -penalty, "Libra's damage")
            else member.stats.maxCombatReadiness.unmodifyFlat(MOD_ID)
        }
    }

    /** The assembly takes up Libra's restoration: Libra is legitimized, a charted Knights market. */
    fun legitimize() {
        KolLibra.data().legitimized = true
        apply()
    }
}

/** The station badly damaged (before the supply contract). */
class KolLibraDamagedCondition : BaseMarketConditionPlugin() {
    override fun apply(id: String) {
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, KolLibraEffects.GROUND_DEFENSE_MULT[2], name)
    }
    override fun unapply(id: String) = market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id)
    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        tooltip.addPara("Station maximum combat readiness %s; ground defenses %s.", 10f, Misc.getNegativeHighlightColor(),
            "-" + (KolLibraEffects.STATION_CR_PENALTY[2] * 100).toInt() + "%", "x" + KolLibraEffects.GROUND_DEFENSE_MULT[2])
    }
}

/** Repairs under way (the supply contract until the final restoration): the damage reduced. */
class KolLibraRepairingCondition : BaseMarketConditionPlugin() {
    override fun apply(id: String) {
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, KolLibraEffects.GROUND_DEFENSE_MULT[1], name)
    }
    override fun unapply(id: String) = market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id)
    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        tooltip.addPara("Station maximum combat readiness %s; ground defenses %s.", 10f, Misc.getNegativeHighlightColor(),
            "-" + (KolLibraEffects.STATION_CR_PENALTY[1] * 100).toInt() + "%", "x" + KolLibraEffects.GROUND_DEFENSE_MULT[1])
    }
}

/** Under-crewed (before the crew contract): no fleets fly from Libra. */
class KolLibraUndercrewedCondition : BaseMarketConditionPlugin() {
    override fun apply(id: String) {
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, 0f, name)
    }
    override fun unapply(id: String) = market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(id)
    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        tooltip.addPara("No fleets fly from Libra.", Misc.getNegativeHighlightColor(), 10f)
    }
}

/** A crew of its own (after the crew contract): accessibility. */
class KolLibraCrewedCondition : BaseMarketConditionPlugin() {
    companion object { const val ACCESS_BONUS = 0.3f }
    override fun apply(id: String) {
        market.accessibilityMod.modifyFlat(id, ACCESS_BONUS, name)
    }
    override fun unapply(id: String) = market.accessibilityMod.unmodifyFlat(id)
    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        tooltip.addPara("Accessibility %s.", 10f, Misc.getHighlightColor(), "+" + (ACCESS_BONUS * 100).toInt() + "%")
    }
}

/** Drilled crews (after the drill): better officers and fleet quality. */
class KolLibraDrilledCondition : BaseMarketConditionPlugin() {
    companion object {
        const val OFFICER_PROB_BONUS = 0.3f
        const val QUALITY_BONUS = 0.25f
    }
    override fun apply(id: String) {
        market.stats.dynamic.getMod(Stats.OFFICER_PROB_MOD).modifyFlat(id, OFFICER_PROB_BONUS, name)
        market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).modifyFlat(id, QUALITY_BONUS, name)
    }
    override fun unapply(id: String) {
        market.stats.dynamic.getMod(Stats.OFFICER_PROB_MOD).unmodifyFlat(id)
        market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).unmodifyFlat(id)
    }
    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        tooltip.addPara("Fleet quality %s; more officers.", 10f, Misc.getHighlightColor(), "+" + (QUALITY_BONUS * 100).toInt() + "%")
    }
}
