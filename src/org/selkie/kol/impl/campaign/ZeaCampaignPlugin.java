package org.selkie.kol.impl.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import org.selkie.kol.impl.campaign.interactions.ElysianHypershuntInteraction;

public class ZeaCampaignPlugin extends BaseCampaignPlugin {

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {


        if (interactionTarget.getCustomEntitySpec() != null) {
            CustomEntitySpecAPI spec = interactionTarget.getCustomEntitySpec();
            if (spec.getId().equals("zea_edf_coronal_tap")) return new PluginPick<InteractionDialogPlugin>(new ElysianHypershuntInteraction(), PickPriority.HIGHEST);
        }

       // super.pickInteractionDialogPlugin(interactionTarget)

        return null;
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {

        if (opponent instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
            if (fleet.getMemoryWithoutUpdate().contains("$zea_ninaya")) {
                return new PluginPick<BattleCreationPlugin>(new NinayaBattleCreationPlugin(), PickPriority.HIGHEST);
            }
        }

        return super.pickBattleCreationPlugin(opponent);
    }
}
