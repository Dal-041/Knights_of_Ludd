package org.selkie.kol.shipsystems

import com.fs.starfarer.api.impl.combat.OrionDeviceStats
import java.awt.Color

class hallowedOrionDevice : OrionDeviceStats() {
    init {
        p = OrionDeviceParams()
//        p.shapedExplosionColor = Color(59, 193, 255 , 155)
//        p.jitterColor = Color(59, 193, 255 , 55)
        p.bombWeaponId = "kol_hallowedODLauncher"
    }
}