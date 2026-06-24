package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import org.selkie.kol.listeners.SaveListener

// class literally only exists to makle the explosion not really explode much
class AbyssSeaWaveExplosion: ExplosionEntityPlugin(), SaveListener {

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        shockwaveAccel *= 0.3f
        Global.getSector().listenerManager.addListener(this)
    }

    override fun beforeGameSave() {
        return
    }

    override fun onGameLoad() {
        sprite = Global.getSettings().getSprite("misc", "nebula_particles")

        if (!entity.isAlive) {
            Global.getSector().listenerManager.removeListener(this)
        }

        return
    }

    override fun afterGameSave() {
        return
    }

    override fun onGameSaveFailed() {
        return
    }

}