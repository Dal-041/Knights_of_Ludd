package org.selkie.kol.console

import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.selkie.kol.campaign.tech.KolTechData

/**
 * Console Commands: `kol_AddScrip <amount>`. Adds spendable scrip only, for testing Helensis' requisitions.
 * Lifetime scrip and the Technology situation are untouched; advance those by handing over technology.
 * Only loaded by the Console Commands mod (data/console/commands.csv), so the mod doesn't depend on it.
 */
class AddScripCommand : BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (!context.isInCampaign) {
            Console.showMessage("This command can only be used in the campaign.")
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }
        val amount = args.trim().toIntOrNull()
        if (amount == null || amount <= 0) {
            Console.showMessage("Usage: kol_AddScrip <positive amount>")
            return BaseCommand.CommandResult.BAD_SYNTAX
        }
        val data = KolTechData.get()
        data.balance += amount
        Console.showMessage("Added $amount spendable scrip. Balance ${data.balance} (lifetime ${data.lifetimeScrip}, unchanged).")
        return BaseCommand.CommandResult.SUCCESS
    }
}
