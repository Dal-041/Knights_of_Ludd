package org.selkie.zea.combat.subsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.magiclib.subsystems.MagicSubsystem
import java.awt.Color
import java.util.EnumSet

/**
 * Accelerated Ammo Feeder as a subsystem: same stat effect as vanilla AmmoFeedStats, with an AI that
 * approximates vanilla's WEAPON_BOOST system AI (fire it while ballistics are engaging a ship in range).
 */
class AmmoFeedSubsystem(ship: ShipAPI) : MagicSubsystem(ship) {
    companion object {
        const val ROF_BONUS = 1f
        const val FLUX_REDUCTION = 0.5f
        const val COOLDOWN = 20f
        private const val MAX_FLUX_FOR_AI = 0.9f
        private val GLOW_COLOR = Color(255, 200, 0, 155)
        private val BALLISTIC = EnumSet.of(WeaponType.BALLISTIC)

        fun activeDuration(hullSize: HullSize): Float =
            if (hullSize == HullSize.CRUISER || hullSize == HullSize.CAPITAL_SHIP) 3f else 5f
    }

    private val id = "zea_ammo_feed_${ship.id}"
    private val statusKey = Any()
    private var applied = false
    private val aiInterval = IntervalUtil(0.25f, 0.5f)
    private val ballistics: List<WeaponAPI> = ship.allWeapons.filter {
        !it.isDecorative && it.type == WeaponType.BALLISTIC && !it.hasAIHint(WeaponAPI.AIHints.PD)
    }

    override fun getDisplayText(): String = "Accelerated Ammo Feeder"

    override fun getBaseInDuration(): Float = 1f
    override fun getBaseActiveDuration(): Float = activeDuration(ship.hullSize)
    override fun getBaseOutDuration(): Float = 1f
    override fun getBaseCooldownDuration(): Float = COOLDOWN

    /** Shortens the remaining cooldown by [fraction] of its full duration; used by Photosphere. */
    fun advanceCooldown(fraction: Float) {
        if (state == State.COOLDOWN) stateInterval.advance(stateInterval.intervalDuration * fraction)
    }

    override fun onActivate() {
        Global.getSoundPlayer().playSound("system_ammo_feeder", 1f, 1f, ship.location, ship.velocity)
    }

    override fun shouldActivateAI(amount: Float): Boolean {
        aiInterval.advance(amount)
        if (!aiInterval.intervalElapsed()) return false
        if (ballistics.isEmpty() || ship.isPhased || ship.fluxLevel > MAX_FLUX_FOR_AI) return false

        val target = ship.shipTarget?.takeIf { it.isAlive && it.owner != ship.owner && !it.isFighter }
            ?: AIUtils.getNearestEnemy(ship)?.takeIf { !it.isFighter }
            ?: return false

        // At least one ballistic gun must be firing and able to reach the target.
        val distance = MathUtils.getDistance(ship, target)
        return ballistics.any { !it.isDisabled && it.isFiring && it.range >= distance }
    }

    override fun advance(amount: Float, isPaused: Boolean) {
        if (isPaused) return
        val stats = ship.mutableStats
        val level = effectLevel
        if (isOn() && ship.isAlive) {
            applied = true
            stats.ballisticRoFMult.modifyMult(id, 1f + ROF_BONUS * level)
            stats.ballisticWeaponFluxCostMod.modifyMult(id, 1f - FLUX_REDUCTION)
            ship.setWeaponGlow(level, GLOW_COLOR, BALLISTIC)
            if (Global.getCombatEngine().playerShip === ship) {
                Global.getCombatEngine().maintainStatusForPlayerShip(
                    statusKey, "graphics/icons/hullsys/ammo_feeder.png", displayText,
                    "ballistic rate of fire +${(ROF_BONUS * level * 100).toInt()}%, flux use -${(FLUX_REDUCTION * 100).toInt()}%", false
                )
            }
        } else if (applied) {
            applied = false
            stats.ballisticRoFMult.unmodify(id)
            stats.ballisticWeaponFluxCostMod.unmodify(id)
            ship.setWeaponGlow(0f, GLOW_COLOR, BALLISTIC)
        }
    }
}
