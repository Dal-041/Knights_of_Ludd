package org.selkie.zea.shipsystems.AI

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.combat.StarficzAIUtilsKt


class SolarFlareAI: ShipSystemAIScript {
    val interval = IntervalUtil(0.5f, 1f)

    val nearbyEnemies: MutableMap<ShipAPI, StarficzAIUtilsKt.DpsProfile> = mutableMapOf()

    lateinit var ship: ShipAPI
    override fun init(
        ship: ShipAPI,
        system: ShipSystemAPI?,
        flags: ShipwideAIFlags?,
        engine: CombatEngineAPI?
    ) {
        this.ship = ship
    }

    override fun advance(
        amount: Float,
        missileDangerDir: Vector2f?,
        collisionDangerDir: Vector2f?,
        target: ShipAPI?
    ) {
        interval.advance(amount)
        if (interval.intervalElapsed()){
            AIUtils.getNearbyEnemies(ship, 2500f)
                .filter { foundEnemy ->
                    foundEnemy.hullSize != ShipAPI.HullSize.FIGHTER &&
                    foundEnemy.isAlive &&
                    !foundEnemy.isFighter &&
                    !nearbyEnemies.containsKey(foundEnemy)
                }
                .forEach { foundEnemy ->
                    nearbyEnemies[foundEnemy] = StarficzAIUtilsKt.createShipDpsProfile(foundEnemy)
                }

            nearbyEnemies.keys.removeIf {
                !it.isAlive || !MathUtils.isWithinRange(it, ship, 3000f)
            }

            var dpsDanger = 0f
            nearbyEnemies.forEach { enemy ->
                dpsDanger += enemy.value.getDpsDangerAtLocation(ship.location)
            }

            if (AIUtils.canUseSystemThisFrame(ship)) {
                if(AIUtils.getNearbyAllies(ship, 2000f).count() >= 2 && dpsDanger < 500) {
                    if (!ship.system.isActive && ship.fluxLevel < 0.7f)
                        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0)
                }
                else if (ship.system.isActive && ship.fluxLevel > 0.8f) {
                    ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0)
                }
            }
        }
    }
}
