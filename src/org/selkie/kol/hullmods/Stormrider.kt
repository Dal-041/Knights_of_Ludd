package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.HyperStormBoost
import org.lazywizard.lazylib.VectorUtils
import org.selkie.kol.ReflectionUtils;
import java.util.Random

class Stormrider : BaseHullMod()  {

    val boostScriptNeedsAdjusting = mutableSetOf<String>()
    var fleetHasScripts = false
    val rand = Random()

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        val fleet = member?.fleetData?.fleet
        if (fleet == null) return

        for(script in fleet.scripts){
            if(script is HyperStormBoost){
                fleetHasScripts = true
                if (script.toString() !in boostScriptNeedsAdjusting){
                    boostScriptNeedsAdjusting.add(script.toString())
                    ReflectionUtils.set("angle", script, (VectorUtils.getFacing(fleet.velocity) + rand.nextGaussian()*30f).toFloat())
                }
            }
        }

        for(member in fleet.fleetData.membersListCopy){
            member.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult("Stormrider_fleet", 0.1f);
        }
    }

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        boostScriptNeedsAdjusting.clear() // lazy way to prevent a memory leak
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return "90%"
    }
}