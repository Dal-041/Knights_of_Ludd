package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;

public class AbyssEventHorizon extends EventHorizonPlugin {

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            if (entity.hasTag("abyss_rulesfortheebutnotforme")) return;
        }
        super.applyEffect(entity, days);
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float alpha = viewport.getAlphaMult();
        viewport.setAlphaMult(alpha * 0.8f); //much lower
        super.render(layer, viewport);
        viewport.setAlphaMult(alpha);
    }
}
