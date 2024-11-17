package org.selkie.kol.abilities

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.OrbitalStationAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.abilities.GraviticScanAbility
import com.fs.starfarer.api.impl.campaign.abilities.GraviticScanData
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.ReflectionUtils

class OracleScanData(val ability: GraviticScanAbility) : GraviticScanData(ability) {

    val artificialConditions = setOf<String>(Conditions.RUINS_EXTENSIVE, Conditions.RUINS_VAST, Conditions.RUINS_SCATTERED, Conditions.RUINS_WIDESPREAD)

    override fun doSpecialPings() {} // do nothing, special pings are random, all pings have been moved to high source

    override fun maintainHighSourcePings() {
        val fleet: CampaignFleetAPI = ability.getFleet()
        val loc = fleet.location
        val location = fleet.containingLocation

        maintainSlipstreamPings()

        val abyss = Misc.isInAbyss(fleet)
        if (fleet.isInHyperspace && !abyss) {
            return
        }

        val all: MutableList<SectorEntityToken> = ArrayList<SectorEntityToken>(location.planets)

        for (entity in location.getEntities(CustomCampaignEntityAPI::class.java)) {
            if (entity is SectorEntityToken) {
                if (abyss && !Misc.isInAbyss(entity)) continue
                all.add(entity)
            }
        }

        for (curr in location.fleets) {
            if (fleet === curr) continue
            if (abyss && !Misc.isInAbyss(fleet)) continue
            all.add(curr)
        }

        for (entity in location.getEntities(OrbitalStationAPI::class.java)) {
            if (entity is SectorEntityToken) {
                if (abyss && !Misc.isInAbyss(entity)) continue
                all.add(entity)
            }
        }

        val pings : ArrayList<GSPing> = ReflectionUtils.getFromSuper("pings", this) as ArrayList<GSPing>

        for (entity in all) {
            if (entity is PlanetAPI) {
                val isArtificial = entity.market?.conditions?.any{it.id in artificialConditions} ?: false
                if(!isArtificial) continue
            }

            if (entity.radius <= 0) continue

            if (Tags.NEUTRINO !in entity.tags && Tags.NEUTRINO_LOW !in entity.tags ) continue


            val dist = Misc.getDistance(loc, entity.location)

            var arc = Misc.computeAngleSpan(entity.radius, dist)
            arc *= 2f
            if (arc > 150f) arc = 150f
            if (arc < 20) arc = 20f
            val angle = Misc.getAngleInDegrees(loc, entity.location)

            var g: Float = getGravity(entity)

            g *= .2f
            if (entity is OrbitalStationAPI) {
                g *= 2f;
            }
            g *= getRangeGMult(dist)


            val ping = GSPing(angle, arc, g, 0.05f,  0.05f)

            pings.add(ping)
        }
    }
}