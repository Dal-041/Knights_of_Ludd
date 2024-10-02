package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import org.magiclib.util.MagicIncompatibleHullmods
import org.selkie.kol.helpers.KolStaticStrings

class KnightShields : BaseHullMod() {

    /*
    Desc:
    - Max duration of [yellowtext] # [/y] seconds.
    - Shield charge regenerates while shields are off.
    - Shield emitters forcibly shut down at [y] 0% [/y] charge until fully regenerated.

    [orange text] Incompatible with Shield Conversion - Omni
    [orange text] Shield arc cannot be extended by any means.
     */

    private val INNERLARGE = "graphics/fx/kol_shield_fx.png"
    private val OUTERLARGE = "graphics/fx/kol_shield_fx.png"

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
        }
        if (ship.variant.hasHullMod(HullMods.ACCELERATED_SHIELDS)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, HullMods.ACCELERATED_SHIELDS, KolStaticStrings.KNIGHT_REFIT)
        if (ship.variant.hasHullMod(HullMods.OMNI_SHIELD_CONVERSION)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, HullMods.OMNI_SHIELD_CONVERSION, KolStaticStrings.KNIGHT_REFIT)
        if (ship.variant.hasHullMod(HullMods.FRONT_SHIELD_CONVERSION)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, HullMods.FRONT_SHIELD_CONVERSION, KolStaticStrings.KNIGHT_REFIT)
        if (ship.variant.hasHullMod(HullMods.EXTENDED_SHIELDS)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, HullMods.EXTENDED_SHIELDS, KolStaticStrings.KNIGHT_REFIT)
    }
}