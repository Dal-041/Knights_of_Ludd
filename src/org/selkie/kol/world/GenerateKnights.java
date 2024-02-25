package org.selkie.kol.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.econ.Market;
import exerelin.campaign.AllianceManager;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.SectorManager;
import exerelin.campaign.alliances.Alliance;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.fleets.KnightsExpeditionsManager;
import org.selkie.kol.fleets.SpawnInvictus;
import org.selkie.kol.fleets.SpawnRetribution;
import org.selkie.kol.helpers.MarketHelpers;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.intel.ZeaMechanicIntel;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.plugins.KOL_ModPlugin;

public class GenerateKnights {

	public static Logger log = Global.getLogger(GenerateKnights.class);

	public static int baseKnightExpeditions = 2;
	
	public static void genCorvus() {
		Global.getSector().getStarSystem("Eos Exodus").setBackgroundTextureFilename("graphics/backgrounds/kol_bg_1.jpg");
		Global.getSector().getStarSystem("Kumari Kandam").setBackgroundTextureFilename("graphics/backgrounds/kol_bg_2.jpg");
		Global.getSector().getStarSystem("Canaan").setBackgroundTextureFilename("graphics/backgrounds/kol_bg_3.jpg");
		Global.getSector().getStarSystem("Al Gebbar").setBackgroundTextureFilename("graphics/backgrounds/kol_bg_4.jpg");
		genKnightsBattlestation();
		genKnightsStarfortress();
	}

	public static void genAlways() {
		genBattlestarLibra();
		SpawnInvictus.spawnInvictus();
		SpawnRetribution.spawnRetribution();
		copyChurchEquipment();
		startupRelations();
		genKnightsExpeditions();
		GenerateKnights.addKoLIntel();
	}

	public static void startupRelations() {
		if (Global.getSector().getFaction(Factions.LUDDIC_CHURCH) != null && Global.getSector().getFaction(KOL_ModPlugin.kolID) != null) {
			FactionAPI church = Global.getSector().getFaction(Factions.LUDDIC_CHURCH);
			FactionAPI knights = Global.getSector().getFaction(KOL_ModPlugin.kolID);

			if(church.getRelToPlayer().isAtWorst(RepLevel.SUSPICIOUS)) {
				church.getRelToPlayer().setRel(Math.max(church.getRelToPlayer().getRel(), knights.getRelToPlayer().getRel()));
				knights.getRelToPlayer().setRel(Math.max(church.getRelToPlayer().getRel(), knights.getRelToPlayer().getRel()));
			}

			for(FactionAPI faction:Global.getSector().getAllFactions()) {
				knights.setRelationship(faction.getId(), church.getRelationship(faction.getId()));
			}
			if (Misc.getCommissionFactionId() != null && Misc.getCommissionFactionId().equals(KOL_ModPlugin.kolID)) {
				FactionAPI player = Global.getSector().getPlayerFaction();
				for(FactionAPI faction:Global.getSector().getAllFactions()) {
					player.setRelationship(faction.getId(), knights.getRelationship(faction.getId()));
				}
			}
			
			// THE CHURCH OF GALACTIC REDEMPTION.
			if (KOL_ModPlugin.haveNex) {
				knights.setRelationship(Factions.LUDDIC_CHURCH, 1.00f);
				if (true || Global.getSettings().getBoolean("foundCOGH")) {
					Alliance COGH = AllianceManager.createAlliance(KOL_ModPlugin.kolID, Factions.LUDDIC_CHURCH, AllianceManager.getBestAlignment(KOL_ModPlugin.kolID, Factions.LUDDIC_CHURCH));
					COGH.addPermaMember(KOL_ModPlugin.kolID);
					COGH.addPermaMember(Factions.LUDDIC_CHURCH);
					COGH.setName(Global.getSettings().getString("knights_of_ludd", "ChurchOfGalacticRedemption"));
				}

				NexFactionConfig factionConfig = NexConfig.getFactionConfig(KOL_ModPlugin.kolID);
				if (!DiplomacyManager.isRandomFactionRelationships()) {factionConfig.minRelationships.clear();factionConfig.minRelationships.put(Factions.LUDDIC_CHURCH, 0.251f);}
				if (KOL_ModPlugin.kolID.equals(PlayerFactionStore.getPlayerFactionIdNGC()) || Factions.LUDDIC_CHURCH.equals(PlayerFactionStore.getPlayerFactionIdNGC())) {
					knights.setRelationship(Factions.LUDDIC_CHURCH, 0.75f); //Prevents you from instantly gaining 1 story point repping up to 100...
				}
			}
		}
	}

	public static void addKoLIntel() {
		if (Global.getSector() == null) return;
		while (ZeaMechanicIntel.unknownMechanics(KOL_ModPlugin.kolID) > 0) {
			Global.getSector().getIntelManager().addIntel(ZeaMechanicIntel.getNextMechanicIntel(KOL_ModPlugin.kolID));
		}
	}

	public static void genKnightsBattlestation() {
		String entID = "kol_cygnus";
		StarSystemAPI Canaan = Global.getSector().getStarSystem("Canaan");
        SectorEntityToken cygnus = Canaan.addCustomEntity(entID, "Battlestation Cygnus", "station_lowtech2", "knights_of_selkie");
        cygnus.setCircularOrbitPointingDown(Canaan.getEntityById("canaan_gate"), 33, 475, 99);
        cygnus.setCustomDescriptionId("kol_cygnus_desc");
		//cygnus.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.KEEP_PLAYING_LOCATION_MUSIC_DURING_ENCOUNTER_MEM_KEY, true);
        
        MarketHelpers.addMarketplace("knights_of_selkie", cygnus, null, "Battlestation Cygnus", 4,
        		new ArrayList<String>(Arrays.asList(Conditions.OUTPOST,
                        Conditions.POPULATION_4)),
        		new ArrayList<String>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
						"kol_garden",
						Industries.PATROLHQ,
                        Industries.LIGHTINDUSTRY,
                        Industries.GROUNDDEFENSES,
                        Industries.BATTLESTATION)),
        		new ArrayList<String>(Arrays.asList(
        				Submarkets.SUBMARKET_STORAGE,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN)),
        		0.3f
        );

		cygnus.getMarket().removeSubmarket(Submarkets.SUBMARKET_BLACK);
		if (KOL_ModPlugin.haveNex) SectorManager.NO_BLACK_MARKET.add(cygnus.getMarket().getId());

		cygnus.setInteractionImage("illustrations", "kol_tree_canaan_large");

		MarketHelpers.addMarketPeople(cygnus.getMarket());

		PersonAPI master = MagicCampaign.addCustomPerson(cygnus.getMarket(), "Master", "Helensis", "kol_chaptermaster",
				FullName.Gender.FEMALE, KOL_ModPlugin.kolID, Ranks.ELDER, Ranks.POST_MILITARY_ADMINISTRATOR,
				false, 1, 1);

		PersonAPI lackey1 = MagicCampaign.addCustomPerson(cygnus.getMarket(), "Brother", "Enarms", "kol_agent_m",
				FullName.Gender.MALE, KOL_ModPlugin.kolID, Ranks.KNIGHT_CAPTAIN, Ranks.POST_GUARD_LEADER,
				false, 0, 0);

		master.setId("kol_chaptermaster");
		lackey1.setId("kol_knightcaptain");

		master.setImportance(PersonImportance.HIGH);
		master.setVoice(Voices.SOLDIER);

		lackey1.setImportance(PersonImportance.MEDIUM);
		lackey1.setVoice(Voices.FAITHFUL);
	}
	
	public static void genKnightsStarfortress() {
		String entID = "kol_lyra";
		StarSystemAPI Eos = Global.getSector().getStarSystem("Eos Exodus");
        SectorEntityToken lyra = Eos.addCustomEntity(entID, "Star Keep Lyra", "station_lowtech3", "knights_of_selkie");
        lyra.setCircularOrbitPointingDown(Eos.getEntityById("eos_exodus_gate"), 33, 475, 99);
        lyra.setCustomDescriptionId("kol_lyra_desc");
		//yra.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.KEEP_PLAYING_LOCATION_MUSIC_DURING_ENCOUNTER_MEM_KEY, true);

		MarketHelpers.addMarketplace("knights_of_selkie", lyra, null, "Star Keep Lyra", 5,
        		new ArrayList<String>(Arrays.asList(Conditions.OUTPOST,
                        Conditions.POPULATION_5)),
        		new ArrayList<String>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
						"kol_garden",
                        Industries.MILITARYBASE,
                        Industries.ORBITALWORKS,
                        Industries.FUELPROD,
                        Industries.HEAVYBATTERIES,
                        Industries.STARFORTRESS,
                        Industries.WAYSTATION)),
        		new ArrayList<String>(Arrays.asList(
        				Submarkets.SUBMARKET_STORAGE,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN)),
        		0.3f
        );

		lyra.setInteractionImage("illustrations", "kol_citadel_large");

		lyra.getMarket().removeSubmarket(Submarkets.SUBMARKET_BLACK);
		if (KOL_ModPlugin.haveNex) SectorManager.NO_BLACK_MARKET.add(lyra.getMarket().getId());

		MarketHelpers.addMarketPeople(lyra.getMarket());

		PersonAPI grandmaster = MagicCampaign.addCustomPerson(lyra.getMarket(), "Grandmaster", "Lyon", "kol_grandmaster",
				FullName.Gender.MALE, KOL_ModPlugin.kolID, Ranks.FACTION_LEADER, Ranks.POST_FACTION_LEADER,
				false, 0, 1);

		PersonAPI lackey2 = MagicCampaign.addCustomPerson(lyra.getMarket(), "Rebecca", "Greenflight", "kol_agent_f",
				FullName.Gender.FEMALE, KOL_ModPlugin.kolID, Ranks.SISTER, Ranks.POST_INTELLIGENCE_DIRECTOR,
				false, 0, 0);

		grandmaster.setId("kol_grandmaster");
		lackey2.setId("kol_intel_director");

		grandmaster.setImportance(PersonImportance.VERY_HIGH);
		grandmaster.setVoice(Voices.FAITHFUL);

		lackey2.setImportance(PersonImportance.HIGH);
		lackey2.setVoice(Voices.FAITHFUL);
	}

	public static void genBattlestarLibra() {
		String entID = "kol_libra";
		StarSystemAPI home = getLibraHome(Long.parseLong(Global.getSector().getSeedString().substring(3)));
		if (home == null) {;
			log.error(String.format("KOL: Could not find a system for Libra"));
			return;
		}
		//SectorEntityToken libra = home.addCustomEntity(entID, "Battlestar Libra", "kol_battlestar_libra_entity", "knights_of_selkie");
		//libra.setCircularOrbitPointingDown(home.getStar(), (float)Math.random()*360f, 4750, 199);

		LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<BaseThemeGenerator.LocationType, Float>();
		weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 2f);
		weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 10f);
		weights.put(BaseThemeGenerator.LocationType.IN_RING, 5f);
		weights.put(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 6f);
		weights.put(BaseThemeGenerator.LocationType.GAS_GIANT_ORBIT, 3f);
		weights.put(BaseThemeGenerator.LocationType.PLANET_ORBIT, 3f);
		weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 0.01f);
		weights.put(BaseThemeGenerator.LocationType.JUMP_ORBIT, 0.03f);
		WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = BaseThemeGenerator.getLocations(null, home, null, 100f, weights);
		BaseThemeGenerator.EntityLocation loc = locs.pick();

		if (loc == null) {;
			log.error(String.format("KOL: Could not find a location for Libra in %s", home.getId()));
			return;
		}

		home.getMemoryWithoutUpdate().set("$kol_libra_start_system", true);
		// Debug
		/*
		runcode org.selkie.kol.world.GenerateKnights.genBattlestarLibra();
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.getMemoryWithoutUpdate().contains("$kol_libra_start_system")) {
				$print(system.getId());
			}
		}
		 */

		String name = "Battlestar Libra";

		BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addNonSalvageEntity(home, loc, "kol_battlestar_libra_entity", KOL_ModPlugin.kolID);
		SectorEntityToken libra = added.entity;

		MarketAPI market = MarketHelpers.addMarketplace("knights_of_selkie", libra, null, name, 3,
				new ArrayList<String>(Arrays.asList(Conditions.OUTPOST,
						Conditions.POPULATION_3)),
				new ArrayList<String>(Arrays.asList(
						Industries.POPULATION,
						Industries.SPACEPORT,
						"kol_garden",
						Industries.HIGHCOMMAND,
						Industries.HEAVYBATTERIES,
						"kol_battlestation_libra",
						Industries.WAYSTATION)),
				new ArrayList<String>(Arrays.asList(
						Submarkets.SUBMARKET_STORAGE,
						Submarkets.GENERIC_MILITARY,
						Submarkets.SUBMARKET_BLACK)),
				1f
		);

		libra.setName(name);
		libra.setCustomDescriptionId("kol_libra_port_desc");
		libra.setInteractionImage("illustrations", "kol_garden_large");
		libra.setDiscoverable(true);
		libra.setSensorProfile(1f);
		libra.getDetectedRangeMod().modifyFlat("gen", 5000f);
		libra.setDiscoveryXP(10000f);

		market.setHidden(true);
		market.setSurveyLevel(MarketAPI.SurveyLevel.NONE);
		market.getMemoryWithoutUpdate().set("$kol_market_libra", true);
		market.getMemoryWithoutUpdate().set(MemFlags.HIDDEN_BASE_MEM_FLAG, true);
		market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		market.setEconGroup(market.getId());
		market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true);

//		boolean down = false;
//		if (entity.getOrbitFocus() instanceof PlanetAPI) {
//			PlanetAPI planet = (PlanetAPI) entity.getOrbitFocus();
//			if (!planet.isStar()) {
//				down = true;
//			}
//		}
//		if (down) {
//			BaseThemeGenerator.convertOrbitPointingDown(entity);
//		}
		BaseThemeGenerator.convertOrbitWithSpin(libra, -2f);

		market.reapplyIndustries();

		Global.getSector().getEconomy().addMarket(market, true);

		log.info(String.format("KOL: Initialized libra market in [%s]", home.getName()));

		MarketHelpers.addMarketPeople(market);

		//This one does have a black market
		//if (KOL_ModPlugin.haveNex) SectorManager.NO_BLACK_MARKET.add(lyra.getMarket().getId());

		PersonAPI elder = MagicCampaign.addCustomPerson(market, "Knightmaster", "Martins", "kol_grandmaster",
				FullName.Gender.MALE, KOL_ModPlugin.kolID, Ranks.ELDER, Ranks.POST_STATION_COMMANDER,
				true, 1, 0);

		elder.setId("kol_libramaster");
		elder.addTag(Tags.CONTACT_MILITARY);
		elder.addTag(Tags.CONTACT_TRADE);

		elder.setImportance(PersonImportance.VERY_HIGH);
		elder.setVoice(Voices.SOLDIER);

		MarketHelpers.addMarketPeople(market);
	}

	protected static final String[] libraExclusionTags = {
			Tags.NOT_RANDOM_MISSION_TARGET,
			Tags.THEME_HIDDEN,
			Tags.THEME_REMNANT_MAIN,
			Tags.THEME_REMNANT_RESURGENT,
			Tags.THEME_UNSAFE,
			Tags.THEME_SPECIAL,
			Tags.THEME_CORE,
	};

	public static StarSystemAPI getLibraHome(long seed) {
		WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(new Random(seed));
		float width = Global.getSettings().getFloat("sectorWidth");
		float height = Global.getSettings().getFloat("sectorHeight");
		OUTER: for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.getStar() == null || system.getStar().getTypeId().equals(StarTypes.NEUTRON_STAR) || system.getStar().getTypeId().equals(StarTypes.BLACK_HOLE) || system.getStar().getTypeId().equals(StarTypes.BLUE_SUPERGIANT)) continue;
			if (system.getPlanets().isEmpty()) continue;
			if (PrepareAbyss.isWithinCoreSpace(system.getLocation().getX(), system.getLocation().getY())) continue;
			for (String tag : libraExclusionTags) {
				if (system.hasTag(tag)) {
					continue OUTER;
				}
			}
			float w = 1f;
			if (system.hasTag(Tags.THEME_INTERESTING)) w *= 10f;
			if (system.hasTag(Tags.THEME_INTERESTING_MINOR)) w *= 5f;
			if (system.getLocation().getX() <= width/-2 + 5000) w *= 5f; //West bias
			if (system.getLocation().getX() <= width/-2 + 10000) w *= 5f; //West bias
			if (system.getLocation().getX() <= width/-2 + 20000) w *= 5f; //West bias
			if (system.getLocation().getX() <= width/-2 + 35000) w *= 5f; //West bias
			if (system.hasSystemwideNebula()) w *= 2f;
			if (Misc.getNumStableLocations(system) < 1) w *= 0.1;
			if (Misc.getNumStableLocations(system) < 2) w *= 0.5;

			picker.add(system, w);
		}
		return picker.pick();
	}


	public static void genKnightsExpeditions() {
		org.selkie.kol.fleets.KnightsExpeditionsManager expeditions = new KnightsExpeditionsManager();
		if (Global.getSector().getStarSystem("Eos Exodus") != null) {
			Global.getSector().getStarSystem("Eos Exodus").addScript(expeditions);
		} else {
			//Random sector
			WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();

			for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
				if (market.getFactionId().equals(Factions.LUDDIC_CHURCH)) {
					picker.add((StarSystemAPI) market.getContainingLocation(), 1f);
				}
				if (market.getFactionId().equals(KOL_ModPlugin.kolID)) {
					picker.add((StarSystemAPI) market.getContainingLocation(), 100f);
				}
			}
			picker.pick().addScript(expeditions);
		}
	}

	public static void copyChurchEquipment() {
		// The knights don't want the misc modiverse ships
		// Unless they have no other choice
		FactionAPI KOL = Global.getSector().getFaction(KOL_ModPlugin.kolID);
	    for (String ship : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getKnownShips()) {
            if (!KOL.knowsShip(ship)
					&& !KOL.getAlwaysKnownShips().contains(ship)) {
                Global.getSector().getFaction(KOL_ModPlugin.kolID).addUseWhenImportingShip(ship);
            }
        }
        //for (String baseShip : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getAlwaysKnownShips()) {
        //    if (!Global.getSector().getFaction(KOL_ModPlugin.kolID).useWhenImportingShip(baseShip)) {
        //        Global.getSector().getFaction(KOL_ModPlugin.kolID).addUseWhenImportingShip(baseShip);
        //    }
        //}
		for (String entry : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getKnownWeapons()) {
			if (!KOL.knowsWeapon(entry)) {
				KOL.addKnownWeapon(entry, false);
			}
		}
		for (String entry : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getKnownFighters()) {
			if (!KOL.knowsFighter(entry)) {
				KOL.addKnownFighter(entry, false);
			}
		}
		for (String entry : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getKnownHullMods()) {
			if (!KOL.knowsHullMod(entry)) {
				KOL.addKnownHullMod(entry);
			}
		}
	}

	public static class KnightsFleetTypes {

		public static final String SCOUT = "kolScout";
		public static final String HEADHUNTER = "kolHeadHunter";
		public static final String WARRIORS = "kolHolyWarriors";
		public static final String PATROL = "kolPatrol";
		public static final String ARMADA = "kolArmada";
	}
}

