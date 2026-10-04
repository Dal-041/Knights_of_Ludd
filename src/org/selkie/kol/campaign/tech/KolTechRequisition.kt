package org.selkie.kol.campaign.tech

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomProductionPickerDelegateImpl
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FactionProductionAPI
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RuleBasedDialog
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.command.CustomProductionPanel
import com.fs.starfarer.loading.specs.FactionProduction
import org.selkie.kol.ReflectionUtilsV2
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.rulecmd.KolTechCMD
import kotlin.math.roundToInt

/**
 * Helensis' requisitions: ordained Knights equipment for scrip, through vanilla's custom production screen
 * (pattern: Privateering's requisition bonds). Catalog = the Knights' known ships, weapons and fighters minus the
 * unique hulls, gated by stage; price = credit value / the stage's store rate. Delivered at once to Cygnus' storage.
 */
object KolTechRequisition {
    private val log = Global.getLogger(KolTechRequisition::class.java)
    private var warnedSwap = false

    /** Unlocks capital ships; Knights hulls delivered from this stage come restored to the low-tech standard. */
    const val CAPITAL_STAGE = 4

    fun open(dialog: InteractionDialogAPI, stage: Int) {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
        val picker = Picker(knights, stage, dialog)
        dialog.showCustomProductionPicker(picker)
        swapProduction(dialog, knights, picker)
    }

    /**
     * Vanilla totals ignore getCostOverride; swap the panel's FactionProduction for one that uses it (Privateering's
     * approach). FactionProduction is DoNotObfuscate; the panel is found as the dialog's last child. If anything
     * fails, the picker still works with vanilla cost x getCostMult() (1 / rate), which is close to the override.
     */
    private fun swapProduction(dialog: InteractionDialogAPI, faction: FactionAPI, picker: Picker) {
        try {
            @Suppress("UNCHECKED_CAST")
            val children = ReflectionUtilsV2.invoke("getChildrenNonCopy", dialog as UIPanelAPI) as List<Any>
            val panel = ReflectionUtilsV2.get(null, children.last(), CustomProductionPanel::class.java)!!
            val production = ScripProduction(faction, picker)
            ReflectionUtilsV2.set(null, panel, production, FactionProduction::class.java)
        } catch (t: Throwable) {
            if (!warnedSwap) log.warn("KOL: requisitions could not swap the production panel's costs; using the cost multiplier", t)
            warnedSwap = true
        }
    }

    /** Price of one unit in scrip at the given stage. */
    fun price(creditValue: Float, stage: Int): Int =
        (creditValue / KolTechCMD.storeRate(stage)).roundToInt().coerceAtLeast(1)

    private fun uniqueHullIds(): Set<String> = KolTechSettings.uniques.mapNotNull {
        if (Global.getSettings().doesVariantExist(it.variant)) Global.getSettings().getVariant(it.variant).hullSpec.hullId else null
    }.toSet()

    class Picker(private val faction: FactionAPI, private val stage: Int, private val dialog: InteractionDialogAPI) :
        BaseCustomProductionPickerDelegateImpl() {

        override fun getMaximumValue(): Float = KolTechData.get().balance.toFloat()
        override fun isUseCreditSign(): Boolean = false
        override fun withQuantityLimits(): Boolean = false
        override fun getMaximumOrderValueLabelOverride(): String = "Scrip available"
        override fun getCurrentOrderValueLabelOverride(): String = "Scrip required"
        override fun getItemGoesOverMaxValueStringOverride(): String = "Not enough scrip"
        override fun getCustomOrderLabelOverride(): String = "Requisition"
        override fun getCostMult(): Float = 1f / KolTechCMD.storeRate(stage)

        override fun getCostOverride(item: Any?): Int = when (item) {
            is WeaponSpecAPI -> price(item.baseValue, stage)
            is ShipHullSpecAPI -> price(item.baseValue, stage)
            is FighterWingSpecAPI -> price(item.baseValue, stage)
            else -> super.getCostOverride(item)
        }

        override fun getAvailableWeapons(): MutableSet<String> = faction.knownWeapons.filter {
            val size = Global.getSettings().getWeaponSpec(it).size
            if (size == WeaponSize.LARGE) stage >= 3 else stage >= 1
        }.toMutableSet()

        override fun getAvailableFighters(): MutableSet<String> =
            if (stage >= 1) faction.knownFighters.toMutableSet() else mutableSetOf()

        override fun getAvailableShipHulls(): MutableSet<String> {
            val uniques = uniqueHullIds()
            return faction.knownShips.filter { id ->
                if (id in uniques) return@filter false
                when (Global.getSettings().getHullSpec(id).hullSize) {
                    HullSize.FRIGATE, HullSize.DESTROYER -> stage >= 2
                    HullSize.CRUISER -> stage >= 3
                    HullSize.CAPITAL_SHIP -> stage >= CAPITAL_STAGE
                    else -> false
                }
            }.toMutableSet()
        }

        override fun notifyProductionSelected(production: FactionProductionAPI?) {
            production ?: return
            val items = production.current
            val total = items.sumOf { unitPrice(it) * it.quantity }
            if (total <= 0 || !KolTechData.get().spend(total)) return
            deliver(items)
            val text = dialog.textPanel
            text.setFontSmallInsignia()
            text.addPara("Spent %s scrip", Misc.getNegativeHighlightColor(), Misc.getHighlightColor(),
                Misc.getWithDGS(total.toFloat()))
            text.setFontInsignia()

            // Helensis comments on what was requisitioned, then the menu refreshes its balance
            val memoryMap = (dialog.plugin as? RuleBasedDialog)?.memoryMap ?: return
            val types = items.map { it.type }.toSet()
            val type = when {
                types.size > 1 -> "mixed"
                ProductionItemType.SHIP in types -> "ships"
                ProductionItemType.FIGHTER in types -> "fighters"
                else -> "weapons"
            }
            KolTechCMD.afterPurchase(dialog, memoryMap, type)
            FireBest.fire(null, dialog, memoryMap, KolStaticStrings.KolTech.TRIGGER_REQ_MENU)
        }

        fun unitPrice(item: FactionProductionAPI.ItemInProductionAPI): Int = when (item.type) {
            ProductionItemType.WEAPON -> getCostOverride(item.weaponSpec)
            ProductionItemType.FIGHTER -> getCostOverride(item.wingSpec)
            ProductionItemType.SHIP -> getCostOverride(item.shipSpec)
            else -> item.baseCost
        }

        /** Straight into Cygnus' storage (the player is docked there), else the fleet's cargo. */
        private fun deliver(items: List<FactionProductionAPI.ItemInProductionAPI>) {
            val market = Global.getSector().economy.getMarket(KolStaticStrings.KOL_CYGNUS)
            // the Order opens its storage to you for the delivery (storage at a market you don't own may be locked)
            (market?.getSubmarket(Submarkets.SUBMARKET_STORAGE)?.plugin as? StoragePlugin)?.setPlayerPaidToUnlock(true)
            val cargo = market?.let { Misc.getStorageCargo(it) } ?: Global.getSector().playerFleet.cargo
            val text = dialog.textPanel
            for (item in items) {
                when (item.type) {
                    ProductionItemType.WEAPON -> {
                        cargo.addWeapons(item.specId, item.quantity)
                        AddRemoveCommodity.addWeaponGainText(item.specId, item.quantity, text)
                    }
                    ProductionItemType.FIGHTER -> {
                        cargo.addFighters(item.specId, item.quantity)
                        AddRemoveCommodity.addFighterGainText(item.specId, item.quantity, text)
                    }
                    ProductionItemType.SHIP -> repeat(item.quantity) {
                        cargo.initMothballedShips(KolStaticStrings.kolFactionID)
                        val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, item.specId + "_Hull")
                        if (stage >= CAPITAL_STAGE) KolLowTech.convert(member)
                        cargo.mothballedShips.addFleetMember(member)
                        AddRemoveCommodity.addFleetMemberGainText(member, text)
                    }
                    else -> {}
                }
            }
            text.addPara("Delivered to storage at ${market?.name ?: "your fleet"}.")
        }
    }

    /** FactionProduction whose costs come from the picker's per-item override (scrip), not credit value. */
    class ScripProduction(faction: FactionAPI, private val picker: Picker) : FactionProduction(faction) {
        init {
            delegate = picker
        }

        override fun getTotalCurrentCost(): Int = current.sumOf { picker.unitPrice(it) * it.quantity }

        override fun getUnitCost(type: ProductionItemType, specId: String?): Int {
            val spec: Any? = when (type) {
                ProductionItemType.WEAPON -> Global.getSettings().getWeaponSpec(specId)
                ProductionItemType.FIGHTER -> Global.getSettings().getFighterWingSpec(specId)
                ProductionItemType.SHIP -> Global.getSettings().getHullSpec(specId)
                else -> null
            }
            val override = picker.getCostOverride(spec)
            return if (override >= 0) override else super.getUnitCost(type, specId)
        }
    }
}
