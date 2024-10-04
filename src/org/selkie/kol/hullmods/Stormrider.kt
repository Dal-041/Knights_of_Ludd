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
    val rand = Random()

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        val fleet = member?.fleetData?.fleet ?: return

        for(script in fleet.scripts){
            if(script is HyperStormBoost){
                if (script.toString() !in boostScriptNeedsAdjusting && !script.isDone){
                    boostScriptNeedsAdjusting.add(script.toString())
                    ReflectionUtils.set("angle", script, (VectorUtils.getFacing(fleet.velocity) + rand.nextGaussian()*30f).toFloat())
                }
                if (script.isDone){
                    boostScriptNeedsAdjusting.remove(script.toString())
                }
            }
        }

        val resistance = if(fleet.isInHyperspace) 0.9f else 0.45f
        for(otherMember in fleet.fleetData.membersListCopy){
            otherMember.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult("Stormrider_fleet", 1f-resistance);
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return "90%"
    }
}