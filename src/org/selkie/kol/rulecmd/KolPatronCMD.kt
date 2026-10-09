package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.ch2.KolPatron
import org.selkie.kol.campaign.story.ch2.KolPatronParley

/**
 * Rule commands for the patron quest (add-knights-patron-lobbying):
 * - `KolPatronCMD askable` (condition): the active person has a hint about a patron the player hasn't learned.
 * - `KolPatronCMD ask [returnTrigger]`: asks them (returning to PopulateOptions, or the given vanilla option list); each hint plays `FireBest KolPatronAskReply` (`$kolPatron_role`, `_lead`, `_placeText`).
 * - `KolPatronCMD payBribe`: pays a hostile pirate's price, then `FireBest KolPatronBribePaid` and any other hints.
 * - `KolPatronCMD askFleet <returnTrigger>`: a Path fleet's captain directs the player to Chalcedon, then `FireAll <returnTrigger>`.
 * - `KolPatronCMD ownOpen`: opens the own-forces meeting at Lyra (publishes the Lector's tokens).
 * - `KolPatronCMD signOwn <commitment>`: the meeting settles the player's own forces; the quest moves to ANNOUNCE.
 * - `KolPatronCMD announceOpen`: the announcing assembly's scene (publishes `$global.kolPatron_name`).
 * Great powers (KolPatronParley; the League is the reference):
 * - `KolPatronCMD reception <power>`: publishes `$kolPatron_reception` and `$kolPatron_powerName`.
 * - `KolPatronCMD leagueOffice` / `leagueOfficeIs <petition|meeting>` (conditions): Gens Hannan's office has League business.
 * - `KolPatronCMD advance <power> <stage>`: moves a branch on.
 * - `KolPatronCMD mazalotOpen` (condition) / `mazalotDone`: Mazalot's congregation for the League.
 * - `KolPatronCMD hegemonyOfficeIs <petition|curate>` (condition) / `hegemonySentAway` / `jangalaOpen` (condition) /
 *   `jangalaDone` / `hegemonyMeetingReady` (condition): the Hegemony's steps.
 * - `KolPatronCMD diktatOfficeIs proposal` (condition) / `diktatProposed` / `diktatAtLyra` (condition) / `diktatDispatched` /
 *   `diktatMeetingReady` (condition): the Diktat's steps.
 * - `KolPatronCMD dropOff`: the delegation goes home (its departure scene at Lyra).
 * - `KolPatronCMD gateNext`: the gate at Lyra plays its next pre-assembly scene, or hands the dock back.
 * - `KolPatronCMD charterReacted`: Greenflight has reacted to the charter (aboard or in the gate).
 * - `KolPatronCMD charterHere` (condition) / `charterOpen` / `charterPitch` / `signCharter`: the mercenary broker and
 *   the representative at New Maxios or Cethlenn; `charterOffense`: the assembly's reputation penalty.
 * - `KolPatronCMD pathAdmin` (condition) / `pathAsk` / `pathTried`: Chalcedon's administrator; `pathMurmur`: the next
 *   assembly's murmur and reputation penalty, once.
 * - The pirates: `pirateArrangeOpen` (condition) / `pirateArrangeOffer` / `pirateAudiencePay` (the Station King);
 *   `pirateShuttle` (condition) / `pirateMeet` / `pirateAgreementOffer` / `piratePayAgreement` / `pirateTerms` /
 *   `signPirate` (Kanta's delegate); `pirateDelegationGone` / `pirateSwarm` (the arrival at Lyra); `swarmActive` /
 *   `swarmRebuff` (conditions) / `swarmRebuffed` (Lyra); `pirateCut` (the Station King pays the turncoat).
 * - `KolPatronCMD volturnLament` (condition): Standfast's lament at Volturn's shrine is available.
 * - `KolPatronCMD meet <role>`: shows a parley's remembered person; tokens `$global.kolPerson_<role>_*`.
 * - `KolPatronCMD delegationWanted` (condition) / `collectOpen` / `collect`: the Order's delegates at Lyra.
 * - `KolPatronCMD openMeeting <power>` / `confirmTooltip <option>` / `sign <power> <lever>`: the meeting and the signing.
 * - `KolPatronCMD congregationHere` (condition): this market has a Luddic congregation to seek out.
 * - `KolPatronCMD congregation`: seeks it out; records leads, then `FireBest KolPatronCongregationReply`.
 */
class KolPatronCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        val market = dialog.interactionTarget?.market
        return when (params?.getOrNull(0)?.getString(map)) {
            "askable" -> dialog.interactionTarget !is com.fs.starfarer.api.campaign.CampaignFleetAPI && // fleets: askFleet
                    KolPatron.askable(dialog.interactionTarget?.activePerson, market)
            "ask" -> KolPatron.ask(dialog, map, params.getOrNull(1)?.getString(map) ?: "PopulateOptions")
            "payBribe" -> KolPatron.payBribe(dialog, map)
            "ownOpen" -> { KolPatron.ownOpen(); true }
            "announceOpen" -> { KolPatron.announceOpen(); true }
            "reception" -> { KolPatronParley.reception(power(params, map) ?: return false, map); true }
            "leagueOffice" -> KolPatronParley.leagueOffice() != null
            "leagueOfficeIs" -> KolPatronParley.leagueOffice() == params.getOrNull(1)?.getString(map)
            "advance" -> {
                val power = power(params, map) ?: return false
                KolPatronParley.advance(power, params.getOrNull(2)?.getString(map) ?: return false, dialog.textPanel); true
            }
            "hegemonyOfficeIs" -> KolPatronParley.hegemonyOffice() == params.getOrNull(1)?.getString(map)
            "hegemonySentAway" -> { KolPatronParley.hegemonySentAway(dialog.textPanel); true }
            "jangalaOpen" -> KolPatronParley.jangalaOpen()
            "jangalaFound" -> { KolPatronParley.jangalaFound(dialog.textPanel); true }
            "jangalaDone" -> { KolPatronParley.jangalaDone(dialog.textPanel); true }
            "hegemonyMeetingReady" -> KolPatronParley.hegemonyMeetingReady()
            "mazalotOpen" -> KolPatronParley.mazalotOpen()
            "mazalotDone" -> { KolPatronParley.mazalotDone(dialog.textPanel); true }
            "meet" -> KolPatronParley.meet(params.getOrNull(1)?.getString(map) ?: return false, dialog)
            "delegationWanted" -> KolPatronParley.delegationWanted() != null && !KolPatronParley.diktatAtLyra() // that scene collects them
            "dropOff" -> { KolPatronParley.dropOff(); true }
            "gateNext" -> { KolPatronParley.gateNext(dialog, map); true }
            "charterReacted" -> { KolPatronParley.charterReacted(); true }
            "churchWelcomes" -> KolPatron.churchWelcomes()
            "briefRefused" -> { com.fs.starfarer.api.Global.getSector().memoryWithoutUpdate.set("\$kolCh2Patron_briefRefused", true); true }
            "introduce" -> { KolPatron.introduce(power(params, map) ?: return false, dialog.textPanel); true }
            "talked" -> { KolPatronParley.setTalked(params.getOrNull(1)?.getString(map) ?: return false); true }
            "charterHere" -> KolPatron.charterRoute(market) != null
            "charterOpen" -> KolPatron.charterOpen(dialog, map)
            "charterPitch" -> KolPatron.charterPitch(dialog, map)
            "signCharter" -> KolPatron.signCharter(dialog, map)
            "charterOffense" -> { KolPatron.charterOffense(dialog.textPanel); true }
            "pathAdmin" -> KolPatron.pathAdmin(dialog.interactionTarget?.activePerson, market)
            "pathAsk" -> KolPatron.pathAsk(dialog, map)
            "pathTried" -> { KolPatron.pathTried(dialog.textPanel); true }
            "pathMurmur" -> { KolPatron.pathMurmur(dialog.textPanel); true }
            "volturnLament" -> com.fs.starfarer.api.Global.getSector().memoryWithoutUpdate.let {
                it.getBoolean(org.selkie.kol.helpers.KolStaticStrings.KolPatron.VOLTURN_FILES_GIVEN) &&
                        it.getBoolean(org.selkie.kol.helpers.KolStaticStrings.KolCh2.PATRON_SECURED) &&
                        !it.getBoolean("\$kolPatron_volturnLamentSeen")
            }
            "pirateArrangeOpen" -> KolPatron.pirateArrangeOpen(market)
            "pirateArrangeOffer" -> KolPatron.pirateArrangeOffer(dialog, map)
            "pirateAudiencePay" -> KolPatron.pirateAudiencePay(dialog)
            "pirateShuttle" -> KolPatron.pirateShuttle(market)
            "pirateMeet" -> KolPatron.pirateMeet(dialog, map)
            "pirateAgreementOffer" -> KolPatron.pirateAgreementOffer(dialog, map)
            "piratePayAgreement" -> KolPatron.piratePayAgreement(dialog, map)
            "pirateTerms" -> KolPatron.pirateTerms(dialog, map)
            "signPirate" -> KolPatron.signPirate(dialog, map)
            "pirateDelegationGone" -> {
                if (KolPatronParley.delegationAboard()) map[com.fs.starfarer.api.campaign.rules.MemKeys.LOCAL]?.set("\$kolPatron_delegationLeft", true, 0f)
                KolPatronParley.dropOff(); true
            }
            "pirateSwarm" -> { org.selkie.kol.campaign.story.ch2.KolPirateSwarm.begin(); true }
            "swarmActive" -> org.selkie.kol.campaign.story.ch2.KolPirateSwarm.active()
            "swarmRebuff" -> org.selkie.kol.campaign.story.ch2.KolPirateSwarm.rebuffPending()
            "swarmRebuffed" -> { org.selkie.kol.campaign.story.ch2.KolPirateSwarm.rebuffed(dialog.textPanel); true }
            "pirateCut" -> org.selkie.kol.campaign.story.ch2.KolPirateCut.collect(dialog, map)
            "volturnOption" -> { dialog.optionPanel.addOption("[PLACEHOLDER] Look for Mother Standfast", "kolPatron_volturnLament"); true }
            "diktatOfficeIs" -> KolPatronParley.diktatOffice() == params.getOrNull(1)?.getString(map)
            "diktatProposed" -> { KolPatronParley.diktatProposed(dialog.textPanel); true }
            "diktatAtLyra" -> KolPatronParley.diktatAtLyra()
            "diktatDispatched" -> { KolPatronParley.diktatDispatched(dialog); true }
            "diktatMeetingReady" -> KolPatronParley.diktatMeetingReady()
            "collectOpen" -> { KolPatron.ownOpen(); true } // the Lector's tokens
            "collect" -> { KolPatronParley.collect(dialog); true }
            "openMeeting" -> { KolPatronParley.openMeeting(power(params, map) ?: return false, map); true }
            "confirmTooltip" -> {
                val option = params.getOrNull(1)?.getString(map) ?: return false
                dialog.optionPanel.setTooltip(option, "[PLACEHOLDER] " +
                        com.fs.starfarer.api.Global.getSector().memoryWithoutUpdate.getString("\$kolPatron_priceConfirm"))
                true
            }
            "sign" -> {
                val power = power(params, map) ?: return false
                KolPatronParley.sign(power, params.getOrNull(2)?.getString(map) ?: KolPatronParley.leverOf(power), dialog, map)
            }
            "signOwn" -> KolPatron.signOwn(params.getOrNull(1)?.getString(map) ?: "standard", dialog, map)
            "askFleet" -> KolPatron.askFleet(params.getOrNull(1)?.getString(map) ?: "PopulateOptions", dialog, map)
            "congregationHere" -> KolPatron.hasCongregation(market)
            "congregation" -> KolPatron.seekCongregation(dialog, map)
            else -> false
        }
    }

    private fun power(params: MutableList<Misc.Token>, map: MutableMap<String, MemoryAPI>) =
        org.selkie.kol.campaign.story.ch2.KolPatronPower.of(params.getOrNull(1)?.getString(map))
}
