package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin.ExplosionFleetDamage
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.RingRenderer
import com.fs.starfarer.api.impl.campaign.terrain.ShoveFleetScript
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import jdk.internal.vm.vector.VectorSupport.test
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.OpenGlUtils
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.terrain.AbyssSea.AbyssSeaWaveManager.Companion.LUNA_COLOR
import org.selkie.zea.terrain.AbyssShoalNebulaTerrain
import org.selkie.zea.terrain.KOL_rangeBlockerWithEnds
import java.awt.Color
import java.util.EnumSet
import kotlin.collections.any
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceAtMost
import kotlin.ranges.until

class AbyssSeaWave(): BaseRingTerrain() {

    companion object {
        fun createPulse(source: SectorEntityToken, params: AbyssSeaWaveParams): AbyssSeaWave {
            val terrain = source.containingLocation.addTerrain(
                ZeaStaticStrings.ZeaTerrain.ZEA_SEA_WAVE,
                params
            ) as CampaignTerrainAPI
            terrain.setLocation(source.location.x, source.location.y)
            return terrain.plugin as AbyssSeaWave
        }

        const val BASE_STRIKE_DAMAGE = 25

        const val BASE_SHOCKWAVE_SPEED = 5000f

        const val BASE_BURN_MULT = 0.8f
        const val BASE_BURN_MULT_DURATION = 0.2f

        const val SHIELDS_FROM_TAG = "zea_blockAbyssSeaWave"
        const val IGNORES_MEMORY_FLAG = "\$zea_ignoresAbyssSeaWave"
    }

    class AbyssSeaWaveParams(
        /** The distance this pulse will travel before completely expiring. */
        val distance: Float,
        /** Works with distance. When within this many SU of the source, the pulse does the max effect possible. Outside, it will fade from 100% to 0% opacity - hitting 0% at the end of lifespan.*/
        val maxEffectRange: Float,
        bandWidth: Float,
        relatedEntity: SectorEntityToken? = null,
        startingRange: Float = 0f,
        name: String = "Dawntide",
        val spawnSounds: List<String> = listOf("mote_attractor_targeted_ship", "gate_explosion"),
        /** In SU/s. Does not increase the distance of the wave. */
        val speed: Float = BASE_SHOCKWAVE_SPEED,
        var damage: ExplosionFleetDamage = ExplosionFleetDamage.LOW,
        var explosionDamageMult: Float = 1f,
        val burnMult: Float = BASE_BURN_MULT,
        /** Days. */
        val burnMultDuration: Float = BASE_BURN_MULT_DURATION,
        /** Do we respect the memory flag that says 'we dont take damage from this terrain'?*/
        val respectIgnore: Boolean = true
    ): RingParams(bandWidth, startingRange, relatedEntity, name)

    @Transient
    var blockerUtil: KOL_rangeBlockerWithEnds = KOL_rangeBlockerWithEnds(1440, 10000f, SHIELDS_FROM_TAG)
        get() {
            if (field == null) field = KOL_rangeBlockerWithEnds(1440, 10000f, SHIELDS_FROM_TAG)
            return field
        }

    val alreadyHit = kotlin.collections.HashSet<CampaignFleetAPI>()
    var idealBandwidth: Float = 0f

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)

        if (entity == null) return
        if (name == null || name == "Unknown") name = "Dawntide"
        val casted = getCastedParams()
        if (casted.spawnSounds.isNotEmpty() && relatedEntity?.containingLocation?.isCurrentLocation == true || entity.containingLocation?.isCurrentLocation == true) {

            val viewport = Global.getSector().viewport
            var volume = 1f

            var soundLoc = Vector2f(entity.location)

            if (!viewport.isNearViewport(soundLoc, 10f)) {
                val vec = VectorUtils.getDirectionalVector(soundLoc, viewport.center)
                val dist = MathUtils.getDistance(soundLoc, viewport.center)
                soundLoc = vec.scale(dist * 0.9f) as Vector2f

                volume = 0.1f
            }

            for (id in casted.spawnSounds) {
                Global.getSoundPlayer().playSound(id, 1f, volume, soundLoc, Misc.ZERO)
            }
        }
        idealBandwidth = params.bandWidthInEngine
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val mult = getEffectMult(null)
        if (mult <= 0f) {
            delete()
            return
        }

        val casted = getCastedParams()
        val days = Misc.getDays(amount)
        val adjustPos = casted.speed * days
        casted.middleRadius += adjustPos

        if (!blockerUtil.wasEverUpdated()) {
            blockerUtil.updateAndSync(entity, entity.starSystem?.star, 0.1f)
        }

        blockerUtil.updateLimits(entity, entity.starSystem?.star, 0.5f)
        blockerUtil.advance(amount, 100f, 0.5f)
    }

    @Transient
    var sprite: SpriteAPI = Global.getSettings().getSprite("terrain", "zea_wavefront")
        get() {
            field = Global.getSettings().getSprite("terrain", "zea_wavefront")
            return field
        }
    override fun render(layer: CampaignEngineLayers?, v: ViewportAPI?) {
        super.render(layer, v)

        if (layer == null || v == null) return

        params.bandWidthInEngine = idealBandwidth.coerceAtMost(params.middleRadius) // prevents really weird inverse ring behavior
        // glitches on advance, trust me, i tried

        val params = getCastedParams()
        OpenGlUtils.drawTexturedRing(
            Vector2f(entity.location),
            params.middleRadius,
            params.bandWidthInEngine,
            380, // polygons of the circle
            85, // segments of the ring
            500f,
            sprite,
            90f,
            getEffectMult(null),
        )
    }

    @Transient
    var rr: RingRenderer? = null
    override fun renderOnMap(factor: Float, alphaMult: Float) {
        if (params == null) return
        val castedParams = getCastedParams()
        if (rr == null) {
            rr = RingRenderer("systemMap", "map_asteroid_belt")
        }
        val playerFleet = Global.getSector().playerFleet
        var adjustedAlpha = if (shouldHitFleet(playerFleet, false)) alphaMult else alphaMult * 0.5f
        val mult = getEffectMult(null)
        adjustedAlpha *= mult
        val max = castedParams.middleRadius + (castedParams.bandWidthInEngine / 2f)
        val min = max - 500f
        rr!!.render(
            entity.location,
            min,
            max,
            LUNA_COLOR,
            false, factor, adjustedAlpha
        )
    }

    override fun renderOnRadar(radarCenter: Vector2f?, factor: Float, alphaMult: Float) {
        if (radarCenter == null) return

        GL11.glPushMatrix();
        GL11.glTranslatef(-radarCenter.x * factor, -radarCenter.y * factor, 0f);
        renderOnMap(factor, alphaMult);
        GL11.glPopMatrix();
    }

    private fun delete() {
        val containing = entity.containingLocation
        containing.removeEntity(entity)
    }

    fun getCastedParams(): AbyssSeaWaveParams = params as AbyssSeaWaveParams

    val UID = Misc.genUID()
    override fun getTerrainId(): String? {
        return UID // allows multiple pulses to affect a fleet
    }

    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        super.applyEffect(entity, days)

        if (entity !is CampaignFleetAPI) return

        if (!shouldAffectFleet(entity)) return
        // place behavior that also affects the dawntide fleets here
        if (!shouldHitFleet(entity)) return
        hitFleet(entity)
    }

    fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        return true
    }

    fun shouldHitFleet(fleet: CampaignFleetAPI, useShouldAffect: Boolean = true): Boolean {
        if (fleetIsImmune(fleet)) return false
        if (fleet in alreadyHit) return false
        if (fleetIsBlocked(fleet)) return false

        if (useShouldAffect && !shouldAffectFleet(fleet)) return false
        return true
    }

    fun getMiddle(): Vector2f {
        return (Vector2f(entity.location).translate(params.middleRadius, params.middleRadius))
    }

    fun hitFleet(fleet: CampaignFleetAPI) {
        val mult = getEffectMult(fleet)
        val casted = getCastedParams()

        val members = fleet.fleetData.membersListCopy
        if (members.isEmpty()) return

        var totalValue = 0f
        for (member in members) {
            totalValue += member.stats.suppliesToRecover.modifiedValue
        }
        if (totalValue <= 0) return

        var damageFraction = 0f
        damageFraction = when (casted.damage) {
            ExplosionFleetDamage.NONE -> return
            ExplosionFleetDamage.LOW -> 0.4f
            ExplosionFleetDamage.MEDIUM -> 0.7f
            ExplosionFleetDamage.HIGH -> 0.8f
            ExplosionFleetDamage.EXTREME -> 0.9f
            else -> return
        }

        damageFraction *= mult

        if (fleet.isInCurrentLocation && fleet.isVisibleToPlayerFleet) {
            val dist = Misc.getDistance(fleet, Global.getSector().playerFleet)
            if (dist < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE) {
                val volumeMult = 3f * damageFraction
                Global.getSoundPlayer()
                    .playSound("gate_explosion_fleet_impact", 1f, volumeMult, fleet.location, Misc.ZERO)
            }
        }

        //float strikeValue = totalValue * damageFraction * (0.5f + (float) Math.random() * 0.5f);


        //float strikeValue = totalValue * damageFraction * (0.5f + (float) Math.random() * 0.5f);
        val picker = WeightedRandomPicker<FleetMemberAPI>()
        for (member in members) {
            var w = 1f
            if (member.isFrigate) w *= 0.1f
            if (member.isDestroyer) w *= 0.2f
            if (member.isCruiser) w *= 0.5f
            picker.add(member, w)
        }

        val numStrikes = picker.items.size
        alreadyHit += fleet

        for (i in 0 until numStrikes) {
            val member = picker.pick() ?: return
            val crPerDep = member.deployCost
            //if (crPerDep <= 0) continue;
            val suppliesPerDep = member.stats.suppliesToRecover.modifiedValue
            if (suppliesPerDep <= 0 || crPerDep <= 0) return
            val suppliesPer100CR = suppliesPerDep * 1f / Math.max(0.01f, crPerDep)

            // half flat damage, half scaled based on ship supply cost cost
            var strikeSupplies = ((BASE_STRIKE_DAMAGE) + suppliesPer100CR) * 0.5f * damageFraction
            strikeSupplies *= casted.explosionDamageMult
            //strikeSupplies = suppliesPerDep * 0.5f * damageFraction;
            var strikeDamage = strikeSupplies / suppliesPer100CR * (0.75f + Math.random().toFloat() * 0.5f)
            strikeDamage *= 0.6f

            //float strikeDamage = damageFraction * (0.75f + (float) Math.random() * 0.5f);
            val resistance = member.stats.dynamic.getValue(Stats.CORONA_EFFECT_MULT)
            strikeDamage *= resistance
            if (strikeDamage > HyperspaceTerrainPlugin.STORM_MAX_STRIKE_DAMAGE) {
                strikeDamage = HyperspaceTerrainPlugin.STORM_MAX_STRIKE_DAMAGE
            }
            if (strikeDamage > 0) {
                val currCR = member.repairTracker.baseCR
                val crDamage = currCR.coerceAtMost(strikeDamage)
                if (crDamage > 0) {
                    member.repairTracker.applyCREvent(
                        -crDamage, "explosion_" + entity.id,
                        "Damaged by explosion"
                    )
                }
                val hitStrength = member.stats.armorBonus.computeEffective(member.hullSpec.armorRating)
                //hitStrength *= strikeDamage / crPerDep;
                var numHits = (strikeDamage / 0.1f).toInt()
                if (numHits < 1) numHits = 1
                for (j in 0 until numHits) {
                    member.status.applyDamage(hitStrength)
                }
                //member.getStatus().applyHullFractionDamage(1f);
                if (member.status.hullFraction < 0.01f) {
                    member.status.hullFraction = 0.01f
                    picker.remove(member)
                } else {
                    val w = picker.getWeight(member)
                    picker.setWeight(picker.items.indexOf(member), w * 0.5f)
                }
            }
            //picker.remove(member);
        }

        val shoveDir = Misc.getAngleInDegrees(entity.location, fleet.location)
        val shoveIntensity = (damageFraction * 20f).coerceAtMost(casted.speed / 12000f) // not arbitrary, it mostly keeps it locked to the speed of the pulse
        fleet.addScript(ShoveFleetScript(fleet, shoveDir, shoveIntensity)) // EDIT

        val remainder = 1 - casted.burnMult
        val add = remainder * (1 - mult)
        val finalMult = casted.burnMult + add
        if (finalMult < 1f) {
            fleet.stats.addTemporaryModMult(
                casted.burnMultDuration,
                UID,
                "Reeling from $name impact",
                finalMult,
                fleet.stats.fleetwideMaxBurnMod
            )
        }

        if (fleet.isPlayerFleet) {
            Global.getSector().campaignUI.addMessage(
                "Your fleet suffers damage from the Dawntide", Misc.getNegativeHighlightColor()
            )
        }
    }

    private fun getEffectMult(fleet: CampaignFleetAPI?): Float {
        val progress = getProgress()
        val casted = getCastedParams()
        val progressOutsideMaxRange = (progress - casted.maxEffectRange).coerceAtLeast(0f)
        val lifespanOutsideRange = casted.distance - casted.maxEffectRange
        val mult = 1 - (progressOutsideMaxRange / lifespanOutsideRange)
        return mult
    }

    fun getProgress(): Float = MathUtils.getDistance(entity.location, getMiddle())

    fun fleetIsImmune(fleet: CampaignFleetAPI): Boolean {
        if (fleet.memoryWithoutUpdate.getBoolean(IGNORES_MEMORY_FLAG) && getCastedParams().respectIgnore) return true

        return false
    }

    fun fleetIsBlocked(fleet: CampaignFleetAPI): Boolean {
        val location = fleet.location
        val angle = VectorUtils.getAngle(entity.location, location)
        val containing = entity.containingLocation ?: return false
        val blockers = containing.planets + containing.getEntitiesWithTag(SHIELDS_FROM_TAG)
        if (blockers.any { MathUtils.getDistance(it, fleet) <= 0f }) return true

        val range = blockerUtil.getBlockedRangeAt(angle)
        val dist = MathUtils.getDistance(entity.location, location)

        if (dist > range.first && dist < range.second) return true

        val shoal = containing.terrainCopy.firstOrNull { it.plugin is AbyssShoalNebulaTerrain } ?: return false
        val plugin = shoal.plugin as AbyssShoalNebulaTerrain
        return plugin.containsEntity(fleet)
    }

    override fun getEffectCategory(): String? {
        return null
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

    override fun hasAIFlag(flag: Any?): Boolean {
        return false // it just passes over fleets
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
        return EnumSet.of(CampaignEngineLayers.ABOVE_STATIONS)
    }

    override fun hasTooltip(): Boolean = true

    override fun isTooltipExpandable(): Boolean {
        return false
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(tooltip, expanded)

        if (tooltip == null) return

        val baseName = nameForTooltip
        tooltip.addTitle(baseName)
        tooltip.addPara(Global.getSettings().getDescription(spec.id, Description.Type.TERRAIN).text1, 5f)

        // nothing else here. most of the intel on what these do and how to avoid them will be in a system-spanning terrain
    }

    override fun getNameColor(): Color? {
        val base = super.nameColor
        val special = Misc.getNegativeHighlightColor()

        return Misc.interpolateColor(base, special, Global.getSector().campaignUI.sharedFader.brightness * 1f)
    }
}