package org.selkie.kol.impl.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NameGenData;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.fleets.*;
import org.selkie.kol.impl.listeners.TrackFleet;
import org.selkie.kol.impl.terrain.AbyssCorona;
import org.selkie.kol.impl.terrain.AbyssEventHorizon;

import java.awt.*;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.addSalvageEntity;

public class PrepareAbyss {

	public static final String dawnID = "kol_dawn";
	public static final String duskID = "kol_dusk";
	public static final String elysianID = "kol_elysians";
	public static final String undergateID = "kol_undergate";
	public static boolean useDomres = false;
	public static boolean useLostech = false;
	public static boolean useDustkeepers = false;
	public static boolean useEnigma = false;
	public static String[] hullBlacklist = {
			"guardian",
			"radiant",
			"tahlan_nirvana"
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
			"abyss_boss_nineveh_Souleater",
			"abyss_boss_nineveh_Souleater",
			"abyss_boss_ninmah_Skinthief",
			"abyss_boss_ninmah_Skinthief",
			"abyss_boss_ninaya_Gremlin",
			"abyss_boss_ninaya_Gremlin",
			"abyss_boss_ninaya_Nightdemon"
	};
	public static final String[] elysianBossSupportingFleet = {
			"abyss_edf_tamamo_Striker",
			"abyss_edf_tamamo_Striker"
	};

    public static void generate(SectorAPI sector) {
		prepareAbyssalFleets();

    	GenerateElysia(sector);
    	GenerateUnderworld(sector);
    	GenerateLunaSea(sector);
		generateDynamicDuskHole(sector);
		generateDynamicDuskHole(sector);
		generateDynamicDuskHole(sector);

		for (FactionAPI faction:Global.getSector().getAllFactions()) {
			if (!faction.getId().equals(dawnID)) {
				faction.setRelationship(dawnID, -100);
			}
			if (!faction.getId().equals(duskID)) {
				faction.setRelationship(duskID, -100);
			}
			if (!faction.getId().equals(elysianID)) {
				faction.setRelationship(elysianID, -100);
			}
		}
		SpawnDuskBoss.SpawnDuskBoss();
		SpawnElysianAmaterasu.SpawnElysianAmaterasu();
		SpawnElysianHeart.SpawnElysianHeart();
		SpawnDawnBoss.SpawnDawnBoss();
    }

	public static void prepareAbyssalFleets() {
		if(Global.getSettings().getModManager().isModEnabled("lost_sector")) {
			for (FactionAPI faction:Global.getSector().getAllFactions()) {
				if (faction.getId().equals("enigma")) useEnigma = true;
			}
		}
		if(Global.getSettings().getModManager().isModEnabled("lost_sector")) {
			useLostech = true;
		}
		for (FactionAPI faction:Global.getSector().getAllFactions()) {
			if (faction.getId().equals("domres")) useDomres = true;
			if (faction.getId().equals("sotf_dustkeepers")) useDustkeepers = true;
		}
	}

	public static void copyFleetMembers(String fromID, CampaignFleetAPI from, CampaignFleetAPI to) {
		for (FleetMemberAPI member : from.getMembersWithFightersCopy()) {
			boolean skip = false;
			for (String s : hullBlacklist) {
				if (s.equals(member.getHullId())) {
					skip = true;
				}
			}
			if (skip) continue;
			member.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
			if (member.getVariant().hasTag("auto_rec")) member.getVariant().removeTag("auto_rec");
			if (member.isFlagship()) member.setFlagship(false);
			member.setShipName(Global.getSector().getFaction(fromID).pickRandomShipName());
			to.getFleetData().addFleetMember(member);
		}
	}

	/*
    POOL A
        comprises 60% of the fleet
        is always the star system owner (ie, luna sea fleets will always be 60% dawntide boats)

    POOL B
        randomly picked
        can be one of the other factions that is NOT the star system owner (ie, if luna sea fleet, pool B can potentially also have Dusk or Elysian ships)
        only boss fleets should ever be 100% single-faction
        [?] never spawns capitals/supercapitals
        the following can also be used :
        Remnants
        Domain Derelicts
        Domres (HMI)
        Lostech (Tahlan)
        Dustkeepers (SOTF)
        DAWNTIDE-ONLY: Enigma (LOST_SECTOR)
     */

	/*
	    for (String ship : Global.getSector().getFaction(Factions.A).getKnownShips()) {
            if (!Global.getSector().getFaction(B).knowsShip(ship)) {
                Global.getSector().getFaction(B).addKnownShip(ship, true);
            }
        }
        for (String baseShip : Global.getSector().getFaction(Factions.A).getAlwaysKnownShips()) {
            if (!Global.getSector().getFaction(B).useWhenImportingShip(baseShip)) {
                Global.getSector().getFaction(B).addUseWhenImportingShip(baseShip);
            }
        }
        etc
	 */

    public static void GenerateElysia(SectorAPI sector) {
    	
    	int beeg = 1500;
    	double posX = 2600;
    	double posY = 24900;
    	
    	//Variable location
    	//posX = Math.random()%(Global.getSettings().getFloat("sectorWidth")-10000);
    	//posY = Math.random()%(Global.getSettings().getFloat("sectorHeight")-8000);
        
    	StarSystemAPI system = sector.createStarSystem("Elysia");
    	system.getLocation().set((int)posX, (int)posY);
    	system.setBackgroundTextureFilename("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/backgrounds/abyss_bg_elysia.png");
		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_kol_elysia_theme");

		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.THEME_UNSAFE);

    	//PlanetAPI elysia = system.initStar("Elysian_Abyss", StarTypes.BLACK_HOLE, beeg, -750f);
		system.initNonStarCenter();
    	PlanetAPI elysia = system.addPlanet("abyss_elysia_abyss", system.getCenter(), "Elysia", "red_hole", 0f, beeg, 0f, 100000f);

    	elysia.getSpec().setBlackHole(true);
    	system.setName("Elysian Abyss");
    	elysia.applySpecChanges();
    	SectorEntityToken horizon1 = system.addTerrain("kol_eventHorizon", new AbyssEventHorizon.CoronaParams(
    			4000,
				250,
				elysia,
				-10f,
				0f,
				5f)
    		);
    	
    	//runcode org.selkie.kol.impl.world.prepareAbyss.GenerateAbyss(Global.getSector());
    	//runcode Global.getSector().getStarSystem("Elysia").addRingBand(Global.getSector().getStarSystem("Elysia").getStar(), "misc", "rings_dust0", 1620, 0, Color.red, 1620, 3434, 17, Terrain.RING, "Accretion Disk");
		//runcode Global.getSector().getPlayerFleet().addTag("abyss_edf_rulesfortheebutnotforme");

    	system.addRingBand(elysia, "misc", "rings_dust0", 1620, 0, Color.red, 1620, 3124, 17, Terrain.RING, "Accretion Disk");
    	//SectorEntityToken accretion1 = system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(5000, 1500, elysia, "Accretion Disk"));
    	//accretion1.addTag(Tags.ACCRETION_DISK);
    	//accretion1.setCircularOrbit(elysia, 0, 0, 10);
    	
    	SectorEntityToken elysian_nebula = Misc.addNebulaFromPNG("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/terrain/pinwheel_nebula.png",
                                                                    0, 0, // center of nebula
                                                                    system, // location to add to
                                                                    "terrain", "nebula_kol_redgrey", // "nebula_blue", // texture to use, uses xxx_map for map
                                                                    4, 4, StarAge.AVERAGE); // number of cells in texture

    	system.setType(StarSystemType.TRINARY_1CLOSE_1FAR);
    	
    	PlanetAPI gaze = system.addPlanet("abyss_elysia_gaze", elysia, "Watcher", StarTypes.NEUTRON_STAR, 125, 50, 9400, 60);
    	gaze.getSpec().setPulsar(true);
    	gaze.applySpecChanges();
    	system.setSecondary(gaze);
    	
		SectorEntityToken gazeBeam1 = system.addTerrain("kol_pulsarBeam",
				new AbyssCorona.CoronaParams(25000,
						3250,
						gaze,
						150f,
						1f,
						5f)
		);
		gazeBeam1.setCircularOrbit(gaze, (float)(Math.random() * 360), 0, 15);

		SectorEntityToken gazeBeam2 = system.addTerrain("kol_pulsarBeam",
				new AbyssCorona.CoronaParams(25000,
						3250,
						gaze,
						150f,
						1f,
						5f)
		);
		gazeBeam2.setCircularOrbit(gaze, (float)(Math.random() * 360), 0, 16);
    	
    	PlanetAPI silence = system.addPlanet("abyss_elysia_silence", elysia, "Silence", StarTypes.BLUE_SUPERGIANT, 255, 1666, 18500, 0);
    	system.setTertiary(silence);
    	silence.setFixedLocation(-14000, -16500);
    	
		SectorEntityToken silence_corona = system.addTerrain("kol_corona",
				new AbyssCorona.CoronaParams(7000,
						0,
						silence,
						3f,
						0.1f,
						1f)
		);
		silence_corona.setCircularOrbit(silence, 0, 0, 15);

    	PlanetAPI first = system.addPlanet("abyss_elysia_asclepius", elysia, "Asclepius", "barren-bombarded", (float)(Math.random() * 360), 100, 4900, 50);
    	PlanetAPI second = system.addPlanet("abyss_elysia_appia", elysia, "Appia", "toxic", (float)(Math.random() * 360), 100, 4100, 40);
    	PlanetAPI third = system.addPlanet("abyss_elysia_orpheus", elysia, "Orpheus", "irradiated", (float)(Math.random() * 360), 100, 3300, 30);

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

		SectorEntityToken ring = system.addTerrain(Terrain.RING, new RingParams(456, 3200, null, "Call of the Void"));
		ring.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring2 = system.addTerrain(Terrain.RING, new RingParams(456, 3656, null, "Call of the Void"));
		ring2.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring3 = system.addTerrain(Terrain.RING, new RingParams(456, 4112, null, "Call of the Void"));
		ring3.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring4 = system.addTerrain(Terrain.RING, new RingParams(456, 4678, null, "Call of the Void"));
		ring4.setCircularOrbit(elysia, 0, 0, 100);
		
		system.addAsteroidBelt(elysia, 200, 3128, 256, 20, 20, "kol_asteroidBelt", null);
		system.addAsteroidBelt(elysia, 200, 3516, 512, 30, 30, "kol_asteroidBelt", null);
		system.addAsteroidBelt(elysia, 200, 4024, 1024, 40, 40, "kol_asteroidBelt", null);
		system.addAsteroidBelt(elysia, 200, 4536, 1024, 40, 60, "kol_asteroidBelt", null);

        SectorEntityToken derelict1 = addSalvageEntity(system, "alpha_site_weapons_cache", PrepareAbyss.elysianID);
        derelict1.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3100, 20);
        SectorEntityToken derelict2 = addSalvageEntity(system, "alpha_site_weapons_cache", PrepareAbyss.elysianID);
        derelict2.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3400, 25);
        SectorEntityToken derelict3 = addSalvageEntity(system, "alpha_site_weapons_cache", PrepareAbyss.elysianID);
        derelict3.setCircularOrbit(elysia, (float)(Math.random() * 360f), 3700, 30);
        SectorEntityToken derelict4 = addSalvageEntity(system, "alpha_site_weapons_cache", PrepareAbyss.elysianID);
        derelict4.setCircularOrbit(elysia, (float)(Math.random() * 360f), 4000, 35);
        SectorEntityToken derelict5 = addSalvageEntity(system, "alpha_site_weapons_cache", PrepareAbyss.elysianID);
        derelict5.setCircularOrbit(elysia, (float)(Math.random() * 360f), 4400, 40);
    	
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("abyss_elysia_jp", "First Trial");
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

		AbyssalFleetManager fleets = new AbyssalFleetManager(system, elysianID, 14, 60, 240);
		system.addScript(fleets);

		EveryFrameScript tracker = new TrackFleet();
		system.addScript(tracker);

    }

	public static void GenerateUnderworld(SectorAPI sector) {

		// No direct access, see ReportTransit and TrackFleet listeners

		StarSystemAPI system = sector.createStarSystem("Underspace");
		system.setName("Underspace");
		LocationAPI hyper = Global.getSector().getHyperspace();
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.THEME_UNSAFE);

		system.setBackgroundTextureFilename("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/backgrounds/abyss_bg_dusk.png");
		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_kol_underworld_theme");

		system.getLocation().set(2100, -4200);
		SectorEntityToken center = system.initNonStarCenter();
		SectorEntityToken underspace_nebula = Misc.addNebulaFromPNG("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/terrain/pinwheel_nebula.png",
				0, 0, // center of nebula
				system, // location to add to
				"terrain", "nebula_kol_black_shiny", // texture to use, uses xxx_map for map
				4, 4, StarAge.AVERAGE); // number of cells in texture

		system.setLightColor(new Color(225,170,255,255)); // light color in entire system, affects all entities
		new AbyssBackgroundWarper(system, 8, 0.125f);

		PlanetAPI starVoid = system.addPlanet("abyss_underworld_void", system.getCenter(), "Void", "white_hole", 135, 166, 12500, 0);
		//system.setStar(starVoid);

		starVoid.setFixedLocation(-5512, 9420);

		SectorEntityToken void_corona = system.addTerrain("kol_corona",
				new AbyssCorona.CoronaParams(1200,
						0,
						starVoid,
						10f,
						0.1f,
						3f)
		);
		void_corona.setCircularOrbit(starVoid, 0, 0, 15);

		//system.generateAnchorIfNeeded();

		SectorEntityToken gate = system.addCustomEntity(undergateID, "Undergate", Entities.INACTIVE_GATE, Factions.DERELICT);
		gate.setCircularOrbit(center, (float)(Math.random() * 360f), 15000, 1000);

		SectorEntityToken stationResearch = addSalvageEntity(system, "station_research_remnant", PrepareAbyss.duskID);
		stationResearch.setFixedLocation(-5230, 8860);
		stationResearch.setName("Null");

		for(String variant:uwDerelictsNormal) {
			if (Math.random() <= 0.45f) {
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
				//system.addEntity(wreck);
			}
		}
		for(String variant:uwDerelictsPhase) {
			if (Math.random() <= 0.66f) {
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
			}
		}
	}

	public static void GenerateLunaSea(SectorAPI sector) {

		StarSystemAPI system = sector.createStarSystem("Luna Sea");
		system.setName("The Luna Sea"); //No "-Star System"

		LocationAPI hyper = Global.getSector().getHyperspace();

		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.THEME_UNSAFE);

		system.setBackgroundTextureFilename("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/backgrounds/abyss_bg_dawn.png");
		new AbyssBackgroundWarper(system, 8, 0.25f);

		if (!sector.getDifficulty().equals(Difficulties.EASY)) {
			SectorEntityToken lunasea_nebula = Misc.addNebulaFromPNG("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/terrain/flower_nebula.png",
					0, 0, // center of nebula
					system, // location to add to
					"terrain", "nebula_kol_purpleblue", // "nebula_blue", // texture to use, uses xxx_map for map
					8, 8, StarAge.AVERAGE); // number of cells in texture
		} else {
			SectorEntityToken lunasea_nebula = Misc.addNebulaFromPNG("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/terrain/luwunasea_nebula2.png",
					0, 0, // center of nebula
					system, // location to add to
					"terrain", "nebula_kol_purpleblue", // "nebula_blue", // texture to use, uses xxx_map for map
					4, 4, StarAge.AVERAGE); // number of cells in texture
		}

		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_kol_lunasea_theme");

		PlanetAPI luna = system.initStar("lunasea_star", StarTypes.BLUE_SUPERGIANT, 2500, 30500, -30500, 0);
		if (sector.getDifficulty().equals(Difficulties.EASY)) {
			luna.setName("The Luwuna Sea");
		}
		system.setDoNotShowIntelFromThisLocationOnMap(true);

		SectorEntityToken star_corona = system.addTerrain("kol_corona",
				new AbyssCorona.CoronaParams(11000,
						0,
						luna,
						5f,
						0.1f,
						1f)
		);

		SectorEntityToken lunaBeam1 = system.addTerrain("kol_seaWave",
				new AbyssCorona.CoronaParams(50000,
						2500,
						luna,
						20f,
						1f,
						5f)
		);
		lunaBeam1.setCircularOrbit(luna, (float)Math.random()*360, 0, 19);

		//system.generateAnchorIfNeeded();
		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("abyss_lunasea_jp", "Second Trial");
		//OrbitAPI orbit = Global.getFactory().createCircularOrbit(luna, 90, 10000, 25);
		jumpPoint.setFixedLocation(6500,-1500);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		PlanetAPI first = system.addPlanet("abyss_lunasea_one", luna, "Id", "barren-bombarded", 50, 150, 10900, 3000000);
		PlanetAPI second = system.addPlanet("abyss_lunasea_two", luna, "Doubt", "desert", 29, 170, 6100, 3000000);
		PlanetAPI third = system.addPlanet("abyss_lunasea_three", luna, "Wild", "jungle", 12, 70, 15800, 3000000);
		PlanetAPI fourth = system.addPlanet("abyss_lunasea_four", jumpPoint, "Slip", "frozen", 190, 250, 500, 3000000);
		PlanetAPI fifth = system.addPlanet("abyss_lunasea_five", luna, "Savage", "barren", 336, 145, 12300, 3000000);
		PlanetAPI sixth = system.addPlanet("abyss_lunasea_six", luna, "Feral", "toxic", 306, 165, 8100, 3000000);

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
		float minRadius = plugin.getTileSize() * 1f;

		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		String lootEntity;
		lootEntity = (Math.random() >= 0.5f) ? "station_research_remnant" : "weapons_cache_remnant";
		SectorEntityToken loot1 = addSalvageEntity(system, lootEntity, PrepareAbyss.dawnID);
		loot1.setCircularOrbitPointingDown(luna, 87, 6240, 100000);
		lootEntity = (Math.random() >= 0.5f) ? "station_research_remnant" : "weapons_cache_remnant";
		SectorEntityToken loot2 = addSalvageEntity(system, lootEntity, PrepareAbyss.dawnID);
		loot2.setCircularOrbitPointingDown(luna, 38, 21330, 100000);
		lootEntity = (Math.random() >= 0.5f) ? "station_research_remnant" : "weapons_cache_remnant";
		SectorEntityToken loot3 = addSalvageEntity(system, lootEntity, PrepareAbyss.dawnID);
		loot3.setCircularOrbitPointingDown(third, 101, 5155, 100000);
		lootEntity = (Math.random() >= 0.5f) ? "station_research_remnant" : "weapons_cache_remnant";
		SectorEntityToken loot4 = addSalvageEntity(system, lootEntity, PrepareAbyss.dawnID);
		loot4.setCircularOrbitPointingDown(fourth, 171, 1500, 100000);
		lootEntity = (Math.random() >= 0.5f) ? "station_research_remnant" : "weapons_cache_remnant";
		SectorEntityToken loot5 = addSalvageEntity(system, lootEntity, PrepareAbyss.dawnID);
		loot5.setCircularOrbitPointingDown(fifth, 285, 6353, 100000);
		lootEntity = (Math.random() >= 0.5f) ? "station_research_remnant" : "weapons_cache_remnant";
		SectorEntityToken loot6 = addSalvageEntity(system, lootEntity, PrepareAbyss.dawnID);
		loot6.setCircularOrbitPointingDown(sixth, 217, 4770, 100000);

		org.selkie.kol.impl.fleets.AbyssalFleetManager fleets = new org.selkie.kol.impl.fleets.AbyssalFleetManager(system, dawnID, 16, 40, 140);
		org.selkie.kol.impl.fleets.AbyssalFleetManager fleetsMiniboss = new AbyssalFleetManager(system, dawnID, 4, 250, 350);
		system.addScript(fleets);
		system.addScript(fleetsMiniboss);
	}

	public static void generateDynamicDuskHole(SectorAPI sector) {
		//Variable location
		double posX = Math.random()*(Global.getSettings().getFloat("sectorWidth")/2);
		double posY = Math.random()*(Global.getSettings().getFloat("sectorHeight")/2);
		if (Math.abs(posX) < 20000) posX += 20000; //Coreworld area
		if (Math.abs(posY) < 20000) posY += 20000;
		if (Math.random() < 0.5f) posX *= -1;
		if (Math.random() < 0.5f) posY *= -1;

		StarSystemAPI system = sector.createStarSystem(ProcgenUsedNames.pickName(NameGenData.TAG_STAR, null, null).nameWithRomanSuffixIfAny);
		system.getLocation().set((float)posX, (float)posY);
		if (Math.random() < 0.5f) {
			system.setBackgroundTextureFilename("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/backgrounds/abyss_bh_duskbh1.png");
		} else {
			system.setBackgroundTextureFilename("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/backgrounds/abyss_bh_duskbh2.png");
		}
		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_kol_underworld_theme");

		system.addTag(Tags.THEME_UNSAFE);
		system.addTag(Tags.THEME_SPECIAL);

		//fancy bg script

		PlanetAPI sing = system.initStar("dusk_DBH" + Misc.genUID(), "star_black_neutron", 200, -200f);
		String tempName = system.getBaseName();
		system.setName(tempName);
		sing.setName(tempName);
		ProcgenUsedNames.notifyUsed(system.getBaseName());
		sing.getSpec().setBlackHole(true);
		sing.applySpecChanges();

		SectorEntityToken horizon1 = system.addTerrain("kol_eventHorizon", new AbyssEventHorizon.CoronaParams(
				1000,
				50,
				sing,
				-5f,
				0f,
				5f)
		);

		SectorEntityToken dbhBeam1 = system.addTerrain("kol_blackBeam",
				new AbyssCorona.CoronaParams(25000,
						250,
						sing,
						-50f,
						1f,
						5f)
		);
		dbhBeam1.setCircularOrbit(sing, (float)(Math.random() * 360), 0, 20);

		//sophistimacated
		float orbRadius1 = (float)(Math.random() * 8500f);
		SectorEntityToken cacheRem = addSalvageEntity(system, "weapons_cache_remnant", PrepareAbyss.duskID);
		cacheRem.setCircularOrbit(sing, (float)(Math.random() * 360f), orbRadius1, orbRadius1/10f);

		float orbRadius2 = (float)(Math.random()*8500f);
		SectorEntityToken stationResearch = addSalvageEntity(system, "station_research_remnant", PrepareAbyss.duskID);
		stationResearch.setCircularOrbit(sing, (float)(Math.random() * 360f), orbRadius2, orbRadius2/10f);

		float orbRadius3 = (float)(Math.random()*8500f);
		SectorEntityToken cacheRemSmall = addSalvageEntity(system, "weapons_cache_small_remnant", PrepareAbyss.duskID);
		cacheRemSmall.setCircularOrbit(sing, (float)(Math.random() * 360f), orbRadius3, orbRadius3/10f);

		system.autogenerateHyperspaceJumpPoints(true, true); //begone evil clouds
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 1.5f;

		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

		AbyssalFleetManager fleets = new AbyssalFleetManager(system, duskID, 6, 60, 175);
		AbyssalFleetManager fleetsMiniboss = new AbyssalFleetManager(system, duskID, 3, 250, 350);
		system.addScript(fleets);
		system.addScript(fleetsMiniboss);
	}
}