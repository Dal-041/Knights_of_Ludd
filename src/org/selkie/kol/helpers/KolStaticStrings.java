package org.selkie.kol.helpers;

import org.jetbrains.annotations.NonNls;

@NonNls
public class KolStaticStrings {
    public static final String ModID = "Knights of Ludd";
    public static final String kolFactionID = "knights_of_selkie";

    public static final String[] KNIGHT_HULLS = {
            "kol_alysse",
            "kol_larkspur",
            "kol_libra",
            "kol_lotus",
            "kol_lunaria",
            "kol_marigold",
            "kol_mimosa",
            "kol_protea",
            "kol_snowdrop",
            "kol_sundew",
            "kol_tamarisk"
    };

    // hullmod
    public static final String KNIGHT_REFIT = "kol_refit";

    // stat mod
    public static final String KNIGHT_REFIT_MODIFIER = "knightRefit";

    // mod id's
    public static final String LOST_SECTOR = "lost_sector";
    public static final String TAHLAN = "tahlan";
    public static final String MORE_MILITARY_MISSIONS = "MoreMilitaryMissions";
    public static final String KNIGHTS_OF_LUDD_MAPS = "knights_of_ludd_maps";
    public static final String SHADER_LIB = "shaderLib";
    public static final String NEXERELIN = "nexerelin";
    
    //mod strings
    public static final String TAHLAN_ALLMOTHER = "tahlan_allmother";
    public static final String SOTF_DUSTKEEPERS = "sotf_dustkeepers";
    public static final String DOMRES = "domres";
    public static final String ENIGMA = "enigma";
    public static final String TAHLAN_CHILD = "tahlan_child";
    public static final String KOL_MODULE_HULKED = "kol_module_hulked";
    public static final String KOL_MODULE_DEAD = "kol_module_dead";
    public static final String KOL_BOSS_RET_LP = "kol_boss_ret_lp";
    public static final String KOL_INVICTUS_LP = "kol_invictus_lp";
    public static final String KOL_BOSS_RET_LP_OVERDRIVEN = "kol_boss_ret_lp_Overdriven";
    public static final String KOL_INVICTUS_LP_HALLOWED = "kol_invictus_lp_Hallowed";
    
    // intel tag
    public static final String KNIGHTS_OF_LUDD = "Knights of Ludd";

    // fleet tags
    public static final String KOL_FLEET_MERGED_KNIGHTS = "kol_merged_knights";
    
    // campaign entity
    public static final String KOL_CYGNUS = "kol_cygnus";
    public static final String KOL_LYRA = "kol_lyra";
    public static final String MEMKEY_KOL_SCHISMED = "$kol_knights_schism";

    public static final String MGA_DIRECTOR = "AmadouSembene";

    public static class KnightsFleetTypes {

        public static final String SCOUT = "kolScout";
        public static final String HEADHUNTER = "kolHeadHunter";
        public static final String WARRIORS = "kolHolyWarriors";
        public static final String PATROL = "kolPatrol";
        public static final String ARMADA = "kolArmada";
    }

    public static class KolMemKeys {
        public static final String KOL_INTIALIZED = "$kol_initialized";
        
        public static final String KOL_MARKET_LIBRA = "$kol_market_libra";
        public static final String KOL_LIBRA_START_SYSTEM = "$kol_libra_start_system";

        public static final String KOL_BOSS_LP_RETRIBUTION_FLEET = "$kol_boss_retribution";
        public static final String KOL_BOSS_LP_INVICTUS_FLEET = "$kol_boss_invictus";

        public static final String KOL_LP_RETRIBUTION_DONE = "$kol_lp_retribution_dropped";
        public static final String KOL_LP_INVICTUS_DONE = "$kol_lp_invictus_dropped";

        public static final String KOL_TAKEOVERS_ENDED = "$kol_takeovers_ended";
    }

    // Knights prelude quest chain (openspec change add-knights-prelude-chain).
    // Global memory keys; rules.csv reads them as $global.<key without $>.
    public static class KolPrelude {
        public static final String ENARMS_ID = "kol_knightcaptain";
        public static final String MARTINS_ID = "kol_libramaster";

        // mission ids (person_missions.csv / bar_events.csv)
        public static final String HOOK_ID = "kolPreludeHook";
        public static final String PIRATE_ID = "kolPreludePirate";
        public static final String LIBRA_ID = "kolPreludeLibra";

        // mission references (setGlobalReference)
        public static final String HOOK_REF = "$kolPreludeHook_ref";
        public static final String PIRATE_REF = "$kolPreludePirate_ref";
        public static final String LIBRA_REF = "$kolPreludeLibra_ref";

        // Permanent flags (never registered with a mission, so they survive mission end)
        public static final String HOOK_SEEN = "$kolPreludeHook_seen";
        public static final String ENARMS_MET = "$kolPrelude_metEnarms";
        public static final String PIRATE_COMPLETE = "$kolPrelude_pirateComplete";
        public static final String LIBRA_COMPLETE = "$kolPrelude_libraComplete";
        public static final String PRELUDE_DONE = "$kol_prelude_done";

        // Mission-owned flags: set while a mission runs and auto-unset by BaseHubMission when it ends.
        // Rules may gate "during" states on these, never "after" states.
        public static final String PIRATE_ACTIVE = "$kolPreludePirate_active";     // setGlobalReference in-progress flag
        public static final String LIBRA_ACTIVE = "$kolPreludeLibra_active";
        public static final String HOOK_TALKED = "$kolPreludeHook_talked";         // stage trigger: hook -> COMPLETED
        public static final String PIRATE_BEATEN = "$kolPreludePirate_beaten";     // stage trigger: -> REPORT
        public static final String PIRATE_DONE = "$kolPreludePirate_done";         // stage trigger: -> COMPLETED
        public static final String LIBRA_DELIVERED = "$kolPreludeLibra_delivered"; // stage trigger: -> REPORT
        public static final String LIBRA_DONE = "$kolPreludeLibra_done";           // stage trigger: -> COMPLETED

        // rules.csv trigger fired when the pirate fleet is beaten
        public static final String PIRATE_DEFEAT_TRIGGER = "KolPreludePirateDefeated";
    }

    /** Knights Chapter 1: Enarms' quarters, the black pulsar scouting run, the make-work bounty, the council, Lyra. */
    public static class KolCh1 {
        public static final String HELENSIS_ID = "kol_chaptermaster";
        public static final String GREENFLIGHT_ID = "kol_intel_director";

        // mission ids (person_missions.csv)
        public static final String SCOUT_ID = "kolCh1Scout";
        public static final String BOUNTY_ID = "kolCh1Bounty";
        public static final String LYRA_ID = "kolCh1Lyra";
        public static final String RETURN_ID = "kolCh1Return";

        // mission references (setGlobalReference)
        public static final String SCOUT_REF = "$kolCh1Scout_ref";
        public static final String BOUNTY_REF = "$kolCh1Bounty_ref";
        public static final String LYRA_REF = "$kolCh1Lyra_ref";
        public static final String RETURN_REF = "$kolCh1Return_ref";

        // Mission-owned flags: auto-unset by BaseHubMission when the mission ends ("during" states only)
        public static final String SCOUT_ACTIVE = "$kolCh1Scout_active";
        public static final String SCOUT_SCOUTED = "$kolCh1Scout_scouted";     // stage trigger: -> REPORT
        public static final String SCOUT_DONE = "$kolCh1Scout_done";           // stage trigger: -> COMPLETED
        public static final String BOUNTY_ACTIVE = "$kolCh1Bounty_active";
        public static final String LYRA_ACTIVE = "$kolCh1Lyra_active";
        public static final String GREENFLIGHT_MET = "$kolCh1Lyra_met";        // stage trigger: -> COMPLETED
        public static final String RETURN_ACTIVE = "$kolCh1Return_active";
        public static final String RETURN_DONE = "$kolCh1Return_done";         // stage trigger: -> COMPLETED (set when the council opens)

        // Permanent flags: chapter progress
        public static final String QUARTERS_DONE = "$kolCh1_quartersDone";     // stances given, oath asked
        public static final String OATH_DEFERRED = "$kolCh1_oathDeferred";
        public static final String SCOUT_COMPLETE = "$kolCh1_scoutComplete";
        public static final String BOUNTY_COMPLETE = "$kolCh1_bountyComplete";
        public static final String COUNCIL_DONE = "$kolCh1_councilDone";
        public static final String CH1_DONE = "$kol_ch1_done";

        // Permanent flags: what the player said and did (for later chapters to quote)
        public static final String DECLINE = "$kolCh1_decline";                // grieve / indifferent / glad / defer
        public static final String DECLINE_LIE = "$kolCh1_declineLie";
        public static final String TECH = "$kolCh1_tech";                      // oppose / embrace / defer
        public static final String TECH_LIE = "$kolCh1_techLie";
        public static final String OATH_SWORN = "$kolCh1_oathSworn";
        public static final String OATH_LIE = "$kolCh1_oathLie";
        public static final String FOUGHT_DUSK = "$kolCh1_foughtDusk";
        public static final String PULLED_THROUGH = "$kolCh1_pulledThrough";
        public static final String DOUBTED_LINK = "$kolCh1_doubtedLink";

        // Situations (placeholder event intels)
        public static final String TECH_SITUATION_KEY = "$kolTechSituation_ref";
        public static final String LIBRA_SITUATION_KEY = "$kolLibraSituation_ref";
    }

    /** The Technology situation: Helensis' scrip "bounty" on AI technology and her requisitions. */
    public static class KolTech {
        public static final String SETTINGS_KEY = "kol_tech";        // JSON object in settings.json
        public static final String DATA_KEY = "kol_techData";        // sector persistent data

        // Permanent flags other content reads (set on stage reached)
        public static final String STAGE = "$kolTech_stage";         // 0 START .. 4 CONSECRATED, 5 at the bar maximum
        public static final String[] STAGE_FLAGS = {
                "$kolTech_start", "$kolTech_requisitions", "$kolTech_trusted",
                "$kolTech_armory", "$kolTech_consecrated", "$kolTech_max"};
        public static final String EXCOMMUNICATED = "$kol_excommunicated";

        // Rule triggers fired by KolTechCMD
        public static final String TRIGGER_AFTER_HANDOVER = "KolTechAfterHandover";
        public static final String TRIGGER_SHROUDED = "KolTechShrouded";
        public static final String TRIGGER_UNIQUE_BOUGHT = "KolTechUniqueBought";  // + slot number
        public static final String TRIGGER_AFTER_PURCHASE = "KolTechAfterPurchase"; // $kolTech_purchaseType set
        public static final String TRIGGER_REQ_MENU = "KolTechReqMenu";
    }
}
