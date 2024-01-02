package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.selkie.kol.impl.world.PrepareAbyss;

public class TrackFleet implements EveryFrameScript {

    private boolean inited = false;
    protected IntervalUtil iMain = new IntervalUtil(1f, 1f);
    protected IntervalUtil iSecond = new IntervalUtil(7f, 7f);
    protected StarSystemAPI elysia = null;
    protected SectorEntityToken targ = null;
    //private float days = 0;

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
            CampaignFleetAPI fleet = null;
            if (Global.getSector().getPlayerFleet() != null) {
                fleet = Global.getSector().getPlayerFleet();
                if (fleet.isInHyperspaceTransition()) return;
                if (fleet.getContainingLocation().getId().equals("elysia")) {
                    if (Math.abs(fleet.getLocation().getX()) <= 250 && Math.abs(fleet.getLocation().getY()) <= 250 && Global.getSector().getStarSystem("Underspace").getEntityById(PrepareAbyss.undergateID) != null) {
                        iSecond.advance(amount);
                        if (iSecond.intervalElapsed()) {
                            targ = Global.getSector().getStarSystem("Underspace").getEntityById(PrepareAbyss.undergateID);
                            JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(targ, null);
                            Global.getSector().doHyperspaceTransition(fleet, fleet, dest);
//                            fleet.getContainingLocation().removeEntity(fleet);
//                            targ.getContainingLocation().addEntity(fleet);
//                            Global.getSector().setCurrentLocation(targ.getContainingLocation());
//                            fleet.setLocation(targ.getLocation().x, targ.getLocation().y);
                            fleet.setNoEngaging(1.0f);
                        }
                    } else {
                        iSecond.forceCurrInterval(0f);
                    }
                }
            }
        }
    }
}
