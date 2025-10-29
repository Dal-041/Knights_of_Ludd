package org.selkie.kol

import com.fs.starfarer.api.Global
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings.lunaSeaSysName
import org.selkie.zea.world.PrepareAbyss.addLunaSeaWaves

object CompatabilityUtils {
    fun run(version: String) {
        if (version >= "1.4.0" && !Global.getSector().memoryWithoutUpdate.getBoolean("\$KOL_didLunaSeaWaveGen")) {
            updateLunaSea()
        }
    }

    private fun updateLunaSea() {
        val luna = Global.getSector().getStarSystem(lunaSeaSysName) ?: return
        val star = luna.star ?: return
        for (terrain in luna.terrainCopy) {
            if (terrain.plugin.spec.id == ZeaStaticStrings.ZeaTerrain.ZEA_SEA_WAVE) {
                luna.removeEntity(terrain) // sanitize
            }
        }

        addLunaSeaWaves(star, luna)
    }
}