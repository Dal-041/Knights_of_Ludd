package org.selkie.zea.helpers;

import org.jetbrains.annotations.NonNls;
import org.selkie.zea.cores.BaseCoreOfficerSkill;
import org.selkie.zea.cores.DawnBossCoreSkill;
import org.selkie.zea.cores.DuskBossCoreSkill;
import org.selkie.zea.cores.ElysiaBossCoreSkill;

import java.util.Objects;

@NonNls
public class ZeaStaticStrings {

    public static class ZeaMemKeys {
        public static final String ZEA_INTIALIZED = "$zea_initialized";

        public static final String ZEA_BOSS_TAG = "$zea_boss";
        public static final String ZEA_NINMAH_WRECK = "$zea_ninmah_wreck";
        public static final String ZEA_NINAYA_WRECK = "$zea_ninaya_wreck";
        public static final String ZEA_NINEVEH_WRECK = "$zea_nineveh_wreck";

        public static final String ZEA_ELYSIAN_BOSS_1_DONE = "$zea_elysian_boss1_done";
        public static final String ZEA_ELYSIAN_BOSS_2_DONE = "$zea_elysian_boss2_done";
        public static final String ZEA_DAWN_BOSS_DONE = "$zea_dawn_boss_done";
        public static final String ZEA_DUSK_BOSS_DONE = "$zea_dusk_boss_done";

        public static final String ZEA_TT_NINAYA_DONE = "$zea_defeatedNinaya";
        public static final String ZEA_TT_NINMAH_DONE = "$zea_defeatedNinmah";
        public static final String ZEA_TT_NINEVEH_DONE = "$zea_defeatedNineveh";

        public static final String ZEA_NINAYA_BOSS_FLEET = "$zea_ninaya";
        public static final String ZEA_NINEVEH_BOSS_FLEET = "$zea_nineveh";
        public final static String ZEA_CORRUPTING_HEART_BOSS_FLEET = "$zea_corruptingheart";
        public static final String ZEA_AMATERASU_BOSS_FLEET = "$zea_amaterasu";
        public final static String ZEA_DAWN_BOSS_FLEET = "$zea_nian";
        public final static String ZEA_DUSK_BOSS_FLEET = "$zea_yukionna";

        public static final String ZEA_TT_3_BLACK_SITE = "$zea_TT3BlackSite";
        public static final String ZEA_TT_3_NASCENT_WELL = "$zea_TT3BlackSite_well";
        public static final String ZEA_TT_3_WEAPONS_CACHE = "$zea_TT3WeaponsCache";

        public static final String ZEA_TT_2_STATION = "$zea_boss_station_tritachyon"; //Sync with rules.csv
        public static final String ZEA_TT_2_SYSTEM = "$zea_tt_boss2_system";
        public static final String ZEA_TT_3_SYSTEM = "$zea_tt_boss3_system";

        public static final String ZEA_ELYSIAN_WITNESS = "$zea_elysian_witness";
        public static final String ZEA_SPOILERS = "$zea_spoilers";
    }

    public static class ZeaEntities {
        public static final String ZEA_ELYSIA_SILENCE = "zea_elysia_silence";
        public static final String ZEA_NULLSPACE_VOID = "zea_nullspace_void";
        public static final String ZEA_LUNASEA_STAR = "zea_lunasea_star";

        public static final String ZEA_LUNASEA_PLANET_ONE = "zea_lunasea_one";
        public static final String ZEA_LUNASEA_PLANET_TWO = "zea_lunasea_two";
        public static final String ZEA_LUNASEA_PLANET_THREE = "zea_lunasea_three";
        public static final String ZEA_LUNASEA_PLANET_FOUR = "zea_lunasea_four";
        public static final String ZEA_LUNASEA_PLANET_FIVE = "zea_lunasea_five";
        public static final String ZEA_LUNASEA_PLANET_SIX = "zea_lunasea_six";

        public static final String ZEA_ELYSIA_PLANET_ONE = "zea_elysia_asclepius";
        public static final String ZEA_ELYSIA_PLANET_TWO = "zea_elysia_appia";
        public static final String ZEA_ELYSIA_PLANET_THREE = "zea_elysia_orpheus";

        public static final String ZEA_ELYSIA_ABYSS = "zea_elysia_abyss";
        public static final String ZEA_ELYSIA_GAZE = "zea_elysia_gaze";
        public static final String ZEA_TT_3_SITE_PLANET = "zea_site_three";

        public static final String ZEA_EDF_CORONAL_TAP = "zea_edf_coronal_tap";
        public static final String ZEA_BOSS_STATION_TRITACHYON = "zea_boss_station_tritachyon";
        public static final String ZEA_NULL_STATION_DUSK = "zea_null_station_dusk";
        public static final String ZEA_NULLGATE_DUSK = "zea_nullgate_dusk";

        public static final String ZEA_RESEARCH_STATION_ELYSIA = "zea_research_station_elysia";
        public static final String ZEA_RESEARCH_STATION_DAWN = "zea_research_station_dawn";
        public static final String ZEA_RESEARCH_STATION_DUSK = "zea_research_station_dusk";

        public static final String ZEA_ELYSIA_JP = "zea_elysia_jp";
        public static final String ZEA_LUNASEA_JP = "zea_lunasea_jp";

        public static final String ZEA_CACHE_LOW = "zea_cache_low";
        public static final String ZEA_CACHE_MED = "zea_cache_med";
        public static final String ZEA_CACHE_HIGH = "zea_cache_high";
    }

    public static class ZeaDrops {
        public static final String TECHMINING_FIRST_FIND = "techmining_first_find";
        public static final String OMEGA_WEAPONS_SMALL = "omega_weapons_small";
        public static final String OMEGA_WEAPONS_MEDIUM = "omega_weapons_medium";
        public static final String OMEGA_WEAPONS_LARGE = "omega_weapons_large";

        public static final String ZEA_OMEGA_SMALL_LOW = "zea_omega_small_low";
        public static final String ZEA_OMEGA_MEDIUM_LOW = "zea_omega_medium_low";
        public static final String ZEA_OMEGA_LARGE_LOW = "zea_omega_large_low";

        public static final String ZEA_HEGFLEET_LORE = "zea_hegfleet_lore";
        public static final String ZEA_TTFLEET_LORE = "zea_ttfleet_lore";
        public static final String ZEA_WEAPONS_HIGH = "zea_weapons_high";
    }

    public static class ZeaStarTypes {
        public static final String US_STAR_BLUE_GIANT = "US_star_blue_giant";
        public static final String US_STAR_YELLOW = "US_star_yellow";
        public static final String US_STAR_ORANGE_GIANT = "US_star_orange_giant";
        public static final String US_STAR_RED_GIANT = "US_star_red_giant";
        public static final String US_STAR_WHITE = "US_star_white";
        public static final String US_STAR_BROWNDWARF = "US_star_browndwarf";

        public static final String ZEA_STAR_BLACK_NEUTRON = "zea_star_black_neutron";
        public static final String ZEA_WHITE_HOLE = "zea_white_hole";
        public static final String ZEA_RED_HOLE = "zea_red_hole";

        public static final String TIANDONG_SHAANXI = "tiandong_shaanxi";
        public static final String STAR_BRSTAR = "star_brstar";
        public static final String STAR_YELLOW_SUPERGIANT = "star_yellow_supergiant";
        public static final String QUASAR = "quasar";
    }

    public static class ZeaTerrain {
        public static final String ZEA_EVENT_HORIZON = "zea_eventHorizon";
        public static final String ZEA_PULSAR_BEAM = "zea_pulsarBeam";
        public static final String ZEA_CORONA = "zea_corona";
        public static final String RINGS_THICC_DARKRED = "rings_thicc_darkred";
        public static final String ZEA_ASTEROID_BELT = "zea_asteroidBelt";
        public static final String NEBULA_ZEA_BLACK_SHINY = "nebula_zea_black_shiny";
        public static final String NEBULA_ZEA_STORM = "nebula_zea_storm";
        public static final String NEBULA_ZEA_SHOAL = "nebula_zea_shoal";
        public static final String ZEA_SEA_WAVE = "zea_seaWave";
        public static final String ZEA_BLACK_BEAM = "zea_blackBeam";
    }

    public static class GfxCat {
        public static final String CHARACTERS = "characters";
        public static final String ILLUSTRATIONS = "illustrations";
        public static final String BACKGROUNDS = "backgrounds";
        public static final String TERRAIN = "terrain";
        public static final String ICONS = "icons";
        public static final String MISC = "misc";
        public static final String FX = "fx";

        public static final String LORE_ITEM = "lore_item";
        public static final String PHASE_GLOWS = "phase_glows";

        public static final String CORES = "cores";
        public static final String UI = "ui";
    }

    //tags
    public static final String ZEA_EXCLUDE_TAG = "zea_rulesfortheebutnotforme";
    public static final String SIC_DELAY_XO = "sic_fleet_encounter2";
    public static final String DRONE_SHIELD_TARGET_KEY = "droneShieldTargetKey";
    public static final String EDF_HYPERSHUNT = "edf_Hypershunt";
    public static final String EDF_HEADQUARTERS = "edf_Headquarters";
    public static final String IntelBreadcrumbTag = "Dark Deeds";
    public static final String IntelLoreTag = "Elysian Lore";

    // hulls
    public static final String ZEA_BOSS_NINAYA = "zea_boss_ninaya";
    public static final String ZEA_BOSS_NINMAH = "zea_boss_ninmah";
    public static final String ZEA_BOSS_NINEVENH = "zea_boss_nineveh";

    public static final String ZEA_BOSS_CORRUPTINGHEART = "zea_boss_corruptingheart";
    public static final String ZEA_BOSS_AMATERASU = "zea_boss_amaterasu";
    public static final String ZEA_BOSS_YUKIONNA = "zea_boss_yukionna";
    public static final String ZEA_BOSS_NIAN = "zea_boss_nian";

    // variants
    public static final String ZEA_BOSS_NINAYA_NIGHTDEMON = "zea_boss_ninaya_Nightdemon";
    public static final String ZEA_BOSS_NINMAH_UNDOER = "zea_boss_ninmah_Undoer";
    public static final String ZEA_BOSS_NINEVEH_SOULEATER = "zea_boss_nineveh_Souleater";

    public static final String ZEA_BOSS_HARBINGER_STRIKE = "zea_boss_harbinger_Strike";
    public static final String ZEA_BOSS_HYPERION_STRIKE = "zea_boss_hyperion_Strike";
    public static final String ZEA_BOSS_DOOM_STRIKE = "zea_boss_doom_Strike";

    public static final String ZEA_BOSS_CORRUPTINGHEART_UNHOLY = "zea_boss_corruptingheart_Unholy";
    public static final String ZEA_BOSS_AMATERASU_BLINDING = "zea_boss_amaterasu_Blinding";
    public static final String ZEA_BOSS_YUKIONNA_ULTIMATE = "zea_boss_yukionna_Ultimate";
    public static final String ZEA_BOSS_NIAN_SALVATION = "zea_boss_nian_Salvation";

    public static final String ZEA_DAWN_CHIWEN_WING = "zea_dawn_chiwen_wing";
    public static final String ZEA_EDF_SHACHIHOKO_WING = "zea_edf_shachihoko_wing";
    public static final String ZEA_EDF_SHACHI_WING = "zea_edf_shachi_wing";

    // rules event hooks
    public static class ruleCMD {
        public static final String ZEA_BOSS_STATION_TT_SALVAGE_2 = "zea_BossStationTT_Salvage2";
        public static final String ZEA_AFTER_NINEVEH_DEFEAT = "zea_AfterNinevehDefeat";
        public static final String ZEA_AFTER_NINAYA_DEFEAT = "zea_AfterNinayaDefeat";
        public static final String ZEA_SPOILERS_POPUP = "zea_spoilers_popup";

        public static final String ADD_INTEL_TTBOSS_1 = "addIntelTTBoss1";
        public static final String ADD_INTEL_TTBOSS_2 = "addIntelTTBoss2";
        public static final String ADD_INTEL_TTBOSS_3 = "addIntelTTBoss3";

        public static final String END_MUSIC = "endMusic";
        public static final String ADD_BOSS_TAGS = "addBossTags";
        public static final String ADD_YUKI_IMAGE = "addYukiImage";
        public static final String SHOW_FLEET_INFO = "showFleetInfo";
    }

    // planets
    public static final String TOXIC = "toxic";
    public static final String JUNGLE = "jungle";

    // missing vanilla statics
    public static final String PHASE_ANCHOR_CAN_DIVE = "phaseAnchor_canDive";
    public static final String EPIPHANY = "epiphany";
    public static final String CHALCEDON = "chalcedon";
    public static final String EOS_EXODUS = "Eos Exodus";
    public static final String KUMARI_KANDAM = "Kumari Kandam";
    public static final String CANAAN = "Canaan";
    public static final String ARCADIA = "Arcadia";
    public static final String AL_GEBBAR = "Al Gebbar";
    public static final String OFFICER_LEVEL_UP = "officerLevelUp";
    public static final String PROGRESS_BAR_FUEL_COLOR = "progressBarFuelColor";
    public static final String RUINS_EXPLORED = "$ruinsExplored";

    // stat mod id
    public static final String BOSS_PHASE_TWO_MODIFIER = "boss_phase_two_modifier";

    // theme
    public static final String THEME_ZEA = "theme_zea";
    public static final String THEME_STORM = "theme_zea_storm";

    // all boss core strings
    public static class BossCore {
        public static final String SPECIAL_BOSS_CORE_ID = "zea_boss_core_special";

        public static class CoreData {
            public final String itemID;
            public final Class<? extends BaseCoreOfficerSkill> exclusiveSkill;
            public final String exclusiveSkillID;
            public final String portraitID;

            public CoreData(String itemId, Class<? extends BaseCoreOfficerSkill> exclusiveSkill, String exclusiveSkillID, String portraitId) {
                this.itemID = itemId;
                this.exclusiveSkill = exclusiveSkill;
                this.exclusiveSkillID = exclusiveSkillID;
                this.portraitID = portraitId;
            }
        }

        public static final CoreData DUSK_CORE = new CoreData("zea_dusk_boss_core", DuskBossCoreSkill.class, "zea_dusk_boss_core_skill", "zea_dusk_boss_core");
        public static final String DORMANT_DUSK_CORE = "zea_dormant_dusk_boss_core";
        public static final CoreData DAWN_CORE = new CoreData("zea_dawn_boss_core", DawnBossCoreSkill.class, "zea_dawn_boss_core_skill", "zea_dawn_boss_core");
        public static final CoreData ELYSIAN_CORE = new CoreData("zea_elysia_boss_core", ElysiaBossCoreSkill.class, "zea_elysia_boss_core_skill","zea_elysia_boss_core");
        public static final CoreData[] CORES = { DUSK_CORE, DAWN_CORE, ELYSIAN_CORE };
        public static final String[] ITEM_ID_LIST = new String[CORES.length];
        public static final String[] SKILL_ID_LIST = new String[CORES.length];
        public static final String[] PORTRAIT_ID_LIST = new String[CORES.length];

        static {
            for (int i = 0; i < CORES.length; i++) {
                CoreData core = CORES[i];
                ITEM_ID_LIST[i] = core.itemID;
                SKILL_ID_LIST[i] = core.exclusiveSkillID;
                PORTRAIT_ID_LIST[i] = core.portraitID;
            }
        }

        public static CoreData getCore(String itemID){
            for(CoreData core : CORES){
                if(Objects.equals(core.itemID, itemID)) return core;
            }
            throw new IllegalArgumentException("Commodity Item: "+itemID+" does not exist");
        }
    }

    public static final String[] techInheritIDs = {
        "remnant",
        "mercenary"
    };

    public static final String elysianID = "zea_elysians";
    public static final String duskID = "zea_dusk";
    public static final String dawnID = "zea_dawn";
    public static final String[] factionIDs = {
        dawnID,
        duskID,
        elysianID
    };
    public static final String nbsSysPrefix = "zea_nbs_"; //Neutron black star
    public static final String nullspaceSysName = "Nullspace";
    public static final String lunaSeaSysName = "The Luna Sea";
    public static final String elysiaSysName = "Elysia";
    public static final String[] systemNames = {
        elysiaSysName,
        nullspaceSysName,
        lunaSeaSysName,
        nbsSysPrefix,
    };

    public static final String[] hullBlacklist = {
        "guardian",
        "radiant",
        "tahlan_asria",
        "tahlan_nirvana",
        "zea_dusk_ayakashi",
        "zea_dawn_ao",
        "zea_dawn_qilin",
        "zea_edf_ryujin",
        "zea_edf_kiyohime"
    };
    public static final String[] weaponBlacklist = {
        "lightdualmg",
        "lightmortar",
        "lightmg",
        "mininglaser",
        "atropos_single",
        "harpoon_single",
        "sabot_single",
        "hammer",
        "hammer_single",
        "hammerrack",
        "jackhammer"
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
    public static final String[] elysianBossSupportingFleet = {
        "zea_edf_tamamo_Striker",
        "zea_edf_tamamo_Striker"
    };
    public static final String[] duskBossSupportingFleet = {
        "zea_dusk_ayakashi_Vengeful",
        "zea_dusk_ayakashi_Whispered",
    };
    public static final String[] zeaAIOverrideShips = {
        "zea_edf_kiyohime",
        "zea_edf_mizuchi",
        "zea_dawn_tianma",
    };

    //campaign abilities
    public static final String abilityJumpElysia = "fracture_jump_elysia";
    public static final String abilityJumpDawn = "fracture_jump_luna_sea";
    public static final String abilityJumpDusk = "fracture_jump_pullsar";

    //Shipsystems
    public static final String systemIDBlizzard = "zea_boss_blizzard";
    public static final String systemIDSupernova = "zea_boss_supernova";
    public static final String systemIDCorruption = "zea_boss_corruptionjets";
    public static final String systemIDFlareWave = "zea_dawn_flare_wave";

    public static final String pathPortraits = "graphics/zea/portraits/";
    public static final String[] portraitsDawnPaths = {
            pathPortraits.concat("zea_dawn_1.png"),
            pathPortraits.concat("zea_dawn_2.png"),
            pathPortraits.concat("zea_dawn_3.png"),
            pathPortraits + "zea_dawn_4.png",
            pathPortraits + "zea_dawn_5.png"
    };
    public static final String[] portraitsDuskPaths = {
            pathPortraits.concat("zea_dusk_1.png"),
            pathPortraits.concat("zea_dusk_2.png"),
            pathPortraits.concat("zea_dusk_3.png")
    };
    public static final String[] portraitsElysianPaths = {
            pathPortraits.concat("zea_edf_1.png"),
            pathPortraits.concat("zea_edf_3.png"),
            pathPortraits.concat("zea_edf_2.png")
    };
    public static final String[] portraitsDawn = {
            "zea_dawn_1",
            "zea_dawn_2",
            "zea_dawn_3",
            "zea_dawn_4",
            "zea_dawn_5"
    };
    public static final String[] portraitsDusk = {
            "zea_dusk_1",
            "zea_dusk_2",
            "zea_dusk_3"
    };
    public static final String[] portraitsElysian = {
            "zea_edf_1",
            "zea_edf_3",
            "zea_edf_2"
    };
    public static final String portraitDawnBoss = "zea_boss_nian";
    public static final String portraitDuskBoss = "zea_boss_yukionna";
    public static final String portraitElysianBoss = "zea_boss_corrupting_heart";
    public static final String portraitAmaterasuBoss = "zea_boss_amaterasu";
    public static final String portraitAlphaPlus = "zea_boss_alphaplus";
    public static final String pathCrests = "graphics/zea/factions/";
    public static final String crestDawn = "zea_crest_dawntide";
    public static final String crestDusk = "zea_crest_dusk";
    public static final String crestEDF = "zea_crest_edf";
    public static final String crestTT = "graphics/factions/crest_tritachyon";


}