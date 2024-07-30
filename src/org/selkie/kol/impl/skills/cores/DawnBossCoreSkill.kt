package org.selkie.kol.impl.skills.cores

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.subsystems.MagicSubsystemsManager
import org.selkie.kol.impl.combat.subsystems.SimpleShieldDronesSubsystem
import org.selkie.kol.impl.combat.subsystems.SmartShieldDronesSubsystem

class DawnBossCoreSkill : BaseCoreOfficerSkill() {

    var modID = "zea_dawn_boss_skill"

    override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
        return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
    }

    override fun createCustomDescription(stats: MutableCharacterStatsAPI?,  skill: SkillSpecAPI?, info: TooltipMakerAPI?,  width: Float) {
        info!!.addSpacer(2f)
        info!!.addPara("Provides the ship with the \"Chiewn\" shield drone subsystem if it does not have it.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        info!!.setBulletedListMode("    - ")
        info!!.addPara("Upgrades existing drone subsystem with 2 more drones that all actively block shots.", 0f, Misc.getTextColor(), Misc.getHighlightColor())
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

            if(!hasSmartDrones){
                if(hasDrones){
                    MagicSubsystemsManager.removeSubsystemFromShip(ship, SimpleShieldDronesSubsystem::class.java)
                    MagicSubsystemsManager.addSubsystemToShip(ship, SmartShieldDronesSubsystem(ship))
                }else{
                    MagicSubsystemsManager.addSubsystemToShip(ship, SimpleShieldDronesSubsystem(ship))
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
