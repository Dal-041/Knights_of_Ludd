package org.selkie.kol.world;

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
import exerelin.campaign.AllianceManager;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.SectorManager;
import exerelin.campaign.alliances.Alliance;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import org.apache.log4j.Logger;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.fleets.KnightsExpeditionsManager;
import org.selkie.kol.fleets.SpawnInvictus;
import org.selkie.kol.fleets.SpawnRetribution;
import org.selkie.kol.helpers.KolStaticStrings;
import org.selkie.kol.helpers.MarketHelpers;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat;
import org.selkie.zea.intel.ZeaMechanicIntel;
import org.selkie.zea.world.PrepareAbyss;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;

public class GenerateKnights {

	public static final Logger log = Global.getLogger(GenerateKnights.class);

	public static final int baseKnightExpeditions = 2;
	
	public static void genCorvus() {
		Global.getSector().getStarSystem(ZeaStaticStrings.EOS_EXODUS).setBackgroundTextureFilename("graphics/kol/backgrounds/kol_bg_1.jpg");
		Global.getSector().getStarSystem(ZeaStaticStrings.KUMARI_KANDAM).setBackgroundTextureFilename("graphics/kol/backgrounds/kol_bg_2.jpg");
		Global.getSector().getStarSystem(ZeaStaticStrings.CANAAN).setBackgroundTextureFilename("graphics/kol/backgrounds/kol_bg_3.jpg");
		Global.getSector().getStarSystem(ZeaStaticStrings.AL_GEBBAR).setBackgroundTextureFilename("graphics/kol/backgrounds/kol_bg_4.jpg");
		genKnightsBattlestation();
		genKnightsStarfortress();
	}

	public static void genAlways() {
		ensureLibra();
		SpawnInvictus.spawnInvictus();
		SpawnRetribution.spawnRetribution();
		copyChurchEquipment();
		startupRelations();
		genKnightsExpeditions();
		GenerateKnights.addKoLIntel();
	}

	public static void startupRelations() {
		if (Global.getSector().getFaction(Factions.LUDDIC_CHURCH) != null && Global.getSector().getFaction(KolStaticStrings.kolFactionID) != null) {
			FactionAPI church = Global.getSector().getFaction(Factions.LUDDIC_CHURCH);
			FactionAPI knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID);

			if(church.getRelToPlayer().isAtWorst(RepLevel.SUSPICIOUS)) {
				church.getRelToPlayer().setRel(Math.max(church.getRelToPlayer().getRel(), knights.getRelToPlayer().getRel()));
				knights.getRelToPlayer().setRel(Math.max(church.getRelToPlayer().getRel(), knights.getRelToPlayer().getRel()));
			}

			for(FactionAPI faction:Global.getSector().getAllFactions()) {
				knights.setRelationship(faction.getId(), church.getRelationship(faction.getId()));
			}
			if (Misc.getCommissionFactionId() != null && Misc.getCommissionFactionId().equals(KolStaticStrings.kolFactionID)) {
				FactionAPI player = Global.getSector().getPlayerFaction();
				for(FactionAPI faction:Global.getSector().getAllFactions()) {
					player.setRelationship(faction.getId(), knights.getRelationship(faction.getId()));
				}
			}
			
			// THE CHURCH OF GALACTIC REDEMPTION.
			if (KOL_ModPlugin.haveNex) {
				knights.setRelationship(Factions.LUDDIC_CHURCH, 1.00f);
				if (true || Global.getSettings().getBoolean("foundCOGH")) {
					Alliance COGH = AllianceManager.createAlliance(KolStaticStrings.kolFactionID, Factions.LUDDIC_CHURCH, AllianceManager.getBestAlignment(KolStaticStrings.kolFactionID, Factions.LUDDIC_CHURCH));
					COGH.addPermaMember(KolStaticStrings.kolFactionID);
					COGH.addPermaMember(Factions.LUDDIC_CHURCH);
					COGH.setName(Global.getSettings().getString("knights_of_ludd", "ChurchOfGalacticRedemption"));
				}

				NexFactionConfig factionConfig = NexConfig.getFactionConfig(KolStaticStrings.kolFactionID);
				if (!DiplomacyManager.isRandomFactionRelationships()) {factionConfig.minRelationships.clear();factionConfig.minRelationships.put(Factions.LUDDIC_CHURCH, 0.251f);}
				if (KolStaticStrings.kolFactionID.equals(PlayerFactionStore.getPlayerFactionIdNGC()) || Factions.LUDDIC_CHURCH.equals(PlayerFactionStore.getPlayerFactionIdNGC())) {
					knights.setRelationship(Factions.LUDDIC_CHURCH, 0.75f); //Prevents you from instantly gaining 1 story point repping up to 100...
				}
			}
		}
	}

	public static void addKoLIntel() {
		if (Global.getSector() == null) return;
		while (ZeaMechanicIntel.unknownMechanics(KolStaticStrings.kolFactionID) > 0) {
			Global.getSector().getIntelManager().addIntel(ZeaMechanicIntel.getNextMechanicIntel(KolStaticStrings.kolFactionID));
		}
	}

	public static void genKnightsBattlestation() {
		StarSystemAPI Canaan = Global.getSector().getStarSystem(ZeaStaticStrings.CANAAN);
        SectorEntityToken cygnus = Canaan.addCustomEntity(KolStaticStrings.KOL_CYGNUS, "Battlestation Cygnus", "station_lowtech2", KolStaticStrings.kolFactionID);
        cygnus.setCircularOrbitPointingDown(Canaan.getEntityById("canaan_gate"), 33, 475, 99);
        cygnus.setCustomDescriptionId("kol_cygnus_desc");
		//cygnus.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.KEEP_PLAYING_LOCATION_MUSIC_DURING_ENCOUNTER_MEM_KEY, true);
        
        MarketHelpers.addMarketplace(KolStaticStrings.kolFactionID, cygnus, null, "Battlestation Cygnus", 4,
                new ArrayList<>(Arrays.asList(Conditions.OUTPOST,
                        Conditions.POPULATION_4)),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        "kol_garden",
                        Industries.MILITARYBASE,
                        Industries.LIGHTINDUSTRY,
                        Industries.HEAVYBATTERIES,
                        Industries.BATTLESTATION)),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN)),
        		0.3f
        );

		cygnus.getMarket().removeSubmarket(Submarkets.SUBMARKET_BLACK);
		if (KOL_ModPlugin.haveNex) SectorManager.NO_BLACK_MARKET.add(cygnus.getMarket().getId());

		cygnus.setInteractionImage(GfxCat.ILLUSTRATIONS, "kol_tree_canaan_large");

		MarketHelpers.addMarketPeople(cygnus.getMarket());

		PersonAPI master = MagicCampaign.addCustomPerson(cygnus.getMarket(), "Master", "Helensis", "kol_chaptermaster",
				FullName.Gender.FEMALE, KolStaticStrings.kolFactionID, Ranks.ELDER, Ranks.POST_MILITARY_ADMINISTRATOR,
				false, 1, 1);

		PersonAPI lackey1 = MagicCampaign.addCustomPerson(cygnus.getMarket(), "Brother", "Enarms", "kol_agent_m",
				FullName.Gender.MALE, KolStaticStrings.kolFactionID, Ranks.KNIGHT_CAPTAIN, Ranks.POST_GUARD_LEADER,
				false, 0, 0);

		master.setId("kol_chaptermaster");
		lackey1.setId("kol_knightcaptain");

		master.setImportance(PersonImportance.HIGH);
		master.setVoice(Voices.SOLDIER);

		lackey1.setImportance(PersonImportance.MEDIUM);
		lackey1.setVoice(Voices.FAITHFUL);
		lackey1.addTag(Tags.CONTACT_MILITARY); // story character who can later offer contact missions
		Global.getSector().getImportantPeople().addPerson(lackey1);
	}

	/** Brother Enarms: registered as an important person with a contact tag. Idempotent; also fixes older saves. */
	public static void ensureEnarms() {
		PersonAPI enarms = Global.getSector().getImportantPeople().getPerson(KolStaticStrings.KolPrelude.ENARMS_ID);
		if (enarms == null) {
			MarketAPI cygnus = Global.getSector().getEconomy().getMarket(KolStaticStrings.KOL_CYGNUS);
			if (cygnus == null) return; // no Cygnus (e.g. Nexerelin random sector): no Enarms, no prelude chain
			for (PersonAPI person : cygnus.getPeopleCopy()) {
				if (KolStaticStrings.KolPrelude.ENARMS_ID.equals(person.getId())) {
					enarms = person;
					Global.getSector().getImportantPeople().addPerson(enarms);
					break;
				}
			}
			if (enarms == null) return;
		}
		if (!enarms.hasTag(Tags.CONTACT_MILITARY)) enarms.addTag(Tags.CONTACT_MILITARY);
	}
	
	public static void genKnightsStarfortress() {
		String entID = "kol_lyra";
		StarSystemAPI Eos = Global.getSector().getStarSystem(ZeaStaticStrings.EOS_EXODUS);
        SectorEntityToken lyra = Eos.addCustomEntity(entID, "Star Keep Lyra", "station_lowtech3", KolStaticStrings.kolFactionID);
        lyra.setCircularOrbitPointingDown(Eos.getEntityById("eos_exodus_gate"), 33, 475, 99);
        lyra.setCustomDescriptionId("kol_lyra_desc");
		//yra.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.KEEP_PLAYING_LOCATION_MUSIC_DURING_ENCOUNTER_MEM_KEY, true);

		MarketAPI lyraMarket = MarketHelpers.addMarketplace(KolStaticStrings.kolFactionID, lyra, null, "Star Keep Lyra", 5,
                new ArrayList<>(Arrays.asList(Conditions.OUTPOST,
                        Conditions.POPULATION_5)),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        "kol_garden",
                        Industries.HIGHCOMMAND,
                        Industries.ORBITALWORKS,
                        Industries.FUELPROD,
                        Industries.HEAVYBATTERIES,
                        Industries.STARFORTRESS,
                        Industries.WAYSTATION)),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK,
                        Submarkets.SUBMARKET_OPEN)),
        		0.3f
        );

		lyraMarket.getIndustry(Industries.HIGHCOMMAND).setImproved(true);
		lyraMarket.getIndustry(Industries.HEAVYBATTERIES).setImproved(true);

		lyra.setInteractionImage(GfxCat.ILLUSTRATIONS, "kol_citadel_large");

		lyraMarket.removeSubmarket(Submarkets.SUBMARKET_BLACK);
		if (KOL_ModPlugin.haveNex) SectorManager.NO_BLACK_MARKET.add(lyraMarket.getId());

		MarketHelpers.addMarketPeople(lyraMarket);

		PersonAPI grandmaster = MagicCampaign.addCustomPerson(lyraMarket, "Grandmaster", "Lyon", "kol_grandmaster",
				FullName.Gender.MALE, KolStaticStrings.kolFactionID, Ranks.FACTION_LEADER, Ranks.POST_FACTION_LEADER,
				false, 0, 1);

		PersonAPI lackey2 = MagicCampaign.addCustomPerson(lyraMarket, "Rebecca", "Greenflight", "kol_agent_f",
				FullName.Gender.FEMALE, KolStaticStrings.kolFactionID, Ranks.SISTER, Ranks.POST_INTELLIGENCE_DIRECTOR,
				false, 0, 0);

		grandmaster.setId("kol_grandmaster");
		lackey2.setId("kol_intel_director");

		grandmaster.setImportance(PersonImportance.VERY_HIGH);
		grandmaster.setVoice(Voices.FAITHFUL);

		lackey2.setImportance(PersonImportance.HIGH);
		lackey2.setVoice(Voices.FAITHFUL);
	}

	/** The Libra market, or null if this sector has none. */
	public static MarketAPI getLibraMarket() {
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.getMemoryWithoutUpdate().getBoolean(KolStaticStrings.KolMemKeys.KOL_MARKET_LIBRA)) return market;
		}
		return null;
	}

	/** Creates Libra if the sector doesn't have it (older saves, or a failed placement). Idempotent. */
	public static void ensureLibra() {
		if (getLibraMarket() != null) return;
		log.info("KOL: Battlestar Libra missing, creating it");
		genBattlestarLibra();
	}

	public static void genBattlestarLibra() {
		long seed = Long.parseLong(Global.getSector().getSeedString().substring(3));
		// Strict pass first (same first pick as before), then relaxed rules if no system works
		if (placeLibra(getLibraHomePicker(seed, libraExclusionTags, true), "strict")) return;
		if (placeLibra(getLibraHomePicker(seed, libraRelaxedExclusionTags, false), "relaxed")) return;
		log.error("KOL: Could not find a system and location for Libra, even with relaxed rules");
	}

	private static boolean placeLibra(WeightedRandomPicker<StarSystemAPI> homes, String pass) {
		while (!homes.isEmpty()) {
			StarSystemAPI home = homes.pickAndRemove();
			BaseThemeGenerator.EntityLocation loc = pickLibraLocation(home);
			if (loc == null) {
				log.warn(String.format("KOL: No location for Libra in %s, trying another system", home.getId()));
				continue;
			}
			log.info(String.format("KOL: Placing Libra in [%s] (%s pass)", home.getName(), pass));
			buildBattlestarLibra(home, loc);
			return true;
		}
		return false;
	}

	private static BaseThemeGenerator.EntityLocation pickLibraLocation(StarSystemAPI home) {
		LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<>();
		weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 2f);
		weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 10f);
		weights.put(BaseThemeGenerator.LocationType.IN_RING, 5f);
		weights.put(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 6f);
		weights.put(BaseThemeGenerator.LocationType.GAS_GIANT_ORBIT, 3f);
		weights.put(BaseThemeGenerator.LocationType.PLANET_ORBIT, 3f);
		weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 0.01f);
		weights.put(BaseThemeGenerator.LocationType.JUMP_ORBIT, 0.03f);
		WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = BaseThemeGenerator.getLocations(null, home, null, 100f, weights);
		return locs.pick();
	}

	private static void buildBattlestarLibra(StarSystemAPI home, BaseThemeGenerator.EntityLocation loc) {
		home.getMemoryWithoutUpdate().set(KolStaticStrings.KolMemKeys.KOL_LIBRA_START_SYSTEM, true);
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

		BaseThemeGenerator.AddedEntity added = BaseThemeGenerator.addNonSalvageEntity(home, loc, "kol_battlestar_libra_entity", KolStaticStrings.kolFactionID);
		SectorEntityToken libra = added.entity;

		MarketAPI market = MarketHelpers.addMarketplace(KolStaticStrings.kolFactionID, libra, null, name, 3,
                new ArrayList<>(Arrays.asList(Conditions.OUTPOST,
                        Conditions.POPULATION_3)),
                new ArrayList<>(Arrays.asList(
                        Industries.POPULATION,
                        Industries.SPACEPORT,
                        "kol_garden",
                        Industries.HIGHCOMMAND,
                        Industries.HEAVYBATTERIES,
                        "kol_battlestation_libra",
                        Industries.WAYSTATION)),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_BLACK)),
				1f
		);

		libra.setName(name);
		libra.setCustomDescriptionId("kol_libra_port_desc");
		libra.setInteractionImage(GfxCat.ILLUSTRATIONS, "kol_garden_large");
		libra.setDiscoverable(true);
		libra.setSensorProfile(1f);
		libra.getDetectedRangeMod().modifyFlat("gen", 5000f);
		libra.setDiscoveryXP(10000f);

		market.setHidden(true);
		market.setSurveyLevel(MarketAPI.SurveyLevel.NONE);
		market.getMemoryWithoutUpdate().set(KolStaticStrings.KolMemKeys.KOL_MARKET_LIBRA, true);
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
				FullName.Gender.MALE, KolStaticStrings.kolFactionID, Ranks.ELDER, Ranks.POST_STATION_COMMANDER,
				true, 1, 0);

		elder.setId("kol_libramaster");
		elder.addTag(Tags.CONTACT_MILITARY);
		elder.addTag(Tags.CONTACT_TRADE);

		elder.setImportance(PersonImportance.VERY_HIGH);
		elder.setVoice(Voices.SOLDIER);
		Global.getSector().getImportantPeople().addPerson(elder);

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

	// Fallback when no system passes the strict rules: still never the core worlds or Remnant strongholds
	protected static final String[] libraRelaxedExclusionTags = {
			Tags.THEME_REMNANT_MAIN,
			Tags.THEME_REMNANT_RESURGENT,
			Tags.THEME_CORE,
	};

	public static StarSystemAPI getLibraHome(long seed) {
		return getLibraHomePicker(seed, libraExclusionTags, true).pick();
	}

	private static WeightedRandomPicker<StarSystemAPI> getLibraHomePicker(long seed, String[] exclusionTags, boolean strictStars) {
		WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(new Random(seed));
		float width = Global.getSettings().getFloat("sectorWidth");
		float height = Global.getSettings().getFloat("sectorHeight");
		OUTER: for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.getStar() == null) continue;
			if (strictStars && (system.getStar().getTypeId().equals(StarTypes.NEUTRON_STAR) || system.getStar().getTypeId().equals(StarTypes.BLACK_HOLE) || system.getStar().getTypeId().equals(StarTypes.BLUE_SUPERGIANT))) continue;
			if (system.getPlanets().isEmpty()) continue;
			if (PrepareAbyss.isWithinCoreSpace(system.getLocation().getX(), system.getLocation().getY())) continue;
			for (String tag : exclusionTags) {
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
			if (Misc.getNumStableLocations(system) < 1) w *= 0.1F;
			if (Misc.getNumStableLocations(system) < 2) w *= 0.5F;

			picker.add(system, w);
		}
		return picker;
	}


	public static void genKnightsExpeditions() {
		org.selkie.kol.fleets.KnightsExpeditionsManager expeditions = new KnightsExpeditionsManager();
		if (Global.getSector().getStarSystem(ZeaStaticStrings.EOS_EXODUS) != null) {
			Global.getSector().getStarSystem(ZeaStaticStrings.EOS_EXODUS).addScript(expeditions);
		} else {
			//Random sector
			WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();

			for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
				if (market.getFactionId().equals(Factions.LUDDIC_CHURCH)) {
					picker.add((StarSystemAPI) market.getContainingLocation(), 1f);
				}
				if (market.getFactionId().equals(KolStaticStrings.kolFactionID)) {
					picker.add((StarSystemAPI) market.getContainingLocation(), 100f);
				}
			}
			picker.pick().addScript(expeditions);
		}
	}

	public static void copyChurchEquipment() {
		// The knights don't want the misc modiverse ships
		// Unless they have no other choice
		FactionAPI KOL = Global.getSector().getFaction(KolStaticStrings.kolFactionID);
	    for (String ship : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getKnownShips()) {
            if (!KOL.knowsShip(ship)
					&& !KOL.getAlwaysKnownShips().contains(ship)) {
                Global.getSector().getFaction(KolStaticStrings.kolFactionID).addUseWhenImportingShip(ship);
            }
        }
        //for (String baseShip : Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getAlwaysKnownShips()) {
        //    if (!Global.getSector().getFaction(ZeaStaticStrings.kolID).useWhenImportingShip(baseShip)) {
        //        Global.getSector().getFaction(ZeaStaticStrings.kolID).addUseWhenImportingShip(baseShip);
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

}

