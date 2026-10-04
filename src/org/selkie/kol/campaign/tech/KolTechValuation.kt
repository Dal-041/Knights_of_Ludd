package org.selkie.kol.campaign.tech

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.DModManager
import kotlin.math.roundToInt

/**
 * What Helensis accepts and what it earns, in scrip. Flat values that ignore credit value:
 * weapons base x size step x tier step; fighter wings as large weapons at their tier; AI cores by id;
 * hulls hullBase x 2^(size step) / max(1, d-mods); story items at fixed values, accepted whatever their maker.
 * Accepted makers: settings `acceptedManufacturers`. Shrouded (settings `forbiddenManufacturers`) is never paid
 * for: it voids a handover (see KolTechCMD).
 */
object KolTechValuation {

    // --- items ------------------------------------------------------------------------------------------------

    fun manufacturer(stack: CargoStackAPI): String? = when {
        stack.isWeaponStack -> stack.weaponSpecIfWeapon?.manufacturer
        stack.isFighterWingStack -> stack.fighterWingSpecIfWing?.variant?.hullSpec?.manufacturer
        else -> null
    }

    private fun itemId(stack: CargoStackAPI): String? = when {
        stack.isWeaponStack -> stack.weaponSpecIfWeapon?.weaponId
        stack.isFighterWingStack -> stack.fighterWingSpecIfWing?.id
        stack.isCommodityStack -> stack.commodityId
        stack.isSpecialStack -> stack.specialDataIfSpecial?.id
        else -> null
    }

    fun isForbidden(stack: CargoStackAPI): Boolean =
        manufacturer(stack)?.let { it in KolTechSettings.forbiddenManufacturers } == true

    /** Scrip for one unit, or 0 if Helensis won't take it. */
    fun unitValue(stack: CargoStackAPI): Int {
        val s = KolTechSettings
        itemId(stack)?.let { id -> s.fixedValues[id]?.let { return it } }
        if (stack.isCommodityStack) return s.coreValues[stack.commodityId] ?: 0
        val maker = manufacturer(stack) ?: return 0
        if (maker !in s.acceptedManufacturers) return 0
        return when {
            stack.isWeaponStack -> {
                val spec = stack.weaponSpecIfWeapon ?: return 0
                weaponValue(spec.size, spec.tier)
            }
            stack.isFighterWingStack -> weaponValue(WeaponSize.LARGE, stack.fighterWingSpecIfWing?.tier ?: 0)
            else -> 0
        }
    }

    private fun weaponValue(size: WeaponSize, tier: Int): Int {
        val s = KolTechSettings
        val sizeStep = s.sizeSteps[when (size) { WeaponSize.SMALL -> 0; WeaponSize.MEDIUM -> 1; else -> 2 }]
        val tierStep = s.tierSteps[tier.coerceIn(0, s.tierSteps.size - 1)]
        return (s.baseValue * sizeStep * tierStep).roundToInt()
    }

    fun isListed(stack: CargoStackAPI) = isForbidden(stack) || unitValue(stack) > 0

    /** Total for a selection; Shrouded stacks count 0 (they void the handover instead). */
    fun total(cargo: CargoAPI): Int = cargo.stacksCopy.sumOf {
        if (isForbidden(it)) 0 else unitValue(it) * it.size.roundToInt()
    }

    fun containsForbidden(cargo: CargoAPI) = cargo.stacksCopy.any { isForbidden(it) }

    // --- ships ------------------------------------------------------------------------------------------------

    fun isForbidden(member: FleetMemberAPI) = member.hullSpec.manufacturer in KolTechSettings.forbiddenManufacturers

    /** Scrip for a ship, or 0 if Helensis won't take it. */
    fun shipValue(member: FleetMemberAPI): Int {
        val s = KolTechSettings
        val hull = member.hullSpec
        s.fixedValues[hull.baseHullId]?.let { return it }
        s.fixedValues[hull.hullId]?.let { return it }
        if (hull.manufacturer !in s.acceptedManufacturers) return 0
        val step = when (hull.hullSize) {
            HullSize.FRIGATE -> 0; HullSize.DESTROYER -> 1; HullSize.CRUISER -> 2; HullSize.CAPITAL_SHIP -> 3
            else -> return 0
        }
        val dmods = DModManager.getNumDMods(member.variant).coerceAtLeast(1)
        return (s.hullBaseValue * (1 shl step) / dmods).roundToInt()
    }
}
