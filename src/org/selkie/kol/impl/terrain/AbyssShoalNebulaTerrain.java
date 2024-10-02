package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaDrops;


public class AbyssShoalNebulaTerrain extends NebulaTerrainPlugin {

    @Override
    public String getTerrainName() {
        return "Dawnshoal";
    }

    @Override
    public String getNameForTooltip() {
        return "Coronal Wave Break";
    }

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
            String desc = "Under the Luna Sea";
            //if (fleet.getContainingLocation().getId().equalsIgnoreCase("luna sea")) desc = "Under the Luna Sea";
            fleet.getStats().addTemporaryModMult(0.1f, "LunaSea_1", //getModId() +
                    desc, VISIBLITY_MULT,
                    fleet.getStats().getDetectedRangeMod());

            if (fleet.hasTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG)) {
                return;
            }
            float penalty = 0.2f;
            fleet.getStats().addTemporaryModMult(0.1f, "LunaSea_2", //getModId() +
                    desc, penalty,
                    fleet.getStats().getFleetwideMaxBurnMod());
        }
    }

    @Override
    public boolean hasAIFlag(Object flag, CampaignFleetAPI fleet) {
        if (fleet.hasTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG)) {
            return flag == TerrainAIFlags.REDUCES_DETECTABILITY;
        }
        return flag == TerrainAIFlags.REDUCES_DETECTABILITY ||
                flag == TerrainAIFlags.REDUCES_SPEED_LARGE||
                flag == TerrainAIFlags.TILE_BASED;
    }

}
