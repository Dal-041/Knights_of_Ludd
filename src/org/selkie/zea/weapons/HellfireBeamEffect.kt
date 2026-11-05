package org.selkie.zea.weapons

import com.fs.starfarer.api.combat.BeamAPI
import com.fs.starfarer.api.combat.BeamEffectPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

/**
 * Embers and flame spitting off the hellfire beam, plus a flare where it lands.
 */
class HellfireBeamEffect : BeamEffectPlugin {

    private val emberInterval = IntervalUtil(0.02f, 0.04f)
    private val impactInterval = IntervalUtil(0.08f, 0.16f)

    override fun advance(amount: Float, engine: CombatEngineAPI, beam: BeamAPI) {
        val source = beam.source
        if (source == null || !source.isAlive) return

        emberInterval.advance(amount)
        if (emberInterval.intervalElapsed()) {
            val along = MathUtils.getRandomNumberInRange(0.05f, 1f)
            val emberLocation = Vector2f(
                beam.from.x + (beam.to.x - beam.from.x) * along,
                beam.from.y + (beam.to.y - beam.from.y) * along
            )
            val emberVelocity = MathUtils.getPointOnCircumference(
                source.velocity,
                MathUtils.getRandomNumberInRange(20f, 80f),
                MathUtils.getRandomNumberInRange(0f, 360f)
            )

            engine.addHitParticle(
                emberLocation,
                emberVelocity,
                beam.width * MathUtils.getRandomNumberInRange(0.3f, 0.8f),
                1f,
                MathUtils.getRandomNumberInRange(0.25f, 0.55f),
                if (Math.random() < 0.35) EMBER_CORE_COLOR else beam.fringeColor
            )
        }

        impactInterval.advance(amount)
        if (impactInterval.intervalElapsed() && beam.didDamageThisFrame()) {
            engine.addHitParticle(
                beam.to,
                source.velocity,
                beam.width * MathUtils.getRandomNumberInRange(3f, 4.5f),
                1f,
                MathUtils.getRandomNumberInRange(0.12f, 0.25f),
                beam.fringeColor
            )
            engine.addNebulaParticle(
                beam.to,
                source.velocity,
                beam.width * MathUtils.getRandomNumberInRange(2f, 3f),
                1.8f,
                0.1f,
                0.25f,
                MathUtils.getRandomNumberInRange(0.5f, 1f),
                EMBER_SMOKE_COLOR,
                true
            )
        }
    }

    companion object {
        private val EMBER_CORE_COLOR = Color(255, 190, 60, 255)
        private val EMBER_SMOKE_COLOR = Color(60, 25, 15, 190)
    }
}