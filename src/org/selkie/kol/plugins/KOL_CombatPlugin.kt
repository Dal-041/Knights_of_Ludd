package org.selkie.kol.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.selkie.kol.combat.ParticleController

class KOL_CombatPlugin : BaseEveryFrameCombatPlugin() {
    override fun init(engine: CombatEngineAPI) {
        engine.addLayeredRenderingPlugin(ParticleController())

    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)
    }
}
