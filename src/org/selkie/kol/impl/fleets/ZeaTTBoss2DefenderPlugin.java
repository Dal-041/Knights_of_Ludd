package org.selkie.kol.impl.fleets;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SalvageDefenderModificationPlugin;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareDarkDeeds;

import static org.selkie.kol.impl.world.PrepareDarkDeeds.DEFEATED_NINMAH_KEY;

public class ZeaTTBoss2DefenderPlugin extends BaseGenericPlugin implements SalvageDefenderModificationPlugin {

    public float getStrength(SDMParams p, float strength, Random random, boolean withOverride) {
        // doesn't matter, just something non-zero so we end up with a fleet
        // the auto-generated fleet will get replaced by this anyway
        return strength;
    }
    public float getMinSize(SDMParams p, float minSize, Random random, boolean withOverride) {
        return minSize;
    }

    public float getMaxSize(SDMParams p, float maxSize, Random random, boolean withOverride) {
        return maxSize;
    }

    public float getProbability(SDMParams p, float probability, Random random, boolean withOverride) {
        return probability;
    }

    public void reportDefeated(SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
        Global.getSector().getMemoryWithoutUpdate().set(DEFEATED_NINMAH_KEY, true);
    }

    public void modifyFleet(SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {

        //Misc.addDefeatTrigger(fleet, "PK14thDefeated");

        fleet.setNoFactionInName(true);
        fleet.setName("Unidentified Vessel");

        fleet.getFleetData().clear();
        fleet.getFleetData().setShipNameRandom(random);


        FleetMemberAPI member = fleet.getFleetData().addFleetMember("zea_boss_ninmah_Undoer");
        member.setShipName("TTS Ninmah");
        member.setId("tt2boss_" + random.nextLong());

        Map<String, Integer> skills = new HashMap<>();
        skills.put(Skills.HELMSMANSHIP, 2);
        skills.put(Skills.COMBAT_ENDURANCE, 2);
        skills.put(Skills.IMPACT_MITIGATION, 2);
        skills.put(Skills.DAMAGE_CONTROL, 2);
        skills.put(Skills.FIELD_MODULATION, 2);
        skills.put(Skills.TARGET_ANALYSIS, 2);
        skills.put(Skills.SYSTEMS_EXPERTISE, 2);
        skills.put(Skills.ENERGY_WEAPON_MASTERY, 2);

        PersonAPI TT2BossCaptain = MagicCampaign.createCaptainBuilder(Factions.TRITACHYON)
                .setIsAI(true)
                .setAICoreType("alpha_core")
                .setPortraitId("zea_boss_alphaplus")
                .setLevel(8)
                .setFirstName("Alpha")
                .setLastName("(+)")
                .setGender(FullName.Gender.ANY)
                .setPersonality(Personalities.AGGRESSIVE)
                .setSkillLevels(skills)
                .create();

        TT2BossCaptain.getStats().setSkipRefresh(true);
        TT2BossCaptain.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        TT2BossCaptain.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
        TT2BossCaptain.getStats().setSkipRefresh(false);

        member.setCaptain(TT2BossCaptain);
        fleet.setCommander(TT2BossCaptain);

        for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
        }

        fleet.getFlagship().getVariant().addTag(ZeaUtils.BOSS_TAG);
        fleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
        fleet.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getMemoryWithoutUpdate().set(ZeaUtils.BOSS_TAG, true);
    }


    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SDMParams)) return 0;
        SDMParams p = (SDMParams) params;

        if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(
                PrepareDarkDeeds.TTBOSS2_STATION_KEY)) {
            return 2;
        }
        return -1;
    }
    public float getQuality(SDMParams p, float quality, Random random, boolean withOverride) {
        return quality;
    }
}



