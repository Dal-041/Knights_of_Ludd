package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.loading.MissileSpecAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.MagicSubsystemsManager.addSubsystemToShip
import org.selkie.kol.combat.StarficzAIUtils
import org.selkie.kol.impl.combat.subsystems.NianFlaresSubsystem
import org.selkie.kol.impl.combat.subsystems.ShieldDronesSubsystem
import org.selkie.kol.impl.helpers.ZeaStaticStrings
import org.selkie.kol.impl.shipsystems.SupernovaStats
import java.awt.Color
import java.util.*
import kotlin.math.abs

class NianBoss : BaseHullMod() {
    class NianBossEnragedScript(var ship: ShipAPI) : AdvanceableListener, HullDamageAboutToBeTakenListener {
        private var enraged = false
        private var enragedTransitionTime = 0f
        var engine: CombatEngineAPI = Global.getCombatEngine()
        override fun notifyAboutToTakeHullDamage(param: Any, ship: ShipAPI, point: Vector2f, damageAmount: Float): Boolean {
            if (ship.hitpoints - damageAmount <= ship.maxHitpoints * 0.5f && !enraged) {
                enraged = true
                ship.mutableStats.ballisticRoFMult.modifyMult(ENRAGED_ID, ENRAGED_FIRERATE_MULT)
                ship.mutableStats.energyRoFMult.modifyMult(ENRAGED_ID, ENRAGED_FIRERATE_MULT)
                ship.mutableStats.missileRoFMult.modifyMult(ENRAGED_ID, ENRAGED_FIRERATE_MULT)
                ship.mutableStats.ballisticWeaponFluxCostMod.modifyMult(ENRAGED_ID, ENRAGED_FLUX_COST_MULT)
                ship.mutableStats.energyWeaponFluxCostMod.modifyMult(ENRAGED_ID, ENRAGED_FLUX_COST_MULT)
                ship.mutableStats.missileWeaponFluxCostMod.modifyMult(ENRAGED_ID, ENRAGED_FLUX_COST_MULT)
                ship.mutableStats.engineDamageTakenMult.modifyMult(ENRAGED_ID, ENRAGED_ENGINE_DAMAGE_MULT)
                ship.mutableStats.hullDamageTakenMult.modifyMult(ENRAGED_ID, 0f)
                ship.setWeaponGlow(1f, Color.RED, EnumSet.allOf(WeaponType::class.java))
                ship.mutableStats.peakCRDuration.modifyFlat(ENRAGED_ID, ship.hullSpec.noCRLossSeconds)
                ship.setCustomData(ENRAGED_ID, true)
                return true
            }
            if (enraged) {
                val hullMult = 1f - ship.hullLevel / 0.5f //larger as health below half gets lower
                ship.mutableStats.minArmorFraction.modifyPercent(ENRAGED_ID, hullMult * 400f)
            }
            return false
        }

        override fun advance(amount: Float) {
            if (enraged) {
                if (enragedTransitionTime <= ENRAGED_TRANSITION_TIME) {
                    enragedTransitionTime += amount
                    if (enragedTransitionTime > ENRAGED_TRANSITION_TIME) {
                        ship.mutableStats.hullDamageTakenMult.unmodify(ENRAGED_ID)
                        return
                    }
                    StarficzAIUtils.stayStill(ship)
                    if (ship.shield != null) {
                        ship.shield.toggleOff()
                        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK)
                    }
                } else {
                    ship.setJitterUnder(ENRAGED_ID, Color(125, 255, 65), 1f, 5, 10f)
                }
            }
        }

        companion object {
            const val ENRAGED_TRANSITION_TIME = 5f
            const val ENRAGED_FIRERATE_MULT = 2f
            const val ENRAGED_FLUX_COST_MULT = 0.5f
            const val ENRAGED_ENGINE_DAMAGE_MULT = 0.1f
            const val ENRAGED_ID = "boss_enraged_modifier"
        }
    }

    class NianAIScript(var ship: ShipAPI) : AdvanceableListener {
        var engine: CombatEngineAPI = Global.getCombatEngine()
        var fireTimer = IntervalUtil(15f, 15f)
        var targetSearchTimer = IntervalUtil(0.9f, 1f)
        var target: ShipAPI? = null
        var readyToFire = false
        var needInit = true
        var shotSpeed = 1000f

        fun init(){
            needInit = false
            for(weaponSpec in Global.getSettings().allWeaponSpecs){
                if (weaponSpec.weaponId == "zea_nian_maingun_l"){
                    shotSpeed = (weaponSpec.projectileSpec as MissileSpecAPI).launchSpeed
                }
            }
        }
        override fun advance(amount: Float) {
            init()
            if (!ship.isAlive || ship.parentStation != null || !engine.isEntityInPlay(ship)) {
                return
            }
            targetSearchTimer.advance(amount)
            if(targetSearchTimer.intervalElapsed()){
                for (potentialTarget in engine.ships){
                    if (potentialTarget.owner == 100 ||  potentialTarget.owner == ship.owner) continue
                    target = target?.let { current ->
                        when {
                            current.hullSize < potentialTarget.hullSize -> potentialTarget
                            abs(ship.facing - VectorUtils.getAngle(ship.location, potentialTarget.location)) < abs(ship.facing - VectorUtils.getAngle(ship.location, current.location)) -> potentialTarget
                            else -> current
                        }
                    } ?: potentialTarget
                }
            }

            var angleToTarget = 180f
            target?.let {
                val targetPoint = Vector2f.add(it.location, Vector2f(it.velocity).scale(MathUtils.getDistance(it, ship)/shotSpeed) as Vector2f, null)
                StarficzAIUtils.turnToPoint(ship, targetPoint)
                angleToTarget = MathUtils.getShortestRotation(ship.facing, VectorUtils.getAngle(ship.location, targetPoint))
            }

            fireTimer.advance(amount)
            if(fireTimer.intervalElapsed()){
                readyToFire = true
            }

            if(readyToFire){
                if(angleToTarget < 1f){
                    //TODO: Fire Main Gun
                    readyToFire = false
                } else if(AIUtils.getNearbyEnemies(ship, SupernovaStats.EXPLOSION_RADIUS).size >= 1){
                    //TODO: Supernova Explosion
                    readyToFire = false
                }
            }
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        addSubsystemToShip(ship, NianFlaresSubsystem(ship))
        val isBoss = ship.variant.hasTag(ZeaStaticStrings.BOSS_TAG) || ship.fleetMember != null && ship.fleetMember.fleetData != null && ship.fleetMember.fleetData.fleet != null && ship.fleetMember.fleetData.fleet.memoryWithoutUpdate.contains(ZeaStaticStrings.BOSS_TAG)
        if (isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            if (!ship.hasListenerOfClass(NianBossEnragedScript::class.java)) ship.addListener(NianBossEnragedScript(ship))
            // Ill do this one day
            //if (!ship.hasListenerOfClass(NianAIScript::class.java)) ship.addListener(NianAIScript(ship))
            addSubsystemToShip(ship, ShieldDronesSubsystem(ship, 5, true))
        }
    }
}
