package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;

public class AbyssEventHorizon extends EventHorizonPlugin {

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            if (entity.hasTag("abyss_rulesfortheebutnotforme")) return;
        }
        super.applyEffect(entity, days);
    }
}
