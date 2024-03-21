package org.selkie.kol.helpers

import com.fs.starfarer.api.combat.ShipAPI
import org.lazywizard.lazylib.FastTrig
import org.lwjgl.util.vector.Vector2f

object MathHelpers {
    @JvmStatic
    fun computeSpriteCenter(ship: ShipAPI): Vector2f {
        val sprite = ship.spriteAPI
        val offsetX = sprite.width / 2 - sprite.centerX
        val offsetY = sprite.height / 2 - sprite.centerY
        val trueOffsetX = FastTrig.cos(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetX - FastTrig.sin(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetY
        val trueOffsetY = FastTrig.sin(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetX + FastTrig.cos(Math.toRadians((ship.facing - 90f).toDouble())).toFloat() * offsetY
        return Vector2f(ship.location.x + trueOffsetX, ship.location.y + trueOffsetY)
    }
}