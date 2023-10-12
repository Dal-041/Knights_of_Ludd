package org.selkie.kol.impl.world;

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

        FleetParamsV3 params = new FleetParamsV3(
                system.getLocation(),
                PrepareAbyss.elysianID,
                5f,
                FleetTypes.PATROL_LARGE,
                MathUtils.getRandomNumberInRange(60f,200f), // combatPts
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
        fleet.addTag("abyss_rulesfortheebutnotforme");
        if (fleet == null) return null;

        system.addEntity(fleet);
        fleet.setFacing(random.nextFloat() * 360f);

        int numActive = 0;
        for (SeededFleet f : fleets) {
            if (f.fleet != null) numActive++;
        }

        initElysianFleetProperties(random, fleet, false);

        fleet.addScript(new RemnantAssignmentAI(fleet, system, null));

        return fleet;
    }


    public static SectorEntityToken pickEntityToGuard(Random random, StarSystemAPI system, CampaignFleetAPI fleet) {
        WeightedRandomPicker<SectorEntityToken> picker = new WeightedRandomPicker<SectorEntityToken>(random);

        for (SectorEntityToken entity : system.getEntitiesWithTag(Tags.SALVAGEABLE)) {
            float w = 1f;
            if (entity.hasTag(Tags.NEUTRINO_HIGH)) w = 3f;
            if (entity.hasTag(Tags.NEUTRINO_LOW)) w = 0.33f;
            picker.add(entity, w);
        }

        for (SectorEntityToken entity : system.getJumpPoints()) {
            picker.add(entity, 1f);
        }

        return picker.pick();
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
                new SeededFleetManagerElysian.ElysianFleetInteractionConfigGen());
    }

    public static void spawnElysiaPatrollers() {
        int numFleetsPatrol = 4;
        int numFleetsActive = 0;

        SectorEntityToken targAbyss = null;
        SectorEntityToken targGaze = null;
        SectorEntityToken targSilence = null;

        if(Global.getSector().getEntityById("abyss_elysia_abyss")!=null){
            targAbyss = Global.getSector().getEntityById("abyss_elysia_abyss");
        }
        if(Global.getSector().getEntityById("abyss_elysia_gaze")!=null){
            targGaze = Global.getSector().getEntityById("abyss_elysia_gaze");
        }
        if(Global.getSector().getEntityById("abyss_elysia_silence")!=null){
            targSilence = Global.getSector().getEntityById("abyss_elysia_silence");
        }


        if (targGaze != null && numFleetsActive < numFleetsPatrol) {
            FleetParamsV3 params = new FleetParamsV3(
                    null,
                    null,
                    PrepareAbyss.elysianID,
                    5f,
                    FleetTypes.PATROL_LARGE,
                    MathUtils.getRandomNumberInRange(200f,400f), // combatPts
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

            MagicCampaign.spawnFleet(fleet, targGaze, FleetAssignment.PATROL_SYSTEM, targGaze, true, false, false);

        }
    }

    public static PersonAPI createEDFCaptain() {
        return MagicCampaign.createCaptainBuilder(PrepareAbyss.duskID)
                .setIsAI(true)
                .setAICoreType("alpha_core")
                .setPersonality(Personalities.AGGRESSIVE)
                .setSkillPreference(OfficerManagerEvent.SkillPickPreference.YES_ENERGY_NO_BALLISTIC_NO_MISSILE_YES_DEFENSE)
                .create();
    }
}
