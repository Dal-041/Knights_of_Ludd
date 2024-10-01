package org.selkie.kol.impl.helpers;

import org.jetbrains.annotations.NonNls;
import org.selkie.kol.impl.skills.cores.BaseCoreOfficerSkill;
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill;
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill;
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill;

import java.util.Objects;

@NonNls
public class ZeaStaticStrings {

    // mod id's
    public static final String LOST_SECTOR = "lost_sector";
    public static final String TAHLAN = "tahlan";

    //mod strings
    public static final String TAHLAN_ALLMOTHER = "tahlan_allmother";
    public static final String SOTF_DUSTKEEPERS = "sotf_dustkeepers";
    public static final String DOMRES = "domres";
    public static final String ENIGMA = "enigma";
    public static final String TAHLAN_CHILD = "tahlan_child";

    //tags
    public static final String ZEA_EXCLUDE_TAG = "zea_rulesfortheebutnotforme";
    public static final String DRONE_SHIELD_TARGET_KEY = "droneShieldTargetKey";

    //sounds
    public static final String HIT_HEAVY = "hit_heavy";
    public static final String HIT_SHIELD_HEAVY_GUN = "hit_shield_heavy_gun";
    public static final String SYSTEM_PHASE_CLOAK_ACTIVATE = "system_phase_cloak_activate";
    public static final String SYSTEM_PHASE_CLOAK_DEACTIVATE = "system_phase_cloak_deactivate";
    public static final String LIDAR_WINDUP = "lidar_windup";
    public static final String SYSTEM_TARGETING_FEED_LOOP = "system_targeting_feed_loop";
    public static final String SYSTEM_CANISTER_FLAK_EXPLOSION = "system_canister_flak_explosion";
    public static final String UI_ACQUIRED_BLUEPRINT = "ui_acquired_blueprint";
    public static final String MOTE_ATTRACTOR_IMPACT_EMP_ARC = "mote_attractor_impact_emp_arc";
    public static final String MOTE_ATTRACTOR_IMPACT_DAMAGE = "mote_attractor_impact_damage";
    public static final String MOTE_ATTRACTOR_IMPACT_NORMAL = "mote_attractor_impact_normal";

    //sprite category
    public static final String BACKGROUNDS = "backgrounds";
    public static final String TERRAIN = "terrain";
    public static final String LORE_ITEM = "lore_item";
    public static final String ZEA_PHASE_GLOWS = "zea_phase_glows";
    public static final String FX = "fx";
    public static final String ILLUSTRATIONS = "illustrations";
    public static final String ICONS = "icons";
    public static final String CHARACTERS = "characters";
    public static final String KOL_FX = "kol_fx";

    //sprite id
    public static final String AURORA = "aurora";
    public static final String PULSAR = "pulsar";
    public static final String ZEA_ELYSIA_VORTICE = "zea_elysia_vortice";
    public static final String ZEA_BANNER = "zea_banner";
    public static final String GAME_ICON = "game_icon";
    public static final String ZEA_RING_TARGETING = "zea_ring_targeting";
    public static final String ZEA_TT_DELTA_SITE = "zea_tt_delta_site";

    // CR id
    public static final String CORONA = "corona";
    public static final String FLARE = "flare";

    //hulls
    public static final String ZEA_BOSS_NINAYA = "zea_boss_ninaya";
    public static final String ZEA_BOSS_NINMAH = "zea_boss_ninmah";
    public static final String ZEA_BOSS_NINEVENH = "zea_boss_nineveh";
    public static final String DEM_DRONE = "dem_drone";

    //variants
    public static final String ZEA_BOSS_NINAYA_NIGHTDEMON = "zea_boss_ninaya_Nightdemon";
    public static final String ZEA_BOSS_NINMAH_UNDOER = "zea_boss_ninmah_Undoer";
    public static final String ZEA_BOSS_NINEVEH_SOULEATER = "zea_boss_nineveh_Souleater";

    public static final String KOL_BOSS_RET_LP_OVERDRIVEN = "kol_boss_ret_lp_Overdriven";
    public static final String KOL_INVICTUS_LP_HALLOWED = "kol_invictus_lp_Hallowed";

    public static final String ZEA_BOSS_CORRUPTINGHEART_UNHOLY = "zea_boss_corruptingheart_Unholy";
    public static final String ZEA_BOSS_AMATERASU_BLINDING = "zea_boss_amaterasu_Blinding";
    public static final String ZEA_BOSS_YUKIONNA_ULTIMATE = "zea_boss_yukionna_Ultimate";
    public static final String ZEA_BOSS_NIAN_SALVATION = "zea_boss_nian_Salvation";

    public static final String ZEA_DAWN_CHIWEN_WING = "zea_dawn_chiwen_wing";
    public static final String ZEA_EDF_SHACHIHOKO_WING = "zea_edf_shachihoko_wing";

    //weapons
    public static final String TARGETINGLASER_2 = "targetinglaser2";
    public static final String ZEA_RADIANCE_DUMMY_WPN = "zea_radiance_dummy_wpn";

    //portrait id
    public static final String ZEA_BOSS_ALPHAPLUS = "zea_boss_alphaplus";

    //rules event hooks
    public static final String ZEA_BOSS_STATION_TT_SALVAGE_2 = "zea_BossStationTT_Salvage2";
    public static final String ZEA_AFTER_NINEVEH_DEFEAT = "zea_AfterNinevehDefeat";
    public static final String ZEA_AFTER_NINAYA_DEFEAT = "zea_AfterNinayaDefeat";
    public static final String ZEA_SPOILERS_POPUP = "zea_spoilers_popup";

    public static final String ADD_INTEL_TTBOSS_1 = "addIntelTTBoss1";
    public static final String ADD_INTEL_TTBOSS_2 = "addIntelTTBoss2";
    public static final String ADD_INTEL_TTBOSS_3 = "addIntelTTBoss3";
    public static final String END_MUSIC = "endMusic";
    public static final String ADD_BOSS_TAGS = "addBossTags";

    //intel tag
    public static final String KNIGHTS_OF_LUDD = "Knights of Ludd";

    //campaign entity
    public static final String ZEA_STAR_BLACK_NEUTRON = "zea_star_black_neutron";
    public static final String ZEA_NULLGATE = "zea_nullgate";
    public static final String ZEA_NULL_STATION = "zea_null_station_dusk";

    public static final String ZEA_LUNASEA_PLANET_ONE = "zea_lunasea_one";
    public static final String ZEA_LUNASEA_PLANET_TWO = "zea_lunasea_two";
    public static final String ZEA_LUNASEA_PLANET_THREE = "zea_lunasea_three";
    public static final String ZEA_LUNASEA_PLANET_FOUR = "zea_lunasea_four";
    public static final String ZEA_LUNASEA_PLANET_FIVE = "zea_lunasea_five";
    public static final String ZEA_LUNASEA_PLANET_SIX = "zea_lunasea_six";

    public static final String ZEA_ELYSIA_ABYSS = "zea_elysia_abyss";
    public static final String ZEA_ELYSIA_GAZE = "zea_elysia_gaze";
    public static final String ZEA_TT_3_SITE_PLANET = "zea_site_three";

    public static final String ZEA_EDF_CORONAL_TAP = "zea_edf_coronal_tap";
    public static final String ZEA_NULL_STATION_DUSK = "zea_null_station_dusk";
    public static final String ZEA_BOSS_STATION_TRITACHYON = "zea_boss_station_tritachyon";

    //market
    public static final String KOL_CYGNUS = "kol_cygnus";
    public static final String KOL_LYRA = "kol_lyra";

    // vanilla missing statics
    public static final String PHASE_ANCHOR_CAN_DIVE = "phaseAnchor_canDive";
    public static final String EPIPHANY = "epiphany";
    public static final String CHALCEDON = "chalcedon";
    public static final String OFFICER_LEVEL_UP = "officerLevelUp";
    public static final String PROGRESS_BAR_FUEL_COLOR = "progressBarFuelColor";
    public static final String RUINS_EXPLORED = "$ruinsExplored";

    //stat mod id
    public static final String BOSS_PHASE_TWO_MODIFIER = "boss_phase_two_modifier";


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
    public static final String IntelBreadcrumbTag = "Dark Deeds";
    public static final String IntelLoreTag = "Elysian Lore";
    public static final String KEY_ELYSIA_WITNESS = "$zea_elysian_witness";
    public static final String KEY_ZEA_SPOILERS = "$zea_spoilers";
    public static final String THEME_ZEA = "theme_zea";
    public static final String THEME_STORM = "theme_zea_storm";
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
    public static final String abilityJumpElysia = "fracture_jump_elysia";
    public static final String abilityJumpDawn = "fracture_jump_luna_sea";
    public static final String abilityJumpDusk = "fracture_jump_pullsar";
    public static final String systemIDBlizzard = "zea_boss_blizzard";
    public static final String systemIDSupernova = "zea_boss_supernova";
    public static final String systemIDCorruption = "zea_boss_corruptionjets";
    public static final String pathPortraits = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/";
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
    public static final String pathCrests = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/factions/";
    public static final String crestDawn = "zea_crest_dawntide";
    public static final String crestDusk = "zea_crest_dusk";
    public static final String crestEDF = "zea_crest_edf";
    public static final String crestTT = "graphics/factions/crest_tritachyon";



    public static class MemKeys {
        public static final String BOSS_TAG = "$zea_boss";
        public static final String MEMKEY_ZEA_NINMAH_WRECK = "$zea_ninmah_wreck";
        public static final String MEMKEY_ZEA_NINAYA_WRECK = "$zea_ninmaya_wreck";
        public static final String MEMKEY_ZEA_NINEVEH_WRECK = "$zea_nineveh_wreck";

        public static final String MEMKEY_ZEA_ELYSIAN_BOSS_1_DONE = "$zea_elysian_boss1_done";
        public static final String MEMKEY_ZEA_ELYSIAN_BOSS_2_DONE = "$zea_elysian_boss2_done";
        public static final String MEMKEY_ZEA_DAWN_BOSS_DONE = "$zea_dawn_boss_done";
        public static final String MEMKEY_ZEA_DUSK_BOSS_DONE = "$zea_dusk_boss_done";
        public static final String MEMKEY_ZEA_TT_NINAYA_DONE = "$zea_defeatedNinaya";
        public static final String MEMKEY_ZEA_TT_NINMAH_DONE = "$zea_defeatedNinmah";
        public static final String MEMKEY_ZEA_TT_NINEVEH_DONE = "$zea_defeatedNineveh";

        public static final String MEMKEY_ZEA_NINAYA_BOSS_FLEET = "$zea_ninaya";
        public static final String MEMKEY_ZEA_NINEVEH_BOSS_FLEET = "$zea_nineveh";
        public final static String MEMKEY_ZEA_CORRUPTING_HEART_BOSS_FLEET = "$zea_corruptingheart";
        public static final String MEMKEY_ZEA_AMATERASU_BOSS_FLEET = "$zea_amaterasu";
        public final static String MEMKEY_ZEA_DAWN_BOSS_FLEET = "$zea_nian";
        public final static String MEMKEY_ZEA_DUSK_BOSS_FLEET = "$zea_yukionna";

        public static final String MEMKEY_ZEA_TT_3_BLACK_SITE = "$zea_TT3BlackSite";
        public static final String MEMKEY_ZEA_TT_3_NASCENT_WELL = "$zea_TT3BlackSite_well";
        public static final String MEMKEY_ZEA_TT_3_WEAPONS_CACHE = "$zea_TT3WeaponsCache";

        public static final String MEMKEY_ZEA_TT_2_STATION = "$zea_boss_station_tritachyon"; //Sync with rules.csv
        public static final String MEMKEY_ZEA_TT_2_SYSTEM = "$zea_tt_boss2_system";
        public static final String MEMKEY_ZEA_TT_3_SYSTEM = "$zea_tt_boss3_system";
    }
}