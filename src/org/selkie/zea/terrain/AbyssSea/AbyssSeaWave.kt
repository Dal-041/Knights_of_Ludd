package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.LocationAPI
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
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin
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
import org.magiclib.kotlin.addGlowyParticle
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.terrain.AbyssSea.AbyssSeaWaveManager.Companion.LUNA_COLOR
import org.selkie.zea.terrain.AbyssShoalNebulaTerrain
import java.awt.Color
import java.util.EnumSet
import kotlin.collections.any
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceAtMost
import kotlin.ranges.until

/** The wave itself */
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

        const val PARTICLE_GEN_DIST = 10000f
        const val PARTICLES_PER_SECOND = 45f
        /** Sea foam thrown over a shelter's edges where the wave runs past it: particles per second per 100 SU of edge. */
        const val FOAM_PER_100SU = 18f
        /** Extra foam burst off the front of a shelter while the wave crashes into it, particles per second. */
        const val CRASH_FOAM_PER_SECOND = 68f
        /** Edge foam density at the front of the tear and at its tip, as multiples of [FOAM_PER_100SU]; it thins
         * evenly in between. */
        const val FOAM_START_MULT = 1.6f
        const val FOAM_END_MULT = 0.4f
        val FOAM_COLOR = Color(235, 248, 255)
        /** Chance that a fleck of foam along an edge, or off the crash, also leaves a cloud puff under it. */
        const val PUFF_CHANCE_EDGE = 0.32f
        const val PUFF_CHANCE_CRASH = 0.67f
        val PUFF_COLOR = Color(175, 215, 255)
        /** Peak opacity of a puff. They are drawn with normal blending, so they veil the seam rather than light it. */
        const val PUFF_ALPHA = 0.45f

        /** Extra radius added around an obstacle's shelter, in SU. Small, so the wave's tear hugs the obstacle; the
         * tear's soft edge ([AbyssSeaWaveRenderer.EDGE_FADE]) already adds a little visual margin past this. */
        const val SHELTER_GRACE = 15f
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
        /** Will ALWAYS play, even if the player is too far away. If the player IS far away, plays a lot more quietely.*/
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

    /** The calm left behind each obstacle the wave breaks on. Rebuilt every frame since obstacles orbit. */
    @Transient
    private var shelters: List<AbyssSeaShelter>? = null

    private fun getShelters(): List<AbyssSeaShelter> = shelters ?: computeShelters().also { shelters = it }

    /** Everything a fleet can hide behind or against. */
    private fun getBlockers(): List<SectorEntityToken> {
        val containing = entity.containingLocation ?: return emptyList()
        return containing.planets + containing.getEntitiesWithTag(SHIELDS_FROM_TAG)
    }

    private fun computeShelters(): List<AbyssSeaShelter> {
        val star = entity.starSystem?.star
        val maxDistance = getCastedParams().distance
        return getBlockers()
            .filter { it != entity && it != relatedEntity && it != star }
            .map { AbyssSeaShelter(entity.location, Vector2f(it.location), it.radius + SHELTER_GRACE) }
            .filter { it.distance - it.radius < maxDistance }
    }

    /** A list of fleets we've already hit. Used to avoid spamming hits over and over. */
    val alreadyHit = kotlin.collections.HashSet<CampaignFleetAPI>()
    /** The bandwidth we started with. Our bandwith can change based on the position of our ring, and if its too small. */
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

    var particleCache = 0f

    /** Seconds of game time this wave has been seen running; drives the vortex street behind its tears. */
    @Transient
    private var rippleTime = 0f

    override fun advance(amount: Float) {
        rippleTime = (rippleTime + amount) % 10000f
        if (params !is AbyssSeaWaveParams) {
            relatedEntity?.containingLocation?.removeEntity(entity)
            return // wait for compatibility utils to clean us up
        }

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

        shelters = computeShelters()
        advancePuffs(amount)
        splash(amount, casted)

        particleCache += amount * PARTICLES_PER_SECOND
        if (particleCache >= 1) {
            while (particleCache-- > 0) {
                val point = MathUtils.getRandomPointOnCircumference(
                    entity.location,
                    casted.middleRadius + (casted.bandWidthInEngine / 2)
                )
                if (MathUtils.getDistance(point, Global.getSector().viewport.center) > PARTICLE_GEN_DIST) return
                if (getShelters().any { it.contains(point) }) continue
                val particle = entity.containingLocation.addGlowyParticle(
                    Global.getSector().playerFleet.location, // we're tricking the particles to spawn even when they will be outside our viewport
                    VectorUtils.getDirectionalVector(entity.location, point).scale(casted.speed * MathUtils.getRandomNumberInRange(0.04f, 0.06f)) as Vector2f,
                    MathUtils.getRandomNumberInRange(50f, 60f),
                    0.1f,
                    MathUtils.getRandomNumberInRange(20f, 30f),
                    Color(0, 90, 0)
                )
                for (part in particle) {
                    if (part == null) continue
                    part.setPos(point.x, point.y)
                }
                particle[1]?.color = Color(0, 30, 0)
                particle[2]?.color = Color(0, 30, 0)
            }
        }
    }

    @Transient
    private var foamCache = 0f
    @Transient
    private var crashCache = 0f

    /**
     * Sea foam where the wave meets each shelter: white spray burst back off the front where the wave crashes into the
     * obstacle, and foam spilling across both edges of the tear as the wave runs past, so the line between the wave
     * and the calm is never clean.
     */
    private fun splash(amount: Float, casted: AbyssSeaWaveParams) {
        val inner = casted.middleRadius - casted.bandWidthInEngine / 2f
        val outer = casted.middleRadius + casted.bandWidthInEngine / 2f
        val viewCenter = Global.getSector().viewport.center
        val location = entity.containingLocation ?: return
        for (shelter in getShelters()) {
            val from = maxOf(-shelter.radius, inner - shelter.distance)
            val to = minOf(shelter.tailLength, outer - shelter.distance)
            if (to <= from) continue
            if (MathUtils.getDistance(shelter.center, viewCenter) > PARTICLE_GEN_DIST + shelter.tailLength) continue
            val downstream = shelter.downstream()

            // the crash: the band is over the obstacle's upstream face
            val crashTo = minOf(to, 0f)
            if (crashTo > from) {
                crashCache += amount * CRASH_FOAM_PER_SECOND * (crashTo - from) / shelter.radius
                while (crashCache >= 1f) {
                    crashCache -= 1f
                    val along = MathUtils.getRandomNumberInRange(from, crashTo)
                    val point = shelter.edgePoint(along, if (Math.random() < 0.5) 1f else -1f)
                    // flung back off the face, then carried on by the wave
                    val out = Vector2f(point.x - shelter.center.x, point.y - shelter.center.y)
                    if (out.lengthSquared() > 0f) out.normalise()
                    val throwSpeed = MathUtils.getRandomNumberInRange(60f, 260f)
                    val carry = casted.speed * MathUtils.getRandomNumberInRange(0.0f, 0.03f)
                    val velocity = Vector2f(
                        out.x * throwSpeed + downstream.x * carry,
                        out.y * throwSpeed + downstream.y * carry
                    )
                    foam(location, point, velocity)
                    if (Math.random() < PUFF_CHANCE_CRASH) puff(point, velocity)
                }
            }

            // the run past: foam spilling over both edges
            // sampled at the densest rate, then thinned towards the tip
            foamCache += amount * FOAM_PER_100SU * FOAM_START_MULT * 2f * (to - from) / 100f
            while (foamCache >= 1f) {
                foamCache -= 1f
                val along = MathUtils.getRandomNumberInRange(from, to)
                if (shelter.halfWidth(along) <= 0f) continue
                val down = (along / shelter.tailLength).coerceIn(0f, 1f)
                val density = FOAM_START_MULT + (FOAM_END_MULT - FOAM_START_MULT) * down
                if (Math.random() * FOAM_START_MULT > density) continue
                val side = if (Math.random() < 0.5) 1f else -1f
                val point = shelter.edgePoint(along, side)
                val normal = shelter.sideNormal(side)
                val straddle = MathUtils.getRandomNumberInRange(-30f, 30f)
                point.x += normal.x * straddle
                point.y += normal.y * straddle

                val forward = casted.speed * MathUtils.getRandomNumberInRange(0.02f, 0.05f)
                val sideways = MathUtils.getRandomNumberInRange(-90f, 40f) // mostly into the tear
                val velocity = Vector2f(
                    downstream.x * forward + normal.x * sideways,
                    downstream.y * forward + normal.y * sideways
                )
                foam(location, point, velocity)
                if (Math.random() < PUFF_CHANCE_EDGE) puff(point, velocity)
            }
        }
    }

    /** A soft nebula cloud left under the foam where the wave breaks, veiling the tear's seams. */
    private class Puff(
        val x: Float, val y: Float, val vx: Float, val vy: Float,
        val size: Float, val angle: Float, val spin: Float, val life: Float, val cell: Int
    ) {
        var age = 0f
        var px = x
        var py = y
    }

    @Transient
    private var puffs: ArrayList<Puff>? = null

    @Transient
    private var puffSprite: SpriteAPI? = null

    private fun puff(point: Vector2f, velocity: Vector2f) {
        val list = puffs ?: ArrayList<Puff>().also { puffs = it }
        val slow = MathUtils.getRandomNumberInRange(0.25f, 0.5f) // clouds lag behind the spray
        list += Puff(
            point.x, point.y, velocity.x * slow, velocity.y * slow,
            MathUtils.getRandomNumberInRange(110f, 240f),
            MathUtils.getRandomNumberInRange(0f, 360f),
            MathUtils.getRandomNumberInRange(-25f, 25f),
            MathUtils.getRandomNumberInRange(1.4f, 2.8f),
            (Math.random() * 16).toInt()
        )
    }

    private fun advancePuffs(amount: Float) {
        val list = puffs ?: return
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age += amount
            if (p.age >= p.life) {
                iterator.remove()
                continue
            }
            p.px += p.vx * amount
            p.py += p.vy * amount
        }
    }

    /** Draws the puffs over the wave and under the foam particles, from the 4x4 cloud sheet vanilla explosions use. */
    private fun renderPuffs(viewport: ViewportAPI) {
        val list = puffs ?: return
        if (list.isEmpty()) return
        val sprite = puffSprite ?: Global.getSettings().getSprite("misc", "nebula_particles").also { puffSprite = it }
        sprite.setTexWidth(0.25f)
        sprite.setTexHeight(0.25f)
        sprite.setNormalBlend()
        sprite.color = PUFF_COLOR
        val mult = viewport.alphaMult * getEffectMult(null)
        for (p in list) {
            if (!viewport.isNearViewport(Vector2f(p.px, p.py), p.size * 2f)) continue
            val t = p.age / p.life
            val fade = if (t < 0.2f) t / 0.2f else (1f - t) / 0.8f
            val size = p.size * (1f + 0.6f * t)
            sprite.setTexX((p.cell % 4) * 0.25f)
            sprite.setTexY((p.cell / 4) * 0.25f)
            sprite.angle = p.angle + p.spin * p.age
            sprite.setSize(size, size)
            sprite.alphaMult = PUFF_ALPHA * fade * mult
            sprite.renderAtCenter(p.px, p.py)
        }
    }

    /** One fleck of foam: mostly small sharp bits, sometimes a large soft clump. */
    private fun foam(location: LocationAPI, point: Vector2f, velocity: Vector2f) {
        val clump = Math.random() < 0.2
        val particle = location.addParticle(
            Global.getSector().playerFleet.location, // spawned on the player, then moved, as the wave's own particles are
            velocity,
            if (clump) MathUtils.getRandomNumberInRange(60f, 120f) else MathUtils.getRandomNumberInRange(12f, 35f),
            if (clump) MathUtils.getRandomNumberInRange(0.25f, 0.45f) else MathUtils.getRandomNumberInRange(0.6f, 1f),
            0.05f,
            MathUtils.getRandomNumberInRange(0.4f, 1.12f),
            FOAM_COLOR
        )
        particle?.setPos(point.x, point.y)
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
        val innerRadius = params.middleRadius - params.bandWidthInEngine / 2f
        val outerRadius = params.middleRadius + params.bandWidthInEngine / 2f
        AbyssSeaWaveRenderer.render(
            Vector2f(entity.location), innerRadius, outerRadius, getShelters(), sprite, getEffectMult(null),
            rippleTime
        )
        renderPuffs(v)
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
        // make it a bit transparent if the wave isnt a current threat
        var adjustedAlpha = if (shouldHitFleet(playerFleet)) alphaMult else alphaMult * 0.5f
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

        // place behavior that also affects the dawntide fleets here
        if (!shouldHitFleet(entity)) return
        hitFleet(entity)
    }

    fun shouldHitFleet(fleet: CampaignFleetAPI): Boolean {
        if (fleetIsImmune(fleet)) return false
        if (fleet in alreadyHit) return false
        if (fleetIsBlocked(fleet)) return false

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
            val suppliesPer100CR = suppliesPerDep * 1f / 0.01f.coerceAtLeast(crPerDep)

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

    // corresponds with the opacity of the wave
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
        val containing = entity.containingLocation ?: return false
        if (getBlockers().any { MathUtils.getDistance(it, fleet) <= 0f }) return true
        if (getShelters().any { it.contains(fleet.location) }) return true

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