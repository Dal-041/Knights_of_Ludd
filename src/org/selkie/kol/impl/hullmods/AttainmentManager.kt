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
        // disabling this until we can track down the bug where the ships are unrecoverable in the player fleet.
        /*
        if (Global.getSector() != null && stats.fleetMember != null) {
            val faction = stats.fleetMember.fleetData?.fleet?.faction?.id
            if (!faction.equals(Global.getSector().playerFaction?.id)) {
                if (Math.random() > ZeaUtils.attainmentFactor) stats.variant.addTag(Tags.VARIANT_UNBOARDABLE)
                else stats.variant.removeTag(Tags.VARIANT_UNBOARDABLE)
            } else {
                stats.variant.removeTag(Tags.VARIANT_UNBOARDABLE)
            }
        }
         */
    }
}