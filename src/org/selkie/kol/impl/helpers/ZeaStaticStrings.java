package org.selkie.kol.impl.helpers;

import org.selkie.kol.impl.skills.cores.BaseCoreOfficerSkill;
import org.selkie.kol.impl.skills.cores.DawnBossCoreSkill;
import org.selkie.kol.impl.skills.cores.DuskBossCoreSkill;
import org.selkie.kol.impl.skills.cores.ElysiaBossCoreSkill;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.util.Objects;

public class ZeaStaticStrings {
    public static final String BOSS_TAG = "$zea_boss";

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

    public static final String[] factionIDs = {
        PrepareAbyss.dawnID,
        PrepareAbyss.duskID,
        PrepareAbyss.elysianID
    };
    public static final String[] systemNames = {
        PrepareAbyss.elysiaSysName,
        PrepareAbyss.nullspaceSysName,
        PrepareAbyss.lunaSeaSysName,
        PrepareAbyss.nbsSysPrefix,
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
    public static final String pathCrests = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/factions/";
    public static final String crestDawn = "zea_crest_dawntide";
    public static final String crestDusk = "zea_crest_dusk";
    public static final String crestEDF = "zea_crest_edf";
    public static final String crestTT = "graphics/factions/crest_tritachyon";
}