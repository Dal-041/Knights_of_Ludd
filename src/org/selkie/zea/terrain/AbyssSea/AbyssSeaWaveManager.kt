package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.selkie.zea.helpers.ZeaStaticStrings
import java.awt.Color

// a simple script that spawns waves on an interval
class AbyssSeaWaveManager(
    val host: SectorEntityToken,
    val params: AbyssSeaWaveManagerParams
): EveryFrameScript {

    data class AbyssSeaWaveManagerParams(
        val minDays: Float = DEFAULT_MIN_DAYS,
        // good to stagger this stuff so you dont end up with lag spikes
        val maxDays: Float = DEFAULT_MAX_DAYS,

    )

    companion object {
        fun addToObject(entity: SectorEntityToken, params: AbyssSeaWaveManagerParams): AbyssSeaWaveManager {
            val script = AbyssSeaWaveManager(entity, params)
            entity.addScript(script)
            return script
        }

        const val MAX_RANGE = 62225f
        const val DEFAULT_MIN_DAYS = 10f
        const val DEFAULT_MAX_DAYS = 10f
        val LUNA_COLOR = Color(61, 178, 255, 255)
    }

    val pulseInterval = IntervalUtil(params.minDays, params.maxDays)

    override fun isDone(): Boolean = !host.isAlive
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        pulseInterval.advance(days)
        if (pulseInterval.intervalElapsed() && host.containingLocation.isCurrentLocation) { // pulses can cause overhead so lets only do it if the player is in here
            doPulse()
        }
    }

    private fun doPulse() {
        val params = AbyssSeaWave.AbyssSeaWaveParams(
            MAX_RANGE,
            50000f, // arbitrary
            2000f,
            host,
            host.radius * 0.8f,
            spawnSounds = listOf("mote_attractor_targeted_ship"),
            damage = ExplosionEntityPlugin.ExplosionFleetDamage.MEDIUM,
        )
        val pulse = AbyssSeaWave.createPulse(
            host,
            params
        )

        // purely visual
        val explosionParams = ExplosionEntityPlugin.ExplosionParams(
            LUNA_COLOR,
            host.containingLocation,
            Vector2f(host.location),
            host.radius * 7f,
            4f
        )
        explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.NONE

        val explosion = host.containingLocation.addCustomEntity(
            Misc.genUID(),
            "you shouldnt see this",
            ZeaStaticStrings.ZeaEntities.ZEA_SEA_WAVE_EXPLOSION,
            Factions.NEUTRAL,
            explosionParams
        )
    }

}