package org.selkie.kol.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolAssembly
import org.selkie.kol.campaign.story.ch2.KolCh2Story
import org.selkie.kol.campaign.story.ch2.KolConvocation
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import org.selkie.kol.helpers.KolStaticStrings.KolPrelude

/**
 * Rule commands for Knights Chapter 2:
 * - `KolCh2CMD startPatron` / `startNinaya` / `startOzymandias`: start the mission from the dialog.
 * - `KolCh2CMD showPerson <grandmaster|inquisitor|agent|bench:<slot>|<person id>>`: show a person's card.
 * - `KolCh2CMD convocationDone`: the convocation is over.
 * - `KolCh2CMD canOfferOwn` (condition): the player's own forces can stand as the Church's patron (at the convocation).
 * - `KolCh2CMD inquestOpen`: publishes the lead Inquisitor's name tokens (`$global.kolPerson_inquisitor_*`).
 * - `KolCh2CMD readOn <trigger>`: plays the best rule on the trigger as a continuation of the reading (KolCh2Story.readOn).
 * - `KolCh2CMD convOpen`: opens the convocation (bench voices and world state, `$global.kolConv_*`).
 * - `KolCh2CMD convOath <sworn|lie>`: the oath for offering the player's own forces.
 * - `KolCh2CMD convEffects`: settles what the player said at the convocation.
 * - `KolCh2CMD ninayaBounty`: Ninaya was beaten before the operation; pay the bounty, carry half to the next one.
 * - `KolCh2CMD inspect`: the inquest's inspection of the player's fleet (publishes `$global.kolInquest_*`).
 * - `KolCh2CMD inquestAnswer <candid|guarded|defiant>`: records the interview, then the verdict scene.
 * - `KolCh2CMD inquestResolve <none|surrender|refuse>`: settles the inquest, then the outcome scene.
 */
class KolCh2CMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "startPatron" -> KolCh2Story.start(KolCh2.PATRON_ID, KolCh1.GREENFLIGHT_ID, dialog) != null
            "startNinaya" -> KolCh2Story.start(KolCh2.NINAYA_ID, KolCh1.GREENFLIGHT_ID, dialog) != null
            "startOzymandias" -> KolCh2Story.start(KolCh2.OZY_ID, KolPrelude.ENARMS_ID, dialog) != null
            "showPerson" -> {
                val who = params.getOrNull(1)?.getString(map) ?: return false
                val person = when (who) {
                    "grandmaster" -> Global.getSector().importantPeople.getPerson(KolCh2.GRANDMASTER_ID)
                    "inquisitor" -> KolCh2Story.inquisitor("lead")
                    "agent" -> KolCh2Story.agent()
                    else -> if (who.startsWith("bench:")) KolConvocation.bench(who.removePrefix("bench:"))
                            else Global.getSector().importantPeople.getPerson(who)
                } ?: return false
                dialog.visualPanel.showPersonInfo(person, true)
                true
            }
            "ninayaBounty" -> { KolCh2Story.alreadyDoneBounty(KolCh2.NINAYA_OP_DONE, dialog); true }
            "inspect" -> { KolCh2Story.inspect(); true }
            "inquestAnswer" -> {
                KolCh2Story.answerInquest(params.getOrNull(1)?.getString(map) ?: "guarded")
                com.fs.starfarer.api.impl.campaign.rulecmd.FireBest.fire(null, dialog, map, "KolCh2InquestVerdict")
                true
            }
            "inquestResolve" -> {
                KolCh2Story.resolveInquest(params.getOrNull(1)?.getString(map) ?: "none", dialog)
                com.fs.starfarer.api.impl.campaign.rulecmd.FireBest.fire(null, dialog, map, "KolCh2InquestOutcome")
                true
            }
            "canOfferOwn" -> KolCh2Story.canOfferOwn()
            "inquestOpen" -> {
                KolConvocation.publishPerson("\$kolPerson_inquisitor_", KolCh2Story.inquisitor("lead"))
                Global.getSector().memoryWithoutUpdate.unset("\$kolInquest_readCount")
                true
            }
            "readOn" -> KolCh2Story.readOn(params.getOrNull(1)?.getString(map) ?: return false, ruleId, dialog, map)
            "convOpen" -> { KolConvocation.open(); true }
            "convOath" -> { KolConvocation.oath(params.getOrNull(1)?.getString(map) == "lie", dialog); true }
            "convEffects" -> { KolConvocation.effects(dialog); true }
            "convocationDone" -> {
                Global.getSector().memoryWithoutUpdate.set(KolCh2.CONVOCATION_DONE, true)
                KolAssembly.report("convocation")
                true
            }
            else -> false
        }
    }
}
