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
}
