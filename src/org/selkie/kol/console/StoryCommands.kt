package org.selkie.kol.console

import com.fs.starfarer.api.Global
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console
import org.selkie.kol.campaign.story.KolAssembly
import org.selkie.kol.campaign.story.KolAssemblyData
import org.selkie.kol.campaign.story.KolChapter
import org.selkie.kol.campaign.story.KolChapterRequirements
import org.selkie.kol.campaign.story.KolChronicle
import org.selkie.kol.campaign.story.KolDesertion
import org.selkie.kol.campaign.story.KolDesertionData

// Console Commands (data/console/commands.csv) for testing the story foundations. Loaded only by that mod.

private fun campaignOnly(context: CommandContext): CommandResult? {
    if (context.isInCampaign) return null
    Console.showMessage("This command can only be used in the campaign.")
    return CommandResult.WRONG_CONTEXT
}

/** `kol_Chronicle` lists facts and grades; `kol_Chronicle <fact>` records a fact; `kol_Chronicle <thread> <grade>` sets a grade. */
class ChronicleCommand : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        campaignOnly(context)?.let { return it }
        val parts = args.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.isEmpty() -> {
                KolChronicle.check()
                val memory = Global.getSector().memoryWithoutUpdate
                val facts = memory.keys.filter { it.startsWith("\$kolChron_") && !it.endsWith("_day") && !it.endsWith("_chapter") }.sorted()
                Console.showMessage("Chapter ${KolChapter.get()}. Grades: " +
                        KolChronicle.THREADS.joinToString { "$it ${KolChronicle.grade(it)}" })
                Console.showMessage("Facts: " + facts.joinToString { it.removePrefix("\$kolChron_") + "=" + memory.get(it) })
                Console.showMessage("Visited sealable systems: " + KolChronicle.visitedSealable().joinToString())
            }
            parts.size == 2 && parts[0] in KolChronicle.THREADS -> {
                val grade = parts[1].toIntOrNull() ?: return CommandResult.BAD_SYNTAX
                Global.getSector().memoryWithoutUpdate.set(KolChronicle.key("${parts[0]}_grade"), grade)
                Console.showMessage("Set ${parts[0]} grade to $grade.")
            }
            parts.size == 1 -> {
                Console.showMessage(if (KolChronicle.record(parts[0])) "Recorded ${parts[0]}." else "${parts[0]} was already recorded.")
            }
            else -> return CommandResult.BAD_SYNTAX
        }
        return CommandResult.SUCCESS
    }
}

/** `kol_Assembly [now|chapter|status]`: open the next assembly now, as the chapter assembly, or show the schedule. */
class AssemblyCommand : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        campaignOnly(context)?.let { return it }
        val data = KolAssembly.data()
        when (args.trim().lowercase()) {
            "", "status" -> {
                val chapter = KolChapter.get()
                Console.showMessage("Chapter $chapter; state ${data.state}; important ${data.important}; unique ${data.unique}; " +
                        "chapter ready ${KolAssembly.chapterReady()}; pending ${data.pending}")
                if (data.nextSit != 0L) Console.showMessage("  sits ${KolAssembly.dateOf(KolAssembly.sitTime())} to " +
                        "${KolAssembly.dateOf(KolAssembly.closeTime())}; then ${KolAssembly.dateOf(KolAssembly.sitTime(1))}")
                for ((key, entry) in KolChapterRequirements.entries(chapter)) {
                    Console.showMessage("  $key: valid ${entry.valid()}, done ${entry.done()}")
                }
            }
            "now" -> { KolAssembly.forceNow(false); Console.showMessage("Assembly open at Star Keep Lyra (${data.state}).") }
            "chapter" -> { KolAssembly.forceNow(true); Console.showMessage("Chapter assembly open at Star Keep Lyra (${data.state}).") }
            else -> return CommandResult.BAD_SYNTAX
        }
        if (data.state != KolAssemblyData.State.SITTING && args.trim().lowercase() in listOf("now", "chapter")) {
            Console.showMessage("Note: assemblies begin in Chapter 2 (current ${KolChapter.get()}).")
        }
        return CommandResult.SUCCESS
    }
}

/** `kol_Chapter <n>` sets the story chapter; `kol_Chapter veto <n>` / `unveto <n>` toggles a chapter's conditions. */
class ChapterCommand : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        campaignOnly(context)?.let { return it }
        val parts = args.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.size == 1 && parts[0].toIntOrNull() != null -> {
                KolChapter.set(parts[0].toInt())
                Console.showMessage("Chapter set to ${parts[0]}.")
            }
            parts.size == 2 && parts[0] == "veto" -> { KolChapterRequirements.vetoed.add(parts[1].toIntOrNull() ?: return CommandResult.BAD_SYNTAX); Console.showMessage("Chapter ${parts[1]} vetoed.") }
            parts.size == 2 && parts[0] == "unveto" -> { KolChapterRequirements.vetoed.remove(parts[1].toIntOrNull() ?: return CommandResult.BAD_SYNTAX); Console.showMessage("Chapter ${parts[1]} unvetoed.") }
            else -> return CommandResult.BAD_SYNTAX
        }
        return CommandResult.SUCCESS
    }
}

/** `kol_Desertion [amount]`: adds to the desertion budget, or shows it. */
class DesertionCommand : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        campaignOnly(context)?.let { return it }
        val data = KolDesertionData.get()
        val text = args.trim()
        if (text.isNotEmpty()) data.budget += text.toFloatOrNull() ?: return CommandResult.BAD_SYNTAX
        Console.showMessage("Desertion budget ${data.budget}; monthly rate ${KolDesertion.monthlyRate()}; " +
                "total ${data.total} ships in ${data.fleetsUpgraded} fleets.")
        return CommandResult.SUCCESS
    }
}
