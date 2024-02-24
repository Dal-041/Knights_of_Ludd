package org.selkie.kol.impl.skills.cores

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.subsystems.MagicSubsystemsManager
import org.selkie.kol.impl.combat.activators.PDDroneActivator

class ElysiaBossCoreSkill : BaseCoreOfficerSkill() {

    var modID = "zea_elysia_boss_skill"

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
            "Provides the ship with the \"Shachi\" PD drone system if it does not have it.",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        //info!!.addPara("Adds an additional fighter bay to the ship.", 0f, Misc.getHighlightColor(), Misc.getHighlightColor())
        info!!.addPara(
            "-20%% ordnance point cost for all fighters.",
            0f,
            Misc.getHighlightColor(),
            Misc.getHighlightColor()
        )
        info.addSpacer(2f)
    }

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        var variant = stats!!.variant

        stats.dynamic.getStat(Stats.ALL_FIGHTER_COST_MOD).modifyMult(modID, 0.8f)

        if (stats!!.entity is ShipAPI) {
            var ship = stats.entity as ShipAPI

            if (!variant.hasHullMod("zea_edf_pd_drones")) {
                MagicSubsystemsManager.addSubsystemToShip(ship, PDDroneActivator(ship))
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
        if (stats!!.entity is ShipAPI) {
            var ship = stats.entity as ShipAPI
        }

    }
}
