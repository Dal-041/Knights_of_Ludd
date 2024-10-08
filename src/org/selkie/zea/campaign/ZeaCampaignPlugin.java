package org.selkie.zea.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import org.selkie.zea.campaign.interactions.DuskNullStationInteraction;
import org.selkie.zea.campaign.interactions.ElysianHypershuntInteraction;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaEntities;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;

public class ZeaCampaignPlugin extends BaseCampaignPlugin {

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {


        if (interactionTarget.getCustomEntitySpec() != null) {
            CustomEntitySpecAPI spec = interactionTarget.getCustomEntitySpec();
            if (spec.getId().equals(ZeaEntities.ZEA_EDF_CORONAL_TAP)) return new PluginPick<InteractionDialogPlugin>(new ElysianHypershuntInteraction(), PickPriority.HIGHEST);
            if (spec.getId().equals(ZeaEntities.ZEA_NULL_STATION_DUSK)) return new PluginPick<InteractionDialogPlugin>(new DuskNullStationInteraction(), PickPriority.HIGHEST);
        }

       // super.pickInteractionDialogPlugin(interactionTarget)

        return null;
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {

        if (opponent instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) opponent;
            if (fleet.getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_NINAYA_BOSS_FLEET)) {
                return new PluginPick<BattleCreationPlugin>(new NinayaBattleCreationPlugin(), PickPriority.HIGHEST);
            }
        }

        return super.pickBattleCreationPlugin(opponent);
    }
}
