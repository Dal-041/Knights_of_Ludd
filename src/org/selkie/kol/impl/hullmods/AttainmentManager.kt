package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Tags
import org.selkie.kol.impl.helpers.ZeaUtils

class AttainmentManager : BaseHullMod() {
    //private val checkKey = "EDF_UB"
    //private val interval = IntervalUtil(60f, 60f)

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI, id: String?) {
        if (Global.getSector() != null && stats.fleetMember != null) {
            val faction = stats.fleetMember.fleetData?.fleet?.faction?.id
            if (!faction.equals(Global.getSector().playerFaction?.id)) {
                if (Math.random() > ZeaUtils.attainmentFactor) stats.variant.addTag(Tags.VARIANT_UNBOARDABLE)
                else stats.variant.removeTag(Tags.VARIANT_UNBOARDABLE)
            } else {
                stats.variant.removeTag(Tags.VARIANT_UNBOARDABLE)
            }
        }
    }

    /* Non-functional dynamic unboardable allies, probably needs a different assignment
    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        interval.advance(amount)
        if (interval.intervalElapsed()) {
            if (Global.getSector() != null && ship != null) {
                if (ship.fleetMember?.fleetData?.fleet?.faction?.isPlayerFaction == true || ship.customData.containsKey(checkKey)) return;

                if (Math.random() > PrepareAbyss.attainmentFactor) ship.variant.addTag(Tags.VARIANT_UNBOARDABLE)
                else ship.variant.removeTag(Tags.VARIANT_UNBOARDABLE)
                ship.customData.set(checkKey, true)

                for (ally in ship.getAlliesOnMap()) {
                    if (ally.customData.containsKey(checkKey)) continue
                    else {
                        if (Math.random() > PrepareAbyss.attainmentFactor) ally.variant?.addTag(Tags.VARIANT_UNBOARDABLE)
                        else ally.variant?.removeTag(Tags.VARIANT_UNBOARDABLE)
                        ally.customData.set(checkKey, true)
                    }
                }
            }
        }
    }
     */
}