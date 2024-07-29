package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener

class LunariaArmorModuleVectoring : BaseHullMod() {
    class LunariaArmorModuleVectoringListener(var ship: ShipAPI) : AdvanceableListener {
        var thrustLevel = 0f
        val maxRotation = 30f
        override fun advance(amount: Float) {
            if(ship.parentStation == null) return
            if (ship.parentStation.engineController.isAccelerating) thrustLevel += amount * 1
            else thrustLevel -= amount * 1
            thrustLevel = thrustLevel.coerceIn(0f, 1f)
            if(ship.hullSpec.hullId.contains("lr")){
                ship.facing = ship.parentStation.facing + thrustLevel * maxRotation
            } else{
                ship.facing = ship.parentStation.facing - thrustLevel * maxRotation
            }
        }

    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (!ship.hasListenerOfClass(LunariaArmorModuleVectoringListener::class.java)) ship.addListener(LunariaArmorModuleVectoringListener(ship))
    }
}
