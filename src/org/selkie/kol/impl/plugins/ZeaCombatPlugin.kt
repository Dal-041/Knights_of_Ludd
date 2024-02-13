package org.selkie.kol.impl.plugins

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import org.selkie.kol.impl.combat.ParticleController

class ZeaCombatPlugin : BaseEveryFrameCombatPlugin() {
    override fun init(engine: CombatEngineAPI) {
        engine.addLayeredRenderingPlugin(ParticleController())
    }
}