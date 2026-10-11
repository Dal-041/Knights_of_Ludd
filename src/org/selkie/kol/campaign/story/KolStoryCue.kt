package org.selkie.kol.campaign.story

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import java.awt.Color

/**
 * Cues the player to a story mission they haven't started, the vanilla way: its giver is marked important, and a
 * notice in the intel points to them. Both clear once the mission is running or done, or the story is severed.
 * Checked when a cue may become due (the patron's announcement) and at each load.
 */
object KolStoryCue {
    class Cue(val personId: String, val title: String, val text: String, val step: String, val wanted: () -> Boolean)

    private val memory get() = Global.getSector().memoryWithoutUpdate

    private val cues: Map<String, Cue> = mapOf(
        // after the patron's announcement: Ozymandias, if it hasn't been taken up
        "ozymandias" to Cue(KolStaticStrings.KolPrelude.ENARMS_ID, "Knights: Brother Enarms Asks for You",
            "[PLACEHOLDER] With the patron announced, Brother Enarms has asked to see you at Cygnus. The Order has work for you beyond the assembly hall.",
            "Speak with Brother Enarms at Cygnus") {
            memory.getBoolean(KolCh2.PATRON_SECURED) && !memory.getBoolean(KolCh2.OZY_ACTIVE) &&
                    !memory.getBoolean(KolCh2.OZY_DONE) && !KolSeverance.severed()
        },
        // some days after ARMORY: the early Ordained rollout, until the player answers
        "ordainedEarly" to Cue(KolStaticStrings.KolCh1.HELENSIS_ID, "Knights: Master Helensis Asks for You",
            "[PLACEHOLDER] Master Helensis has sent word to your fleet: she asks that you call on her at Battlestation Cygnus " +
                    "at your earliest convenience. The message does not say why.",
            "Speak with Master Helensis at Cygnus") { org.selkie.kol.campaign.tech.KolOrdainedEarly.due() },
    )

    fun cue(key: String): Cue? = cues[key]

    fun wanted(key: String): Boolean = cues[key]?.wanted?.invoke() == true

    fun person(key: String): PersonAPI? = cues[key]?.let { Global.getSector().importantPeople.getPerson(it.personId) }

    /** Posts every cue that's due and not already shown. */
    @JvmStatic
    fun check() {
        val shown = Global.getSector().intelManager.getIntel(KolStoryCueIntel::class.java).map { (it as KolStoryCueIntel).key }.toSet()
        for (key in cues.keys) {
            if (key in shown || !wanted(key)) continue
            person(key)?.let { Misc.makeImportant(it, reason(key)) }
            Global.getSector().intelManager.addIntel(KolStoryCueIntel(key))
        }
    }

    fun clear(key: String) {
        person(key)?.let { Misc.makeUnimportant(it, reason(key)) }
    }

    private fun reason(key: String) = "kolStoryCue_$key"
}

/** A cue's notice: who to see and where. Removed (and its marker cleared) once the cue is no longer wanted. */
class KolStoryCueIntel(val key: String) : BaseIntelPlugin() {
    override fun shouldRemoveIntel(): Boolean {
        if (isEnded || !KolStoryCue.wanted(key)) {
            KolStoryCue.clear(key)
            return true
        }
        return false
    }

    override fun getName(): String = KolStoryCue.cue(key)?.title ?: "Knights"

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode, isUpdate: Boolean, tc: Color, initPad: Float) {
        val cue = KolStoryCue.cue(key) ?: return
        bullet(info)
        info.addPara(cue.step, tc, initPad)
        unindent(info)
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val cue = KolStoryCue.cue(key) ?: return
        KolStoryCue.person(key)?.let { info.addImage(it.portraitSprite, width, 128f, 0f) }
        info.addPara(cue.text, 10f)
    }

    override fun getIcon(): String = KolStoryCue.person(key)?.portraitSprite
        ?: Global.getSector().getFaction(KolStaticStrings.kolFactionID).crest

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_STORY)
        tags.add(KolStaticStrings.kolFactionID)
        return tags
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? = KolStoryCue.person(key)?.market?.primaryEntity
}
