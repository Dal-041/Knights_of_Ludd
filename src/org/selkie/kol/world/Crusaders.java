package org.selkie.kol.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.awt.*;

public class Crusaders {
    public static String MEMKEY_KOL_SCHISMED = "$kol_knights_schism";
    public static String nameCrusaders = "Luddic Crusade";
    protected static FactionAPI crusaders = Global.getSector().getFaction(KOL_ModPlugin.kolID);
    protected static String[] priorityShips = {
            "kol_alysse",
            "kol_lunaria",
            "kol_tamarisk",
            "kol_lotus",
            "kol_larkspur",
            "kol_snowdrop",
            "kol_mimosa",
            "kol_marigold",
            "kol_protea",
            "kol_sundew",
    };


    public static void startSchism() {
        //TODO break the COGR alliance
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_SCHISMED, true);
        setCrusadeNames();
        setCrusadeDoctrine();
        setCrusadeRelationships();
    }

    public static void setCrusadeNames() {
        if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_SCHISMED)
                || !Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_SCHISMED)) {
            return;
        }
        crusaders.setDisplayNameOverride(nameCrusaders);
        crusaders.setDisplayNameWithArticleOverride("The " + nameCrusaders);
        crusaders.setShipNamePrefixOverride("HCS");
    }

    public static void setCrusadeDoctrine() {
        for (String ship : priorityShips) {
            crusaders.addPriorityShip(ship);
        }
        crusaders.getDoctrine().setAggression(4);
        crusaders.getDoctrine().setNumShips(4);
        crusaders.getDoctrine().setOfficerQuality(3);
        crusaders.getDoctrine().setShipQuality(3);
    }

    public static void setCrusadeRelationships() {
        crusaders.setRelationship(Factions.LUDDIC_CHURCH, RepLevel.SUSPICIOUS);
        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).setRelationship(crusaders.getId(), RepLevel.SUSPICIOUS);
        crusaders.setRelationship(Factions.HEGEMONY, RepLevel.NEUTRAL);
        Global.getSector().getFaction(Factions.HEGEMONY).setRelationship(crusaders.getId(), RepLevel.SUSPICIOUS);
        crusaders.setRelationship(Factions.TRITACHYON, RepLevel.HOSTILE);
        Global.getSector().getFaction(Factions.TRITACHYON).setRelationship(crusaders.getId(), RepLevel.HOSTILE);
    }
}
