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

        if (!Arrays.asList(KOLStaticStrings.knightsShips).contains(hullId) && !Arrays.asList(ZeaUtils.zeaAIOverrideShips).contains(hullId)) return null;

        //HullSize size = ship.getHullSize();

        ShipAIConfig config = new ShipAIConfig();
        config.alwaysStrafeOffensively = false;
        config.backingOffWhileNotVentingAllowed = true;
        config.turnToFaceWithUndamagedArmor = true;
        config.burnDriveIgnoreEnemies = false;

        boolean reckless = false;
        boolean aggressive = false;
        boolean steady = false;
        boolean cautious = false;

        if (hullId.startsWith("kol_alysse")
                || hullId.startsWith("kol_snowdrop")) {
            reckless = true;
        } else if (hullId.startsWith("kol_larkspur")) {
            aggressive = true;
        } else if (hullId.startsWith("kol_mimosa")
                || hullId.startsWith("kol_lotus")
                || hullId.startsWith("kol_tamarisk")
                || hullId.startsWith("kol_sundew")
                || hullId.startsWith("zea_edf_mizuchi")
                || hullId.startsWith("zea_edf_kiyohime")
                || hullId.startsWith("zea_dawn_tianma")) {
            steady = true;
        } else if (false) {
            cautious = true;
        }


        //Bump toward target personality 1 step
        if (ship.getCaptain() != null) {
            if (cautious) {
                if (ship.getCaptain().isDefault() || ship.getCaptain().isPlayer()) config.personalityOverride = Personalities.CAUTIOUS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.RECKLESS)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.STEADY)) config.personalityOverride = Personalities.CAUTIOUS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.TIMID)) config.personalityOverride = Personalities.CAUTIOUS;

                config.turnToFaceWithUndamagedArmor = true;

            } else if (steady) {
                if (ship.getCaptain().isDefault() || ship.getCaptain().isPlayer()) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.CAUTIOUS)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.STEADY)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.TIMID)) config.personalityOverride = Personalities.CAUTIOUS;

            } else if (aggressive) {
                if (ship.getCaptain().isDefault() || ship.getCaptain().isPlayer()) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.CAUTIOUS)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.RECKLESS)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.STEADY)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.TIMID)) config.personalityOverride = Personalities.CAUTIOUS;

                config.turnToFaceWithUndamagedArmor = false;

            } else if (reckless) {
                if (ship.getCaptain().isDefault() || ship.getCaptain().isPlayer()) config.personalityOverride = Personalities.RECKLESS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.CAUTIOUS)) config.personalityOverride = Personalities.STEADY;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE)) config.personalityOverride = Personalities.RECKLESS;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.STEADY)) config.personalityOverride = Personalities.AGGRESSIVE;
                else if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.TIMID)) config.personalityOverride = Personalities.CAUTIOUS;
                config.turnToFaceWithUndamagedArmor = false;
                config.burnDriveIgnoreEnemies = true;
            } else {
                return null;
            }
        }

        return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
}
