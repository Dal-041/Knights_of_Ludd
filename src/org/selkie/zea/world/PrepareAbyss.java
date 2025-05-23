package org.selkie.zea.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.RingSystemTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;
import org.selkie.zea.fleets.*;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.listeners.TrackFleet;
import org.selkie.zea.terrain.AbyssCorona;
import org.selkie.zea.terrain.AbyssEventHorizon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.fs.starfarer.api.impl.MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.addSalvageEntity;
import static org.selkie.zea.helpers.ZeaStaticStrings.*;
import static org.selkie.zea.helpers.ZeaUtils.checkAbyssalFleets;
import static org.selkie.zea.helpers.ZeaUtils.copyHighgradeEquipment;

public class PrepareAbyss {

	public static final Logger log = Global.getLogger(PrepareAbyss.class);

	public static void generate() {
		checkAbyssalFleets();
		copyHighgradeEquipment();
    	GenerateElysia();
    	GenerateUnderworld();
    	GenerateLunaSea();
		generateDynamicDuskHole();
		generateDynamicDuskHole();
		generateDynamicDuskHole();

		for (FactionAPI faction:Global.getSector().getAllFactions()) {
			if (faction.getId().equals(Factions.NEUTRAL)
					|| faction.getId().equals(Factions.OMEGA)
					|| faction.getId().equals("famous_bounty")) continue;
			if (!faction.getId().equals(ZeaStaticStrings.dawnID)) {
				faction.setRelationship(ZeaStaticStrings.dawnID, -100);
			}
			if (!faction.getId().equals(ZeaStaticStrings.duskID)) {
				faction.setRelationship(ZeaStaticStrings.duskID, -100);
			}
			if (!faction.getId().equals(ZeaStaticStrings.elysianID)) {
				faction.setRelationship(ZeaStaticStrings.elysianID, -100);
			}
		}
		SpawnDuskBoss.SpawnDuskBoss();
		SpawnElysianAmaterasu.SpawnElysianAmaterasu();
		SpawnElysianHeart.SpawnElysianHeart();
		SpawnDawnBoss.SpawnDawnBoss();
    }



    public static void GenerateElysia() {
    	
    	int beeg = 1500;
    	double posX = 2150;
    	double posY = 31940;

    	//Variable location
    	//posX = Math.random()%(Global.getSettings().getFloat("sectorWidth")-10000);
    	//posY = Math.random()%(Global.getSettings().getFloat("sectorHeight")-8000);
        
    	StarSystemAPI system = Global.getSector().createStarSystem(ZeaStaticStrings.elysiaSysName);
    	system.getLocation().set((int)posX, (int)posY);
    	system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS, "zea_bg_elysia"));
		system.getMemoryWithoutUpdate().set(MUSIC_SET_MEM_KEY, "music_zea_elysia_system");

		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.THEME_UNSAFE);
		system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
		system.addTag(ZeaStaticStrings.THEME_ZEA);

		system.initStar(ZeaEntities.ZEA_ELYSIA_ABYSS, ZeaStarTypes.ZEA_RED_HOLE, beeg, -beeg/2f);
		PlanetAPI elysia = system.getStar();
		elysia.setName("Elysian Abyss");
    	elysia.getSpec().setBlackHole(false); //Minimize fleet mishandling
		system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
		elysia.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
    	system.setName(ZeaStaticStrings.elysiaSysName);
    	elysia.applySpecChanges();
		elysia.addTag(Tags.NON_CLICKABLE);
		//system.addTerrain(Terrain.EVENT_HORIZON, new EventHorizonPlugin.CoronaParams(2300, 1500, elysia, -5, 0, 1));

		SectorEntityToken horizon1 = system.addTerrain(ZeaTerrain.ZEA_EVENT_HORIZON, new AbyssEventHorizon.CoronaParams(
    			5200,
				150,
				elysia,
				-13f,
				0f,
				4f));

    	SectorEntityToken elysian_nebula = Misc.addNebulaFromPNG(Global.getSettings().getSpriteName(GfxCat.TERRAIN, "zea_pinwheel_nebula"),
                                                                    0, 0, // center of nebula
                                                                    system, // location to add to
				GfxCat.TERRAIN, "nebula_zea_redgrey", // "nebula_blue", // texture to use, uses xxx_map for map
                                                                    4, 4, StarAge.AVERAGE); // number of cells in texture

    	system.setType(StarSystemType.TRINARY_1CLOSE_1FAR);
    	
    	PlanetAPI gaze = system.addPlanet(ZeaEntities.ZEA_ELYSIA_GAZE, elysia, "Watcher", StarTypes.NEUTRON_STAR, 125, 50, 9400, 60);
    	gaze.getSpec().setPulsar(true);
    	gaze.applySpecChanges();
    	system.setSecondary(gaze);
    	
		SectorEntityToken gazeBeam1 = system.addTerrain(ZeaTerrain.ZEA_PULSAR_BEAM,
				new AbyssCorona.CoronaParams(25000,
						3250,
						gaze,
						50f,
						1f,
						6f)
		);
		gazeBeam1.setCircularOrbit(gaze, (float)(Math.random() * 360), 0, 15);

		SectorEntityToken gazeBeam2 = system.addTerrain(ZeaTerrain.ZEA_PULSAR_BEAM,
				new AbyssCorona.CoronaParams(25000,
						3250,
						gaze,
						50f,
						1f,
						6f)
		);
		gazeBeam2.setCircularOrbit(gaze, (float)(Math.random() * 360), 0, 16);
    	
    	PlanetAPI silence = system.addPlanet(ZeaEntities.ZEA_ELYSIA_SILENCE, elysia, "Silence", StarTypes.BLUE_SUPERGIANT, 255, 1666, 18500, 0);
    	system.setTertiary(silence);
    	silence.setFixedLocation(-14000, -16500);
    	
		SectorEntityToken silence_corona = system.addTerrain(ZeaTerrain.ZEA_CORONA,
				new AbyssCorona.CoronaParams(7000,
						0,
						silence,
						3f,
						0.1f,
						1f)
		);
		silence_corona.setCircularOrbit(silence, 0, 0, 15);

		PlanetAPI first = system.addPlanet(ZeaEntities.ZEA_ELYSIA_PLANET_ONE, elysia, "Asclepius", Planets.BARREN_BOMBARDED, (float)(Math.random() * 360), 100, 4200, 40);
    	PlanetAPI second = system.addPlanet(ZeaEntities.ZEA_ELYSIA_PLANET_TWO, elysia, "Appia", ZeaStaticStrings.TOXIC, (float)(Math.random() * 360), 100, 3750, 30);
    	PlanetAPI third = system.addPlanet(ZeaEntities.ZEA_ELYSIA_PLANET_THREE, elysia, "Orpheus", Planets.IRRADIATED, (float)(Math.random() * 360), 100, 3300, 24);

    	first.getMarket().addCondition(Conditions.IRRADIATED);
    	first.getMarket().addCondition(Conditions.METEOR_IMPACTS);
    	first.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		first.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);

    	second.getMarket().addCondition(Conditions.HIGH_GRAVITY);
    	second.getMarket().addCondition(Conditions.IRRADIATED);
		second.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
    	second.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
		second.getMarket().addCondition(Conditions.RUINS_EXTENSIVE);

    	third.getMarket().addCondition(Conditions.HIGH_GRAVITY);
    	third.getMarket().addCondition(Conditions.IRRADIATED);
    	third.getMarket().addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY);
    	third.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);

		system.addRingBand(elysia, GfxCat.TERRAIN, ZeaTerrain.RINGS_THICC_DARKRED, 1000, 0, Color.gray, 2300, 3884, 27, Terrain.RING, "Accretion Disk");

		SectorEntityToken ring = system.addTerrain(Terrain.RING, new RingParams(456, 3200, null, "Call of the Void"));
		ring.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring2 = system.addTerrain(Terrain.RING, new RingParams(456, 3656, null, "Call of the Void"));
		ring2.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring3 = system.addTerrain(Terrain.RING, new RingParams(456, 4112, null, "Call of the Void"));
		ring3.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring4 = system.addTerrain(Terrain.RING, new RingParams(456, 4678, null, "Call of the Void"));
		ring4.setCircularOrbit(elysia, 0, 0, 100);
		
		system.addAsteroidBelt(elysia, 200, 3128, 256, 20, 15, ZeaTerrain.ZEA_ASTEROID_BELT, null);
		system.addAsteroidBelt(elysia, 200, 3516, 512, 30, 22, ZeaTerrain.ZEA_ASTEROID_BELT, null);
		system.addAsteroidBelt(elysia, 200, 4024, 1024, 40, 30, ZeaTerrain.ZEA_ASTEROID_BELT, null);
		system.addAsteroidBelt(elysia, 200, 4536, 1024, 40, 40, ZeaTerrain.ZEA_ASTEROID_BELT, null);

		//generateBlackholeSpiral(elysia.getStarSystem(), elysia, 8000f, Color.red, Color.red);


		//Placed entities
		SectorEntityToken hypershunt = system.addCustomEntity(ZeaEntities.ZEA_EDF_CORONAL_TAP, "Coronal Hypershunt", ZeaEntities.ZEA_EDF_CORONAL_TAP, Factions.NEUTRAL);
		hypershunt.setCircularOrbitPointingDown(silence, (float)Math.random() * 360f, silence.getRadius() + 500f, 1111);
		hypershunt.addTag(ZeaStaticStrings.EDF_HYPERSHUNT);
		SectorEntityToken edfResearchStation = addSalvageEntity(system, ZeaEntities.ZEA_RESEARCH_STATION_ELYSIA, ZeaStaticStrings.elysianID);
		edfResearchStation.setCircularOrbitPointingDown(gaze, (float)Math.random() * 360f, gaze.getRadius() + 100f, 51);
		edfResearchStation.addTag(ZeaStaticStrings.EDF_HEADQUARTERS);

		//Hegemony witness
		SectorEntityToken wreckHeg = MagicCampaign.createDerelict("dominator_XIV_Elite", ShipRecoverySpecial.ShipCondition.WRECKED, true, 5000, false, silence, (float) Math.random() * 360f, 3333f + (float)Math.random()*4777f, 200);
		wreckHeg.addTag(Tags.UNRECOVERABLE);
		wreckHeg.addDropRandom(ZeaDrops.ZEA_HEGFLEET_LORE, 1);
		wreckHeg.addDropRandom(Drops.LOW_WEAPONS2, 6);
		wreckHeg.addDropRandom(Drops.ANY_HULLMOD_HIGH, 2);
		//wreckHeg.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
		wreckHeg.getMemoryWithoutUpdate().set(ZeaStaticStrings.ZeaMemKeys.ZEA_ELYSIAN_WITNESS, true);


		//Random loot
        SectorEntityToken derelict1 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.elysianID, 1f), ZeaStaticStrings.elysianID);
        derelict1.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3100, 20);
        SectorEntityToken derelict2 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.elysianID, 1f), ZeaStaticStrings.elysianID);
        derelict2.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3350, 25);
        SectorEntityToken derelict3 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.elysianID, 1f), ZeaStaticStrings.elysianID);
        derelict3.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3600, 30);
        SectorEntityToken derelict4 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.elysianID, 1f), ZeaStaticStrings.elysianID);
        derelict4.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3900, 35);
        SectorEntityToken derelict5 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.elysianID, 1f), ZeaStaticStrings.elysianID);
        derelict5.setCircularOrbit(elysia, (float)(Math.random() * 360f), 4250, 40);
    	
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(ZeaEntities.ZEA_ELYSIA_JP, "First Trial");
        OrbitAPI orbit = Global.getFactory().createCircularOrbit(first, 90, 100, 25);
        jumpPoint.setOrbit(orbit);
        jumpPoint.setRelatedPlanet(first);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);
        
        system.autogenerateHyperspaceJumpPoints(false, false); //begone evil clouds
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		//FP bumped to account for backup capital ships getting pruned
		ZeaFleetManager fleets = new ZeaFleetManager(system, ZeaStaticStrings.elysianID, 6, 60, 180);
		ZeaFleetManager fleetsMiniboss = new ZeaFleetManager(system, ZeaStaticStrings.elysianID, 12, 180, 340);
		system.addScript(fleets);
		system.addScript(fleetsMiniboss);

		EveryFrameScript tracker = new TrackFleet();
		system.addScript(tracker);

    }

	public static void GenerateUnderworld() {

		// No direct access, see ReportTransit and TrackFleet listeners

		StarSystemAPI system = Global.getSector().createStarSystem(ZeaStaticStrings.nullspaceSysName);
		system.setName(ZeaStaticStrings.nullspaceSysName);

		LocationAPI hyper = Global.getSector().getHyperspace();
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.THEME_UNSAFE);
		system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
		system.addTag(ZeaStaticStrings.THEME_ZEA);
		system.addTag(ZeaStaticStrings.THEME_STORM);

		system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS,"zea_bg_dusk"));
		system.getMemoryWithoutUpdate().set(MUSIC_SET_MEM_KEY, "music_zea_underworld_theme");

		system.getLocation().set(2100, -5200);
		SectorEntityToken center = system.initNonStarCenter();
		SectorEntityToken nullspace_nebula = addNebulaFromPNG(
				Global.getSettings().getSpriteName(GfxCat.TERRAIN,"zea_pinwheel_nebula_big"),
				0, 0, // center of nebula
				system, // location to add to
				GfxCat.TERRAIN, ZeaTerrain.NEBULA_ZEA_BLACK_SHINY, // texture to use, uses xxx_map for map
				4, 4, // number of cells in texture
				ZeaTerrain.NEBULA_ZEA_STORM, StarAge.AVERAGE, 10000, 2); //terrain plugin, age, nebula resolution, tile size

		system.setLightColor(new Color(225,170,255,255)); // light color in entire system, affects all entities
		new AbyssBackgroundWarper(system, 8, 0.125f);

		PlanetAPI starVoid = system.addPlanet(ZeaEntities.ZEA_NULLSPACE_VOID, system.getCenter(), "Void", ZeaStarTypes.ZEA_WHITE_HOLE, 135, 166, 12500, 0);
		//system.setStar(starVoid);

		starVoid.setFixedLocation(-5512, 9420);

		SectorEntityToken void_corona = system.addTerrain(ZeaTerrain.ZEA_CORONA,
				new AbyssCorona.CoronaParams(1200,
						0,
						starVoid,
						10f,
						0.1f,
						3f)
		);
		void_corona.setCircularOrbit(starVoid, 0, 0, 15);

		//system.generateAnchorIfNeeded();

		SectorEntityToken gate = system.addCustomEntity(ZeaEntities.ZEA_NULLGATE_DUSK, "Nullgate", Entities.INACTIVE_GATE, Factions.DERELICT);
		gate.setCircularOrbit(center, (float)(Math.random() * 360f), 15000, 1000);

		SectorEntityToken stationResearch = system.addCustomEntity(ZeaEntities.ZEA_NULL_STATION_DUSK, "Shielded Research Station", ZeaEntities.ZEA_NULL_STATION_DUSK, Factions.DERELICT);
		stationResearch.setFixedLocation(-5230, 8860);

		float count = ZeaStaticStrings.uwDerelictsNormal.length; //16
		float odds = 0.45f;
		count *= odds;
		//int target = 5; //3 entries, some redundancy
		//float oddsLore = target/count;
		for(String variant: ZeaStaticStrings.uwDerelictsNormal) {
			if (MathUtils.getRandom().nextFloat() <= odds) {
				SectorEntityToken wreck = MagicCampaign.createDerelict(
						variant,
						ShipRecoverySpecial.ShipCondition.WRECKED,
						true,
						100,
						false,
						system.getCenter(),
						170 + (15f*(float)Math.random()),
						7450 + (7000*(float)Math.random()),
						100000
				);
				wreck.setFacing((float)Math.random()*360f);
				if (MathUtils.getRandom().nextFloat() > 0.33f || variant.contains("paragon") || variant.contains("astral")) wreck.addTag(Tags.UNRECOVERABLE);
				if (MathUtils.getRandom().nextFloat() < 0.5f) wreck.addDropRandom(ZeaDrops.ZEA_TTFLEET_LORE, 1);
				//system.addEntity(wreck);
			}
		}
		for(String variant: ZeaStaticStrings.uwDerelictsPhase) {
			if (MathUtils.getRandom().nextFloat() <= 0.66f) {
				SectorEntityToken wreck = MagicCampaign.createDerelict(
						variant,
						ShipRecoverySpecial.ShipCondition.AVERAGE,
						true,
						100,
						false,
						system.getCenter(),
						170 + (15f*(float)Math.random()),
						6700 + (7000f*(float)Math.random()),
						100000
				);
				wreck.setFacing((float)Math.random()*360f);
				if (MathUtils.getRandom().nextFloat() > 0.4f) wreck.addTag(Tags.UNRECOVERABLE);
			}
		}
	}

	public static void GenerateLunaSea() {

		StarSystemAPI system = Global.getSector().createStarSystem(lunaSeaSysName);
		system.setName(lunaSeaSysName); //No "-Star System"

		LocationAPI hyper = Global.getSector().getHyperspace();

		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.THEME_UNSAFE);
		system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
		system.addTag(ZeaStaticStrings.THEME_ZEA);

		system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS,"zea_bg_dawn"));
		new AbyssBackgroundWarper(system, 8, 0.25f);

		//if (!Global.getSector().getDifficulty().equals(Difficulties.EASY)) { // Disabled
			SectorEntityToken lunasea_nebula = Misc.addNebulaFromPNG(Global.getSettings().getSpriteName(GfxCat.TERRAIN,"zea_flower_nebula_cut"),
					0, 0, // center of nebula
					system, // location to add to
					GfxCat.TERRAIN, "nebula_zea_purpleblue", // "nebula_blue", // texture to use, uses xxx_map for map
					8, 8, StarAge.AVERAGE); // number of cells in texture
		/*} else {
			SectorEntityToken lunasea_nebula = Misc.addNebulaFromPNG("graphics/zea/terrain/luwunasea_nebula2.png",
					0, 0, // center of nebula
					system, // location to add to
					ZeaStaticStrings.TERRAIN, "nebula_zea_purpleblue", // "nebula_blue", // texture to use, uses xxx_map for map
					4, 4, StarAge.AVERAGE); // number of cells in texture
		}*/

		SectorEntityToken lunasea_nebula2 = Misc.addNebulaFromPNG(Global.getSettings().getSpriteName(GfxCat.TERRAIN,"zea_flower_nebula_layer2"),
					0, 0, system,
				GfxCat.TERRAIN, "nebula_zea_dawntide",
					4, 4, ZeaTerrain.NEBULA_ZEA_SHOAL, StarAge.AVERAGE);

		system.getMemoryWithoutUpdate().set(MUSIC_SET_MEM_KEY, "music_zea_lunasea_theme");

		PlanetAPI luna = system.initStar(ZeaEntities.ZEA_LUNASEA_STAR, StarTypes.BLUE_SUPERGIANT, 2500, 54500, -42100, 0);
		if (Global.getSector().getDifficulty().equals(Difficulties.EASY)) {
			luna.setName("The Luwuna Sea");
		}
		else {luna.setName(lunaSeaSysName);}
		system.setDoNotShowIntelFromThisLocationOnMap(true);

		SectorEntityToken star_corona = system.addTerrain(ZeaTerrain.ZEA_CORONA,
				new AbyssCorona.CoronaParams(11000,
						0,
						luna,
						5f,
						0.1f,
						1f)
		);

		SectorEntityToken lunaBeam1 = system.addTerrain(ZeaTerrain.ZEA_SEA_WAVE,
				new AbyssCorona.CoronaParams(50000,
						2500,
						luna,
						25f,
						1f,
						10f)
		);
		lunaBeam1.setCircularOrbit(luna, (float)Math.random()*360, 0, 19);

		//system.generateAnchorIfNeeded();
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(ZeaEntities.ZEA_LUNASEA_JP, "Second Trial");
		//OrbitAPI orbit = Global.getFactory().createCircularOrbit(luna, 90, 10000, 25);
		jumpPoint.setFixedLocation(6500,-1500);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		PlanetAPI first = system.addPlanet(ZeaEntities.ZEA_LUNASEA_PLANET_ONE, luna, "Id", Planets.BARREN_BOMBARDED, 50, 150, 10900, 3000000);
		PlanetAPI second = system.addPlanet(ZeaEntities.ZEA_LUNASEA_PLANET_TWO, luna, "Doubt", Planets.DESERT, 29, 170, 6100, 3000000);
		PlanetAPI third = system.addPlanet(ZeaEntities.ZEA_LUNASEA_PLANET_THREE, luna, "Wild",  ZeaStaticStrings.JUNGLE, 12, 70, 15800, 3000000);
		PlanetAPI fourth = system.addPlanet(ZeaEntities.ZEA_LUNASEA_PLANET_FOUR, jumpPoint, "Ego", Planets.FROZEN, 190, 250, 500, 3000000);
		PlanetAPI fifth = system.addPlanet(ZeaEntities.ZEA_LUNASEA_PLANET_FIVE, luna, "Savage", Planets.BARREN, 336, 145, 12300, 3000000);
		PlanetAPI sixth = system.addPlanet(ZeaEntities.ZEA_LUNASEA_PLANET_SIX, luna, "Feral", ZeaStaticStrings.TOXIC, 306, 165, 8100, 3000000);

		first.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
		first.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		first.getMarket().addCondition(Conditions.METEOR_IMPACTS);
		first.getMarket().addCondition(Conditions.RARE_ORE_ABUNDANT);

		second.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		second.getMarket().addCondition(Conditions.HOT);
		second.getMarket().addCondition(Conditions.EXTREME_WEATHER);
		second.getMarket().addCondition(Conditions.ORE_ABUNDANT);
		second.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);

		third.getMarket().addCondition(Conditions.INIMICAL_BIOSPHERE);
		third.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
		third.getMarket().addCondition(Conditions.ORGANICS_PLENTIFUL);

		fourth.getMarket().addCondition(Conditions.COLD);
		fourth.getMarket().addCondition(Conditions.IRRADIATED);
		fourth.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		fourth.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
		fourth.getMarket().addCondition(Conditions.AI_CORE_ADMIN);

		fifth.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		fifth.getMarket().addCondition(Conditions.ORE_ABUNDANT);

		sixth.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
		sixth.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
		sixth.getMarket().addCondition(Conditions.ORGANICS_COMMON);

		system.autogenerateHyperspaceJumpPoints(false, false); //begone evil clouds
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize();

		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		SectorEntityToken loot1 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.dawnID, 0.66f), ZeaStaticStrings.dawnID);
		loot1.setCircularOrbitPointingDown(luna, (float)Math.random()*60+47, (float)Math.random()*2240+3000, 100000);
		SectorEntityToken loot2 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.dawnID, 0.66f), ZeaStaticStrings.dawnID);
		loot2.setCircularOrbitPointingDown(luna, (float)Math.random()*20+28, (float)Math.random()*3330+18330, 100000);
		SectorEntityToken loot3 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.dawnID, 0.66f), ZeaStaticStrings.dawnID);
		loot3.setCircularOrbitPointingDown(third, (float)Math.random()*50+71, (float)Math.random()*2155+3500, 100000);
		SectorEntityToken loot4 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.dawnID, 0.66f), ZeaStaticStrings.dawnID);
		loot4.setCircularOrbitPointingDown(fourth, 171, (float)Math.random()*1500+1000, 100000);
		SectorEntityToken loot5 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.dawnID, 0.66f), ZeaStaticStrings.dawnID);
		loot5.setCircularOrbitPointingDown(fifth, (float)Math.random()*65+245, (float)Math.random()*2000+5353, 100000);
		SectorEntityToken loot6 = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.dawnID, 0.66f), ZeaStaticStrings.dawnID);
		loot6.setCircularOrbitPointingDown(sixth, (float)Math.random()*60+177, (float)Math.random()*2770+3000, 100000);

		//FP bumped to account for backup capital ships getting pruned
		ZeaFleetManager fleets = new ZeaFleetManager(system, ZeaStaticStrings.dawnID, 16, 60, 180);
		ZeaFleetManager fleetsMiniboss = new ZeaFleetManager(system, ZeaStaticStrings.dawnID, 4, 300, 425);
		system.addScript(fleets);
		system.addScript(fleetsMiniboss);
	}

	public static void generateDynamicDuskHole() {
		//Variable location
		Vector2f pos = Objects.requireNonNull(getRandomHyperspaceCoordForSystem());

		StarSystemAPI system = Global.getSector().createStarSystem(ProcgenUsedNames.pickName(NameGenData.TAG_STAR, null, null).nameWithRomanSuffixIfAny);
		try {
			system.getLocation().set(pos.getX(), pos.getY());
		} catch (NullPointerException e) {
			log.error("ZEA: Could not find valid location for Black Pulsar!");
			return;
		}
		if (Math.random() < 0.5f) {
			system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS, "zea_bg_duskbh1"));
		} else {
			system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS, "zea_bg_duskbh2"));
		}
		system.getMemoryWithoutUpdate().set(MUSIC_SET_MEM_KEY, "music_zea_underworld_theme");

		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_UNSAFE);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
		system.addTag(ZeaStaticStrings.THEME_ZEA);

		//fancy bg script

		PlanetAPI sing = system.initStar(ZeaStaticStrings.nbsSysPrefix + Misc.genUID(), ZeaStarTypes.ZEA_STAR_BLACK_NEUTRON, 200, -200f);
		String tempName = system.getBaseName();
		system.setName(tempName);
		sing.setName(tempName);
		ProcgenUsedNames.notifyUsed(system.getBaseName());
		sing.getSpec().setBlackHole(false); //Avoid fleet weirdness
		sing.applySpecChanges();

		SectorEntityToken horizon1 = system.addTerrain(ZeaTerrain.ZEA_EVENT_HORIZON, new AbyssEventHorizon.CoronaParams(
				1000,
				20,
				sing,
				-18f,
				0f,
				5f)
		);

		SectorEntityToken dbhBeam1 = system.addTerrain(ZeaTerrain.ZEA_BLACK_BEAM,
				new AbyssCorona.CoronaParams(25000,
						50,
						sing,
						-50f,
						1f,
						8f)
		);
		dbhBeam1.setCircularOrbit(sing, (float)(Math.random() * 360), 0, 20);

		StarSystemGenerator.addOrbitingEntities(system, sing, StarAge.YOUNG, 1, 2, 2500, 0, false, false);

		//sophistimacated
		float orbRadius1 = 1000 + (float)(Math.random() * 8500f);
		SectorEntityToken cacheRem = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.duskID, 1f), ZeaStaticStrings.duskID);
		cacheRem.setCircularOrbit(sing, (float)(Math.random() * 360f), orbRadius1, orbRadius1/10f);

		float orbRadius2 = 1500 + (float)(Math.random()*10500f);
		SectorEntityToken stationResearch = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.duskID, 0.5f), ZeaStaticStrings.duskID);
		stationResearch.setCircularOrbit(sing, (float)(Math.random() * 360f), orbRadius2, orbRadius2/10f);

		float orbRadius3 = 2000 + (float)(Math.random()*12500f);
		SectorEntityToken cacheRemSmall = addSalvageEntity(system, getAbyssLootID(ZeaStaticStrings.duskID, 1.5f), ZeaStaticStrings.duskID);
		cacheRemSmall.setCircularOrbit(sing, (float)(Math.random() * 360f), orbRadius3, orbRadius3/10f);

		system.autogenerateHyperspaceJumpPoints(true, true); //begone evil clouds
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 1.5f;

		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		//FP bumped to account for backup capital ships getting pruned
		ZeaFleetManager fleets = new ZeaFleetManager(system, ZeaStaticStrings.duskID, 6, 70, 175);
		ZeaFleetManager fleetsMiniboss = new ZeaFleetManager(system, ZeaStaticStrings.duskID, 3, 300, 425);
		system.addScript(fleets);
		system.addScript(fleetsMiniboss);
	}

	public static String getAbyssLootID(String faction) {
		return getAbyssLootID(faction, 1, new Random());
	}

	public static String getAbyssLootID(String faction, float tier) {
		return getAbyssLootID(faction, tier, new Random());
	}

	public static String getAbyssLootID(String faction, float tier, Random random) {
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(random);
		float w = tier; //Tier 1 has best loot odds, values < 1 even better.
		picker.add(ZeaEntities.ZEA_CACHE_LOW, w);
		picker.add(ZeaEntities.ZEA_CACHE_MED, w);
		picker.add(ZeaEntities.ZEA_CACHE_HIGH, w);
		w = faction.equals(ZeaStaticStrings.dawnID)? 1f : 0f;
		picker.add(ZeaEntities.ZEA_RESEARCH_STATION_DAWN, w);
		w = faction.equals(ZeaStaticStrings.duskID)? 1f : 0f;
		picker.add(ZeaEntities.ZEA_RESEARCH_STATION_DUSK, w);
		w = faction.equals(ZeaStaticStrings.elysianID)? 1f : 0f;
		picker.add(ZeaEntities.ZEA_RESEARCH_STATION_ELYSIA, w);

		return picker.pick();
	}

	//Same as the method in Misc, but allows custom chunkSizes and tilesize mults
	public static SectorEntityToken addNebulaFromPNG(String image, float centerX, float centerY, LocationAPI location,
													 String category, String key, int tilesWide, int tilesHigh,
													 String terrainType, StarAge age, int chunkSize, float tileMult) {
		try {
			BufferedImage img = null;
			//img = ImageIO.read(new File("../starfarer.res/res/data/campaign/terrain/nebula_test.png"));
			img = ImageIO.read(Global.getSettings().openStream(image));

			// int chunkSize = 10000;
			int w = img.getWidth();
			int h = img.getHeight();
			Raster data = img.getData();
			for (int i = 0; i < w; i += chunkSize) {
				for (int j = 0; j < h; j += chunkSize) {

					int chunkWidth = chunkSize;
					if (i + chunkSize > w) chunkWidth = w - i;
					int chunkHeight = chunkSize;
					if (j + chunkSize > h) chunkHeight = h - i;

//		    		boolean hasAny = false;
//		    		for (int x = i; x < i + chunkWidth; x++) {
//		    			for (int y = j; y < j + chunkHeight; y++) {
//		    				int [] pixel = data.getPixel(i, h - j - 1, (int []) null);
//		    				int total = pixel[0] + pixel[1] + pixel[2];
//		    				if (total > 0) {
//		    					hasAny = true;
//		    					break;
//		    				}
//		    			}
//		    		}
//		    		if (!hasAny) continue;

					StringBuilder string = new StringBuilder();
					for (int y = j + chunkHeight - 1; y >= j; y--) {
						for (int x = i; x < i + chunkWidth; x++) {
							int [] pixel = data.getPixel(x, h - y - 1, (int []) null);
							int total = pixel[0] + pixel[1] + pixel[2];
							if (total > 0) {
								string.append("x");
							} else {
								string.append(" ");
							}
						}
					}

					float tileSize = NebulaTerrainPlugin.TILE_SIZE*tileMult;
					float x = centerX - tileSize * (float) w / 2f + (float) i * tileSize + chunkWidth / 2f * tileSize;
					float y = centerY - tileSize * (float) h / 2f + (float) j * tileSize + chunkHeight / 2f * tileSize;

					SectorEntityToken curr = location.addTerrain(terrainType, new BaseTiledTerrain.TileParams(string.toString(),
							chunkWidth, chunkHeight,
							category, key, tilesWide, tilesHigh, null));
					curr.getLocation().set(x, y);

					if (location instanceof StarSystemAPI) {
						StarSystemAPI system = (StarSystemAPI) location;

						system.setAge(age);
						system.setHasSystemwideNebula(true);
					}

					return curr;
				}
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Vector2f getRandomHyperspaceCoordForSystem() {
		int attempts = 0;
		while (attempts < 500) {
			float rW = (float) Math.random()*(Global.getSettings().getFloat("sectorWidth")/2);
			float rH = (float) Math.random()*(Global.getSettings().getFloat("sectorHeight")/2);
			float x = MathUtils.getRandomNumberInRange(-rW, rW);
			float y = MathUtils.getRandomNumberInRange(-rH, rH);

			if (!isWithinOrionAbyss(x, y) && !isWithinCoreSpace(x, y)) {
				return new Vector2f(x, y);
			}
			attempts++;
		}
		return null;
	}

	@SuppressWarnings("DataFlowIssue")
    public static boolean isWithinCoreSpace(float x, float y) {
		float coreW = 20000;
		float coreH = 20000;
		Vector2f center = new Vector2f(0,0);
		Vector2f candidate = new Vector2f(x, y);

		if (MathUtils.getDistance(center, candidate) <= (Math.max(coreW, coreH)+5000)) return true;
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.hasTag(Tags.THEME_CORE) || system.hasTag(Tags.THEME_CORE_POPULATED)) {
				if (MathUtils.getDistance(system.getLocation(), candidate) < 10000) return true;
			}
		}

		return false;
	}

	public static boolean isWithinOrionAbyss(Vector2f loc) {
		return isWithinOrionAbyss(loc.getX(), loc.getY());
	}

	public static boolean isWithinOrionAbyss(float x, float y) {
		float w = -Global.getSettings().getFloat("sectorWidth")/2f;
		float h = -Global.getSettings().getFloat("sectorHeight")/2f;

		// Box bounds, rough
		//-w +8000, -h + 18500
		//-w +8000 + 15000, -h + 16000
		//-w +8000 + 15000 + 10000, -h + 7500

		if (x < w+8000 && y < h+18500) return true;
		if (x < w+8000+15000 && y < h+16000) return true;
        return x < w + 8000 + 15000 + 10000 && y < h + 7500;
    }

	public static StarSystemGenerator.GenResult generateBlackholeSpiral(StarSystemAPI system, SectorEntityToken parent, Float orbitRadius, Color minColor, Color maxColor) {

		//float orbitRadius = context.currentRadius * (2f + 2f * StarSystemGenerator.random.nextFloat());
		///float orbitRadius = context.currentRadius * (2f + 2f * StarSystemGenerator.random.nextFloat());

		float bandWidth = 256f;

		//int numBands = (int) (2f + StarSystemGenerator.random.nextFloat() * 5f);
		int numBands = 8;

		float spiralFactor = 3f + StarSystemGenerator.random.nextFloat() * 2f;
		numBands += (int) spiralFactor;

		numBands = 12;
//		boolean leaveRoomInMiddle = context.system.getStar() != null &&
//									parent == context.system.getCenter() &&
//									Misc.getDistance(context.system.getStar().getLocation(), parent.getLocation()) > 100;
		for (float i = 0; i < numBands; i++) {
//			float radiusMult = 0.25f + 0.75f * (i + 1f) / (numBands);
//			radiusMult = 1f;
			//float radius = orbitRadius * radiusMult;
			float radius = orbitRadius - i * bandWidth * 0.25f - i * bandWidth * 0.1f;
			//float radius = orbitRadius - i * bandWidth / 2;

			AccretionDiskGenPlugin.TexAndIndex tex = getTexAndIndex();
			float orbitDays = radius / (30f + 10f * StarSystemGenerator.random.nextFloat());
			Color color = StarSystemGenerator.getColor(minColor, maxColor);
			//color = Color.white;
			RingBandAPI visual = system.addRingBand(parent, GfxCat.MISC, tex.tex, 256f, tex.index, color, bandWidth,
					radius + bandWidth / 2f, -orbitDays);

			spiralFactor = 2f + StarSystemGenerator.random.nextFloat() * 5f;
			visual.setSpiral(true);
			visual.setMinSpiralRadius(0);
			visual.setSpiralFactor(spiralFactor);
		}


		List<SectorEntityToken> rings = new ArrayList<>();
		SectorEntityToken ring = system.addTerrain(Terrain.RING, new RingParams(orbitRadius, orbitRadius / 2f, parent, null));
		ring.addTag(Tags.ACCRETION_DISK);
		if (((CampaignTerrainAPI)ring).getPlugin() instanceof RingSystemTerrainPlugin) {
			((RingSystemTerrainPlugin)((CampaignTerrainAPI)ring).getPlugin()).setNameForTooltip("Accretion Disk");
		}

		ring.setCircularOrbit(parent, 0, 0, -100);

		rings.add(ring);

		StarSystemGenerator.GenResult result = new StarSystemGenerator.GenResult();
		result.onlyIncrementByWidth = false;
		result.orbitalWidth = orbitRadius;
		result.entities.addAll(rings);
		return result;
	}

	protected static AccretionDiskGenPlugin.TexAndIndex getTexAndIndex() {
		AccretionDiskGenPlugin.TexAndIndex result = new AccretionDiskGenPlugin.TexAndIndex();
		WeightedRandomPicker<Integer> indexPicker = new WeightedRandomPicker<>(StarSystemGenerator.random);

		WeightedRandomPicker<String> ringSet = new WeightedRandomPicker<>(StarSystemGenerator.random);
		ringSet.add("ring_ice", 10f);
		ringSet.add("ring_dust", 10f);
		//ringSet.add("ring_special", 1f);

		String set = ringSet.pick();

		if (set.equals("ring_ice")) {
			result.tex = "rings_ice0";
			indexPicker.add(0);
			indexPicker.add(1);
		} else if (set.equals("ring_dust")) {
			result.tex = "rings_dust0";
			indexPicker.add(0);
			indexPicker.add(1);
		}

		result.index = indexPicker.pick();

		return result;
	}

}