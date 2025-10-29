package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin

// class literally only exists to makle the explosion not really explode much
class AbyssSeaWaveExplosion: ExplosionEntityPlugin() {

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        shockwaveAccel *= 0.3f
    }

}