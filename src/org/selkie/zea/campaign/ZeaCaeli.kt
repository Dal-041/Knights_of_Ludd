package org.selkie.zea.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BattleCreationContext
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CoreInteractionListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageEntityGeneratorOld
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.loading.VariantSource
import org.magiclib.util.MagicCampaign
import org.selkie.zea.fleets.ZeaFleetManager
import org.selkie.zea.fleets.ZeaFleetSoloManager
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys
import org.selkie.zea.helpers.ZeaUtils
import org.selkie.zea.world.PrepareAbyss
import java.util.Random

/**
 * Caeli's guardian, in Ozymandias: a derelict Zhi Nu prototype floats beside the Caeli cryosleeper, and either one's
 * dialog offers the fight (rules: `zea_caeli*`). The Zhi Nu and the guardian deploy together as one Dawn fleet, built
 * for each engagement as vanilla's Dweller fights are (DwellerCMD.engageFleet), with SalvageDefenderInteraction's
 * return to the entity afterwards. Beating it removes the system's Dawn spawns and leaves the Zhi Nu salvageable as
 * the Elysian boss wrecks are. Caeli itself never goes through vanilla's salvage flow, so it never becomes a usable
 * cryosleeper.
 */
object ZeaCaeli {
    /** Deploys with the Zhi Nu, as Dawn. */
    const val GUARDIAN_VARIANT = "guardian_Standard"
    const val ZHI_NU_VARIANT = ZeaStaticStrings.ZEA_BOSS_ZHI_NU_DANCING
    /** The Zhi Nu's derelict, from Caeli. */
    private const val ZHI_NU_DISTANCE = 350f
    /** getAbyssLootID's tier for the filler loot (lower is better). */
    private const val LOOT_TIER = 1f

    private val global get() = Global.getSector().memoryWithoutUpdate

    fun system(): StarSystemAPI? = Global.getSector().getStarSystem(ZeaStaticStrings.ozymandiasSysName)

    fun caeli(): SectorEntityToken? = system()?.customEntities?.firstOrNull { it.customEntityType == Entities.DERELICT_CRYOSLEEPER }

    fun zhiNu(): SectorEntityToken? = system()?.customEntities?.firstOrNull { it.memoryWithoutUpdate.getBoolean(ZeaMemKeys.ZEA_CAELI_ZHINU) }

    fun beaten(): Boolean = global.getBoolean(ZeaMemKeys.ZEA_CAELI_BEATEN)

    /** On each load: flags Caeli, and places the Zhi Nu beside it once. */
    @JvmStatic
    fun ensure() {
        val caeli = caeli() ?: return
        caeli.memoryWithoutUpdate.set(ZeaMemKeys.ZEA_CAELI, true)
        if (global.getBoolean(ZeaMemKeys.ZEA_CAELI_ZHINU_PLACED)) return
        global.set(ZeaMemKeys.ZEA_CAELI_ZHINU_PLACED, true)
        val wreck = MagicCampaign.createDerelict(ZHI_NU_VARIANT, ShipRecoverySpecial.ShipCondition.WRECKED, false, -1,
            false, caeli, Random().nextFloat() * 360f, ZHI_NU_DISTANCE, 360f) ?: return
        wreck.name = "[PLACEHOLDER] Derelict Prototype"
        wreck.memoryWithoutUpdate.set(ZeaMemKeys.ZEA_CAELI_ZHINU, true)
        if (beaten()) ZeaUtils.bossWreckCleaner(wreck, true)
    }

    /** The guardian's fleet: the Zhi Nu as flagship and the guardian, both Dawn, under AI cores. */
    private fun createFleet(): CampaignFleetAPI {
        val fleet = Global.getFactory().createEmptyFleet(ZeaStaticStrings.dawnID, "[PLACEHOLDER] Caeli's Guardian", true)
        fleet.isNoFactionInName = true
        fleet.inflater = null
        val zhiNu = fleet.fleetData.addFleetMember(ZHI_NU_VARIANT)
        fleet.fleetData.addFleetMember(GUARDIAN_VARIANT)
        fleet.fleetData.setFlagship(zhiNu)
        val captain = ZeaFleetManager.createAICaptain(ZeaStaticStrings.dawnID)
        zhiNu.captain = captain
        fleet.commander = captain
        ZeaFleetManager.setAICaptains(fleet)

        // the Zhi Nu can't be recovered from the battle: it's salvaged from its wreck on the map afterwards
        zhiNu.setVariant(zhiNu.variant.clone(), false, false)
        zhiNu.variant.source = VariantSource.REFIT
        zhiNu.variant.addTag(ZeaMemKeys.ZEA_BOSS_TAG)
        zhiNu.variant.addTag(Tags.VARIANT_UNBOARDABLE)
        zhiNu.variant.addTag(Tags.SHIP_LIMITED_TOOLTIP)

        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true)
        fleet.memoryWithoutUpdate.set(ZeaMemKeys.ZEA_BOSS_TAG, true)
        fleet.fleetData.setSyncNeeded()
        fleet.fleetData.syncIfNeeded()
        for (member in fleet.fleetData.membersListCopy) member.repairTracker.cr = member.repairTracker.maxCR
        return fleet
    }

    /** From Caeli's or the Zhi Nu's dialog: the fight. Fleets nearby when it starts join: the Knights with the player, Dawn with the guardian. */
    fun engage(dialog: InteractionDialogAPI, memoryMap: Map<String, MemoryAPI>): Boolean {
        val entity = dialog.interactionTarget ?: return false
        val fleet = createFleet()
        fleet.containingLocation = entity.containingLocation
        fleet.setLocation(entity.location.x, entity.location.y)
        dialog.interactionTarget = fleet

        val config = FIDConfig()
        config.leaveAlwaysAvailable = true
        config.showCommLinkOption = false
        config.showEngageText = false
        config.showFleetAttitude = false
        config.showTransponderStatus = false
        config.showWarningDialogWhenNotHostile = false
        config.alwaysAttackVsAttack = true
        config.impactsAllyReputation = true
        config.impactsEnemyReputation = false
        config.pullInAllies = true
        config.pullInEnemies = true
        config.pullInStations = false
        config.lootCredits = false
        config.firstTimeEngageOptionText = "[PLACEHOLDER] Engage the guardian"
        config.afterFirstTimeEngageOptionText = "[PLACEHOLDER] Re-engage the guardian"
        config.noSalvageLeaveOptionText = "Continue"
        config.dismissOnLeave = false
        config.printXPToDialog = true

        val plugin = FleetInteractionDialogPluginImpl(config)
        val original = dialog.plugin
        config.delegate = object : BaseFIDDelegate() {
            override fun notifyLeave(dialog: InteractionDialogAPI) {
                fleet.clearAssignments()
                dialog.plugin = original
                dialog.interactionTarget = entity
                val context = plugin.context as? FleetEncounterContext
                if (context != null && context.didPlayerWinEncounterOutright()) {
                    beat()
                    FireBest.fire(null, dialog, memoryMap, "ZeaCaeliBeaten")
                } else {
                    dialog.dismiss()
                }
            }

            override fun battleContextCreated(dialog: InteractionDialogAPI, bcc: BattleCreationContext) {
                bcc.aiRetreatAllowed = false
                bcc.objectivesAllowed = false
                bcc.enemyDeployAll = true
            }
        }
        dialog.plugin = plugin
        plugin.init(dialog)
        return true
    }

    /** The guardian is beaten: the Dawn spawns stop, and the Zhi Nu's wreck becomes salvageable. */
    private fun beat() {
        global.set(ZeaMemKeys.ZEA_CAELI_BEATEN, true)
        system()?.removeScriptsOfClass(ZeaFleetSoloManager::class.java)
        zhiNu()?.let { ZeaUtils.bossWreckCleaner(it, true) }
    }

    /** Without the Knights' mission: filler loot from the caches' drop groups, once. */
    fun reward(dialog: InteractionDialogAPI) {
        if (global.getBoolean(ZeaMemKeys.ZEA_CAELI_REWARDED)) return
        global.set(ZeaMemKeys.ZEA_CAELI_REWARDED, true)
        val spec = SalvageEntityGeneratorOld.getSalvageSpec(PrepareAbyss.getAbyssLootID(ZeaStaticStrings.dawnID, LOOT_TIER))
        val loot = SalvageEntity.generateSalvage(Random(), 1f, 1f, 1f, 1f, spec.dropValue, spec.dropRandom)
        dialog.visualPanel.showLoot("Recovered", loot, false, true, true, object : CoreInteractionListener {
            override fun coreUIDismissed() {
                dialog.dismiss()
                dialog.hideTextPanel()
                dialog.hideVisualPanel()
            }
        })
    }
}
