package org.selkie.zea.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaEntities;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.zea.helpers.ZeaUtils;
import org.selkie.zea.intel.ZeaLoreIntel;
import org.selkie.zea.intel.ZeaLoreManager;
import org.selkie.zea.intel.ZeaTriTachBreadcrumbIntel;

import java.util.List;
import java.util.Map;

public class ZeaIntelCMD extends BaseCommandPlugin {
    SectorEntityToken TTStationBoss2;
    SectorEntityToken TTBoss3System = null;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        SectorEntityToken entity = dialog.getInteractionTarget();
        CampaignFleetAPI otherFleet = null;

        if (dialog.getInteractionTarget() instanceof CampaignFleetAPI) {
            otherFleet = (CampaignFleetAPI) dialog.getInteractionTarget();
        }

        if (entity.getMarket() != null && !entity.getMarket().isPlanetConditionMarketOnly()) {
            PlanetAPI planet = entity.getMarket().getPlanetEntity();
            if (planet != null) {
                entity = planet;
            }
        }

        if (ZeaStaticStrings.ADD_INTEL_TTBOSS_1.equals(command)) {

            ZeaLoreIntel intelLore = new ZeaLoreIntel(Global.getSector().getFaction(Factions.TRITACHYON).getCrest(), "Project Dusk Datacore #3", ZeaLoreManager.TT1Drop, ZeaLoreManager.TT1DropHLs);
            Global.getSector().getIntelManager().addIntel(intelLore, true, dialog.getTextPanel());

            if (Global.getSector().getEntityById(ZeaEntities.ZEA_BOSS_STATION_TRITACHYON) != null) {
                TTStationBoss2 = Global.getSector().getEntityById(ZeaEntities.ZEA_BOSS_STATION_TRITACHYON);
            } else {
                return false;
            }

            ZeaTriTachBreadcrumbIntel intel = new ZeaTriTachBreadcrumbIntel("Recovered Tri-Tachyon Navigation Log", "A departure entry pulled from the wreck of the Ninaya, supposedly an active Tri-Tachyon development facility. Whatever it may be out there is no doubt terrifyingly dangerous.", TTStationBoss2);
            Global.getSector().getIntelManager().addIntel(intel, false, dialog.getTextPanel());
            return true;

        } else if (ZeaStaticStrings.ADD_INTEL_TTBOSS_2.equals(command)) {

            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();

            ZeaLoreIntel intelLore = new ZeaLoreIntel(Global.getSector().getFaction(Factions.TRITACHYON).getCrest(), "Project Dusk Datacore #1", ZeaLoreManager.TT2Drop, ZeaLoreManager.TT2DropHLs);
            Global.getSector().getIntelManager().addIntel(intelLore, true, dialog.getTextPanel());

            if (fleet == null) return false;

            //Handle ninmah recovery
            boolean foundNinmah = false;
            for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
                if (member.getVariant().getHullSpec().getBaseHullId().startsWith(ZeaStaticStrings.ZEA_BOSS_NINMAH)) {
                    ShipVariantAPI variant = member.getVariant();
                    if (!variant.hasTag(Tags.SHIP_CAN_NOT_SCUTTLE)) variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
                    if (variant.hasTag(ZeaMemKeys.ZEA_BOSS_TAG)) variant.removeTag(ZeaMemKeys.ZEA_BOSS_TAG);
                    if (variant.hasTag(Tags.SHIP_LIMITED_TOOLTIP)) variant.removeTag(Tags.SHIP_LIMITED_TOOLTIP);
                    if (variant.hasTag(Tags.VARIANT_UNBOARDABLE)) variant.removeTag(Tags.VARIANT_UNBOARDABLE);
                    if (variant.getHullSpec().getBaseHullId().startsWith(ZeaStaticStrings.ZEA_BOSS_NINMAH)) foundNinmah = true;
                }
            }

            if (!foundNinmah) {
                //make sure there is a valid location to avoid spawning in the sun
                Vector2f location = fleet.getLocation();

                //spawn the derelict object
                ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData(ZeaStaticStrings.ZEA_BOSS_NINMAH_UNDOER, ShipRecoverySpecial.ShipCondition.WRECKED, 0f);
                ship.shipName = "TTS Ninmah";
                DerelictShipEntityPlugin.DerelictShipData DSD = new DerelictShipEntityPlugin.DerelictShipData(ship, true);
                CustomCampaignEntityAPI wreck = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                        fleet.getContainingLocation(),
                        Entities.WRECK, Factions.NEUTRAL, DSD);
                Misc.makeImportant(wreck, ZeaStaticStrings.ZEA_BOSS_NINMAH);
                wreck.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_NINMAH_WRECK, true);
                wreck.getLocation().x = fleet.getLocation().x + (50f - (float) Math.random() * 100f);
                wreck.getLocation().y = fleet.getLocation().y + (50f - (float) Math.random() * 100f);
                wreck.setFacing((float)Math.random()*360f);
                wreck.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);

                ZeaUtils.bossWreckCleaner(wreck, false);
            }

            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system.getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_TT_3_SYSTEM)) {
                    TTBoss3System = system.getCenter();
                }
            }
            if (TTBoss3System == null) return false;

            ZeaTriTachBreadcrumbIntel intelBC = new ZeaTriTachBreadcrumbIntel("Recovered Project Dusk Datacore", "You've uncovered the location of a third black site, far far on the outskirts of the sector. Could it be the end of the line, or only the beginning of the thread?", TTBoss3System);
            Global.getSector().getIntelManager().addIntel(intelBC, false, dialog.getTextPanel());
            return true;

        } else if (ZeaStaticStrings.ADD_INTEL_TTBOSS_3.equals(command)) {
            ZeaLoreIntel intelLore = new ZeaLoreIntel(Global.getSector().getFaction(Factions.TRITACHYON).getCrest(), "Project Dusk Datacore #2", ZeaLoreManager.TT3Drop, ZeaLoreManager.TT3DropHLs);
            Global.getSector().getIntelManager().addIntel(intelLore, false, dialog.getTextPanel());

            SectorEntityToken LunaSea = null;
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system.getName().equals(ZeaStaticStrings.lunaSeaSysName)) {
                    LunaSea = system.getEntityById(ZeaEntities.ZEA_LUNASEA_PLANET_FOUR);
                }
            }
            if (LunaSea == null) return false;

            ZeaTriTachBreadcrumbIntel intelBC = new ZeaTriTachBreadcrumbIntel("Project Dawn Hyperspatial Tracking Link", "A tracking link from the doomed Tri-Tachyon \"Operation Dawn\" to... something. Whatever they produced, the link is still sending telemetry, but the readings are nonsensical. If you want answers, there's nothing to do but seek out the location for yourself", LunaSea);
            Global.getSector().getIntelManager().addIntel(intelBC, false, dialog.getTextPanel());

        } else if (ZeaStaticStrings.END_MUSIC.equals(command)) {
            Global.getSoundPlayer().restartCurrentMusic();
            return true;
        } else if (ZeaStaticStrings.ADD_BOSS_TAGS.equals(command)) {
            if (otherFleet != null) {
                for (FleetMemberAPI member : otherFleet.getMembersWithFightersCopy()) {
                    if (member.getHullId().startsWith("zea_boss")) {
                        if (!member.getVariant().hasTag(Tags.VARIANT_UNBOARDABLE)) member.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
                        if (!member.getVariant().hasTag(ZeaMemKeys.ZEA_BOSS_TAG))member.getVariant().addTag(ZeaMemKeys.ZEA_BOSS_TAG);
                    }
                }
            }
        } else if ("addYukiImage".equals(command)){
            dialog.getTextPanel().addImage(ZeaStaticStrings.GfxCat.ILLUSTRATIONS,"zea_dusk_yuki2");
        }

        return false;
    }
}
