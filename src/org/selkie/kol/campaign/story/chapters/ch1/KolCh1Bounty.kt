package org.selkie.kol.campaign.story.chapters.ch1

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator
import org.selkie.kol.campaign.story.generic.KolCustomBounty

/**
 * Knights Chapter 1: the make-work bounty Enarms gives while he arranges the council ("banal but critical":
 * pirates raiding the ore lighters that supply Cygnus' yards). Offered with a low/normal/high choice from
 * rules.csv (kolCh1_bounty*). Completion sets the permanent $kolCh1_bountyComplete, which opens the council,
 * and starts KolCh1Return, the intel that points the player back to Enarms.
 */
class KolCh1Bounty : KolCustomBounty() {

    override fun getCreators(): MutableList<CustomBountyCreator> = mutableListOf(KolCh1PirateCreator())

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        Global.getSector().memoryWithoutUpdate.set(Ch1Keys.BOUNTY_COMPLETE, true)
        KolCh1Return.Companion.start() // the thread back to Cygnus, where the council is waiting
    }
}
