package org.selkie.kol.campaign.story.chapters.ch2.jointops

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly

/** Joint operations: what an operation already done carries forward. */
object KolJointOps {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** Share of an operation's strength carried to the next operation (a float in sector memory). */
    const val JOINT_OP_CARRY = "\$kolJointOp_carry"

    /**
     * The player beat the operation's boss before the Order could launch: half of what was set aside is paid to them
     * as a bounty, half strengthens the next operation; the operation counts as done.
     */
    fun alreadyDoneBounty(doneFlag: String, dialog: InteractionDialogAPI) {
        if (memory.getBoolean(doneFlag)) return
        val credits = KolStorySettings.alreadyDoneBountyCredits
        Global.getSector().playerFleet.cargo.credits.add(credits.toFloat())
        AddRemoveCommodity.addCreditsGainText(credits, dialog.textPanel)
        memory.set(JOINT_OP_CARRY, memory.getFloat(JOINT_OP_CARRY) + 0.5f)
        memory.set(doneFlag, true)
        KolAssembly.report("jointOperation")
    }
}
