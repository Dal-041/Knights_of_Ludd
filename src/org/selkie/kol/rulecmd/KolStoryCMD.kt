package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.core.KolDialog
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.systems.chapter.KolChapter
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.systems.dock.KolDockEvents
import org.selkie.kol.campaign.story.systems.duties.KolDutiesBoard
import org.selkie.kol.campaign.story.systems.assembly.AssemblyKeys

/**
 * Rule commands for the story foundations:
 * - `KolStoryCMD dockPending` (condition): a dock event is pending at this market and not yet played this dock.
 * - `KolStoryCMD dockNext`: plays the next pending dock event here, or hands the dock back to the market.
 * - `KolStoryCMD assemblyOpen` (condition): an assembly is sitting at Lyra and the player has not attended it.
 * - `KolStoryCMD assemblyImportant` (condition): the assembly in session is the convocation or a chapter assembly.
 * - `KolStoryCMD assemblyUnique` (condition): the assembly in session is a chapter assembly.
 * - `KolStoryCMD assemblySit`: the player attends the assembly; publishes the reckoning (`$global.kolAssembly_<key>`).
 * - `KolStoryCMD assemblyChapterScene`: plays the chapter scene, `KolAssemblyChapter<N>` for the chapter being entered.
 * - `KolStoryCMD assemblyAdvance`: the chapter assembly advances the chapter.
 * - `KolStoryCMD dutiesAvailable` (condition): this market has a duties board.
 * - `KolStoryCMD dutiesOpen` / `dutiesClose`: make the market's duties-board person active / inactive.
 * - `KolStoryCMD showPerson <grandmaster|inquisitor|agent|bench:<slot>|<person id>>`: show a person's card.
 * - `KolStoryCMD readOn <trigger>`: plays the best rule on the trigger as a continuation of a reading (KolDialog.readOn).
 * - `KolStoryCMD approve <personId | bench:slot> <points>`: a named character's approval of the player changes
 *   (their own standing; vanilla's message is printed).
 */
class KolStoryCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        return when (params?.getOrNull(0)?.getString(map)) {
            "dockPending" -> KolDockEvents.get().hasPending(dialog)
            "dockNext" -> { KolDockEvents.get().playNext(dialog, map); true }
            "assemblyOpen" -> KolAssembly.isOpen()
            "assemblyImportant" -> KolAssembly.data().important
            "assemblyUnique" -> KolAssembly.data().unique
            "assemblySit" -> { KolAssembly.attended(); true }
            "assemblyChapterScene" -> FireBest.fire(null, dialog, map, AssemblyKeys.TRIGGER_ASSEMBLY_CHAPTER + (KolChapter.get() + 1))
            "assemblyAdvance" -> { KolAssembly.advanceChapter(); true }
            "dutiesAvailable" -> dialog.interactionTarget?.market?.let {
                KolPeople.get(KolDutiesBoard.personId(it)) != null
            } ?: false
            "dutiesOpen" -> KolDutiesBoard.open(dialog)
            "dutiesClose" -> { KolDutiesBoard.close(dialog); true }
            "showPerson" -> KolDialog.showPerson(dialog, params.getOrNull(1)?.getString(map) ?: return false)
            "readOn" -> KolDialog.readOn(params.getOrNull(1)?.getString(map) ?: return false, ruleId, dialog, map)
            "approve" -> {
                val id = params.getOrNull(1)?.getString(map) ?: return false
                val points = params.getOrNull(2)?.getString(map)?.toIntOrNull() ?: return false
                KolPeople.approve(KolPeople.byId(id), points, dialog.textPanel)
                true
            }
            else -> false
        }
    }
}
