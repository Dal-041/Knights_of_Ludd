package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import lunalib.lunaExtensions.addLunaElement
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicIncompatibleHullmods
import org.selkie.kol.ReflectionUtils
import org.selkie.kol.Utils
import org.selkie.kol.impl.combat.SparkleAIV2

class DuskBuiltin : BaseHullMod() {
    companion object{
        val allMotes = mutableListOf<MissileAPI>()

        val HF_TAG = "HF_SPARKLES_ACTIVE"

        val maxMotes = mapOf(
                HullSize.FIGHTER to 2,
                HullSize.FRIGATE to 6,
                HullSize.DESTROYER to 9,
                HullSize.CRUISER to 12,
                HullSize.CAPITAL_SHIP to 20)
    }


    val PHASE_COOLDOWN_REDUCTION = 50f
    val DEGRADE_INCREASE_PERCENT = 50f

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        val engine = Global.getCombatEngine()
        allMotes.retainAll { engine.isMissileAlive(it) }

        if(ship?.hullSpec?.hullId == "zea_boss_ninaya"){
            if(ship.system.isActive) ship.addTag(HF_TAG)
            if (ship.hasTag(HF_TAG) && ship.system.cooldownRemaining /ship.system.cooldown < 0.8f){
                ship.tags.remove(HF_TAG)
            }
        }
        if(ship?.hullSpec?.hullId == "zea_boss_ninmah"){
            if(ship.system.isActive) ship.addTag(HF_TAG)
            if (ship.hasTag(HF_TAG) && ship.system.cooldownRemaining /ship.system.cooldown < 0.8f){
                ship.tags.remove(HF_TAG)
            }
        }
    }

    class DuskSparkleSpawner(val ship: ShipAPI) : AdvanceableListener{
        var activeMotes = mutableListOf<MissileAPI>()
        val launchInterval = IntervalUtil(0.25f, 0.75f)
        val launchSlots : WeightedRandomPicker<WeaponSlotAPI> = WeightedRandomPicker<WeaponSlotAPI>()
        val SPARKLE_WEAPON_ID = "zea_dusk_sparkler_wpn";

        init{
            for (slot in ship.hullSpec.allWeaponSlotsCopy) {
                if (slot.isSystemSlot) {
                    if (slot.slotSize == WeaponSize.SMALL) {
                        launchSlots.add(slot)
                    }
                }
            }
        }

        override fun advance(amount: Float) {
            val engine = Global.getCombatEngine()
            if(!ship.isAlive){
                for(mote in activeMotes){
                    mote.explode()
                    engine.removeEntity(mote)
                }
                return
            }

            launchInterval.advance(amount)
            if(launchInterval.intervalElapsed()){
                activeMotes.retainAll { engine.isMissileAlive(it) } // remove all dead missiles
                if(activeMotes.size < maxMotes[ship.hullSize]!! && !ship.fluxTracker.isOverloadedOrVenting && !ship.isPhased){
                    // use the system slots and manually recreate the firing process of a mote
                    val slot = launchSlots.pick()

                    val loc: Vector2f = slot.computePosition(ship)
                    var dir: Float = slot.computeMidArcAngle(ship)
                    val arc: Float = slot.arc
                    dir += arc * Math.random().toFloat() - arc / 2f

                    val mote = engine.spawnProjectile(ship, null, SPARKLE_WEAPON_ID, loc, dir, null) as MissileAPI
                    mote.setWeaponSpec(SPARKLE_WEAPON_ID)
                    mote.missileAI = SparkleAIV2(mote)
                    mote.activeLayers.remove(CombatEngineLayers.FF_INDICATORS_LAYER)
                    mote.empResistance = 10000
                    activeMotes.add(mote)
                    allMotes.add(mote)

                    engine.spawnMuzzleFlashOrSmoke(ship, slot, mote.weaponSpec, 0, dir)

                    Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 0.25f, loc, Vector2f())
                }
            }
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize, stats: MutableShipStatsAPI, id: String) {
        val shipVariant = stats.variant

        if(shipVariant.hullSpec.isPhase) {
            stats.crLossPerSecondPercent.modifyPercent(id, DEGRADE_INCREASE_PERCENT)
            if(shipVariant.hullSpec.baseHullId.contains("boss")) stats.phaseCloakCooldownBonus.modifyMult(id, 1f - PHASE_COOLDOWN_REDUCTION / 100f)
        }

        if (shipVariant.hullMods.contains(HullMods.ADAPTIVE_COILS)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(stats.variant, HullMods.ADAPTIVE_COILS,"zea_dusk_builtin")
        }

        //For save compatibility with old version
        if (stats.variant.hasHullMod("kol_ea_sparkle")) {
            stats.variant.removeMod("kol_ea_sparkle")
        }
        if (stats.variant.hasHullMod("zea_ex_phase_coils")) {
            stats.variant.removeMod("zea_ex_phase_coils")
        }
        if (stats.variant.hasHullMod("delicate")) {
            stats.variant.removeMod("delicate")
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (!ship.hasListenerOfClass(DuskSparkleSpawner::class.java)) ship.addListener(DuskSparkleSpawner(ship))
    }

    override fun getTooltipWidth(): Float {
        return 400f
    }
    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI, width: Float, isForModSpec: Boolean){
        val HEIGHT = 64f
        val headingPad = 20f
        val underHeadingPad = 10f
        val listPad = 3f

        val activeTextColor = Misc.getTextColor()
        val activePositiveColor = Misc.getPositiveHighlightColor()
        val activeNegativeColor = Misc.getNegativeHighlightColor()
        val activeHeaderBannerColor = Misc.getDarkPlayerColor()
        val activeHeaderTextColor = Utils.brighter(Misc.getButtonTextColor(), 0.8f)
        val activeHighlightColor = Misc.getHighlightColor()

        val inactiveTextColor = Misc.getGrayColor()
        val inactivePositiveColor = Misc.getGrayColor().darker()
        val inactiveNegativeColor = Misc.getGrayColor().darker()
        val inactiveHeaderBannerColor = Misc.getDarkPlayerColor().darker().darker()
        val inactiveHeaderTextColor = Misc.getGrayColor().darker()
        val inactiveHighlightColor = Misc.getGrayColor().darker()


        val background = tooltip!!.addLunaElement(0f, 0f)

        if (ship.hullSpec.isPhase){
            tooltip.addSectionHeading("${if(ship.hullSpec.baseHullId.contains("boss")) 5 else 4}th-Generation Phase Coils", activeHeaderTextColor, activeHeaderBannerColor , Alignment.MID, headingPad)
            val phaseCoils = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "dusk_phase_coils"), HEIGHT)
            phaseCoils.setBulletedListMode("•")
            phaseCoils.setBulletWidth(15f)
            val para1 = phaseCoils.addPara("No loss of top speed as hardflux level rises.", listPad, activeTextColor, activeHighlightColor)
            val para2 = phaseCoils.addPara("Maintains parity with standard domain phase coils at a 3x timeflow while phased.", listPad, activeTextColor, activeHighlightColor, "3x")
            val para3 = phaseCoils.addPara("Increases the rate of in-combat CR decay after peak performance time runs out by %s.", listPad, activeTextColor, activeNegativeColor, "50%")
            if(ship.hullSpec.baseHullId.contains("boss")){
                phaseCoils.addPara("Reduces phase cloak cooldown by %s.", listPad, activeTextColor, activeHighlightColor, "50%")
            }
            tooltip.addImageWithText(underHeadingPad)
        }


        tooltip.addSectionHeading("Duskfall", activeHeaderTextColor, activeHeaderBannerColor , Alignment.MID, headingPad)
        val duskfall = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "dusk_duskfall"), HEIGHT)
        duskfall.setBulletedListMode("•")
        duskfall.setBulletWidth(15f)
        val para4 = duskfall.addPara("Launches 2 motes per second around the ship, each mote deals 1500 EMP damage on impact.", listPad, activeTextColor, activeHighlightColor, "2", "1500")
        val para5 = duskfall.addPara("%s %s/%s/%s/%s %s", listPad, inactiveTextColor, activeTextColor,
            "A maximum of", "${maxMotes[HullSize.FRIGATE]}", "${maxMotes[HullSize.DESTROYER]}", "${maxMotes[HullSize.CRUISER]}", "${maxMotes[HullSize.CAPITAL_SHIP]}",
            "motes can be maintained around the ship at any given time.")
        ReflectionUtils.invoke("setColor", duskfall.prev, activeTextColor)

        para5.setHighlightColors(activeTextColor,
            if(ship.hullSize == HullSize.FRIGATE) activeHighlightColor else inactiveTextColor,
            if(ship.hullSize == HullSize.DESTROYER) activeHighlightColor else inactiveTextColor,
            if(ship.hullSize == HullSize.CRUISER) activeHighlightColor else inactiveTextColor,
            if(ship.hullSize == HullSize.CAPITAL_SHIP) activeHighlightColor else inactiveTextColor,
            activeTextColor)

        tooltip.addImageWithText(underHeadingPad)

        var sprite = Global.getSettings().getSprite("kol_ui", "zea_dusk_hmod")
        background.render {
            sprite.setSize(tooltip.widthSoFar + 20, tooltip.heightSoFar + 10)
            sprite.setAdditiveBlend()
            sprite.alphaMult = 0.4f
            sprite.render(tooltip.position.x, tooltip.position.y)
        }
    }

    override fun getDisplaySortOrder(): Int {
        return 0
    }
}