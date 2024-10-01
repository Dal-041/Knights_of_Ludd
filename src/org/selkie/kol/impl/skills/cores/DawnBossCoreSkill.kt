package org.selkie.kol.impl.skills.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.MagicSubsystemsManager
import org.selkie.kol.impl.campaign.cores.BossAICoreOfficerPlugin
import org.selkie.kol.impl.combat.subsystems.ShieldDronesSubsystem
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import org.selkie.kol.impl.hullmods.DawnBuiltin
import kotlin.math.roundToInt


class DawnBossCoreSkill : BaseCoreOfficerSkill() {
    override val skillID = BossCore.DAWN_CORE.exclusiveSkillID
    class DawnBossCoreListener(var ship: ShipAPI) : HullDamageAboutToBeTakenListener, AdvanceableListener {
        var damperTriggered = false
        var damperTimer = 8f
        val DAMAGE_MULT = 0.2f
        val id = "DawnBossCoreDamperBuff"
        val STATUSKEY1 = Any()
        override fun notifyAboutToTakeHullDamage(param: Any?, ship: ShipAPI, point: Vector2f?, damageAmount: Float): Boolean {
            if (ship.hitpoints - damageAmount <= ship.maxHitpoints * 0.5f && !damperTriggered) damperTriggered = true

            return false
        }

        override fun advance(amount: Float) {
            if(damperTriggered && damperTimer > 0){
                val stats = ship.mutableStats
                stats.hullDamageTakenMult.modifyMult(id, DAMAGE_MULT)
                stats.armorDamageTakenMult.modifyMult(id, DAMAGE_MULT)
                stats.empDamageTakenMult.modifyMult(id, DAMAGE_MULT)

                if (ship == Global.getCombatEngine().playerShip) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1, "graphics/icons/hullsys/damper_field.png", "Emergency Damper Field - %.2f second remaining".format(damperTimer),
                        ((1 - DAMAGE_MULT) * 100).roundToInt().toString() + "% less damage taken", false)
                }

                damperTimer -= Global.getCombatEngine().elapsedInLastFrame
            }
        }
    }

    override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
        return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
    }

    override fun createCustomDescription(stats: MutableCharacterStatsAPI?,  skill: SkillSpecAPI?, info: TooltipMakerAPI?,  width: Float) {
        info!!.addSpacer(2f)
        info!!.addPara("Provides the ship with the \"Chiwen\" shield drone subsystem if it does not have it.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        info!!.setBulletedListMode("    - ")
        info!!.addPara("Upgrades existing shield drone subsystem with 1 more drone that all actively block shots.", 0f, Misc.getTextColor(), Misc.getHighlightColor())
        info!!.setBulletedListMode("")
        info!!.addPara("Once per deployment: Below 50%% hull, reduce damage taken by 80%% for 8 seconds.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        info.addSpacer(2f)
    }

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        if (stats!!.entity is ShipAPI) {

            val ship = stats.entity as ShipAPI

            val shieldDronesSubsystem = MagicSubsystemsManager.getSubsystemsForShipCopy(ship)?.find {
                it is ShieldDronesSubsystem } as? ShieldDronesSubsystem

            val smartUpgrade = ship.customData.containsKey(DawnBuiltin.DRONE_ADDED_KEY)

            if (shieldDronesSubsystem == null || shieldDronesSubsystem.isSmart != smartUpgrade) {
                MagicSubsystemsManager.removeSubsystemFromShip(ship, ShieldDronesSubsystem::class.java)
                MagicSubsystemsManager.addSubsystemToShip(ship, ShieldDronesSubsystem(ship, DawnBuiltin.getNumDrones(ship)+1, smartUpgrade))
            }

            if(ship.listenerManager?.hasListenerOfClass(DawnBossCoreListener::class.java) != true) ship.addListener(DawnBossCoreListener(ship))
        }

        stats.fleetMember?.let{ fleetMember ->
            if(fleetMember.hullSpec.manufacturer == "Dawntide"){
                fleetMember.captain?.let{
                    val captainMemory = it.memoryWithoutUpdate
                    captainMemory.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, BossAICoreOfficerPlugin.AUTOMATED_POINTS_MULT_SAME_FACTION)
                }
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
        if (stats!!.entity is ShipAPI) {
            var ship = stats.entity as ShipAPI
        }
    }
}
