package org.selkie.kol.campaign.tech

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.Misc

/**
 * Restores a Knights hull to the low-tech standard by moving its variant onto the hull's restoration skin
 * (`data/hulls/skins/<hull>_lowtech.skin`): no primitive-shield timer, low-tech shield efficiency, no Knights
 * engine cores, low-tech shield and vent colors. The change is saved with the variant and survives
 * d-mods and restoration (a skin has its own d-hull, whose restored hull is the skin).
 *
 * Only shielded, mass-produced Knights hulls have a skin; [convert] leaves any other ship unchanged.
 */
object KolLowTech {
    const val SKIN_SUFFIX = "_lowtech"

    /** The restoration skin for this hull (or for its d-hull's parent), or null if it has none. */
    fun skinFor(hull: ShipHullSpecAPI): ShipHullSpecAPI? {
        val base = if (hull.isDefaultDHull) hull.dParentHull ?: return null else hull
        if (base.hullId.endsWith(SKIN_SUFFIX)) return null
        return hullSpecOrNull(base.hullId + SKIN_SUFFIX)
    }

    fun isConverted(variant: ShipVariantAPI): Boolean {
        val hull = variant.hullSpec
        val base = if (hull.isDefaultDHull) hull.dParentHull ?: hull else hull
        return base.hullId.endsWith(SKIN_SUFFIX)
    }

    /** Converts the member's ship (on a copy of its variant, as variants can be shared). True if it changed. */
    fun convert(member: FleetMemberAPI): Boolean {
        if (skinFor(member.variant.hullSpec) == null) return false
        val variant = member.variant.clone()
        if (!convert(variant)) return false
        member.setVariant(variant, false, true)
        return true
    }

    /** Converts the variant in place. The caller owns the variant (never a spec from settings). */
    fun convert(variant: ShipVariantAPI): Boolean {
        val old = variant.hullSpec
        val skin = skinFor(old) ?: return false
        val target = if (old.isDefaultDHull) hullSpecOrNull(Misc.getDHullId(skin)) ?: return false else skin

        variant.source = VariantSource.REFIT
        variant.setHullSpecAPI(target)
        // setHullSpecAPI adds the new hull's built-ins but keeps the old hull's: drop the ones the skin removed
        for (mod in old.builtInMods) if (!target.isBuiltInMod(mod)) variant.removeMod(mod)
        for (slot in old.builtInWeapons.keys) if (!target.builtInWeapons.containsKey(slot)) variant.clearSlot(slot)
        return true
    }

    private fun hullSpecOrNull(id: String): ShipHullSpecAPI? =
        try { Global.getSettings().getHullSpec(id) } catch (e: Exception) { null }
}
