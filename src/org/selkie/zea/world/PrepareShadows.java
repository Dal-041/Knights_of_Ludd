package org.selkie.zea.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.selkie.zea.fleets.ZeaFleetManager;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.terrain.AbyssCorona;
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaEntities;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaTerrain;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaStarTypes;

import java.util.Random;

import static com.fs.starfarer.api.impl.MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.addSalvageEntity;
import static org.selkie.zea.helpers.ZeaStaticStrings.TOXIC;
import static org.selkie.zea.helpers.ZeaStaticStrings.dawnID;
import static org.selkie.zea.world.PrepareAbyss.getAbyssLootID;

public class PrepareShadows {

    public static void GenerateOzymandias() {

        StarSystemAPI system = Global.getSector().createStarSystem(ZeaStaticStrings.ozymandiasSysName);
        //system.setName(ozymandiasSysName); //No "-Star System"

        LocationAPI hyper = Global.getSector().getHyperspace();

        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.THEME_UNSAFE);
        system.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
        system.addTag(ZeaStaticStrings.THEME_ZEA);
        system.addTag(ZeaStaticStrings.THEME_ZEA_MINOR);

        system.setBackgroundTextureFilename(Global.getSettings().getSpriteName(GfxCat.BACKGROUNDS,"zea_bg_dawn"));
        new AbyssBackgroundWarper(system, 8, 0.0625f);

        SectorEntityToken neb_ozymandias_fingers = Misc.addNebulaFromPNG(Global.getSettings().getSpriteName(GfxCat.TERRAIN,"zea_nebula_fingers"),
                0, 0, system,
                GfxCat.TERRAIN, "nebula_zea_dawntide",
                4, 4, ZeaTerrain.NEBULA_ZEA_SHOAL, StarAge.AVERAGE);

        system.getMemoryWithoutUpdate().set(MUSIC_SET_MEM_KEY, "music_zea_lunasea_theme");

        PlanetAPI ozy = system.initStar(ZeaEntities.ZEA_OZYMANDIAS_STAR, StarTypes.BLUE_SUPERGIANT, 600, 50750, -38500, 0);
        system.setType(StarSystemGenerator.StarSystemType.SINGLE);

        system.setDoNotShowIntelFromThisLocationOnMap(true);

        SectorEntityToken star_corona = system.addTerrain(ZeaTerrain.ZEA_CORONA,
                new AbyssCorona.CoronaParams(432,
                        0,
                        ozy,
                        5f,
                        0.1f,
                        1f)
        );

        //system.generateAnchorIfNeeded();
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("zea_ozy_jp", "Rocky Shore");
        //OrbitAPI orbit = Global.getFactory().createCircularOrbit(luna, 90, 10000, 25);
        jumpPoint.setFixedLocation(-1800,1800);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);

        PlanetAPI first = system.addPlanet(ZeaEntities.ZEA_OZYMANDIAS_PLANET_ONE, ozy, "Mourn", TOXIC, 291, 250, 2900, 3000000);
        PlanetAPI second = system.addPlanet(ZeaEntities.ZEA_OZYMANDIAS_PLANET_TWO, ozy, "Abandon", TOXIC, 291, 108, 4700, 3000000);
        PlanetAPI third = system.addPlanet(ZeaEntities.ZEA_OZYMANDIAS_PLANET_THREE, ozy, "Forget",  TOXIC, 291, 97, 6500, 3000000);
        PlanetAPI fourth = system.addPlanet(ZeaEntities.ZEA_OZYMANDIAS_PLANET_FOUR, ozy, "Rest", TOXIC, 291, 80, 8300, 3000000);

        first.getMarket().addCondition(Conditions.VERY_HOT);
        first.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
        first.getMarket().addCondition(Conditions.IRRADIATED);
        first.getMarket().addCondition(Conditions.HIGH_GRAVITY);
        first.getMarket().addCondition(Conditions.EXTREME_WEATHER);

        second.getMarket().addCondition(Conditions.HIGH_GRAVITY);
        second.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
        second.getMarket().addCondition(Conditions.HOT);
        second.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        second.getMarket().addCondition(Conditions.IRRADIATED);

        third.getMarket().addCondition(Conditions.HIGH_GRAVITY);
        third.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);
        third.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        third.getMarket().addCondition(Conditions.IRRADIATED);

        fourth.getMarket().addCondition(Conditions.COLD);
        fourth.getMarket().addCondition(Conditions.IRRADIATED);
        fourth.getMarket().addCondition(Conditions.HIGH_GRAVITY);
        fourth.getMarket().addCondition(Conditions.EXTREME_WEATHER);
        fourth.getMarket().addCondition(Conditions.TOXIC_ATMOSPHERE);

        system.autogenerateHyperspaceJumpPoints(false, false); //begone evil clouds
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize();

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

        SectorEntityToken loot1 = addSalvageEntity(system, getAbyssLootID(dawnID, 3), dawnID);
        loot1.setCircularOrbitPointingDown(ozy, (float)Math.random()*60+47, (float)Math.random()*2240+3000, 100000);
        SectorEntityToken loot2 = addSalvageEntity(system, getAbyssLootID(dawnID, 3), dawnID);
        loot2.setCircularOrbitPointingDown(ozy, (float)Math.random()*20+28, (float)Math.random()*3330+18330, 100000);
        SectorEntityToken loot3 = addSalvageEntity(system, getAbyssLootID(dawnID, 3), dawnID);
        loot3.setCircularOrbitPointingDown(third, (float)Math.random()*50+71, (float)Math.random()*2155+3500, 100000);
        SectorEntityToken loot4 = addSalvageEntity(system, getAbyssLootID(dawnID, 3), dawnID);
        loot4.setCircularOrbitPointingDown(fourth, 171, (float)Math.random()*1500+1000, 100000);

        SectorEntityToken sleeper = addSalvageEntity(new Random(), system, Entities.DERELICT_CRYOSLEEPER, dawnID);
        sleeper.setFixedLocation(2000, -6000);
        SectorEntityToken hab = addSalvageEntity(new Random(), system, Entities.ORBITAL_HABITAT, Factions.DERELICT);
        hab.setFixedLocation(1500, -4000);
        SectorEntityToken mining = addSalvageEntity(new Random(), system, Entities.STATION_MINING, Factions.DERELICT);
        mining.setFixedLocation(2500, -7500);

        //FP bumped to account for backup capital ships getting pruned
        ZeaFleetManager fleets = new ZeaFleetManager(system, dawnID, 7, 40, 180);
        ZeaFleetManager fleetsMiniboss = new ZeaFleetManager(system, dawnID, 1, 240, 425);
        system.addScript(fleets);
        system.addScript(fleetsMiniboss);
    }


}
