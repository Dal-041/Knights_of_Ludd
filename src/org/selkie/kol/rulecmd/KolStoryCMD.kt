package org.selkie.kol.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolAssembly
import org.selkie.kol.campaign.story.KolChapter
import org.selkie.kol.campaign.story.KolDockEvents
import org.selkie.kol.campaign.story.KolDutiesBoard
import org.selkie.kol.helpers.KolStaticStrings.KolStory

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
            "assemblyChapterScene" -> FireBest.fire(null, dialog, map, KolStory.TRIGGER_ASSEMBLY_CHAPTER + (KolChapter.get() + 1))
            "assemblyAdvance" -> { KolAssembly.advanceChapter(); true }
            "dutiesAvailable" -> dialog.interactionTarget?.market?.let {
                com.fs.starfarer.api.Global.getSector().importantPeople.getPerson(KolDutiesBoard.personId(it)) != null
            } ?: false
            "dutiesOpen" -> KolDutiesBoard.open(dialog)
            "dutiesClose" -> { KolDutiesBoard.close(dialog); true }
            else -> false
        }
    }
}
