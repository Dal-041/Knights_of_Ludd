package org.selkie.kol.impl.skills.cores

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.FighterLaunchBayAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.subsystems.MagicSubsystemsManager
import org.selkie.kol.impl.campaign.cores.BossAICoreOfficerPlugin
import org.selkie.kol.impl.combat.subsystems.PDDroneSubsystem
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore

class ElysiaBossCoreSkill : BaseCoreOfficerSkill() {
    override val skillID = BossCore.ELYSIAN_CORE.exclusiveSkillID

    companion object{
        val DAMAGE_INCREASE_PERCENT = 15f
        fun getFighters(carrier: ShipAPI): List<ShipAPI> {
            val result: MutableList<ShipAPI> = ArrayList()

//		this didn't catch fighters returning for refit
//		for (FighterLaunchBayAPI bay : carrier.getLaunchBaysCopy()) {
//			if (bay.getWing() == null) continue;
//			result.addAll(bay.getWing().getWingMembers());
//		}
            for (ship in Global.getCombatEngine().ships) {
                if (!ship.isFighter) continue
                if (ship.wing == null) continue
                if (ship.wing.sourceShip === carrier) {
                    result.add(ship)
                }
            }
            return result
        }
    }

    class ElysiaBossCoreListener(val ship: ShipAPI) : AdvanceableListener{
        val id = "ElysiaBossCore"
        var doneInitDeploy: MutableSet<FighterLaunchBayAPI> = mutableSetOf()
        override fun advance(amount: Float) {

            for (bay in ship.launchBaysCopy) {
                if (bay.wing == null) continue
                val spec = bay.wing.spec
                val maxTotal = spec.numFighters + 1
                val actualAdd = maxTotal - bay.wing.wingMembers.size

                if (actualAdd > 0) {
                    if(bay !in doneInitDeploy){
                        doneInitDeploy.add(bay)
                        bay.fastReplacements = actualAdd
                    }
                    bay.extraDeployments = actualAdd
                    bay.extraDeploymentLimit = maxTotal
                    bay.extraDuration = 100000f
                }
            }

            for (fighter in getFighters(ship)) {
                if (fighter.isHulk) continue
                val fStats = fighter.mutableStats
                fStats.ballisticWeaponDamageMult.modifyMult(id, 1f + 0.01f * DAMAGE_INCREASE_PERCENT)
                fStats.energyWeaponDamageMult.modifyMult(id, 1f + 0.01f * DAMAGE_INCREASE_PERCENT)
                fStats.missileWeaponDamageMult.modifyMult(id, 1f + 0.01f * DAMAGE_INCREASE_PERCENT)
            }
        }
    }

    override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
        return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
    }

    override fun createCustomDescription(
        stats: MutableCharacterStatsAPI?,
        skill: SkillSpecAPI?,
        info: TooltipMakerAPI?,
        width: Float
    ) {
        info!!.addSpacer(2f)
        info!!.addPara(
            "Provides the ship with the \"Shachi\" PD drone subsystem if it does not have it.",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        info!!.addPara(
            "All Wings get 1 extra craft and +15%% damage.",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        info.addSpacer(2f)
    }

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        var variant = stats!!.variant

        if (stats!!.entity is ShipAPI) {
            val ship = stats.entity as ShipAPI
            var hasDrones = false
            for(subSystem in MagicSubsystemsManager.getSubsystemsForShipCopy(ship) ?: emptyList()){
                if(subSystem is PDDroneSubsystem) hasDrones = true
            }

            if (!hasDrones) {
                MagicSubsystemsManager.addSubsystemToShip(ship, PDDroneSubsystem(ship))
            }
            if(!ship.hasListenerOfClass(ElysiaBossCoreListener::class.java)) ship.addListener(ElysiaBossCoreListener(ship))
        }


        stats.fleetMember?.let{ fleetMember ->
            if(fleetMember.hullSpec.manufacturer == "Elysia"){
                fleetMember.captain?.let{
                    val captainMemory = it.memoryWithoutUpdate
                    captainMemory.set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, BossAICoreOfficerPlugin.AUTOMATED_POINTS_MULT_SAME_FACTION)
                }
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
        if (stats!!.entity is ShipAPI) {
            val ship = stats.entity as ShipAPI
            for (fighter in getFighters(ship)) {
                if (fighter.isHulk) continue
                val fStats = fighter.mutableStats
                fStats.ballisticWeaponDamageMult.unmodify(id)
                fStats.energyWeaponDamageMult.unmodify(id)
                fStats.missileWeaponDamageMult.unmodify(id)
            }
            if(ship.hasListenerOfClass(ElysiaBossCoreListener::class.java)) ship.removeListenerOfClass(ElysiaBossCoreListener::class.java)
        }
    }
}
