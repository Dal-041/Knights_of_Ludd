package org.selkie.kol.impl.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfigGen;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.SeededFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantAssignmentAI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.MagicCampaign;

import java.util.Random;

public class SeededFleetManagerElysian extends SeededFleetManager {

    public static class ElysianFleetInteractionConfigGen implements FIDConfigGen {
        public FIDConfig createConfig() {
            FIDConfig config = new FIDConfig();
            config.showTransponderStatus = false;
            config.delegate = new BaseFIDDelegate() {
                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = true; //false
                    //bcc.objectivesAllowed = false;
                }
            };
            return config;
        }
    }

    protected int minPts;
    protected int maxPts;
    protected float activeChance;

    public SeededFleetManagerElysian(StarSystemAPI system, int minFleets, int maxFleets, int minPts, int maxPts, float activeChance) {
        super(system, 1f);
        this.minPts = minPts;
        this.maxPts = maxPts;
        this.activeChance = activeChance;

        int num = minFleets + StarSystemGenerator.random.nextInt(maxFleets - minFleets + 1);
        for (int i = 0; i < num; i++) {
            long seed = StarSystemGenerator.random.nextLong();
            addSeed(seed);
        }
    }

    @Override
    protected CampaignFleetAPI spawnFleet(long seed) {
        Random random = new Random(seed);
        float factorMain = 0.6f;
        boolean mixAll = false;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(new Random(seed));
        picker.add("derelict",1f);
        picker.add("remnant", 1f);
        picker.add("kol_dawn", 0.7f);
        picker.add("kol_dusk", 0.7f);
        //picker.add("kol_elysians", 1f);
        if (PrepareAbyss.useDomres) picker.add("domres", 1f);
        if (PrepareAbyss.useDustkeepers) picker.add("sotf_dustkeepers", 1f);
        //if (PrepareAbyss.useEnigma) picker.add("enigma", 0.75f);
        if (PrepareAbyss.useLostech) {

            //picker.add("lostech", 0.5f);
        }
        String facSecond = picker.pick();

        FleetParamsV3 params = new FleetParamsV3(
                system.getLocation(),
                PrepareAbyss.elysianID,
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
        params.commander = createEDFCaptain();
        params.officerNumberMult = 5f;
        params.officerLevelBonus = 0;
        params.random = random;

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        params.factionId = facSecond;
        params.combatPts = params.combatPts/factorMain * (1-factorMain);

        CampaignFleetAPI fleet2 = FleetFactoryV3.createFleet(params);

        for (FleetMemberAPI member : fleet2.getMembersWithFightersCopy()) {
            boolean skip = false;
            for (int i = 0; i < PrepareAbyss.hullBlacklist.length; i++) {
                if (PrepareAbyss.hullBlacklist[i].equals(member.getHullId())) {
                    skip = true;
                }
            }
            if (skip) continue;
            member.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
            if (!member.getVariant().hasTag("UNBOARDABLE")) member.getVariant().addTag("UNBOARDABLE");
            if (member.isFlagship()) member.setFlagship(false);
            member.setShipName(Global.getSector().getFaction(facSecond).pickRandomShipName());
            fleet.getFleetData().addFleetMember(member);
        }
        fleet2.despawn();

        //fleet.getFleetData().sort();
        fleet.addTag("abyss_rulesfortheebutnotforme");
        if (fleet == null) return null;

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
            picker.add(entity, 1f);
        }

        for (SectorEntityToken entity : system.getPlanets()) {
            picker.add(entity, 1f);
        }

        return picker.pick();
    }



    public static void initElysianFleetProperties(Random random, CampaignFleetAPI fleet, boolean dormant) {
        if (random == null) random = new Random();

        //fleet.removeAbility(Abilities.EMERGENCY_BURN);
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
                new SeededFleetManagerElysian.ElysianFleetInteractionConfigGen());
    }

    public static void spawnPatrollersElysia(SectorEntityToken targ) {
        int numFleetsPatrol = 8;
        int numFleetsActive = 0;


        if (targ != null && numFleetsActive < numFleetsPatrol) {
            FleetParamsV3 params = new FleetParamsV3(
                    null,
                    targ.getStarSystem().getLocation(),
                    PrepareAbyss.elysianID,
                    5f,
                    FleetTypes.PATROL_LARGE,
                    MathUtils.getRandomNumberInRange(60f, 200f), // combatPts
                    0f, // freighterPts
                    0f, // tankerPts
                    0f, // transportPts
                    0f, // linerPts
                    0f, // utilityPts
                    5f // qualityMod, won't get used since routes mostly have quality override set
            );
            params.averageSMods = 1;
            params.ignoreMarketFleetSizeMult = true;
            params.commander = createEDFCaptain();
            params.officerNumberMult = 5f;
            params.officerLevelBonus = 0;

            CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
            fleet.addTag("abyss_rulesfortheebutnotforme");

            MagicCampaign.spawnFleet(fleet, targ, FleetAssignment.PATROL_SYSTEM, targ, true, false, false);

        }
    }

    public static PersonAPI createEDFCaptain() {
        return MagicCampaign.createCaptainBuilder(PrepareAbyss.duskID)
                .setIsAI(true)
                .setAICoreType("alpha_core")
                .setLevel(7)
                .setPersonality(Personalities.AGGRESSIVE)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_NO_MISSILE_YES_DEFENSE)
                .create();
    }
}
