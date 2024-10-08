package org.selkie.zea.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.selkie.kol.helpers.KolStaticStrings;
import org.selkie.zea.fleets.ZeaFleetManager;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;

import java.util.ArrayList;

import static org.selkie.zea.fleets.ZeaFleetManager.copyFleetMembers;

public class ZeaUtils {
    public static boolean useDomres = false;
    public static boolean useLostech = false;
    public static boolean useDustkeepers = false;
    public static boolean useEnigma = false;

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
        if(Global.getSettings().getModManager().isModEnabled(KolStaticStrings.LOST_SECTOR)) {
            for (FactionAPI faction:Global.getSector().getAllFactions()) {
                if (faction.getId().equals(KolStaticStrings.ENIGMA)) useEnigma = true;
            }
        }
        if(Global.getSettings().getModManager().isModEnabled(KolStaticStrings.TAHLAN)) {
            useLostech = true;
        }
        for (FactionAPI faction:Global.getSector().getAllFactions()) {
            if (faction.getId().equals(KolStaticStrings.DOMRES)) useDomres = true;
            if (faction.getId().equals(KolStaticStrings.SOTF_DUSTKEEPERS)) useDustkeepers = true;
        }
    }

    public static void copyHighgradeEquipment() {
        for (String ID : ZeaStaticStrings.factionIDs) {
            FactionAPI fac = Global.getSector().getFaction(ID);

            for (String parentID : ZeaStaticStrings.techInheritIDs) {
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
                for (String no : ZeaStaticStrings.weaponBlacklist) {
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

    public final static int FLEET_SPAWN_CHUNK = 180;
    public static CampaignFleetAPI ZeaBossGenFleetWeaver (CampaignFleetAPI fleet, int fp) {
        String fac = fleet.getFaction().getId();
        int cut = FLEET_SPAWN_CHUNK;
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
        copy.variant.removeTag(ZeaMemKeys.ZEA_BOSS_TAG);
        copy.variant.removeTag(Tags.VARIANT_UNBOARDABLE);
        //TODO: Special desciption updating logic
        //copy.variant.removeTag(Tags.SHIP_LIMITED_TOOLTIP);
        data.addShip(copy);

        Misc.setSalvageSpecial(wreck, data);
    }

    public static class ZeaBossGenFIDConfig implements FleetInteractionDialogPluginImpl.FIDConfigGen {
        final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

        public interface FIDConfigGen {
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
