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

    /** Knights story foundations (Chapters 2-3): chapters, chronicle, assemblies, dock events, duties, desertion. */
    public static class KolStory {
        public static final String SETTINGS_KEY = "kol_story";       // JSON object in settings.json

        // Chapter counter (int); 2 once Chapter 1 is done, advanced only by chapter assemblies
        public static final String CHAPTER = "$kol_chapter";

        // Chronicle: $global.kolChron_<fact>, _day, _chapter; $global.kolChron_<thread>_grade
        public static final String CHRON_PREFIX = "$kolChron_";
        public static final String CHRON_POWERS_DEFEATED = "$kolChron_powersDefeated";
        public static final String CHRON_TT_SITES_CLEARED = "$kolChron_ttSitesCleared";
        public static final String CHRON_DATA_KEY = "kol_chronicleData";

        // Assemblies (at Star Keep Lyra)
        public static final String ASSEMBLY_DATA_KEY = "kol_assemblyData";
        public static final String ASSEMBLY_PREFIX = "$kolAssembly_";             // + report key (published at an assembly)
        public static final String ASSEMBLY_MISSED = "$kolAssembly_missed";       // int
        public static final String ASSEMBLY_HELD = "$kolAssembly_held";           // int
        public static final String TRIGGER_ASSEMBLY = "KolAssembly";
        public static final String TRIGGER_ASSEMBLY_ITEMS = "KolAssemblyItems";   // FireAll: one line rule per report key
        public static final String TRIGGER_ASSEMBLY_CHAPTER = "KolAssemblyChapter"; // + the chapter being entered
        public static final String TRIGGER_ASSEMBLY_NOTICE = "KolAssemblyNotice";   // the blurb at Lyra while an assembly sits

        // Attending an important assembly (convocation or chapter assembly)
        public static final String ATTEND_ID = "kolAssemblyAttend";
        public static final String ATTEND_REF = "$kolAssemblyAttend_ref";
        public static final String ATTEND_ACTIVE = "$kolAssemblyAttend_active";
        public static final String ATTEND_SITTING = "$kolAssemblyAttend_sitting"; // stage flags, cleared by the mission
        public static final String ATTEND_DONE = "$kolAssemblyAttend_done";
        public static final String ATTEND_MISSED = "$kolAssemblyAttend_missed";

        // Dock event queue (one MarketPostDock guard per Knights market)
        public static final String DOCK_DATA_KEY = "kol_dockEvents";
        public static final String TRIGGER_DOCK_NEXT = "KolDockNext";

        // Duties board (a hidden board person per Knights market carries vanilla's mission hub)
        public static final String DUTIES_PERSON_PREFIX = "kol_duties_";          // + market id
        public static final String DUTIES_TAG = "kol_duties";
        public static final String DUTIES_BOARD_FLAG = "$kolDutiesBoard";         // on the board person
        public static final String DUTIES_DONE = "$kolDuties_done";               // int

        // Desertion
        public static final String DESERTION_DATA_KEY = "kol_desertionData";
        public static final String DESERTER_FLEET_TAG = "kol_deserters";
        public static final String FLEET_MODIFIED_TAG = "kol_spawnModified";     // set by the fleet-spawn hook
    }

    /** Knights Chapter 2: convocation, patron, joint operations, Ozymandias, the agent, the player inquest. */
    public static class KolCh2 {
        // Missions (person_missions.csv ids) and their global references / in-progress flags
        public static final String PATRON_ID = "kolCh2Patron";
        public static final String PATRON_REF = "$kolCh2Patron_ref";
        public static final String PATRON_ACTIVE = "$kolCh2Patron_active";
        public static final String NINAYA_ID = "kolCh2Ninaya";
        public static final String NINAYA_REF = "$kolCh2Ninaya_ref";
        public static final String NINAYA_ACTIVE = "$kolCh2Ninaya_active";
        public static final String OZY_ID = "kolCh2Ozymandias";
        public static final String OZY_REF = "$kolCh2Oz_ref";
        public static final String OZY_ACTIVE = "$kolCh2Oz_active";
        public static final String AGENT_ID = "kolCh2Agent";
        public static final String AGENT_REF = "$kolCh2Agent_ref";
        public static final String INQUEST_ID = "kolCh2Inquest";
        public static final String INQUEST_REF = "$kolCh2Inquest_ref";
        public static final String INQUEST_ACTIVE = "$kolCh2Inquest_active";

        // Permanent flags
        public static final String CONVOCATION_DONE = "$kolCh2_convocationDone";
        public static final String PATRON = "$kol_patron";                    // faction id, or "player"
        public static final String PATRON_SECURED = "$kolCh2_patronSecured";  // boolean twin for rules
        public static final String NINAYA_OP_DONE = "$kolCh2_ninayaOpDone";
        public static final String OZY_DONE = "$kolCh2_ozymandiasDone";
        public static final String CAELI_CHOICE = "$kolCh2_caeliChoice";      // wake / leave / tomb
        public static final String OUTSIDE_POWER = "$kol_outsidePower";       // faction id
        public static final String AGENT_TRIGGERED = "$kolCh2_agentTriggered";
        public static final String AGENT_ANSWER = "$kol_agentAnswer";         // comply / refuse / deflect
        public static final String AGENT_ANSWERED = "$kolCh2_agentAnswered";
        public static final String INQUEST_PLAYER = "$kolInquest_player";     // cleared / watched / censured
        public static final String INQUEST_PLAYER_DONE = "$kolCh2_inquestDone";
        public static final String CONV_OATH_SWORN = "$kolCh2_convOathSworn";   // the player offered their own forces under oath
        public static final String CONV_OATH_LIE = "$kolCh2_convOathLie";       // ... and lied (counts at the inquest)

        // Entity flags (Ozymandias stops)
        public static final String OZY_STOP = "$kolCh2Oz_stop";
        public static final String OZY_CAELI = "$kolCh2Oz_caeli";

        // People
        public static final String GRANDMASTER_ID = "kol_grandmaster";
        public static final String INQUISITOR_PREFIX = "kol_inquisitor_";     // + slot
        public static final String AGENT_PERSON_ID = "kol_outside_agent";
        public static final String BENCH_PREFIX = "kol_bench_";               // + slot (the assembly's regular voices)

        // Rule triggers
        public static final String TRIGGER_INQUEST = "KolInquestPlayer";
        public static final String INQUEST_EVENT_KEY = "kol_inquestPlayer";
    }

    /** The patron quest (add-knights-patron-lobbying). Record keys are permanent; stage keys belong to the mission. */
    public static class KolPatron {
        // Leads the player has learned: + league / hegemony / diktat / charter / tritach / pirate / path
        public static final String LEAD_PREFIX = "$kolPatron_lead_";
        // Per-power parley keys: PREFIX + power key + STAGE_SUFFIX (lead ... signed) or PRICE_SUFFIX (the price on the table)
        public static final String PREFIX = "$kolPatron_";
        public static final String STAGE_SUFFIX = "_stage";
        public static final String PRICE_SUFFIX = "_price";
        public static final String DELEGATION = "$kolPatron_delegation";         // the Order's delegation is aboard (one party, collected once)

        // The record (permanent)
        public static final String LEVER = "$kol_patronLever";
        public static final String PRICE = "$kol_patronPrice";                   // the price key paid
        public static final String CHARTER_SOURCE = "$kol_patronCharterSource";  // independent / tritach
        public static final String CHARTER_SCUTTLED = "$kol_patronCharterScuttled";
        public static final String CHARTER_ACCEPTED = "$kolPatron_charterAccepted";
        public static final String TRIED_PREFIX = "$kolPatron_tried_";           // + pirate / path
        public static final String OWN_COMMITMENT = "$kolPatron_ownCommitment";      // the forces committed at the Lyra meeting
        public static final String VOLTURN_FILES_GIVEN = "$kolPatron_volturnFilesGiven";  // the Diktat's price: the faith's files on Volturn
        public static final String MET_FIXER = "$kolPatron_metFixer";             // the player met the Tri-Tachyon fixer (vanilla records nothing)
        public static final String FIXER_VISIT_SEEN = "$kolPatron_fixerVisitSeen";

        // People
        public static final String STANDIN_PREFIX = "kol_patron_standin_";      // + the vanilla id a remembered stand-in replaces
        public static final String PERSON_PREFIX = "kol_patron_";                // + role: the parleys' remembered people
    }
}
