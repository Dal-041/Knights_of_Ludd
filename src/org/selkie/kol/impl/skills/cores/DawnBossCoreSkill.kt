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
import org.selkie.kol.impl.combat.subsystems.SimpleShieldDronesSubsystem
import org.selkie.kol.impl.combat.subsystems.SmartShieldDronesSubsystem
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore
import org.selkie.kol.impl.hullmods.DawnBuiltin


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
                        Math.round((1-DAMAGE_MULT)*100).toString() + "% less damage taken", false)
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
        info!!.addPara("Upgrades existing shield drone subsystem with 2 more drones that all actively block shots.", 0f, Misc.getTextColor(), Misc.getHighlightColor())
        info!!.setBulletedListMode("")
        info!!.addPara("Once per deployment: Below 50%% hull, reduce damage taken by 80%% for 8 seconds.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        info.addSpacer(2f)
    }

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        if (stats!!.entity is ShipAPI) {
            val ship = stats.entity as ShipAPI
            var hasDrones = false
            var hasSmartDrones = false
            for(subSystem in MagicSubsystemsManager.getSubsystemsForShipCopy(ship) ?: emptyList()){
                if(subSystem is SimpleShieldDronesSubsystem) hasDrones = true
                if(subSystem is SmartShieldDronesSubsystem) hasSmartDrones = true
            }

            var droneLevel = 1
            if(ship.customData.containsKey(DawnBuiltin.DRONE_ADDED_KEY)) droneLevel += 1

            if(droneLevel == 1 && !hasDrones && !hasSmartDrones){
                MagicSubsystemsManager.addSubsystemToShip(ship, SimpleShieldDronesSubsystem(ship))
            }
            if(droneLevel == 2 && !hasSmartDrones){
                MagicSubsystemsManager.removeSubsystemFromShip(ship, SimpleShieldDronesSubsystem::class.java)
                MagicSubsystemsManager.addSubsystemToShip(ship, SmartShieldDronesSubsystem(ship))
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
