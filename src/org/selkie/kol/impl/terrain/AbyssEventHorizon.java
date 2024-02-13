package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class AbyssEventHorizon extends EventHorizonPlugin {

    @Override
    public String getTerrainName() {
        return "HEED THE SIREN'S CALL.";
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        float small = 5f;
        tooltip.addTitle("HEED THE SIREN'S CALL.");
        float nextPad = pad;

        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).getText1(), pad);

        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad);
            nextPad = small;
        }
        tooltip.addPara("Reduces the combat readiness of " +
                "all ships near the event horizon at a steady pace.", nextPad);
        tooltip.addPara("The drive field is also distrupted, making getting away from the event horizon more difficult.", pad);

        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad);
            tooltip.addPara("Reduces the peak performance time of ships and increases the rate of combat readiness degradation in protracted engagements.", small);
        }
    }

    @Override
    public String getNameForTooltip() {
        return "HEED THE SIREN'S CALL.";
    }

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

    //No longer hasty, necessary
    @Override
    public boolean hasAIFlag(Object flag) {
        return flag == TerrainAIFlags.NOT_SUPER_DANGEROUS_UNLESS_GO_SLOW;
    }
}
