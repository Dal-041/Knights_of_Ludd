package org.selkie.kol.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.fleets.misc.MiscFleetRouteManager;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.selkie.kol.fleets.KnightsExpeditionAssignmentAI;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.plugins.KOL_ModPlugin;
import org.selkie.kol.world.GenerateKnights;

import java.util.Random;

import static org.selkie.kol.world.GenerateKnights.baseKnightExpeditions;

public class KnightsExpeditionsManager extends BaseRouteFleetManager {

    public static final Integer ROUTE_PREPARE = 1;
    public static final Integer ROUTE_TRAVEL = 2;
    public static final Integer ROUTE_PATROL = 3;
    public static final Integer ROUTE_RETURN = 4;
    public static final Integer ROUTE_STAND_DOWN = 5;

    public KnightsExpeditionsManager() {
        super(16.0f, 20.0f);
    }

    @Override
    protected void addRouteFleetIfPossible() {
        Random random = new Random();
        MarketAPI from = getSourceMarket();
        if (from == null) return;

        SectorEntityToken to = getTargetToken(random);
        if (to == null) return;

        Long seed = new Random().nextLong();
        String id = getRouteSourceId();

        RouteManager.OptionalFleetData extra = new RouteManager.OptionalFleetData(from);

        RouteManager.RouteData route = RouteManager.getInstance().addRoute(id, from, seed, extra, this);

        float orbitDays = 4f + (float) Math.random() * 2f;
        float deorbitDays = 3f + (float) Math.random() * 2f;
        float patrolDays = 21f + (float) Math.random() * 9f;

        SectorEntityToken target = to;
        if ((float) Math.random() > 0.15f && to.getStarSystem() != null) {
            if ((float) Math.random() > 0.25f) {
                target = to.getStarSystem().getCenter();
            } else {
                target = to.getStarSystem().getHyperspaceAnchor();
            }
        }

        if (from.getContainingLocation() == to.getContainingLocation() && !from.getContainingLocation().isHyperspace()) {
            route.addSegment(new RouteManager.RouteSegment(ROUTE_PREPARE, orbitDays, from.getPrimaryEntity()));
            route.addSegment(new RouteManager.RouteSegment(ROUTE_PATROL, patrolDays, target));
            route.addSegment(new RouteManager.RouteSegment(ROUTE_STAND_DOWN, deorbitDays, from.getPrimaryEntity()));
        } else {
            route.addSegment(new RouteManager.RouteSegment(ROUTE_PREPARE, orbitDays, from.getPrimaryEntity()));
            route.addSegment(new RouteManager.RouteSegment(ROUTE_TRAVEL, from.getPrimaryEntity(), to));
            route.addSegment(new RouteManager.RouteSegment(ROUTE_PATROL, patrolDays, target));
            route.addSegment(new RouteManager.RouteSegment(ROUTE_RETURN, to, from.getPrimaryEntity()));
            route.addSegment(new RouteManager.RouteSegment(ROUTE_STAND_DOWN, deorbitDays, from.getPrimaryEntity()));
        }
    }

    public String pickFleetType(Random random) {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);
        picker.add(GenerateKnights.KnightsFleetTypes.WARRIORS, 20f);
        picker.add(GenerateKnights.KnightsFleetTypes.SCOUT, 5f);
        picker.add(GenerateKnights.KnightsFleetTypes.PATROL, 10f);
        picker.add(GenerateKnights.KnightsFleetTypes.ARMADA, 6f);
        return picker.pick();
    }

    public CampaignFleetAPI spawnFleet(RouteManager.RouteData route) {
        Random random = route.getRandom();

        String type = pickFleetType(random);

        float combat = 0f;
        float tanker = 0f;
        float freighter = 0f;
        switch (type) {
            case GenerateKnights.KnightsFleetTypes.SCOUT:
                combat = Math.round(4f + random.nextFloat() * 2f);
                tanker = Math.round(random.nextFloat()) * 5f;
                break;
            case GenerateKnights.KnightsFleetTypes.WARRIORS:
                combat = Math.round(10f + random.nextFloat() * 10f);
                tanker = Math.round(random.nextFloat()) * 8f;
                break;
            case GenerateKnights.KnightsFleetTypes.PATROL:
                combat = Math.round(8f + random.nextFloat() * 8f);
                tanker = Math.round(random.nextFloat()) * 8f;
                break;
            case GenerateKnights.KnightsFleetTypes.ARMADA:
                combat = Math.round(16f + random.nextFloat() * 12f);
                tanker = Math.round(random.nextFloat()) * 10f;
                freighter = Math.round(random.nextFloat()) * 16f;
                break;
        }

        combat *= 5f;

        FleetParamsV3 params = new FleetParamsV3(
                route.getMarket(),
                null,
                KOL_ModPlugin.kolID,
                route.getQualityOverride() + 0.2f,
                type,
                combat, // combatPts
                freighter, // freighterPts
                tanker, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                0f // qualityMod
        );
        params.timestamp = route.getTimestamp();
        params.random = random;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        if (fleet == null || fleet.isEmpty()) return null;

        route.getMarket().getContainingLocation().addEntity(fleet);
        fleet.setFacing((float) Math.random() * 360f);
        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(route.getMarket().getPrimaryEntity().getLocation().x, route.getMarket().getPrimaryEntity().getLocation().y);

        org.selkie.kol.fleets.KnightsExpeditionAssignmentAI ai = new KnightsExpeditionAssignmentAI(fleet, route);
        fleet.addScript(ai);
        fleet.setTransponderOn(true);

        return fleet;
    }

    @Override
    protected int getMaxFleets() {
        int count = baseKnightExpeditions;
        for(MarketAPI m : Global.getSector().getEconomy().getMarketsCopy()) {
            if (m.getFactionId().equals(KOL_ModPlugin.kolID)) count += 1;
            if (m.getFactionId().equals("kol")) count += 1;
        }
        return count;
    }

    @Override
    protected String getRouteSourceId() {
        return "Knights_global";
    }

    public void reportAboutToBeDespawnedByRouteManager(RouteManager.RouteData route) {

    }

    public boolean shouldCancelRouteAfterDelayCheck(RouteManager.RouteData route) {
        return false;
    }

    public boolean shouldRepeat(RouteManager.RouteData route) {
        return false;
    }

    public static MarketAPI getSourceMarket() {
        MarketAPI startMarket = null;
        if(goodSourceMarket(Global.getSector().getEntityById("kol_lyra")) && Global.getSector().getEntityById("kol_lyra").getFaction() == Global.getSector().getFaction(KOL_ModPlugin.kolID)) {
            startMarket = Global.getSector().getEntityById("kol_lyra").getMarket();
        } else if (Math.random() < 0.33f && goodSourceMarket(Global.getSector().getEntityById("kol_cygnus")) && Global.getSector().getEntityById("kol_cygnus").getFaction() == Global.getSector().getFaction(KOL_ModPlugin.kolID)) {
            startMarket = Global.getSector().getEntityById("kol_cygnus").getMarket();
        } else {
            for(MarketAPI markets : Global.getSector().getEconomy().getMarketsCopy()) {
                if(markets.getFaction().getId().equals(KOL_ModPlugin.kolID)) {
                    if(startMarket==null ||
                            (markets.hasSubmarket(Submarkets.GENERIC_MILITARY)
                                    && goodSourceMarket(markets.getPrimaryEntity())
                                    && (!startMarket.hasSubmarket(Submarkets.GENERIC_MILITARY)
                                    || markets.getSize()>startMarket.getSize()))) {
                        startMarket=markets;
                    }
                }
            }
        }
        return startMarket;
    }

    public SectorEntityToken getTargetToken(Random random) {
        WeightedRandomPicker<SectorEntityToken> pickerTo = new WeightedRandomPicker<SectorEntityToken>(random);
        WeightedRandomPicker<String> pickerS = new WeightedRandomPicker<String>(random);

        pickerS.add(getTargetRemnant(random), 2);
        pickerS.add(getTargetBH(random), 3);

        StarSystemAPI pickSys = Global.getSector().getStarSystem(pickerS.pick());
        if (pickSys == null) { Global.getLogger(this.getClass()).error("KOL: Expedition method pickSys returned null"); return null; }
        for(SectorEntityToken jp : pickSys.getJumpPoints()) {
            pickerTo.add(jp, 100000/MathUtils.getDistance(pickSys.getCenter(),jp.getLocation()));
        }
        SectorEntityToken to = pickerTo.pick();

        if (to == null || to.getContainingLocation().hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) { Global.getLogger(this.getClass()).error("KOL: Expedition method pickerTo returned null or is cut off"); return null; }
        return to;
    }

    /*
    @Override
    public MiscFleetRouteManager.MiscRouteData createRouteParams(MiscFleetRouteManager manager, Random random) {
        MarketAPI from = getSourceMarket();
        WeightedRandomPicker<SectorEntityToken> pickerTo = new WeightedRandomPicker<SectorEntityToken>(random);
        WeightedRandomPicker<String> pickerS = new WeightedRandomPicker<String>(random);

        pickerS.add(getTargetRemnant(random), 2);
        pickerS.add(getTargetBH(random), 3);

        StarSystemAPI pickSys = Global.getSector().getStarSystem(pickerS.pick());
        if (pickSys == null) { Global.getLogger(this.getClass()).error("KOL: Expedition method pickSys returned null"); return null; }
        for(SectorEntityToken jp : pickSys.getJumpPoints()) {
            pickerTo.add(jp, 100000/MathUtils.getDistance(pickSys.getCenter(),jp.getLocation()));
        }
        SectorEntityToken to = pickerTo.pick();

        if (to == null || to.getContainingLocation().hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) { Global.getLogger(this.getClass()).error("KOL: Expedition method pickerTo returned null or is cut off"); return null; }

        MiscFleetRouteManager.MiscRouteData result = createData(from, to);

        return result;
    }
     */

    public static boolean goodSourceMarket(SectorEntityToken marketEnt) {
        if (marketEnt == null) return false;
        if (marketEnt.getMarket() == null) return false;
        MarketAPI market = marketEnt.getMarket();
        boolean valid = true;

        if (market.isHidden()) valid = false;
        if (!market.hasSpaceport()) valid = false; // markets w/o spaceports don't launch fleets
        if (market.getContainingLocation().hasTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER)) valid = false;

        return valid;
    }


    public String getStartingActionText(CampaignFleetAPI fleet, RouteManager.RouteSegment segment, MiscFleetRouteManager.MiscRouteData data) {
        String type = fleet.getMemoryWithoutUpdate().getString(FleetTypes.TASK_FORCE);
        return "preparing for a righteous expedition";
    }

    public String getEndingActionText(CampaignFleetAPI fleet, RouteManager.RouteSegment segment, MiscFleetRouteManager.MiscRouteData data) {
        //return "orbiting " + data.from.getName();
        return "returned from a righteous expedition";
    }

    public String getTravelToDestActionText(CampaignFleetAPI fleet, RouteManager.RouteSegment segment, MiscFleetRouteManager.MiscRouteData data) {
        return "bravely venturing into danger";
    }

    public String getTravelReturnActionText(CampaignFleetAPI fleet, RouteManager.RouteSegment segment, MiscFleetRouteManager.MiscRouteData data) {
        return "returning home";
    }

    public static String getTargetRemnant(Random random) {
        String result = null;
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);

        for(StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag(Tags.THEME_REMNANT)) {
                if (!system.hasTag(Tags.THEME_HIDDEN) && !system.hasTag(Tags.THEME_SPECIAL)) {
                    if (system.hasTag(Tags.THEME_REMNANT_MAIN)) {
                        picker.add(system.getId(), 0.1f);
                    } else if (system.hasTag(Tags.THEME_REMNANT_RESURGENT)) { //Secondaries
                        picker.add(system.getId(), 3);
                    } else if (system.hasTag(Tags.THEME_REMNANT_SUPPRESSED)) {
                        picker.add(system.getId(), 4);
                    }
                }
            }
        }

        if (picker.isEmpty()) {
            for(StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (!system.hasTag(Tags.THEME_HIDDEN) && !system.hasTag(Tags.THEME_SPECIAL)) {
                    if (system.hasTag(Tags.THEME_INTERESTING)) picker.add(system.getId(), 10);
                    if (system.hasTag(Tags.THEME_INTERESTING_MINOR)) picker.add(system.getId(), 1);
                }
            }
        }

        result = picker.pick();

        return result;
    }

    public static String getTargetBH(Random random) {
        String result = null;
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);

        for(StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.getStar() != null
                    && (system.getStar().getTypeId().equals(StarTypes.BLACK_HOLE)
                    || system.getStar().getTypeId().equals("zea_white_hole")
                    || system.getStar().getTypeId().equals("zea_star_black_neutron"))) {
                if (!system.hasTag(Tags.THEME_HIDDEN) && !system.hasTag(Tags.THEME_SPECIAL)) {
                    if (system.hasTag(Tags.THEME_UNSAFE)) {
                        picker.add(system.getId(), 5);
                    } else {
                        picker.add(system.getId(), 2);
                    }
                }
                if (system.getStar().getTypeId().equals("zea_star_black_neutron")) {
                    picker.add(system.getId(), 14);
                }
            }
        }

        result = picker.pick();

        return result;
    }
}
