package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
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
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.AbyssUtils;
import org.selkie.kol.impl.world.*;

import java.util.Random;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class AbyssalFleetManager extends SeededFleetManager {

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

    public AbyssalFleetManager(StarSystemAPI system, String factionID, int maxFleets, int minPts, int maxPts) {
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

    public String getSecondFaction(long seed, String primary) {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(new Random(seed));

        picker.add("derelict",1f);
        picker.add("remnant", 1f);
        float w = primary.equals(PrepareAbyss.dawnID) ? 0f : 1f;
        picker.add(PrepareAbyss.dawnID, w);
        w = primary.equals(PrepareAbyss.duskID) ? 0f : 1f;
        picker.add(PrepareAbyss.duskID, w);
        w = primary.equals(PrepareAbyss.elysianID) ? 0f : 1f;
        picker.add(PrepareAbyss.elysianID, w);
        if (AbyssUtils.useDomres) picker.add("domres", 1f);
        if (AbyssUtils.useDustkeepers) picker.add("sotf_dustkeepers", 0.8f);
        w = primary.equals(PrepareAbyss.dawnID) ? 0.75f : 0f;
        if (AbyssUtils.useEnigma) picker.add("enigma", w);
        if (AbyssUtils.useLostech) picker.add("tahlan_allmother", 0.6f);
        return picker.pick();
    }

    public static void copyFleetMembers(String fromID, CampaignFleetAPI from, CampaignFleetAPI to) {
        for (FleetMemberAPI member : from.getMembersWithFightersCopy()) {
            boolean skip = false;
            if (member.isFighterWing() || member.getHullSpec().getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP)) continue;
            for (String s : AbyssUtils.hullBlacklist) {
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

    @Override
    protected CampaignFleetAPI spawnFleet(long seed) {

        Random random = new Random(seed);
        float factorMain = 0.65f;
        boolean mixAll = false;

        FleetParamsV3 params = new FleetParamsV3(
                system.getLocation(),
                fac,
                5f,
                FleetTypes.PATROL_LARGE,
                MathUtils.getRandomNumberInRange(minPts, maxPts), // combatPts
                0f, // freighterPts
                0f, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                5f // qualityMod, won't get used since routes mostly have quality override set
        );
        params.averageSMods = 1;
        params.ignoreMarketFleetSizeMult = true;
        params.commander = createAbyssalCaptain();
        params.officerNumberMult = 1f;
        //params.officerLevelBonus = 0;
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
        params.random = random;

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        String facSecond = getSecondFaction(seed, fac);
        params.factionId = facSecond;
        params.combatPts = params.combatPts/factorMain * (1-factorMain);

        CampaignFleetAPI fleet2 = FleetFactoryV3.createFleet(params);

        copyFleetMembers(facSecond, fleet2, fleet);

        fleet2.despawn();

        //fleet.getFleetData().sort();

        if (fleet == null) return null;

        setAbyssalCaptains(fleet);
        fleet.addTag(excludeTag);
        system.addEntity(fleet);
        fleet.setFacing(random.nextFloat() * 360f);

        int numActive = 0;
        for (SeededFleet f : fleets) {
            if (f.fleet != null) numActive++;
        }

        initElysianFleetProperties(random, fleet, false);
        addLoreToFleetCheck(fleet);
        fleet.addScript(new AbyssAssignmentAI(fleet, system, pickEntityToGuard(new Random(), system, fleet)));

        return fleet;
    }

    public static SectorEntityToken pickEntityToGuard(Random random, StarSystemAPI system, CampaignFleetAPI fleet) {
        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<SectorEntityToken>(random);

        for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.SALVAGEABLE)) {
            float w = 5f; //1f
            if (entity.hasTag(Tags.NEUTRINO_HIGH)) w = 15f;
            if (entity.hasTag(Tags.NEUTRINO_LOW)) w = 1.5f;
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
                new AbyssalFleetManager.AbyssalFleetInteractionConfigGen());
    }

    public PersonAPI createAbyssalCaptain() {
        return createAbyssalCaptain(fac, false);
    }

    public static PersonAPI createAbyssalCaptain(String faction) {
        return createAbyssalCaptain(faction, false);
    }

    public static PersonAPI createAbyssalCaptain(String faction, boolean random) {
        OfficerManagerEvent.SkillPickPreference skillPref;
        String persona = Personalities.AGGRESSIVE;
        String portrait = Global.getSector().getFaction(faction).getPortraits(FullName.Gender.ANY).pick();

        int level = 8;
        String core = "alpha_core";
        if (random) {
            float rand = (float) Math.random();
            if (rand < 0.35f);
            else if (rand < 0.75f) {
                level = 7;
                core = "beta_core";
            }
            else level = 6; core = "gamma_core";
        }
        //TODO handle modified AI core levels, portrait pools
        //Levels: G3, B5, A7, O9
        if (faction.equals(PrepareAbyss.dawnID)) {
            skillPref = OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE;
            portrait = AbyssUtils.portraitsDawn[level-6];
            persona = Personalities.AGGRESSIVE;
        }
        else if (faction.equals(PrepareAbyss.duskID)) {
            skillPref = OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE;
            persona = Personalities.STEADY;
            portrait = AbyssUtils.portraitsDusk[level-6];
        }
        else if (faction.equals(PrepareAbyss.elysianID)) {
            skillPref = OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE;
            persona = Personalities.STEADY;
            portrait = AbyssUtils.portraitsElysian[level-6];
        }
        else skillPref = OfficerManagerEvent.SkillPickPreference.ANY;

        return MagicCampaign.createCaptainBuilder(faction)
                .setIsAI(true)
                .setAICoreType(core)
                .setLevel(level)
                .setPersonality(persona)
                .setSkillPreference(skillPref)
                .setPortraitId(portrait)
                .create();
    }

    public static void setAbyssalCaptains(CampaignFleetAPI fleet) {
        String fac = fleet.getFaction().getId();
        for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
            if (member.getCaptain() == null) {
                member.setCaptain(createAbyssalCaptain(fac, true));
            } else {
                if (member.getCaptain().getId().equals("tahlan_child")) return; //Special officer
                boolean found = false;
                String portCapt = member.getCaptain().getPortraitSprite();
                for (String port : AbyssUtils.portraitsDawn) {
                    if (portCapt.equalsIgnoreCase(port)) {
                        found = true;
                    }
                }
                for (String port : AbyssUtils.portraitsDusk) {
                    if (portCapt.equalsIgnoreCase(port)) found = true;
                }
                for (String port : AbyssUtils.portraitsElysian) {
                    if (portCapt.equalsIgnoreCase(port)) found = true;
                }
                if (!found) {

                    int level = Math.min(8, member.getCaptain().getStats().getLevel());
                    if (level < 6) {
                        level = 6;
                        member.getCaptain().getStats().setLevel(6);
                    }
                    //AI cores default to "fearless" which is one-note, we bring them down a notch
                    double rand = Math.random();
                    if (rand < 0.5f) member.getCaptain().setPersonality(Personalities.AGGRESSIVE);
                    else if (rand > 0.8f) member.getCaptain().setPersonality(Personalities.RECKLESS);
                    else member.getCaptain().setPersonality(Personalities.STEADY);

                    if (fac.equals(PrepareAbyss.duskID)) {
                        member.getCaptain().setPortraitSprite(AbyssUtils.portraitsDuskPaths[level-6]);
                    }
                    if (fac.equals(PrepareAbyss.elysianID)) {
                        member.getCaptain().setPortraitSprite(AbyssUtils.portraitsElysianPaths[level-6]);
                    }
                    if (fac.equals(PrepareAbyss.dawnID)) {
                        member.getCaptain().setPortraitSprite(AbyssUtils.portraitsDawnPaths[level-6]);
                        member.getCaptain().setPersonality(Personalities.RECKLESS);
                    }
                }
            }
        }
    }

    public void addLoreToFleetCheck(CampaignFleetAPI fleet) {
        String faction = fleet.getFaction().getId();
        if (!faction.equals(PrepareAbyss.dawnID) && faction.equals(PrepareAbyss.duskID) && faction.equals(PrepareAbyss.elysianID)) return;
        float chance = 0.1f;
        if (Math.random() <= chance) {
            fleet.addDropRandom(faction + "_lore", 1);
        }
    }
}
