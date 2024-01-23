package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.CampaignEngine;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.util.Random;

public class TrackFleet implements EveryFrameScript {

    private boolean inited = false;
    protected IntervalUtil iMain = new IntervalUtil(2, 2); //Seconds
    protected IntervalUtil iSecond = new IntervalUtil(3, 3); //multiplied by iMain
    protected StarSystemAPI under = null;
    protected SectorEntityToken targ = null;

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
                if (fleet.getContainingLocation().getId().equalsIgnoreCase(PrepareAbyss.elysiaSysName)) {
                    under = Global.getSector().getStarSystem(PrepareAbyss.nullspaceSysName);
                    if (under!= null && Math.abs(fleet.getLocation().getX()) <= 150 && Math.abs(fleet.getLocation().getY()) <= 150 && under.getEntityById(PrepareAbyss.nullgateID) != null) {
                        iSecond.advance(1);
                        if (iSecond.intervalElapsed()) {
                            targ = under.getEntityById(PrepareAbyss.nullgateID);
                            JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(targ, null);
                            Global.getSector().doHyperspaceTransition(fleet, fleet, dest);
                            fleet.setNoEngaging(1.0f);
                        }
                    } else {
                        iSecond.forceCurrInterval(0f);
                    }
                } else if (fleet.getContainingLocation() instanceof StarSystemAPI
                        && ((StarSystemAPI)fleet.getContainingLocation()).getStar() != null
                        && ((StarSystemAPI)fleet.getContainingLocation()).getStar().getTypeId().equalsIgnoreCase("zea_star_black_neutron")) {
                    if (Math.abs(fleet.getLocation().getX()) <= 50 && Math.abs(fleet.getLocation().getY()) <= 50) {
                        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>(new Random());
                        for (StarSystemAPI sys : Global.getSector().getStarSystems()) {
                            if (sys.isCurrentLocation()) continue;
                            if (sys.getStar() != null && sys.getStar().getTypeId().equalsIgnoreCase("zea_star_black_neutron")) picker.add(sys);
                        }
                        if (!picker.isEmpty()) {
                            StarSystemAPI dest = picker.pick();
                            fleet.getContainingLocation().removeEntity(fleet);
                            dest.addEntity(fleet);
                            Global.getSector().setCurrentLocation(dest);
                            fleet.setLocation(dest.getStar().getLocation().x,
                                    dest.getStar().getLocation().y);
                            fleet.setNoEngaging(2.0f);
                            fleet.clearAssignments();
                            CampaignEngine.getInstance().getCampaignUI().showNoise(0.5f, 0.25f, 1.5f);
                            fleet.getVelocity().normalise().scale(3000f);
                        }
                    }
                }
            }
        }
    }
}
