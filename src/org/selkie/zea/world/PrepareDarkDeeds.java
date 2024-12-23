package org.selkie.zea.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfigGen;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager.RemnantFleetInteractionConfigGen;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipRecoverySpecialData;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;
import com.fs.starfarer.api.impl.campaign.world.ZigLeashAssignmentAI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;
import org.selkie.zea.fleets.ZeaTTBoss2DefenderPlugin;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaEntities;
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.zea.helpers.ZeaUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.selkie.zea.fleets.ZeaFleetManager.copyFleetMembers;

public class PrepareDarkDeeds {


    private static final Logger log = Logger.getLogger(PrepareDarkDeeds.class);

    public static void andBegin() {
        SpawnTT1Boss();
        generateTT2Station();
        generateTT3Site();
    }

    public static void andContinue() {
        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (!plugins.hasPlugin(ZeaTTBoss2DefenderPlugin.class)) {
            plugins.addPlugin(new ZeaTTBoss2DefenderPlugin(), true);
        }
    }

    public static void SpawnTT1Boss() {

        Map<String, Integer> skills = new HashMap<>();
        skills.put(Skills.HELMSMANSHIP, 2);
        skills.put(Skills.COMBAT_ENDURANCE, 2);
        skills.put(Skills.IMPACT_MITIGATION, 2);
        skills.put(Skills.DAMAGE_CONTROL, 2);
        skills.put(Skills.FIELD_MODULATION, 2);
        skills.put(Skills.TARGET_ANALYSIS, 2);
        skills.put(Skills.SYSTEMS_EXPERTISE, 2);
        skills.put(Skills.ENERGY_WEAPON_MASTERY, 2);

        PersonAPI TT1BossCaptain = MagicCampaign.createCaptainBuilder(Factions.TRITACHYON)
                .setIsAI(true)
                .setAICoreType(Commodities.ALPHA_CORE)
                .setPortraitId(ZeaStaticStrings.portraitAlphaPlus)
                .setLevel(8)
                .setFirstName("Alpha")
                .setLastName("(+)")
                .setGender(FullName.Gender.ANY)
                .setPersonality(Personalities.RECKLESS) // With the Ninaya's flux stats Reckless will still back off enough
                .setSkillLevels(skills)
                .create();

        TT1BossCaptain.getStats().setSkipRefresh(true);
        TT1BossCaptain.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        TT1BossCaptain.getStats().setSkipRefresh(false);

        SectorEntityToken token = Global.getSector().getStarSystem("Unknown Location").createToken(11111,11111); //cache loc
        CampaignFleetAPI TT1BossFleet = MagicCampaign.createFleetBuilder()
                .setFleetName("Unidentified Vessel")
                .setFleetFaction(Factions.TRITACHYON)
                .setFleetType(FleetTypes.TASK_FORCE)
                .setFlagshipName("TTS Ninaya")
                .setFlagshipVariant(ZeaStaticStrings.ZEA_BOSS_NINAYA_NIGHTDEMON)
                .setCaptain(TT1BossCaptain)
                .setMinFP(0) //support fleet
                .setQualityOverride(5f)
                .setAssignment(FleetAssignment.DEFEND_LOCATION)
                .setSpawnLocation(token)
                .setIsImportant(true)
                .setTransponderOn(false)
                .create();
        TT1BossFleet.setDiscoverable(true);

		/*
		for(String support : PrepareAbyss.duskBossSupportingFleet) {
			duskBossFleet.getFleetData().addFleetMember(support);
		}*/

        TT1BossFleet.setNoFactionInName(true);
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
        //fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_NINAYA_BOSS_FLEET, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_BOSS_TAG, true);

        // setup and permalock the flagship variant
        FleetMemberAPI flagship = TT1BossFleet.getFlagship();
        flagship.setVariant(flagship.getVariant().clone(), false, false);
        flagship.getVariant().setSource(VariantSource.REFIT);
        flagship.getVariant().addTag(ZeaMemKeys.ZEA_BOSS_TAG);
        flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);

        TT1BossFleet.getFleetData().sort();

        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        TT1BossFleet.addTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG);

        //Using FID config instead
        //TT1BossFleet.addEventListener(new ManageBossTag());

        TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new TT1FIDConfig());

    }


    public static class TT1FIDConfig implements FleetInteractionDialogPluginImpl.FIDConfigGen {
        public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
            FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

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

                    Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_NINAYA_DONE, true);

                    ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData(ZeaStaticStrings.ZEA_BOSS_NINAYA_NIGHTDEMON, ShipRecoverySpecial.ShipCondition.WRECKED, 0f);
                    ship.shipName = "TTS Ninaya";
                    DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
                    CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                            fleet.getContainingLocation(),
                            Entities.WRECK, Factions.NEUTRAL, params);
                    Misc.makeImportant(entity, ZeaStaticStrings.ZEA_BOSS_NINAYA);
                    entity.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_NINAYA_WRECK, true);

                    entity.getLocation().x = fleet.getLocation().x + (50f - (float) Math.random() * 100f);
                    entity.getLocation().y = fleet.getLocation().y + (50f - (float) Math.random() * 100f);

                    ZeaUtils.bossWreckCleaner(entity, false);

                    dialog.setInteractionTarget(entity);
                    RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl(ZeaStaticStrings.ruleCMD.ZEA_AFTER_NINAYA_DEFEAT);
                    dialog.setPlugin(plugin);
                    plugin.init(dialog);
                }

                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = false;
                    bcc.objectivesAllowed = false;
                    bcc.fightToTheLast = true;
                    bcc.enemyDeployAll = true;

                }
            };
            return config;


        }
    }

    public static void generateTT2Station() {
        //Get valid Remnant systems, pick a nexus
        // spawn station around Nexus
        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag(Tags.THEME_REMNANT_RESURGENT) && system.hasTag(Tags.THEME_REMNANT_MAIN)) {
                picker.add(system);
            }
        }
        if (picker.isEmpty()) {
            log.warn("ZEA: Could not locate any valid TT2Station spawn target!");
            return;
        }

        StarSystemAPI system = picker.pick();
        SectorEntityToken token = system.getStar();

        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.getFlagship() == null) continue;
            if (fleet.getFlagship().getHullSpec().getBaseHullId().startsWith("remnant_station")) {
                token = fleet;
            }
        }

        system.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_2_SYSTEM, true);

        /* Runcode
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.getMemoryWithoutUpdate().contains("$zea_tt_boss2_system")) {
                $print(system.getId());
            }
        }
        //Debug runcode
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag("theme_remnant_main") && system.hasTag("theme_remnant_resurgent")) {
                $print(system.getBaseName());
            }
        }
         */

        CustomCampaignEntityAPI stationBoss = system.addCustomEntity(ZeaEntities.ZEA_BOSS_STATION_TRITACHYON, "Suspicious Research Station", ZeaEntities.ZEA_BOSS_STATION_TRITACHYON, Factions.NEUTRAL);
        stationBoss.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_2_STATION, true);
        stationBoss.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        stationBoss.setSensorProfile(1f);
        stationBoss.getDetectedRangeMod().modifyFlat("gen", 5000f);
        if (token.isStar()) {
            stationBoss.setCircularOrbitPointingDown(token, 0, token.getRadius()+500f, (token.getRadius()+500f)/10f);
        } else {
            stationBoss.setCircularOrbitPointingDown(token, 0, 150f, 60);
        }
        Misc.setDefenderOverride(stationBoss, new DefenderDataOverride(Factions.TRITACHYON, 1f, 20, 20, 1)); // doesnt matter, will be overwriten by plugin
        stationBoss.setDiscoverable(true);
    }

    public static class TT3FIDConfig implements FIDConfigGen {
        public FIDConfig createConfig() {
            FIDConfig config = new FIDConfig();

//			config.alwaysAttackVsAttack = true;
//			config.leaveAlwaysAvailable = true;
//			config.showFleetAttitude = false;
            config.showTransponderStatus = false;
            config.showEngageText = false;
            config.alwaysPursue = true;
            config.dismissOnLeave = false;
            //config.lootCredits = false;
            config.withSalvage = true;
            //config.showVictoryText = false;
            config.printXPToDialog = true;

            config.noSalvageLeaveOptionText = "Continue";
//			config.postLootLeaveOptionText = "Continue";
//			config.postLootLeaveHasShortcut = false;

            config.delegate = new BaseFIDDelegate() {
                public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                    new RemnantFleetInteractionConfigGen().createConfig().delegate.
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

                    Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_NINEVEH_DONE, true);

                    PerShipData ship = new PerShipData(ZeaStaticStrings.ZEA_BOSS_NINEVEH_SOULEATER, ShipCondition.WRECKED, 0f);
                    ship.shipName = "TTS Nineveh";
                    DerelictShipData params = new DerelictShipData(ship, true);
                    CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                            fleet.getContainingLocation(),
                            Entities.WRECK, Factions.NEUTRAL, params);
                    Misc.makeImportant(entity, ZeaStaticStrings.ZEA_BOSS_NINEVENH);
                    entity.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_NINEVEH_WRECK, true);

                    entity.getLocation().x = fleet.getLocation().x + (50f - (float) Math.random() * 100f);
                    entity.getLocation().y = fleet.getLocation().y + (50f - (float) Math.random() * 100f);

                    ShipRecoverySpecialData data = new ShipRecoverySpecialData(null);
                    data.notNowOptionExits = true;
                    data.noDescriptionText = true;
                    DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
                    PerShipData copy = dsep.getData().ship.clone();
                    copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
                    copy.variantId = null;
                    copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
                    copy.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
                    copy.variant.removeTag(ZeaMemKeys.ZEA_BOSS_TAG);
                    copy.variant.removeTag(Tags.SHIP_LIMITED_TOOLTIP);
                    data.addShip(copy);

                    CustomCampaignEntityAPI lootbox = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                            fleet.getContainingLocation(),
                            Entities.EQUIPMENT_CACHE_SMALL, Factions.TRITACHYON);
                    lootbox.getLocation().x = fleet.getLocation().x + (50f - (float) Math.random() * 100f);
                    lootbox.getLocation().y = fleet.getLocation().y + (50f - (float) Math.random() * 100f);
                    Misc.makeImportant(lootbox, ZeaStaticStrings.ZEA_BOSS_NINEVENH);
                    lootbox.addDropValue("basic", 40000);
                    lootbox.addDropRandom(ZeaDrops.OMEGA_WEAPONS_SMALL, 1);
                    lootbox.addDropRandom(ZeaDrops.ZEA_OMEGA_SMALL_LOW, 7);
                    lootbox.addDropRandom(ZeaDrops.OMEGA_WEAPONS_MEDIUM, 1);
                    lootbox.addDropRandom(ZeaDrops.ZEA_OMEGA_MEDIUM_LOW, 3); //avg: +0.6
                    lootbox.addDropRandom(ZeaDrops.ZEA_OMEGA_LARGE_LOW, 2); //0.4
                    //lootbox.addDropRandom("zea_tritach_delta_site_log", 1); //handled by intel

                    Misc.setSalvageSpecial(entity, data);

                    dialog.setInteractionTarget(entity);
                    RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl(ZeaStaticStrings.ruleCMD.ZEA_AFTER_NINEVEH_DEFEAT);
                    dialog.setPlugin(plugin);
                    plugin.init(dialog);
                }

                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = false;
                    bcc.objectivesAllowed = false;
                    bcc.fightToTheLast = true;
                    bcc.enemyDeployAll = true;
                }
            };
            return config;
        }
    }

    public static void generateTT3Site() {
        float sectorWidthR = Global.getSettings().getFloat("sectorWidth")/2f;
        float sectorHeightR = Global.getSettings().getFloat("sectorHeight")/2f;

        StarSystemAPI system = Global.getSector().createStarSystem("Delta Site");
        system.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_3_SYSTEM, true);
        system.setBaseName("Delta Site");
        //system.setType(StarSystemType.NEBULA);
        system.setName("Unknown Location"); // to get rid of "Star System" at the end of the name
        system.addTag(Tags.THEME_UNSAFE);
        system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        system.addTag(Tags.THEME_HIDDEN);
        LocationAPI hyper = Global.getSector().getHyperspace();

        system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_campaign_alpha_site");

        system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS, "zea_bg_delta_site"));
        //system.getLocation().set(2500, 3000);
        system.getLocation().set(-sectorWidthR + 30000f, -sectorHeightR + 15000f);

        HyperspaceTerrainPlugin hyperTerrain = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(hyperTerrain);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, 200, 0, 360f);
//		editor.regenNoise();
//		editor.noisePrune(0.8f);
//		editor.regenNoise();

        SectorEntityToken center = system.initNonStarCenter();

        system.setLightColor(new Color(255,140,185,255)); // light color in entire system, affects all entities

        PlanetAPI rock = system.addPlanet(ZeaEntities.ZEA_TT_3_SITE_PLANET, center, "Delta Site", Planets.FROZEN, 0, 166, 1200, 40);
        //rock.setCustomDescriptionId("???");
        rock.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_3_BLACK_SITE, true);

        rock.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        rock.getMarket().addCondition(Conditions.VERY_COLD);
        rock.getMarket().addCondition(Conditions.DARK);
        rock.getMarket().addCondition(Conditions.RUINS_SCATTERED);

        rock.getMarket().getMemoryWithoutUpdate().set(ZeaStaticStrings.RUINS_EXPLORED, true);
        //If this is done, shows up in intel planet list; doesn't matter either way
        //Misc.setFullySurveyed(rock.getMarket(), null, false);

        CoreLifecyclePluginImpl.addRuinsJunk(rock);

        rock.setOrbit(null);
        rock.setLocation(300, 1200);

        SectorEntityToken field = system.addTerrain(Terrain.MAGNETIC_FIELD,
                new MagneticFieldParams(166f, // terrain effect band width
                        500, // terrain effect middle radius
                        rock, // entity that it's around
                        350f, // visual band start
                        650f, // visual band end
                        new Color(60, 60, 150, 90), // base color
                        1f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
                        new Color(130, 60, 150, 130),
                        new Color(150, 30, 120, 150),
                        new Color(200, 50, 130, 190),
                        new Color(250, 70, 150, 240),
                        new Color(200, 80, 130, 255),
                        new Color(75, 0, 160, 255),
                        new Color(127, 0, 255, 255)
                ));
        field.setCircularOrbit(rock, 0, 0, 75);

        CustomCampaignEntityAPI beacon = system.addCustomEntity(null, null, Entities.WARNING_BEACON, Factions.NEUTRAL);
        beacon.setCircularOrbitPointingDown(rock, 0, 2500, 60);

        beacon.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_3_BLACK_SITE, true);
        beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_ID_KEY, Pings.WARNING_BEACON3);
        beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_FREQ_KEY, 1.5f);
        beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_COLOR_KEY, new Color(250,125,0,255));
        beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.GLOW_COLOR_KEY, new Color(250,55,0,255));

        SectorEntityToken cache = BaseThemeGenerator.addSalvageEntity(system, Entities.ALPHA_SITE_WEAPONS_CACHE, Factions.NEUTRAL);
        cache.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_3_WEAPONS_CACHE, true);
        cache.getLocation().set(11111, -11111);

        system.generateAnchorIfNeeded();

        NascentGravityWellAPI well = Global.getSector().createNascentGravityWell(beacon, 50f);
        well.addTag(Tags.NO_ENTITY_TOOLTIP);
        well.setColorOverride(new Color(245, 80, 145));
        hyper.addEntity(well);
        well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(beacon, 0);

        addTT3Fleet(rock, system);

        Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_TT_3_NASCENT_WELL, well);
    }

    public static NascentGravityWellAPI getWell() {
        return (NascentGravityWellAPI) Global.getSector().getMemoryWithoutUpdate().get(ZeaMemKeys.ZEA_TT_3_NASCENT_WELL);
    }

    public static void addTT3Fleet(SectorEntityToken rock, StarSystemAPI system) {
        Map<String, Integer> skills = new HashMap<>();
        skills.put(Skills.HELMSMANSHIP, 2);
        skills.put(Skills.COMBAT_ENDURANCE, 2);
        skills.put(Skills.IMPACT_MITIGATION, 2);
        skills.put(Skills.DAMAGE_CONTROL, 2);
        skills.put(Skills.FIELD_MODULATION, 2);
        skills.put(Skills.TARGET_ANALYSIS, 2);
        skills.put(Skills.SYSTEMS_EXPERTISE, 2);
        skills.put(Skills.GUNNERY_IMPLANTS, 2);

        PersonAPI TT3BossCaptain = MagicCampaign.createCaptainBuilder(Factions.TRITACHYON)
                .setIsAI(true)
                .setAICoreType(Commodities.ALPHA_CORE)
                .setPortraitId(ZeaStaticStrings.portraitAlphaPlus)
                .setLevel(8)
                .setFirstName("Alpha")
                .setLastName("(+)")
                .setGender(FullName.Gender.ANY)
                .setPersonality(Personalities.AGGRESSIVE)
                .setSkillLevels(skills)
                .create();

        TT3BossCaptain.getStats().setSkipRefresh(true);
        TT3BossCaptain.getStats().setSkillLevel(Skills.PHASE_CORPS, 1);
        TT3BossCaptain.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        TT3BossCaptain.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        TT3BossCaptain.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        TT3BossCaptain.getStats().setSkipRefresh(false);


        CampaignFleetAPI TT3BossFleet = MagicCampaign.createFleetBuilder()
                .setFleetName("Black Site Fleet")
                .setFleetFaction(Factions.TRITACHYON)
                .setFleetType(FleetTypes.TASK_FORCE)
                .setFlagshipName("TTS Nineveh")
                .setFlagshipVariant(ZeaStaticStrings.ZEA_BOSS_NINEVEH_SOULEATER)
                .setCaptain(TT3BossCaptain)
                .setMinFP(175) //Tritach fleet
                .setQualityOverride(5f)
                .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
                .setSpawnLocation(rock)
                .setIsImportant(true)
                .setTransponderOn(false)
                .create();
        TT3BossFleet.setDiscoverable(true);
        TT3BossFleet.getFleetData().ensureHasFlagship();
        TT3BossFleet.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_BOSS_TAG, true);

        // setup and permalock the flagship variant
        FleetMemberAPI flagship = TT3BossFleet.getFlagship();
        flagship.setVariant(flagship.getVariant().clone(), false, false);
        flagship.getVariant().setSource(VariantSource.REFIT);
        flagship.getVariant().addTag(ZeaMemKeys.ZEA_BOSS_TAG);
        flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);

        //Support fleet stuff
        FleetParamsV3 params = new FleetParamsV3(
                system.getLocation(),
                Factions.REMNANTS,
                5f,
                FleetTypes.PATROL_LARGE,
                175, // combatPts
                0f, // freighterPts
                0f, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                5f // qualityMod, won't get used since routes mostly have quality override set
        );
        params.averageSMods = 1;
        params.ignoreMarketFleetSizeMult = true;
        params.officerNumberMult = 3f;
        //params.officerLevelBonus = 0;
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
        params.random = new Random();

        CampaignFleetAPI support = FleetFactoryV3.createFleet(params);
        copyFleetMembers(Factions.REMNANTS, support, TT3BossFleet, false);
        support.despawn();

        TT3BossFleet.setNoFactionInName(true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
        //fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_NINEVEH_BOSS_FLEET, true);

        TT3BossFleet.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.KEEP_PLAYING_LOCATION_MUSIC_DURING_ENCOUNTER_MEM_KEY, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        TT3BossFleet.addTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG);

//      TT3BossFleet.clearAbilities();
//		TT3BossFleet.addAbility(Abilities.TRANSPONDER);
//		TT3BossFleet.getAbility(Abilities.TRANSPONDER).activate();
        TT3BossFleet.removeAbility(Abilities.TRANSPONDER);

        // so it never shows as "Unidentified Fleet" but isn't easier to spot due to using the actual transponder ability
        TT3BossFleet.setTransponderOn(false);


        Vector2f loc = new Vector2f(rock.getLocation().x + 300 * ((float) Math.random() - 0.5f),
                rock.getLocation().y + 300 * ((float) Math.random() - 0.5f));
        TT3BossFleet.setLocation(loc.x, loc.y);
        rock.getContainingLocation().addEntity(TT3BossFleet);

        TT3BossFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new TT3FIDConfig());

        TT3BossFleet.addScript(new ZigLeashAssignmentAI(TT3BossFleet, rock));

    }
}
