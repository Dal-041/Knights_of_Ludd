package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch

class MGAIntro : HubMissionWithSearch() {


    enum class Stage {
        VISIT_MGA_HEADQUARTERS,
        COMPLETED,
    }

    protected var amadou: PersonAPI? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        TODO("Not yet implemented")
    }
}