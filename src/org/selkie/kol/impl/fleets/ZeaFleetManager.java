package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfigGen;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.SeededFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.intel.ZeaMechanicIntel;
import org.selkie.kol.impl.world.*;

import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.pickSkill;
import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;
import static org.selkie.kol.impl.world.PrepareAbyss.log;

public class ZeaFleetManager extends SeededFleetManager {

    public static class AbyssalFleetInteractionConfigGen implements FIDConfigGen {
        public FIDConfig createConfig() {
            FIDConfig config = new FIDConfig();
            config.showTransponderStatus = false;
            config.delegate = new BaseFIDDelegate() {
                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = false; //false
                    //bcc.objectivesAllowed = false;
                }
            };
            return config;
        }
    }

    protected int minPts;
    protected int maxPts;
    protected int maxFleets;
    protected StarSystemAPI system;
    String fac;
    protected IntervalUtil interval = new IntervalUtil(3, 5);

    public ZeaFleetManager(StarSystemAPI system, String factionID, int maxFleets, int minPts, int maxPts) {
        super(system, 1f);

        this.minPts = minPts;
        this.maxPts = maxPts;
        this.maxFleets = maxFleets;
        this.system = system;
        this.fac = factionID;

        for (int i = 0; i < maxFleets; i++) {
            long seed = StarSystemGenerator.random.nextLong();
            addSeed(seed);
        }
    }

    public static String getSecondFaction(long seed, String primary) {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(new Random(seed));

        picker.add("derelict",1f);
        picker.add("remnant", 1f);
        float w = primary.equals(PrepareAbyss.dawnID) ? 0f : 1f;
        picker.add(PrepareAbyss.dawnID, w);
        w = primary.equals(PrepareAbyss.duskID) ? 0f : 1f;
        picker.add(PrepareAbyss.duskID, w);
        w = primary.equals(PrepareAbyss.elysianID) ? 0f : 1f;
        picker.add(PrepareAbyss.elysianID, w);
        if (ZeaUtils.useDomres) picker.add("domres", 1f);
        if (ZeaUtils.useDustkeepers) picker.add("sotf_dustkeepers", 0.8f);
        w = primary.equals(PrepareAbyss.dawnID) ? 0.75f : 0f;
        if (ZeaUtils.useEnigma) picker.add("enigma", w);
        if (ZeaUtils.useLostech) picker.add("tahlan_allmother", 0.6f);
        return picker.pick();
    }

    public static void copyFleetMembers(String fromID, CampaignFleetAPI from, CampaignFleetAPI to, boolean pruneCaps) {
        for (FleetMemberAPI member : from.getMembersWithFightersCopy()) {
            boolean skip = false;
            if (member.isFighterWing()) continue;
            if (pruneCaps && member.getHullSpec().getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP)) continue;
            for (String s : ZeaUtils.hullBlacklist) {
                if (s.equals(member.getHullId())) {
                    skip = true;
                }
            }
            if (skip) continue;
            //Unrecoverable status set by hullmod
            if (member.getVariant().hasTag(Tags.AUTOMATED_RECOVERABLE)) member.getVariant().removeTag(Tags.AUTOMATED_RECOVERABLE);
            if (member.isFlagship()) member.setFlagship(false);
            member.setShipName(Global.getSector().getFaction(fromID).pickRandomShipName());
            to.getFleetData().addFleetMember(member);
        }
    }

    public static CampaignFleetAPI spawnFleet(long seed, float factorSecond, StarSystemAPI sys, String fac, int min, int max) {
        Random random = new Random(seed);
        float factorMain = 1f-factorSecond;
        boolean mixAll = false;

        FleetParamsV3 params = new FleetParamsV3(
                sys.getLocation(),
                fac,
                5f,
                FleetTypes.PATROL_LARGE,
                MathUtils.getRandomNumberInRange(min, max), // combatPts
                0f, // freighterPts
                0f, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                5f // qualityMod
        );
        params.averageSMods = 1;
        params.ignoreMarketFleetSizeMult = true;
        params.commander = createAICaptain(fac);
        params.officerNumberMult = 6f;
        //params.officerLevelBonus = 0;
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
        params.random = random;

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        String facSecond = getSecondFaction(seed, fac);
        params.factionId = facSecond;
        params.combatPts = params.combatPts/factorMain * (1-factorMain);

        CampaignFleetAPI fleet2 = FleetFactoryV3.createFleet(params);

        copyFleetMembers(facSecond, fleet2, fleet, true);

        fleet2.despawn();

        //fleet.getFleetData().sort();

        return fleet;
    }

    @Override
    protected CampaignFleetAPI spawnFleet(long seed) {
        float ratioOther = 0.35f;
        Random random = new Random(seed);
        CampaignFleetAPI fleet = spawnFleet(seed, ratioOther, system, fac, minPts, maxPts);

        if (fleet == null) return null;

        setAICaptains(fleet);
        fleet.addTag(excludeTag);
        system.addEntity(fleet);
        fleet.setFacing(random.nextFloat() * 360f);

        int numActive = 0;
        for (SeededFleet f : fleets) {
            if (f.fleet != null) numActive++;
        }

        initElysianFleetProperties(random, fleet, false);
        fleet.addScript(new ZeaAssignmentAI(fleet, system, pickEntityToGuard(new Random(), system, fleet)));

        addLoreToFleetCheck(fleet);
        return fleet;
    }

    public static SectorEntityToken pickEntityToGuard(Random random, StarSystemAPI system, CampaignFleetAPI fleet) {
        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<SectorEntityToken>(random);

        for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.SALVAGEABLE)) {
            float w = 5f; //1f
            if (entity.hasTag(Tags.NEUTRINO_HIGH)) w = 15f;
            if (entity.hasTag(Tags.NEUTRINO_LOW)) w = 1.5f;
            if (entity.getMemoryWithoutUpdate().contains(ZeaUtils.KEY_ELYSIA_WITNESS)) w = 0f;
            picker.add(entity, w);
        }

        for (SectorEntityToken entity : system.getJumpPoints()) {
            picker.add(entity, 0.5f);
        }

        for (SectorEntityToken entity : system.getPlanets()) {
            picker.add(entity, 0.1f);
        }

        return picker.pick();
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        float days = Global.getSector().getClock().convertToDays(amount);
        interval.advance(days);

        if (interval.intervalElapsed() && fleets.size() < maxFleets) {
            for (int i = fleets.size(); i < maxFleets; i++) {
                long seed = StarSystemGenerator.random.nextLong();
                addSeed(seed);
            }
        }
    }

    @Override
    public boolean isDone() {
        String MEMKEY_KOL_DAWN_BOSS_DONE = "$zea_dawn_boss_done";
        String MEMKEY_KOL_DUSK_BOSS_DONE = "$zea_dusk_boss_done";
        String MEMKEY_KOL_ELYSIAN_BOSS2_DONE = "$zea_elysian_boss2_done";
        String MEMKEY_KOL_ELYSIAN_BOSS1_DONE = "$zea_elysian_boss1_done";

        switch (fac) {
            case PrepareAbyss.dawnID:
                if (Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_DAWN_BOSS_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_DAWN_BOSS_DONE)) return true;
            case PrepareAbyss.duskID:
                if (Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_DUSK_BOSS_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_DUSK_BOSS_DONE)) return true;
            case PrepareAbyss.elysianID:
                if (Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_ELYSIAN_BOSS1_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_ELYSIAN_BOSS1_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_ELYSIAN_BOSS2_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_ELYSIAN_BOSS2_DONE)) return true;
            default: return false;
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        //Handle Coronal Capacitor intel
        if (fleet.getFaction().getId().equals(PrepareAbyss.elysianID) || fleet.getFaction().getId().equals(PrepareAbyss.dawnID) || fleet.getFaction().getId().equals(PrepareAbyss.duskID)) {
            ZeaMechanicIntel mechanic = ZeaMechanicIntel.getNextMechanicIntel(fleet.getFaction().getId());
            if (mechanic != null) Global.getSector().getIntelManager().addIntel(mechanic);
        }

        if (fleet.getMembersWithFightersCopy().isEmpty()) {
            for (SeededFleet curr : fleets) {
                if (curr.fleet != null && curr.fleet == fleet) {
                    fleets.remove(curr);
                    if (DEBUG) System.out.println("Removed " + curr.fleet.getName() + " (seed: " + curr.seed + "), remaining: " + fleets.size());
                    break;
                }
            }
        }
    }

    public static void initElysianFleetProperties(Random random, CampaignFleetAPI fleet, boolean dormant) {
        if (random == null) random = new Random();

        fleet.removeAbility(Abilities.EMERGENCY_BURN);
        //fleet.removeAbility(Abilities.SENSOR_BURST);
        fleet.removeAbility(Abilities.GO_DARK);

        // to make sure they attack the player on sight when player's transponder is off
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

        if (dormant) {
            fleet.setTransponderOn(false);
//			fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_PATROL_FLEET);
//			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true); // so they don't turn transponder on
//			fleet.addAssignment(FleetAssignment.HOLD, null, 1000000f, "dormant");
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            fleet.setAI(null);
            fleet.setNullAIActionText("dormant");
        }

        addElysianInteractionConfig(fleet);

        long salvageSeed = random.nextLong();
        fleet.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SEED, salvageSeed);
    }

    public static void addElysianInteractionConfig(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new ZeaFleetManager.AbyssalFleetInteractionConfigGen());
    }

    public static OfficerManagerEvent.SkillPickPreference getFactionSkillPref (String faction) {

        switch (faction) {
            case PrepareAbyss.dawnID:
                return OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE;

            case PrepareAbyss.duskID:
                return OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE;

            case PrepareAbyss.elysianID:
                return OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE;
            default:
                return OfficerManagerEvent.SkillPickPreference.ANY;
        }
    }

    public static PersonAPI createAICaptain(String faction) {
        return createAICaptain(faction, false);
    }

    public static PersonAPI createAICaptain(String faction, boolean randCore) {
        OfficerManagerEvent.SkillPickPreference skillPref;
        String persona = Personalities.AGGRESSIVE;
        String portrait = Global.getSector().getFaction(faction).getPortraits(FullName.Gender.ANY).pick();

        int level = 8;
        String core = "alpha_core";
        if (randCore) {
            float rand = MathUtils.getRandom().nextFloat();
            if (rand < 0.35f) {
                level = 7;
                core = "beta_core";
            } else if (rand < 0.70f) {
                level = 6;
                core = "gamma_core";
            }
        }
        //TODO handle modified AI core levels, portrait pools
        //Vanilla levels (integrated): G4, B6, A8, O9
        skillPref = getFactionSkillPref(faction);

        if (faction.equals(PrepareAbyss.dawnID)) {
            portrait = ZeaUtils.portraitsDawn[level-6];
            persona = Personalities.AGGRESSIVE;
        }
        else if (faction.equals(PrepareAbyss.duskID)) {
            persona = Personalities.STEADY;
            portrait = ZeaUtils.portraitsDusk[level-6];
        }
        else if (faction.equals(PrepareAbyss.elysianID)) {
            persona = Personalities.STEADY;
            portrait = ZeaUtils.portraitsElysian[level-6];
        }

        return MagicCampaign.createCaptainBuilder(faction)
                .setIsAI(true)
                .setAICoreType(core)
                .setLevel(level)
                .setPersonality(persona)
                .setSkillPreference(skillPref)
                .setPortraitId(portrait)
                .create();
    }

    public static void setAICaptains(CampaignFleetAPI fleet) {
        String fac = fleet.getFaction().getId();
        for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            PersonAPI captain = member.getCaptain();
            if (captain == null) {
                member.setCaptain(createAICaptain(fac, true));
            } else {
                if (captain.getId().equals("tahlan_child")) return; //Special officer
                boolean found = false;
                String portCapt = captain.getPortraitSprite();
                for (String port : ZeaUtils.portraitsDawn) {
                    if (portCapt.equalsIgnoreCase(port)) {
                        found = true;
                    }
                }
                for (String port : ZeaUtils.portraitsDusk) {
                    if (portCapt.equalsIgnoreCase(port)) found = true;
                }
                for (String port : ZeaUtils.portraitsElysian) {
                    if (portCapt.equalsIgnoreCase(port)) found = true;
                }
                if (!found) {

                    int level = Math.min(8, captain.getStats().getLevel());
                    if (level < 6) {
                        level = 6;
                        captain.getStats().setLevel(6);
                        captain.getStats().refreshCharacterStatsEffects();
                    }
                    //AI cores default to "fearless" which is one-note, we bring them down a notch
                    double rand = Math.random();
                    if (rand < 0.5f) captain.setPersonality(Personalities.AGGRESSIVE);
                    else if (rand > 0.8f) captain.setPersonality(Personalities.RECKLESS);
                    else captain.setPersonality(Personalities.STEADY);

                    if (fac.equals(PrepareAbyss.duskID)) {
                        captain.setPortraitSprite(ZeaUtils.portraitsDuskPaths[level-6]);
                    }
                    if (fac.equals(PrepareAbyss.elysianID)) {
                        captain.setPortraitSprite(ZeaUtils.portraitsElysianPaths[level-6]);
                    }
                    if (fac.equals(PrepareAbyss.dawnID)) {
                        captain.setPortraitSprite(ZeaUtils.portraitsDawnPaths[level-6]);
                        captain.setPersonality(Personalities.RECKLESS);
                    }

                }
                //Re-inflate skills
                int toAdd = captain.getStats().getLevel() - captain.getStats().getSkillsCopy().size();
                if (toAdd > 0) {
                    OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
                    Random random = MathUtils.getRandom();
                    for (int i = 0; i < toAdd; i++) {
                        List<String> skills = plugin.pickLevelupSkills(captain, random);
                        String skillId = pickSkill(captain, skills, getFactionSkillPref(fac), 0, random);
                        if (skillId != null) {
                            captain.getStats().setSkillLevel(skillId, 2);
                        }
                    }
                }
            }
        }
    }

    public void addLoreToFleetCheck(CampaignFleetAPI fleet) {
        String faction = fleet.getFaction().getId();
        if (!faction.equals(PrepareAbyss.dawnID) && !faction.equals(PrepareAbyss.duskID) && !faction.equals(PrepareAbyss.elysianID)) return;
        float chance = 0.1f;
        if (Math.random() <= chance) {
            fleet.addDropRandom(faction + "_lore", 1);
        }
    }
}
