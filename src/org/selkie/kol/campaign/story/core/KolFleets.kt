package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f

/**
 * Story fleets: creating, placing and retiring them, with the pitfalls already learned built in.
 * - Escorts that should fight use `ORBIT_AGGRESSIVE`: passive orbits and `FOLLOW` never engage anything but the player.
 * - Ignore flags (`FLEET_IGNORES_OTHER_FLEETS`, `..._IGNORED_BY_...`) keep a fleet out of fights; strip them near a target.
 * - A fleet the player may not be near can be auto-despawned: set `isNoAutoDespawn` on fleets that must stay.
 * - A system's edge comes from vanilla's measure (`edgeSpot`), never from entity radii (render-only entities are huge).
 */
object KolFleets {
    /**
     * A combat fleet from the factory; [source] and [locInHyper] are optional. Null if the factory gave nothing, or an
     * empty fleet unless [allowEmpty] (for a fleet the caller fills with its own ships).
     */
    fun combat(factionId: String, type: String, fp: Float, source: MarketAPI? = null, locInHyper: Vector2f? = null,
               allowEmpty: Boolean = false, configure: FleetParamsV3.() -> Unit = {}): CampaignFleetAPI? {
        val params = FleetParamsV3(source, locInHyper, factionId, null, type, fp, 0f, 0f, 0f, 0f, 0f, 0f)
        params.configure()
        return FleetFactoryV3.createFleet(params)?.takeIf { allowEmpty || !it.isEmpty }
    }

    /** Adds [fleet] to [location] at ([x], [y]). */
    fun place(fleet: CampaignFleetAPI, location: LocationAPI, x: Float, y: Float) {
        location.addEntity(fleet)
        fleet.setLocation(x, y)
    }

    /** Adds [fleet] to [at]'s location, on top of it. */
    fun place(fleet: CampaignFleetAPI, at: SectorEntityToken) = place(fleet, at.containingLocation, at.location.x, at.location.y)

    /** Hostile to the Knights and aggressive (raiders and pirates the story sets against the Order). */
    fun hostileToKnights(fleet: CampaignFleetAPI) {
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE + "_" + KolIds.FACTION, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
    }

    /**
     * A point [margin] past the system's edge, as vanilla measures it (planets and their orbits, rings, fields; not
     * nebulae), or past the farthest jump point if that's farther.
     */
    fun edgeSpot(system: StarSystemAPI, margin: Float): Vector2f {
        val center = system.center.location
        val edge = maxOf(BaseThemeGenerator.getOuterRadius(system),
            system.jumpPoints.maxOfOrNull { Misc.getDistance(it.location, center) } ?: 0f)
        return Misc.getPointAtRadius(center, edge + margin)
    }

    /**
     * Sends a story fleet home to despawn (or despawns it at once without a [home]): clears its orders and the flags
     * that held it (`noJump`, the ignore flags, and any [alsoUnset]).
     */
    fun sendHome(fleet: CampaignFleetAPI, home: SectorEntityToken?, text: String, vararg alsoUnset: String) {
        val mem = fleet.memoryWithoutUpdate
        for (key in listOf(MemFlags.MEMORY_KEY_NO_JUMP, MemFlags.FLEET_IGNORES_OTHER_FLEETS, MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS) + alsoUnset)
            mem.unset(key)
        fleet.clearAssignments()
        if (home != null) fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f, text) else fleet.despawn()
    }
}
