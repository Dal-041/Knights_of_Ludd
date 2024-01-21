package org.selkie.kol.impl.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.world.PrepareAbyss;

public class AbyssUtils {

    public static float attainmentFactor = 0.2f;
    public static boolean useDomres = false;
    public static boolean useLostech = false;
    public static boolean useDustkeepers = false;
    public static boolean useEnigma = false;

    public static String[] factionIDs = {
            PrepareAbyss.dawnID,
            PrepareAbyss.duskID,
            PrepareAbyss.elysianID
    };
    public static String[] techInheritIDs = {
            "remnant",
            "mercenary"
    };
    public static String[] hullBlacklist = {
            "guardian",
            "radiant",
            "tahlan_asria",
            "tahlan_nirvana",
            "zea_dusk_ayakashi",
            "zea_dawn_ao",
            "zea_dawn_qilin",
            "zea_edf_ryujin",
            "zea_edf_kiyohime"
    };
    public static String[] weaponBlacklist = {
            "lightdualmg",
            "lightmortar",
            "lightmg",
            "mininglaser",
            "atropos_single",
            "harpoon_single",
            "sabot_single",
            "hammer",
            "hammer_single",
            "hammerrack",
            "jackhammer"
    };
    public static final String[] uwDerelictsNormal = {
            "aurora_Assault",
            "brawler_tritachyon_Standard",
            "brawler_tritachyon_Standard",
            "omen_PD",
            "omen_PD",
            "medusa_PD",
            "medusa_Attack",
            "shrike_Attack",
            "fury_Support",
            "buffalo_tritachyon_Standard",
            "buffalo_tritachyon_Standard",
            "atlas_Standard",
            "prometheus_Super",
            "apogee_Balanced",
            "astral_Elite",
            "paragon_Raider"
    };
    public static final String[] uwDerelictsPhase = {
            "afflictor_Strike",
            "shade_Assault",
            "shade_Assault",
            "harbinger_Strike",
            "harbinger_Strike",
            "doom_Attack"
    };
    //The flagships are spawned separately
    public static final String[] duskBossSupportingFleet = {
            "zea_boss_nineveh_Souleater",
            "zea_boss_nineveh_Souleater",
            "zea_boss_ninmah_Skinthief",
            "zea_boss_ninmah_Skinthief",
            "zea_boss_ninaya_Gremlin",
            "zea_boss_ninaya_Gremlin",
            "zea_boss_ninaya_Nightdemon"
    };
    public static final String[] elysianBossSupportingFleet = {
            "zea_edf_tamamo_Striker",
            "zea_edf_tamamo_Striker"
    };

    public static final String abilityJumpElysia = "fracture_jump_elysia";
    public static final String abilityJumpDawn = "fracture_jump_luna_sea";
    public static final String abilityJumpDusk = "fracture_jump_pullsar";

    public static final String path = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/";
    public static final String[] portraitsDawnPaths = {path.concat("zea_dawn_1.png"), path.concat("zea_dawn_2.png"), path.concat("zea_dawn_3.png")};
    public static final String[] portraitsDuskPaths = {path.concat("zea_dusk_1.png"), path.concat("zea_dusk_2.png"), path.concat("zea_dusk_3.png")};
    public static final String[] portraitsElysianPaths = {path.concat("zea_idk1.png"), path.concat("zea_idk2.png"), path.concat("zea_idk3.png")};
    public static final String[] portraitsDawn = {"zea_dawn_1", "zea_dawn_2", "zea_dawn_3"};
    public static final String[] portraitsDusk = {"zea_dusk_1", "zea_dusk_2", "zea_dusk_3"};
    public static final String[] portraitsElysian = {"zea_idk1", "zea_idk2", "zea_idk3"};

    public static void fixVariant(FleetMemberAPI member) {
        if (member.getVariant().getSource() != VariantSource.REFIT) {
            ShipVariantAPI variant = member.getVariant().clone();
            variant.setOriginalVariant(null);
            variant.setHullVariantId(Misc.genUID());
            variant.setSource(VariantSource.REFIT);
            member.setVariant(variant, false, true);
        }
        member.updateStats();
    }

    public static void checkAbyssalFleets() {
        if(Global.getSettings().getModManager().isModEnabled("lost_sector")) {
            for (FactionAPI faction:Global.getSector().getAllFactions()) {
                if (faction.getId().equals("enigma")) useEnigma = true;
            }
        }
        if(Global.getSettings().getModManager().isModEnabled("tahlan")) {
            useLostech = true;
        }
        for (FactionAPI faction:Global.getSector().getAllFactions()) {
            if (faction.getId().equals("domres")) useDomres = true;
            if (faction.getId().equals("sotf_dustkeepers")) useDustkeepers = true;
        }
    }

    public static void copyHighgradeEquipment() {
        for (String ID : factionIDs) {
            FactionAPI fac = Global.getSector().getFaction(ID);
            boolean skip;
            for (String parentID : techInheritIDs) {
                FactionAPI par = Global.getSector().getFaction(parentID);
                for (String entry : par.getKnownWeapons()) {
                    skip = false;
                    for (String no : weaponBlacklist) {
                        if (entry.equals(no)) {
                            skip = true;
                            break;
                        }
                    }
                    if (!skip && !fac.knowsWeapon(entry)) {
                        fac.addKnownWeapon(entry, false);
                    }
                }
                if (parentID.equals(Factions.REMNANTS)) {
                    for (String entry : par.getKnownFighters()) {
                        if (!fac.knowsFighter(entry)) {
                            fac.addKnownFighter(entry, false);
                        }
                    }
                }
                for (String entry : par.getKnownHullMods()) {
                    if (!fac.knowsHullMod(entry)) {
                        fac.addKnownHullMod(entry);
                    }
                }
            }
        }
    }

}
