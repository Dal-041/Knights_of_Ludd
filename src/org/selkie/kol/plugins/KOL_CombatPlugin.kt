package org.selkie.kol.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.paintjobs.MagicPaintjobManager
import org.selkie.kol.combat.ParticleController

class KOL_CombatPlugin : BaseEveryFrameCombatPlugin() {
    override fun init(engine: CombatEngineAPI) {
        engine.addLayeredRenderingPlugin(ParticleController())

    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
        applyFighterPaintjobs()
    }

    /**
     * Vanilla fighters flown by an Elysian or Dawn carrier get that faction's MagicLib paintjob.
     * The family is the carrier's hull style, so every hull using those styles works without extra data.
     */
    private fun applyFighterPaintjobs() {
        if (!MagicPaintjobManager.isEnabled) return
        val engine = Global.getCombatEngine() ?: return
        for (ship in engine.ships) {
            if (ship.isFighter) continue
            val family = FIGHTER_PAINTJOB_FAMILIES[ship.hullStyleId] ?: continue
            // a carrier with its own paintjob lets MagicLib paint its wings by that paintjob's family
            if (MagicPaintjobManager.getCurrentShipPaintjob(ship.variant) != null) continue
            for (wing in ship.allWings) {
                for (fighter in wing.wingMembers) {
                    val data = fighter.customData
                    if (data.containsKey(FIGHTER_PAINTJOB_KEY) || data.containsKey(MAGIC_PAINTJOB_APPLIED_KEY)) continue
                    MagicPaintjobManager.getPaintjobsForHull(fighter.hullSpec.baseHullId)
                        .firstOrNull { it.paintjobFamily == family }
                        ?.let { MagicPaintjobManager.applyPaintjob(fighter, it) }
                    fighter.setCustomData(FIGHTER_PAINTJOB_KEY, true)
                }
            }
        }
    }

    companion object {
        private const val FIGHTER_PAINTJOB_KEY = "\$kol_fighter_paintjob"
        private const val MAGIC_PAINTJOB_APPLIED_KEY = "MagicPaintjobApplied"
        private val FIGHTER_PAINTJOB_FAMILIES = mapOf(
            "ZEA_ELYSIAN_TECH" to "ZEA_ELYSIAN_TECH",
            "ZEA_DAWN_TECH" to "ZEA_DAWN_TECH"
        )
    }
}