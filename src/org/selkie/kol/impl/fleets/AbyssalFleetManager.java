package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
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
        picker.add("kol_dawn", w);
        w = primary.equals(PrepareAbyss.duskID) ? 0f : 1f;
        picker.add("kol_dusk", w);
        w = primary.equals(PrepareAbyss.elysianID) ? 0f : 1f;
        picker.add("kol_elysians", w);
        if (PrepareAbyss.useDomres) picker.add("domres", 1f);
        if (PrepareAbyss.useDustkeepers) picker.add("sotf_dustkeepers", 0.8f);
        if (PrepareAbyss.useEnigma) picker.add("enigma", 0.75f);
        if (PrepareAbyss.useLostech) {
            picker.add("tahlan_allmother", 0.6f);
        }
        return picker.pick();
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
        params.officerNumberMult = 5f;
        //params.officerLevelBonus = 0;
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
        params.random = random;

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        String facSecond = getSecondFaction(seed, fac);
        params.factionId = facSecond;
        params.combatPts = params.combatPts/factorMain * (1-factorMain);

        CampaignFleetAPI fleet2 = FleetFactoryV3.createFleet(params);

        PrepareAbyss.copyFleetMembers(facSecond, fleet2, fleet);

        fleet2.despawn();

        //fleet.getFleetData().sort();

        if (fleet == null) return null;

        fleet.addTag(excludeTag);
        system.addEntity(fleet);
        fleet.setFacing(random.nextFloat() * 360f);

        int numActive = 0;
        for (SeededFleet f : fleets) {
            if (f.fleet != null) numActive++;
        }

        initElysianFleetProperties(random, fleet, false);

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
            picker.add(entity, 2f);
        }

        for (SectorEntityToken entity : system.getPlanets()) {
            picker.add(entity, 0.2f);
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
        switch (fac) {
            case PrepareAbyss.dawnID:
                if (Global.getSector().getMemoryWithoutUpdate().contains(ManageDawnBoss.MEMKEY_KOL_DAWN_BOSS_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ManageDawnBoss.MEMKEY_KOL_DAWN_BOSS_DONE)) return true;
            case PrepareAbyss.duskID:
                if (Global.getSector().getMemoryWithoutUpdate().contains(ManageDuskBoss.MEMKEY_KOL_DUSK_BOSS_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ManageDuskBoss.MEMKEY_KOL_DUSK_BOSS_DONE)) return true;
            case PrepareAbyss.elysianID:
                if (Global.getSector().getMemoryWithoutUpdate().contains(ManageElysianAmaterasu.MEMKEY_KOL_ELYSIAN_BOSS1_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ManageElysianAmaterasu.MEMKEY_KOL_ELYSIAN_BOSS1_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().contains(ManageElysianCorruptingheart.MEMKEY_KOL_ELYSIAN_BOSS2_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ManageElysianCorruptingheart.MEMKEY_KOL_ELYSIAN_BOSS2_DONE)) return true;
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
        OfficerManagerEvent.SkillPickPreference skillPref;
        if (fac.equals(PrepareAbyss.dawnID)) skillPref = OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE;
        else if (fac.equals(PrepareAbyss.duskID)) skillPref = OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE;
        else if (fac.equals(PrepareAbyss.elysianID)) skillPref = OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE;
        else skillPref = OfficerManagerEvent.SkillPickPreference.ANY;

        return MagicCampaign.createCaptainBuilder(fac)
                .setIsAI(true)
                .setAICoreType("alpha_core")
                .setLevel(7)
                .setPersonality(Personalities.AGGRESSIVE)
                .setSkillPreference(skillPref)
                .create();
    }
    public static PersonAPI createAbyssalCaptain(String faction) {
        OfficerManagerEvent.SkillPickPreference skillPref;
        if (faction.equals(PrepareAbyss.dawnID)) skillPref = OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE;
        else if (faction.equals(PrepareAbyss.duskID)) skillPref = OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE;
        else if (faction.equals(PrepareAbyss.elysianID)) skillPref = OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_YES_MISSILE_YES_DEFENSE;
        else skillPref = OfficerManagerEvent.SkillPickPreference.ANY;

        return MagicCampaign.createCaptainBuilder(faction)
                .setIsAI(true)
                .setAICoreType("alpha_core")
                .setLevel(7)
                .setPersonality(Personalities.AGGRESSIVE)
                .setSkillPreference(skillPref)
                .create();
    }
}
