package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import org.magiclib.util.MagicIncompatibleHullmods

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
        if (ship.variant.hasHullMod("advancedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, "advancedshieldemitter", "kol_refit")
        if (ship.variant.hasHullMod("adaptiveshields")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, "adaptiveshields", "kol_refit")
        if (ship.variant.hasHullMod("frontemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, "frontemitter", "kol_refit")
        if (ship.variant.hasHullMod("extendedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, "extendedshieldemitter", "kol_refit")
    }
}