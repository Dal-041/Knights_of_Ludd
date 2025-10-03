package org.selkie.zea.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getAngleInDegrees
import org.magiclib.kotlin.getAngleInDegreesStrict


class DuskWormController: BaseHullMod() {
    override fun applyEffectsAfterShipAddedToCombatEngine(ship: ShipAPI, id: String?) {

        val head = DuskWormHead(ship)
        ship.addListener(head)

        var currentSegment: DuskWormSegment = head
        ship.childModulesCopy.sortedBy{ it.stationSlot.id }.forEach {
            val nextSegment = DuskWormSegment(it)
            nextSegment.segmentAhead = currentSegment
            currentSegment.segmentBehind = nextSegment
            currentSegment = nextSegment
        }
        // initial locations
        head.updateLocations()
    }

    open class DuskWormSegment(val ship: ShipAPI) {
        var segmentAhead: DuskWormSegment? = null
        var segmentBehind: DuskWormSegment? = null

        var jointAheadLocation: Vector2f? = null
        var jointBehindLocation: Vector2f? = null
        var jointAheadOffset: Vector2f? = null
    }

    class DuskWormHead(ship: ShipAPI) : DuskWormSegment(ship), AdvanceableListener{
        fun updateLocations(){
            var currentSegment: DuskWormSegment? = this

            while (currentSegment != null){
                val currentJointAheadSlot = currentSegment.ship.hullSpec.allWeaponSlotsCopy.firstOrNull { it.id == "JOINT_AHEAD" }
                currentSegment.jointAheadLocation = currentJointAheadSlot?.computePosition(currentSegment.ship) ?: currentSegment.ship.location

                val currentJointBehindSlot = currentSegment.ship.hullSpec.allWeaponSlotsCopy.firstOrNull { it.id == "JOINT_BEHIND" }
                currentSegment.jointBehindLocation  = currentJointBehindSlot?.computePosition(currentSegment.ship) ?: currentSegment.ship.location

                currentSegment.jointAheadOffset = (currentSegment.ship.location - currentSegment.jointAheadLocation!!)
                    .rotate(-currentSegment.ship.facing)

                currentSegment = currentSegment.segmentBehind
            }
        }

        override fun advance(amount: Float) {
            if (!ship.isAlive) {
                return
            }

            // no strafing, force forwards
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT)
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT)
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0)


            // make sure to go towards facing
            ship.velocity.rotate(
                MathUtils.getShortestRotation(
                    ship.velocity.getAngleInDegrees(),
                    ship.facing
                )
            )

            // update the first head joint as that should follow the combat engine
            var currentSegment: DuskWormSegment = this
            currentSegment.jointBehindLocation = ship.hullSpec.allWeaponSlotsCopy.firstOrNull { it.id == "JOINT_BEHIND" }!!.computePosition(ship)

            var nextSegment = currentSegment.segmentBehind

            while (nextSegment != null){

                // simple IK
                nextSegment.ship.facing = (nextSegment.jointBehindLocation ?: nextSegment.ship.location)
                    .getAngleInDegreesStrict(currentSegment.jointBehindLocation!!)
                val offset = Vector2f(nextSegment.jointAheadOffset).rotate(nextSegment.ship.facing)
                nextSegment.ship.location.set(currentSegment.jointBehindLocation!! + offset)

                currentSegment = nextSegment
                nextSegment = nextSegment.segmentBehind

                // after done with updating, record the new locations
                val currentJointAheadSlot = currentSegment.ship.hullSpec.allWeaponSlotsCopy.firstOrNull { it.id == "JOINT_AHEAD" }
                currentSegment.jointAheadLocation = currentJointAheadSlot?.computePosition(currentSegment.ship) ?: currentSegment.ship.location

                val currentJointBehindSlot = currentSegment.ship.hullSpec.allWeaponSlotsCopy.firstOrNull { it.id == "JOINT_BEHIND" }
                currentSegment.jointBehindLocation  = currentJointBehindSlot?.computePosition(currentSegment.ship) ?: currentSegment.ship.location

            }
        }
    }
}