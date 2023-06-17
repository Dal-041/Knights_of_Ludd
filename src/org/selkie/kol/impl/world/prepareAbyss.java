package org.selkie.kol.impl.world;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

public class prepareAbyss implements SectorGeneratorPlugin {
	
    @Override
    public void generate(SectorAPI sector) {
    	GenerateAbyss(sector);
        //PrepareForRelations(sector);
    }
    
    public static void GenerateAbyss(SectorAPI sector) {
    	
    	int beeg = 1500;
    	double posX = 2600;
    	double posY = 24900;
    	
    	//Variable location
    	//posX = Math.random()%(Global.getSettings().getFloat("sectorWidth")-10000);
    	//posY = Math.random()%(Global.getSettings().getFloat("sectorHeight")-8000);
        
    	StarSystemAPI system = sector.createStarSystem("Elysia");
    	system.getLocation().set((int)posX, (int)posY);
    	system.setBackgroundTextureFilename("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/backgrounds/abyss_bg2.jpg");
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_UNSAFE);
		
    	PlanetAPI elysia = system.initStar("Elysian_Abyss", StarTypes.BLACK_HOLE, beeg, 0f);
    	elysia.getSpec().setBlackHole(true);
    	elysia.setName("Elysian Abyss");
    	elysia.applySpecChanges();
    	SectorEntityToken horizon1 = system.addTerrain(Terrain.EVENT_HORIZON, new EventHorizonPlugin.CoronaParams(
    			4000,
				500,
				elysia,
				-10f,
				0f,
				5f)
    		);
    	
    	//runcode org.selkie.kol.impl.world.prepareAbyss.GenerateAbyss(Global.getSector());
    	//runcode Global.getSector().getStarSystem("Elysia").addRingBand(Global.getSector().getStarSystem("Elysia").getStar(), "misc", "rings_dust0", 1620, 0, Color.red, 1620, 3434, 17, Terrain.RING, "Accretion Disk");
    	system.addRingBand(elysia, "misc", "rings_dust0", 1620, 0, Color.red, 1620, 3124, 17, Terrain.RING, "Accretion Disk");
    	//SectorEntityToken accretion1 = system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(5000, 1500, elysia, "Accretion Disk"));
    	//accretion1.addTag(Tags.ACCRETION_DISK);
    	//accretion1.setCircularOrbit(elysia, 0, 0, 10);
    	
    	SectorEntityToken elysian_nebula = Misc.addNebulaFromPNG("data/campaign/terrain/eos_nebula.png",
                                                                    0, 0, // center of nebula
                                                                    system, // location to add to
                                                                    "terrain", "nebula", // "nebula_blue", // texture to use, uses xxx_map for map
                                                                    4, 4, StarAge.AVERAGE); // number of cells in texture

    	system.setType(StarSystemType.TRINARY_1CLOSE_1FAR);
    	
    	PlanetAPI gaze = system.addPlanet("nsf_gaze", elysia, "Watcher", StarTypes.NEUTRON_STAR, 125, 50, 9400, 60);
    	gaze.getSpec().setPulsar(true);
    	gaze.applySpecChanges();
    	system.setSecondary(gaze);
    	
		SectorEntityToken gazeBeam1 = system.addTerrain(Terrain.PULSAR_BEAM,
				new StarCoronaTerrainPlugin.CoronaParams(25000,
						3250,
						gaze,
						150f,
						1f,
						5f)
		);
		gazeBeam1.setCircularOrbit(gaze, 0, 0, 15);

		SectorEntityToken gazeBeam2 = system.addTerrain(Terrain.PULSAR_BEAM,
				new StarCoronaTerrainPlugin.CoronaParams(25000,
						3250,
						gaze,
						150f,
						1f,
						5f)
		);
		gazeBeam2.setCircularOrbit(gaze, 0, 0, 16);
    	
    	PlanetAPI silence = system.addPlanet("nsf_silence", elysia, "Silence", StarTypes.BLUE_SUPERGIANT, 255, 1666, 18500, 0);
    	system.setTertiary(silence);
    	silence.setFixedLocation(-14000, -16500);
    	
		SectorEntityToken silence_corona = system.addTerrain(Terrain.CORONA,
				new StarCoronaTerrainPlugin.CoronaParams(11000,
						0,
						silence,
						5f,
						0.1f,
						1f)
		);
		silence_corona.setCircularOrbit(silence, 0, 0, 15);
    	
    	PlanetAPI first = system.addPlanet("nsf_zealot", elysia, "Zealot", "barren-bombarded", 0, 100, 4900, 50);
    	PlanetAPI second = system.addPlanet("nsf_pilgrim", elysia, "Pilgrim", "barren-bombarded", 0.1f, 100, 4100, 40);
    	PlanetAPI third = system.addPlanet("nsf_apostate", elysia, "Apostate", "barren-bombarded", 0.2f, 100, 3300, 30);
    	
    	first.getMarket().addCondition(Conditions.HIGH_GRAVITY);
    	second.getMarket().addCondition(Conditions.HIGH_GRAVITY);
    	third.getMarket().addCondition(Conditions.HIGH_GRAVITY);
        
		SectorEntityToken ring = system.addTerrain(Terrain.RING, new RingParams(456, 3200, null, "Call of the Void"));
		ring.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring2 = system.addTerrain(Terrain.RING, new RingParams(456, 3656, null, "Call of the Void"));
		ring2.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring3 = system.addTerrain(Terrain.RING, new RingParams(456, 4112, null, "Call of the Void"));
		ring3.setCircularOrbit(elysia, 0, 0, 100);
		SectorEntityToken ring4 = system.addTerrain(Terrain.RING, new RingParams(456, 4678, null, "Call of the Void"));
		ring4.setCircularOrbit(elysia, 0, 0, 100);
		
		system.addAsteroidBelt(elysia, 200, 3128, 256, 20, 20, Terrain.ASTEROID_BELT, null);
		system.addAsteroidBelt(elysia, 200, 3516, 512, 30, 30, Terrain.ASTEROID_BELT, null);
		system.addAsteroidBelt(elysia, 200, 4024, 1024, 40, 40, Terrain.ASTEROID_BELT, null);
		system.addAsteroidBelt(elysia, 200, 4536, 1024, 40, 60, Terrain.ASTEROID_BELT, null);

        SectorEntityToken derelict1 = DerelictThemeGenerator.addSalvageEntity(system, "alpha_site_weapons_cache", Factions.DERELICT);
        derelict1.setCircularOrbit(elysia, (float)(Math.random() % 360f), 3100, 20);
        SectorEntityToken derelict2 = DerelictThemeGenerator.addSalvageEntity(system, "alpha_site_weapons_cache", Factions.DERELICT);
        derelict2.setCircularOrbit(elysia, (float)(Math.random() % 360f), 3400, 25);
        SectorEntityToken derelict3 = DerelictThemeGenerator.addSalvageEntity(system, "alpha_site_weapons_cache", Factions.DERELICT);
        derelict3.setCircularOrbit(elysia, (float)(Math.random() % 360f), 3700, 30);
        SectorEntityToken derelict4 = DerelictThemeGenerator.addSalvageEntity(system, "alpha_site_weapons_cache", Factions.DERELICT);
        derelict4.setCircularOrbit(elysia, (float)(Math.random() % 360f), 4000, 35);
        SectorEntityToken derelict5 = DerelictThemeGenerator.addSalvageEntity(system, "alpha_site_weapons_cache", Factions.DERELICT);
        derelict5.setCircularOrbit(elysia, (float)(Math.random() % 360f), 4400, 40);
    	
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("nsf_jp", "First Trial");
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
        
    }
}