package org.selkie.kol.impl.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParams;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.selkie.kol.impl.fleets.ZeaFleetManager;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.util.ArrayList;
import java.util.HashMap;

import static org.selkie.kol.impl.fleets.ZeaFleetManager.copyFleetMembers;

public class ZeaUtils {


    public static final String BOSS_TAG = "$zea_boss";
    public static float attainmentFactor = 0.15f;
    public static boolean useDomres = false;
    public static boolean useLostech = false;
    public static boolean useDustkeepers = false;
    public static boolean useEnigma = false;

    public static String[] factionIDs = {
            PrepareAbyss.dawnID,
            PrepareAbyss.duskID,
            PrepareAbyss.elysianID
    };
    public static String[] systemNames = {
            PrepareAbyss.elysiaSysName,
            PrepareAbyss.nullspaceSysName,
            PrepareAbyss.lunaSeaSysName,
            PrepareAbyss.nbsSysPrefix,
    };

    public static final String IntelBreadcrumbTag = "Dark Deeds";
    public static final String IntelLoreTag = "Elysian Lore";
    public static final String KEY_ELYSIA_WITNESS = "$zea_elysian_witness";
    public static final String KEY_ZEA_SPOILERS = "$zea_spoilers";
    public static final String THEME_ZEA = "theme_zea";
    public static final String THEME_STORM = "theme_zea_storm";

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
    public static final String[] elysianBossSupportingFleet = {
            "zea_edf_tamamo_Striker",
            "zea_edf_tamamo_Striker"
    };
    public static final String[] duskBossSupportingFleet = {
            "zea_dusk_ayakashi_Vengeful",
            "zea_dusk_ayakashi_Whispered",
    };
    public static final String[] zeaAIOverrideShips = {
            "zea_edf_kiyohime",
            "zea_edf_mizuchi",
            "zea_dawn_tianma",
    };

    public static final String abilityJumpElysia = "fracture_jump_elysia";
    public static final String abilityJumpDawn = "fracture_jump_luna_sea";
    public static final String abilityJumpDusk = "fracture_jump_pullsar";

    public static final String systemIDBlizzard = "zea_boss_blizzard";
    public static final String systemIDSupernova = "zea_boss_supernova";
    public static final String systemIDCorruption = "zea_boss_corruptionjets";
    //public static final String systemIDShieldDrone = "zea_shield_drone";

    public static final String pathPortraits = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/";
    public static final String[] portraitsDawnPaths = {pathPortraits.concat("zea_dawn_1.png"), pathPortraits.concat("zea_dawn_2.png"), pathPortraits.concat("zea_dawn_3.png"), pathPortraits + "zea_dawn_4.png", pathPortraits + "zea_dawn_5.png"};
    public static final String[] portraitsDuskPaths = {pathPortraits.concat("zea_dusk_1.png"), pathPortraits.concat("zea_dusk_2.png"), pathPortraits.concat("zea_dusk_3.png")};
    public static final String[] portraitsElysianPaths = {pathPortraits.concat("zea_edf_1.png"), pathPortraits.concat("zea_edf_3.png"), pathPortraits.concat("zea_edf_2.png")};
    public static final String[] portraitsDawn = {"zea_dawn_1", "zea_dawn_2", "zea_dawn_3", "zea_dawn_4", "zea_dawn_5"};
    public static final String[] portraitsDusk = {"zea_dusk_1", "zea_dusk_2", "zea_dusk_3"};
    public static final String[] portraitsElysian = {"zea_edf_1", "zea_edf_3", "zea_edf_2"};

    public static final String pathCrests = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/factions/";
    public static final String crestDawn = "zea_crest_dawntide";
    public static final String crestDusk = "zea_crest_dusk";
    public static final String crestEDF = "zea_crest_edf";
    public static final String crestTT = "graphics/factions/crest_tritachyon";

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

            for (String parentID : techInheritIDs) {
                FactionAPI par = Global.getSector().getFaction(parentID);
                for (String entry : par.getKnownWeapons()) {
                    if (!fac.knowsWeapon(entry)) {
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
            ArrayList<String> toRemove = new ArrayList<>();
            for (String entry : fac.getKnownWeapons()) {
                for (String no : weaponBlacklist) {
                    if (entry.equals(no)) {
                        toRemove.add(entry);
                    }
                }
            }
            for (String entry : toRemove) {
                fac.removeKnownWeapon(entry);
            }
        }
    }

    public static CampaignFleetAPI ZeaBossGenFleetWeaver (CampaignFleetAPI fleet, int fp) {
        String fac = fleet.getFaction().getId();
        int cut = 180;
        while (fp > 0) {
            CampaignFleetAPI support = ZeaFleetManager.spawnFleet(MathUtils.getRandom().nextLong(), 0, fleet.getStarSystem(), fac, Math.min(fp, cut), Math.min(fp, cut));
            copyFleetMembers(fac, support, fleet, false);
            fp -= Math.min(fp, cut);
        }
        return fleet;
    }

    public static void bossWreckCleaner(SectorEntityToken wreck) {
        bossWreckCleaner(wreck, false);
    }

    public static void bossWreckCleaner(SectorEntityToken wreck, boolean uniqueSig) {
        ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
        data.notNowOptionExits = true;
        data.noDescriptionText = true;
        DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) wreck.getCustomPlugin();
        ShipRecoverySpecial.PerShipData copy = dsep.getData().ship.clone();
        copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
        copy.variantId = null;
        copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
        if (uniqueSig) copy.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        copy.variant.removeTag(ZeaUtils.BOSS_TAG);
        copy.variant.removeTag(Tags.VARIANT_UNBOARDABLE);
        //TODO: Special desciption updating logic
        //copy.variant.removeTag(Tags.SHIP_LIMITED_TOOLTIP);
        data.addShip(copy);

        Misc.setSalvageSpecial(wreck, data);
    }

    public static class ZeaBossGenFIDConfig implements FleetInteractionDialogPluginImpl.FIDConfigGen {
        FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

        public static interface FIDConfigGen {
            FleetInteractionDialogPluginImpl.FIDConfig createConfig();
        }

        public boolean aiRetreatToggle = false;
        public boolean objectivesToggle = false;
        public boolean fttlToggle = true;
        public boolean deployallToggle = true;

        /* Per-Fleet stuff
//			config.alwaysAttackVsAttack = true;
//			config.leaveAlwaysAvailable = true;
//			config.showFleetAttitude = false;
            config.showTransponderStatus = false;
            config.showEngageText = false;
            config.alwaysPursue = true;
            config.dismissOnLeave = false;
            //config.lootCredits = false;
            config.withSalvage = false;
            //config.showVictoryText = false;
            config.printXPToDialog = true;

            config.noSalvageLeaveOptionText = "Continue";
//			config.postLootLeaveOptionText = "Continue";
//			config.postLootLeaveHasShortcut = false;
        */


        public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
            config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {

                public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                    new RemnantSeededFleetManager.RemnantFleetInteractionConfigGen().createConfig().delegate.
                            postPlayerSalvageGeneration(dialog, context, salvage);
                }

                public void notifyLeave(InteractionDialogAPI dialog) {

                    SectorEntityToken other = dialog.getInteractionTarget();
                    if (!(other instanceof CampaignFleetAPI)) {
                        dialog.dismiss();
                        return;
                    }
                    CampaignFleetAPI fleet = (CampaignFleetAPI) other;

                    if (!fleet.isEmpty()) {
                        dialog.dismiss();
                        return;
                    }

                    //Do stuff to the fleet here if desired
                }

                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = aiRetreatToggle;
                    bcc.objectivesAllowed = objectivesToggle;
                    bcc.fightToTheLast = fttlToggle;
                    bcc.enemyDeployAll = deployallToggle;
                }
            };
            return config;
        }

        public void setAlwaysAttack (boolean set) {
            config.alwaysAttackVsAttack = set;
        }
        public void setAlwaysPursue (boolean set) {
            config.alwaysPursue = set;
        }
        public void setLeaveAlwaysAvailable (boolean set) {
            config.leaveAlwaysAvailable = set;
        }
        public void setWithSalvage (boolean set) {
            config.withSalvage = set;
        }
        public void setNoSalvageLeaveOptionText (String set) {
            config.noSalvageLeaveOptionText = set;
        }
    }
}
