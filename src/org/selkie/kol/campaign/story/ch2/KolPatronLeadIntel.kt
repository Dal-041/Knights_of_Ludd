package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import org.selkie.kol.helpers.KolStaticStrings

/**
 * One branch of the patron quest: a lead and how far the player has followed it. The parent mission (KolCh2Patron)
 * has one stage; each branch keeps its own state and map location, and shows its intel in the dialog when found or
 * advanced. Modeled on vanilla's Pilgrim's Path (LuddicPilgrimsPath with one LuddicShrineIntel per shrine).
 */
class KolPatronLeadIntel(val key: String) : BaseIntelPlugin() {
    /** A parley's stage (lead, introduced, received, audience, agreed, delegation, signed); other routes: lead, tried. */
    var stage: String = "lead"
        private set

    private val power get() = KolPatronPower.of(key)

    /** Moves the branch on, and shows its intel in the dialog (or as a notification without one). */
    fun advance(newStage: String, text: TextPanelAPI?) {
        if (stage == newStage) return
        stage = newStage
        refreshStep()
        show(text)
    }

    /**
     * When the branch began ending (the quest is over). Intel isn't advanced, so a delayed end never counts down on its
     * own: the delay is checked here, as vanilla's fleet log does (FleetLogIntel.shouldRemoveIntel).
     */
    private var endingSince: Long? = null

    override fun shouldRemoveIntel(): Boolean {
        if (super.shouldRemoveIntel()) return true
        if (!isEnding) return false
        val since = endingSince ?: Global.getSector().clock.timestamp.also { endingSince = it } // saves from before
        return Global.getSector().clock.getElapsedDaysSince(since) >= DAYS_AFTER_END
    }

    /** The entity marked important for this branch's next step (KolPatronParley.stepPlace), if any. */
    private var stepMarked: SectorEntityToken? = null

    /** Moves the step marker to where the branch now leads, or clears it. */
    fun refreshStep() {
        val place = if (isEnding || isEnded) null else power?.let { KolPatronParley.stepPlace(it) }?.primaryEntity
        if (place === stepMarked) return
        stepMarked?.let { Misc.makeUnimportant(it, "kolPatron_step_$key") }
        place?.let { Misc.makeImportant(it, "kolPatron_step_$key") }
        stepMarked = place
    }

    /** A second route joined the branch (the mercenaries): shown like an advance. */
    fun routeAdded(text: TextPanelAPI?) = show(text)

    private fun show(text: TextPanelAPI?) {
        if (text != null) Global.getSector().intelManager.addIntelToTextPanel(this, text)
        else sendUpdateIfPlayerHasIntel(null, false)
    }

    /** The leads this branch holds: the mercenaries' two routes, else the branch's own lead. */
    private fun leads(): List<String> = if (key == CHARTER) listOf(CHARTER, TRITACH).filter { KolPatron.hasLead(it) } else listOf(key)

    // --- the list entry and description ------------------------------------------------------------------------

    override fun getName(): String = when (key) {
        CHARTER -> "Patron: Mercenaries"
        PLAYER -> "Patron: Your Own Forces"
        PIRATE -> "Patron: Pirate Protection"
        PATH -> "Patron: the Luddic Path"
        else -> "Patron: " + (power?.faction?.displayName ?: key)
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: ListInfoMode) {
        val pre = if (mode == ListInfoMode.MESSAGES && stage == "lead") "New lead: " else ""
        info.addPara(pre + name, getTitleColor(mode), 0f)
        addBulletPoints(info, mode)
    }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: ListInfoMode) {
        val tc = getBulletColorForMode(mode)
        val initPad = if (mode == ListInfoMode.IN_DESC) 10f else 3f
        bullet(info)
        var pad = initPad
        if (key == CHARTER) {
            // the routes known so far, on one line: "New Maxios / Cethlenn", or either alone
            val places = leads().mapNotNull { KolPatron.place(it)?.name }
            if (places.isNotEmpty()) {
                info.addPara("At " + places.joinToString(" / "), pad, tc, Misc.getHighlightColor(), *places.toTypedArray())
                pad = 0f
            }
        } else where()?.let {
            info.addPara("At %s", pad, tc, Misc.getHighlightColor(), it.name)
            pad = 0f
        }
        info.addPara(stageLabel(), tc, pad)
        unindent(info)
    }

    /** [PLACEHOLDER] labels for each stage, shown under the branch's title. */
    private fun stageLabel(): String = when (power) {
        KolPatronPower.LEAGUE -> KolPatronParley.leagueLabel(stage)
        KolPatronPower.HEGEMONY -> KolPatronParley.hegemonyLabel(stage)
        KolPatronPower.DIKTAT -> KolPatronParley.diktatLabel(stage)
        else -> null
    } ?: when (stage) {
        "lead" -> when {
            key == PLAYER -> "Settle the details with Sister Greenflight and the Lector"
            power != null -> "Not yet approached"
            else -> "Not yet pursued"
        }
        "introduced" -> "Introduced"
        "received" -> "Received by the gatekeeper"
        "audience" -> "Audience granted"
        "agreed" -> "Agreed in principle; the Order's delegation is needed"
        "delegation" -> "The delegation is aboard"
        "signed" -> "Signed"
        "tried" -> if (key == PATH) "Refused at Chalcedon" else "Tried"
        "closed" -> "Closed: the Church has found its patron"
        else -> stage
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        for (lead in leads()) KolPatron.leadLine(lead)?.let { info.addPara("[PLACEHOLDER] $it", 10f) }
        if (power == KolPatronPower.DIKTAT) KolPatronParley.diktatTerms()?.let { info.addPara("[PLACEHOLDER] Balashi's terms: $it", 10f) }
        addBulletPoints(info, ListInfoMode.IN_DESC)
        addShowLeadsButton(this, width, height, info)
    }

    // --- placement ---------------------------------------------------------------------------------------------

    /** The seat for a power, the recorded place for the pirate and Path leads; none yet for the others. */
    private fun where() = power?.seat() ?: leads().firstNotNullOfOrNull { KolPatron.place(it) }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? =
        power?.let { KolPatronParley.stepPlace(it) }?.primaryEntity ?: where()?.primaryEntity

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_STORY)
        tags.add(KolStaticStrings.kolFactionID)
        return tags
    }

    override fun getIcon(): String = factionForIcon().crest

    private fun factionForIcon(): FactionAPI = Global.getSector().getFaction(power?.factionId ?: when (key) {
        CHARTER -> Factions.MERCENARY
        PLAYER -> Factions.PLAYER
        PIRATE -> Factions.PIRATES
        PATH -> Factions.LUDDIC_PATH
        else -> Factions.INDEPENDENT
    })

    override fun getFactionForUIColors(): FactionAPI = factionForIcon()

    override fun getSortString(): String = "Patron " + (KolPatronPower.values().indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: 9) + name

    override fun getCommMessageSound(): String = "ui_discovered_entity"

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI) {
        if (buttonId == BUTTON_SHOW_LEADS) {
            toggleLeadList(this, ui)
            return
        }
        super.buttonPressConfirmed(buttonId, ui)
    }

    companion object {
        const val CHARTER = KolPatron.CHARTER
        const val TRITACH = KolPatron.TRITACH
        const val PIRATE = KolPatron.PIRATE
        const val PATH = KolPatron.PATH
        const val PLAYER = KolPatron.PLAYER

        const val BUTTON_SHOW_LEADS = "kolPatron_showLeads"

        private val intelManager get() = Global.getSector().intelManager

        fun all(): List<KolPatronLeadIntel> = intelManager.getIntel(KolPatronLeadIntel::class.java).map { it as KolPatronLeadIntel }

        fun get(key: String): KolPatronLeadIntel? = all().firstOrNull { it.key == key }

        /** Adds the branch if it's missing: shown in the dialog when [text] is given, silent when [quiet]. */
        fun add(key: String, text: TextPanelAPI?, quiet: Boolean): KolPatronLeadIntel {
            get(key)?.let { return it }
            val intel = KolPatronLeadIntel(key)
            intelManager.addIntel(intel, quiet, text)
            return intel
        }

        /** Ends every branch (the quest is over). */
        fun endAll() = all().forEach {
            if (!it.isEnding && !it.isEnded) {
                it.endAfterDelay()
                it.endingSince = Global.getSector().clock.timestamp
            }
            it.refreshStep()
        }

        private const val DAYS_AFTER_END = 3f

        /** "Show leads" / "Go back", as the Pilgrim's Path shows its shrines: the parent and its branches together. */
        fun addShowLeadsButton(curr: IntelInfoPlugin, width: Float, height: Float, info: TooltipMakerAPI) {
            if (!intelManager.hasIntelOfClass(KolCh2Patron::class.java)) return
            val ui = info.intelUI ?: return
            if (!ui.isShowingCustomIntelSubset && curr is KolPatronLeadIntel) return
            val text = if (ui.isShowingCustomIntelSubset) "Go back" else "Show leads"
            info.addSpacer(height - info.heightSoFar - 20f - 20f)
            (curr as BaseIntelPlugin).addGenericButton(info, width, text, BUTTON_SHOW_LEADS).setShortcut(Keyboard.KEY_T, true)
        }

        fun toggleLeadList(curr: IntelInfoPlugin, ui: IntelUIAPI) {
            val parent = intelManager.getFirstIntel(KolCh2Patron::class.java)
            if (ui.isShowingCustomIntelSubset) {
                ui.updateIntelList(true)
                ui.updateUIForItem(curr)
                if (parent != null) ui.selectItem(parent)
            } else {
                val show = ArrayList<IntelInfoPlugin>()
                if (parent != null) show.add(parent)
                show.addAll(all())
                intelManager.sortIntel(show)
                ui.updateIntelList(true, show)
                ui.updateUIForItem(curr)
            }
        }
    }
}
