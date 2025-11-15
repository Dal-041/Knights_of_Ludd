package org.selkie.zea.fleets;

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
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.helpers.KolStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.zea.helpers.ZeaUtils;
import org.selkie.zea.intel.ZeaMechanicIntel;
import org.selkie.zea.terrain.AbyssSea.AbyssSeaWave;

import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.pickSkill;

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

    protected final int minPts;
    protected final int maxPts;
    protected final int maxFleets;
    protected final StarSystemAPI system;
    public final String fac;
    protected final IntervalUtil interval = new IntervalUtil(3, 5);

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
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(new Random(seed));

        picker.add(Factions.DERELICT,1f);
        picker.add(Factions.REMNANTS, 1f);
        float w = primary.equals(ZeaStaticStrings.dawnID) ? 0f : 1f;
        picker.add(ZeaStaticStrings.dawnID, w);
        w = primary.equals(ZeaStaticStrings.duskID) ? 0f : 1f;
        picker.add(ZeaStaticStrings.duskID, w);
        w = primary.equals(ZeaStaticStrings.elysianID) ? 0f : 1f;
        picker.add(ZeaStaticStrings.elysianID, w);
        if (ZeaUtils.useDomres) picker.add(KolStaticStrings.DOMRES, 1f);
        if (ZeaUtils.useDustkeepers) picker.add(KolStaticStrings.SOTF_DUSTKEEPERS, 0.8f);
        w = primary.equals(ZeaStaticStrings.dawnID) ? 0.75f : 0f;
        if (ZeaUtils.useEnigma) picker.add(KolStaticStrings.ENIGMA, w);
        if (ZeaUtils.useLostech) picker.add(KolStaticStrings.TAHLAN_ALLMOTHER, 0.6f);
        return picker.pick();
    }

    public static void copyFleetMembers(String fromID, CampaignFleetAPI from, CampaignFleetAPI to, boolean pruneCaps) {
        for (FleetMemberAPI member : from.getMembersWithFightersCopy()) {
            boolean skip = false;
            if (member.isFighterWing()) continue;
            if (pruneCaps && member.getHullSpec().getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP)) continue;
            for (String s : ZeaStaticStrings.hullBlacklist) {
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
        fleet.addTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG);
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
        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<>(random);

        for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.SALVAGEABLE)) {
            float w = 5f; //1f
            if (entity.hasTag(Tags.NEUTRINO_HIGH)) w = 15f;
            if (entity.hasTag(Tags.NEUTRINO_LOW)) w = 1.5f;
            if (entity.getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_ELYSIAN_WITNESS)) w = 0f;
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
        switch (fac) {
            case ZeaStaticStrings.dawnID:
                return Global.getSector().getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_DAWN_BOSS_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ZeaMemKeys.ZEA_DAWN_BOSS_DONE);
            case ZeaStaticStrings.duskID:
                return Global.getSector().getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_DUSK_BOSS_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ZeaMemKeys.ZEA_DUSK_BOSS_DONE);
            case ZeaStaticStrings.elysianID:
                return Global.getSector().getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_ELYSIAN_BOSS_1_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ZeaMemKeys.ZEA_ELYSIAN_BOSS_1_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_ELYSIAN_BOSS_2_DONE)
                        && Global.getSector().getMemoryWithoutUpdate().getBoolean(ZeaMemKeys.ZEA_ELYSIAN_BOSS_2_DONE);
            default: return false;
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        //Handle Coronal Capacitor intel
        if (fleet.getFaction().getId().equals(ZeaStaticStrings.elysianID) || fleet.getFaction().getId().equals(ZeaStaticStrings.dawnID) || fleet.getFaction().getId().equals(ZeaStaticStrings.duskID)) {
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
        fleet.getMemoryWithoutUpdate().set(AbyssSeaWave.IGNORES_MEMORY_FLAG, true);

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
            case ZeaStaticStrings.dawnID:
                return OfficerManagerEvent.SkillPickPreference.NO_ENERGY_YES_BALLISTIC_NO_MISSILE_YES_DEFENSE;

            case ZeaStaticStrings.duskID:
                return OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_YES_MISSILE_YES_DEFENSE;

            case ZeaStaticStrings.elysianID:
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
        String core = Commodities.ALPHA_CORE;
        if (randCore) {
            float rand = MathUtils.getRandom().nextFloat();
            if (rand < 0.35f) {
                level = 7;
                core = Commodities.BETA_CORE;
            } else if (rand < 0.70f) {
                level = 6;
                core = Commodities.GAMMA_CORE;
            }
        }
        //TODO handle modified AI core levels, portrait pools
        //Vanilla levels (integrated): G4, B6, A8, O9
        skillPref = getFactionSkillPref(faction);

        switch (faction) {
            case ZeaStaticStrings.dawnID:
                portrait = ZeaStaticStrings.portraitsDawn[level - 6];
                persona = Personalities.AGGRESSIVE;
                break;
            case ZeaStaticStrings.duskID:
                persona = Personalities.STEADY;
                portrait = ZeaStaticStrings.portraitsDusk[level - 6];
                break;
            case ZeaStaticStrings.elysianID:
                persona = Personalities.STEADY;
                portrait = ZeaStaticStrings.portraitsElysian[level - 6];
                break;
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
                if (captain.getId().equals(KolStaticStrings.TAHLAN_CHILD)) return; //Special officer
                boolean found = false;
                String portCapt = captain.getPortraitSprite();
                for (String port : ZeaStaticStrings.portraitsDawn) {
                    if (portCapt.equalsIgnoreCase(port)) {
                        found = true;
                        break;
                    }
                }
                for (String port : ZeaStaticStrings.portraitsDusk) {
                    if (portCapt.equalsIgnoreCase(port)) {
                        found = true;
                        break;
                    }
                }
                for (String port : ZeaStaticStrings.portraitsElysian) {
                    if (portCapt.equalsIgnoreCase(port)) {
                        found = true;
                        break;
                    }
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

                    if (fac.equals(ZeaStaticStrings.duskID)) {
                        captain.setPortraitSprite(ZeaStaticStrings.portraitsDuskPaths[level-6]);
                    }
                    if (fac.equals(ZeaStaticStrings.elysianID)) {
                        captain.setPortraitSprite(ZeaStaticStrings.portraitsElysianPaths[level-6]);
                    }
                    if (fac.equals(ZeaStaticStrings.dawnID)) {
                        captain.setPortraitSprite(ZeaStaticStrings.portraitsDawnPaths[level-6]);
                        captain.setPersonality(Personalities.RECKLESS);
                    }

                }
                //Re-inflate skills
                int toAdd = captain.getStats().getLevel() - captain.getStats().getSkillsCopy().size();
                if (toAdd > 0) {
                    OfficerLevelupPlugin plugin = (OfficerLevelupPlugin) Global.getSettings().getPlugin(ZeaStaticStrings.OFFICER_LEVEL_UP);
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
        if (!faction.equals(ZeaStaticStrings.dawnID) && !faction.equals(ZeaStaticStrings.duskID) && !faction.equals(ZeaStaticStrings.elysianID)) return;
        float chance = 0.1f;
        if (Math.random() <= chance) {
            fleet.addDropRandom(faction + "_lore", 1);
        }
    }
}
