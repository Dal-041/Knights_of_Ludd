package org.selkie.kol.campaign.story

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource
import org.selkie.kol.campaign.tech.KolLowTech
import org.selkie.kol.helpers.KolStaticStrings.KolStory

/**
 * One hook for every feature that changes fleets as they spawn (desertion here; the Ordained rollout later).
 * `reportFleetSpawned` fires once per fleet, when it first enters a location. Modifiers are registered at each load
 * (transient); a fleet any modifier changed is tagged so it isn't processed again.
 *
 * Swapped and converted members carry `Tags.TAG_NO_AUTOFIT`, so the fleet inflater keeps their stock loadouts
 * (DefaultFleetInflater skips autofit for that tag).
 */
class KolFleetSpawnHook : BaseCampaignEventListener(false) {

    interface Modifier {
        /** Changes the fleet if it applies; returns true if anything changed. */
        fun apply(fleet: CampaignFleetAPI): Boolean
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        if (fleet == null || fleet.isPlayerFleet || fleet.hasTag(KolStory.FLEET_MODIFIED_TAG)) return
        var changed = false
        for (modifier in modifiers.values) {
            try {
                if (modifier.apply(fleet)) changed = true
            } catch (e: Exception) {
                log.error("KOL: fleet-spawn modifier ${modifier.javaClass.simpleName} failed on ${fleet.name}", e)
            }
        }
        if (changed) fleet.addTag(KolStory.FLEET_MODIFIED_TAG)
    }

    companion object {
        private val log = Global.getLogger(KolFleetSpawnHook::class.java)
        private val modifiers = LinkedHashMap<String, Modifier>()

        /** Registers (or replaces) a modifier by key; call at each load. */
        fun register(key: String, modifier: Modifier) {
            modifiers[key] = modifier
        }

        /** Replaces [old] with a new ship of [variantId], keeping its officer and flagship status. */
        fun swapMember(fleet: CampaignFleetAPI, old: FleetMemberAPI, variantId: String): FleetMemberAPI? {
            if (!Global.getSettings().doesVariantExist(variantId)) return null
            val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId)
            val variant = member.variant.clone()
            variant.source = VariantSource.REFIT
            variant.addTag(Tags.TAG_NO_AUTOFIT)
            member.setVariant(variant, false, true)
            member.shipName = old.shipName
            if (old.captain != null && !old.captain.isDefault) member.captain = old.captain
            val wasFlagship = old.isFlagship
            fleet.fleetData.removeFleetMember(old)
            fleet.fleetData.addFleetMember(member)
            if (wasFlagship) fleet.fleetData.setFlagship(member)
            member.repairTracker.cr = member.repairTracker.maxCR
            return member
        }

        /** Moves [member] onto its `_lowtech` (Ordained) skin; true if it changed. */
        fun convertMember(member: FleetMemberAPI): Boolean {
            if (!KolLowTech.convert(member)) return false
            member.variant.addTag(Tags.TAG_NO_AUTOFIT)
            return true
        }

        /** Re-syncs a fleet after members were changed. */
        fun finish(fleet: CampaignFleetAPI) {
            fleet.fleetData.sort()
            fleet.fleetData.setSyncNeeded()
            fleet.fleetData.syncIfNeeded()
        }
    }
}
