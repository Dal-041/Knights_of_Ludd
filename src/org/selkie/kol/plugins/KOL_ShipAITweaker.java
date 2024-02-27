package org.selkie.kol.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import org.selkie.kol.helpers.KOLStaticStrings;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class KOL_ShipAITweaker {
    public static PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship.isFighter()) return null;
        String hullId = ship.getHullSpec().getBaseHullId();

        if (!Arrays.asList(KOLStaticStrings.knightsShips).contains(hullId) && !Arrays.asList(ZeaUtils.zeaSupportShips).contains(hullId)) return null;

        List<String> supportHulls = new ArrayList<>(5);
        supportHulls.add("kol_mimosa");
        supportHulls.add("kol_lotus");
        supportHulls.add("zea_edf_kiyohime"); //Helps when the player gets them and can only use reckless cores
        supportHulls.add("zea_edf_mizuchi");
        supportHulls.add("zea_dawn_tianma");
        List<String> assaultHulls = new ArrayList<>(3);
        assaultHulls.add("kol_larkspur");
        assaultHulls.add("kol_alysse");
        assaultHulls.add("kol_snowdrop");

        //HullSize size = ship.getHullSize();

        ShipAIConfig config = new ShipAIConfig();
        config.alwaysStrafeOffensively = true;
        config.backingOffWhileNotVentingAllowed = true;
        config.turnToFaceWithUndamagedArmor = true;
        config.burnDriveIgnoreEnemies = true;

        boolean carrier = false;
        boolean support = false;
        boolean assault = false;
        if (ship.getVariant() != null) {
            carrier = ship.getVariant().isCarrier() && !ship.getVariant().isCombat();
        }
        if (supportHulls.contains(hullId)) {
            support = true;
        }
        if (assaultHulls.contains(hullId)) {
            assault = true;
        }

        //Bump toward target personality 1 step
        if (ship.getCaptain() != null) {
            if (carrier || support) {
                if (ship.getCaptain().isDefault() || ship.getCaptain().isPlayer()) config.personalityOverride = Personalities.CAUTIOUS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.RECKLESS)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.STEADY)) config.personalityOverride = Personalities.CAUTIOUS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.TIMID)) config.personalityOverride = Personalities.CAUTIOUS;
            } else if (assault) {
                if (ship.getCaptain().isDefault() || ship.getCaptain().isPlayer()) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.CAUTIOUS)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE)) config.personalityOverride = Personalities.RECKLESS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.STEADY)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.TIMID)) config.personalityOverride = Personalities.CAUTIOUS;
            }
        }

        return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
}
