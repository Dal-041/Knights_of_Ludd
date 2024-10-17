package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.People
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.selkie.kol.helpers.KolStaticStrings
import java.awt.Color

class MGAIntro : HubMissionWithSearch() {

    enum class Stage {
        VISIT_MGA_HEADQUARTERS,
        COMPLETED,
    }

    protected var sembene: PersonAPI? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        // if already accepted by the player, abort
//        if (!setGlobalReference("\$gaIntro_ref")) {
//            return false
//        }

        sembene = getImportantPerson(KolStaticStrings.MGA_DIRECTOR)
        if (sembene == null) return false

        setStartingStage(Stage.VISIT_MGA_HEADQUARTERS)
        addSuccessStages(Stage.COMPLETED)

        setStoryMission()

        makeImportant(sembene!!.market, null, Stage.VISIT_MGA_HEADQUARTERS)
        setStageOnMemoryFlag(Stage.COMPLETED, sembene!!.market, "\$MGAIntro_completed")

        setRepFactionChangesNone()
        setRepPersonChangesNone()

        return true
    }

    override fun updateInteractionDataImpl() {
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val opad = 10f
        if (currentStage == Stage.VISIT_MGA_HEADQUARTERS) {
            info.addPara("Go to Nomios Rapid Autofab and enquire about the contract.", opad)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage == Stage.VISIT_MGA_HEADQUARTERS) {
            info.addPara("Enquire about the Contract.", tc, pad)
            return true
        }
        return false
    }

    override fun getBaseName(): String {
        return "Go to Nominos Rapid Autofab"
    }

    override fun getPostfixForState(): String {
        if (startingStage != null) {
            return ""
        }
        return super.getPostfixForState()
    }
}