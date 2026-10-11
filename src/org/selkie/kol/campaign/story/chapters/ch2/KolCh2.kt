package org.selkie.kol.campaign.story.chapters.ch2

import com.fs.starfarer.api.Global
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.chapters.ch2.ozymandias.OzymandiasKeys
import org.selkie.kol.campaign.story.chapters.ch2.patron.PatronKeys
import org.selkie.kol.campaign.story.situations.tech.milestones.inquest.InquestKeys
import org.selkie.kol.campaign.story.systems.chronicle.ChronicleKeys
import org.selkie.zea.helpers.ZeaStaticStrings


/** Knights Chapter 2 as a whole: flags repaired for older saves. Its features have their own packages. */
object KolCh2Module : KolStoryModule {
    override val id = "ch2"
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /**
     * Saves from before the stage-flag fix lost permanent flags when their missions ended (HubMissions unset the flags
     * they complete on). Restores them from what else was recorded.
     */
    private fun repairFlags() {
        // a patron is recorded at signing; it's secured once the quest completes (at the announcing assembly)
        if (memory.getString(PatronKeys.PATRON) != null && !memory.getBoolean(PatronKeys.PATRON_ACTIVE)) memory.set(PatronKeys.PATRON_SECURED, true)
        if (memory.getString(OzymandiasKeys.CAELI_CHOICE) != null) memory.set(OzymandiasKeys.OZY_DONE, true)
        if (memory.getString(InquestKeys.INQUEST_PLAYER) != null) memory.set(InquestKeys.INQUEST_PLAYER_DONE, true)
        if (memory.getBoolean(ChronicleKeys.CHRON_PREFIX + "ninayaDefeated"))
            memory.set(ZeaStaticStrings.ZeaMemKeys.ZEA_TT_NINAYA_DONE, true)
    }

    override fun onLoad() = repairFlags()
}
