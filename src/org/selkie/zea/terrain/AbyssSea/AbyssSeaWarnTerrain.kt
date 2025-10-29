package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.terrain.AbyssSea.AbyssSeaWaveManager.Companion.DEFAULT_MIN_DAYS

class AbyssSeaWarnTerrain: BaseRingTerrain() {

    lateinit var manager: AbyssSeaWaveManager

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)

        val sysName = relatedEntity?.containingLocation?.name
        if (sysName != null) {
            name = sysName // its only one system so its fine
        }
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    override fun isTooltipExpandable(): Boolean {
        return false
    }

    override fun getTerrainName(): String? {
        return nameForTooltip
    }

    override fun getNameForTooltip(): String? {
        val baseName = name
        val daysLeft = getDaysLeft()
        val sOrNone = if (daysLeft == 1f) "" else "s"
        return "$baseName (${daysLeft.toInt()} day$sOrNone until next ejection)"
    }

    override fun advance(amount: Float) {
        super.advance(amount)
    }

    override fun containsEntity(other: SectorEntityToken?): Boolean {
        return super.containsEntity(other)
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(tooltip, expanded)

        if (tooltip == null) return

        val baseName = name
        tooltip.addTitle(baseName)
        tooltip.addPara(Global.getSettings().getDescription(spec.id, Description.Type.TERRAIN).text1, 5f)

        tooltip.addPara(
            "Every %s or so days, the star will unleash a fast-moving %s, sweeping the star system with its wrath. This ejection will " +
            "travel to the edge of the star system, %s fleets caught in its wake.",
            5f,
            Misc.getHighlightColor(),
            "${manager.pulseInterval.minInterval.toInt()}", "coronal ejection", "severely damaging"
        ).setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor()
        )
        tooltip.addPara(
            "A preliminary scan of the system suggests two methods of %s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "avoidance"
        )
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
        tooltip.addPara(
            "Sheltering behind %s",
            0f,
            Misc.getHighlightColor(),
            "planets"
        )
        tooltip.addPara(
            "Sheltering within the thick %s",
            0f,
            Misc.getHighlightColor(),
            "blue-and-green nebulae"
        )
        tooltip.setBulletedListMode(null)

        tooltip.addPara(
            "Your scientists estimate %s days until the next ejection. Prepare accordingly.",
            5f,
            Misc.getHighlightColor(),
            "${getDaysLeft().toInt()}"
        )
    }

    fun getDaysLeft(): Float {
        val interval = manager.pulseInterval
        val daysLeft = interval.intervalDuration - interval.elapsed
        return daysLeft
    }

    override fun hasAIFlag(flag: Any?): Boolean {
        return false
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

    override fun getEffectCategory(): String? {
        return null
    }

}