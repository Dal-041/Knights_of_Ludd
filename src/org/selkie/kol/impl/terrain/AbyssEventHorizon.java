package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class AbyssEventHorizon extends EventHorizonPlugin {

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            if (entity.hasTag("zea_rulesfortheebutnotforme")) return;
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

    @Override
    public boolean hasAIFlag(Object flag, CampaignFleetAPI fleet) {
        if (!fleet.hasTag(excludeTag)) {
            return flag == TerrainAIFlags.CR_DRAIN ||
                    flag == TerrainAIFlags.BREAK_OTHER_ORBITS ||
                    flag == TerrainAIFlags.EFFECT_DIMINISHED_WITH_RANGE;
        }
        return flag == TerrainAIFlags.BREAK_OTHER_ORBITS;
    }

    //Hasty debug
    @Override
    public boolean hasAIFlag(Object flag) {
        return flag == TerrainAIFlags.EFFECT_DIMINISHED_WITH_RANGE;
    }
}
