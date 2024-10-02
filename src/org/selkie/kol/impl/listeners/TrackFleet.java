package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CampaignEngine;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaStarTypes;

import java.util.Random;

public class TrackFleet implements EveryFrameScript {

    protected final IntervalUtil iMain = new IntervalUtil(2, 2); //Seconds
    protected final IntervalUtil iSecond = new IntervalUtil(3, 3); //multiplied by iMain
    protected StarSystemAPI under = null;
    protected SectorEntityToken targ = null;
    public static boolean zEAVisit = false;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        iMain.advance(amount);
        if (iMain.intervalElapsed()) {
            if (Global.getSector().getPlayerFleet() != null) {
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
                if (fleet.isInHyperspaceTransition()) return;
                if (fleet.getContainingLocation().getId().equalsIgnoreCase(ZeaStaticStrings.elysiaSysName)) {
                    under = Global.getSector().getStarSystem(ZeaStaticStrings.nullspaceSysName);
                    if (under!= null && Math.abs(fleet.getLocation().getX()) <= 150 && Math.abs(fleet.getLocation().getY()) <= 150 && under.getEntityById(ZeaStaticStrings.ZEA_NULLGATE_DUSK) != null) {
                        iSecond.advance(1);
                        if (iSecond.intervalElapsed()) {
                            targ = under.getEntityById(ZeaStaticStrings.ZEA_NULLGATE_DUSK);
                            JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(targ, null);
                            Global.getSector().doHyperspaceTransition(fleet, fleet, dest);
                            fleet.setNoEngaging(1.0f);
                        }
                    } else {
                        iSecond.forceCurrInterval(0f);
                    }
                } else if (fleet.getContainingLocation() instanceof StarSystemAPI
                        && ((StarSystemAPI)fleet.getContainingLocation()).getStar() != null
                        && ((StarSystemAPI)fleet.getContainingLocation()).getStar().getTypeId().equalsIgnoreCase(ZeaStarTypes.ZEA_STAR_BLACK_NEUTRON)) {
                    StarSystemAPI system = (StarSystemAPI) fleet.getContainingLocation();
                    CampaignFleetAPI victim = null;
                    for (CampaignFleetAPI sysFleet : system.getFleets()) {
                        if (sysFleet != victim && Math.abs(sysFleet.getLocation().getX()) <= 50 && Math.abs(sysFleet.getLocation().getY()) <= 50) {
                            victim = sysFleet;
                        }
                    }
                    if (victim != null) {
                        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(new Random());
                        for (StarSystemAPI sys : Global.getSector().getStarSystems()) {
                            if (sys.isCurrentLocation()) continue;
                            if (sys.getStar() != null && sys.getStar().getTypeId().equalsIgnoreCase(ZeaStarTypes.ZEA_STAR_BLACK_NEUTRON)) picker.add(sys);
                        }
                        if (!picker.isEmpty()) {
                            StarSystemAPI dest = picker.pick();
                            system.removeEntity(victim);
                            dest.addEntity(victim);
                            if (victim.equals(fleet)) {
                                Global.getSector().setCurrentLocation(dest);
                                fleet.setNoEngaging(2.0f);
                                fleet.clearAssignments();
                            }
                            victim.setLocation(dest.getStar().getLocation().x, dest.getStar().getLocation().y);
                            victim.setNoEngaging(2.0f);
                            CampaignEngine.getInstance().getCampaignUI().showNoise(0.5f, 0.25f, 1.5f);
                            if (victim.getVelocity().length() == 0) victim.getVelocity().setX(0.1f);
                            victim.getVelocity().normalise().scale(3000f);
                        }
                    }
                }
            }
        }
    }
}
