package org.selkie.kol.impl.helpers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.selkie.kol.impl.fleets.*;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.util.ArrayList;
import java.util.Objects;

import static org.selkie.kol.impl.fleets.ZeaFleetManager.copyFleetMembers;

public class ZeaUtils {
    public static float attainmentFactor = 0.15f;
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

    public static void versionUpdate1_2to1_3(){
        LocationAPI nullspace = Global.getSector().getStarSystem(PrepareAbyss.nullspaceSysName);
        for (SectorEntityToken entity : nullspace.getAllEntities()){
            if (Objects.equals(entity.getId(), "zea_research_station_dusk")){
                SectorEntityToken stationResearch = nullspace.addCustomEntity(PrepareAbyss.nullstationID, "Shielded Research Station", PrepareAbyss.nullstationID, Factions.DERELICT);
                stationResearch.setFixedLocation(-5230, 8860);
                nullspace.removeEntity(entity);
                break;
            }
        }

        // backport cores into the correct places
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        CargoAPI playerCargo = Global.getSector().getPlayerFleet().getCargo();
        if (mem.contains(ManageDuskBoss.MEMKEY_KOL_DUSK_BOSS_DONE)) playerCargo.addCommodity(ZeaStaticStrings.BossCore.DUSK_CORE.itemID, 1);
        else{
            for (CampaignFleetAPI fleet : Global.getSector().getStarSystem(PrepareAbyss.nullspaceSysName).getFleets()){
                if(fleet.getMemoryWithoutUpdate().contains(SpawnDuskBoss.MEMKEY_DUSK_BOSS_FLEET)){
                    // add boss core to cargo
                    CargoAPI cargo = Global.getFactory().createCargo(true);
                    cargo.addCommodity(ZeaStaticStrings.BossCore.DUSK_CORE.itemID, 1);
                    BaseSalvageSpecial.addExtraSalvage(fleet, cargo);
                    break;
                }
            }
        }

        if (mem.contains(ManageDawnBoss.MEMKEY_KOL_DAWN_BOSS_DONE)) playerCargo.addCommodity(ZeaStaticStrings.BossCore.DAWN_CORE.itemID, 1);
        else {
            for (CampaignFleetAPI fleet : Global.getSector().getStarSystem(PrepareAbyss.lunaSeaSysName).getFleets()){
                if(fleet.getMemoryWithoutUpdate().contains(SpawnDawnBoss.MEMKEY_DAWN_BOSS_FLEET)){
                    // add boss core to cargo
                    CargoAPI cargo = Global.getFactory().createCargo(true);
                    cargo.addCommodity(ZeaStaticStrings.BossCore.DAWN_CORE.itemID, 1);
                    BaseSalvageSpecial.addExtraSalvage(fleet, cargo);
                    break;
                }
            }
        }

        if (mem.contains(ManageElysianCorruptingheart.MEMKEY_KOL_ELYSIAN_BOSS2_DONE)) playerCargo.addCommodity(ZeaStaticStrings.BossCore.ELYSIAN_CORE.itemID, 1);
        else{
            for (CampaignFleetAPI fleet : Global.getSector().getStarSystem(PrepareAbyss.elysiaSysName).getFleets()){
                if(fleet.getMemoryWithoutUpdate().contains(SpawnElysianHeart.MEMKEY_ELYSIAN_BOSS_FLEET_2)){
                    // add boss core to cargo
                    CargoAPI cargo = Global.getFactory().createCargo(true);
                    cargo.addCommodity(ZeaStaticStrings.BossCore.ELYSIAN_CORE.itemID, 1);
                    BaseSalvageSpecial.addExtraSalvage(fleet, cargo);
                    break;
                }
            }
        }
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
        copy.variant.removeTag(ZeaStaticStrings.BOSS_TAG);
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
