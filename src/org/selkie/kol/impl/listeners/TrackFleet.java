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
    protected IntervalUtil iMain = new IntervalUtil(10f, 10f);
    protected IntervalUtil iSecond = new IntervalUtil(12f, 12f); //multiplied by iMain
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
            CampaignFleetAPI fleet = null;
            if (Global.getSector().getPlayerFleet() != null) {
                fleet = Global.getSector().getPlayerFleet();
                if (fleet.isInHyperspaceTransition()) return;
                if (fleet.getContainingLocation().getId().equals("elysia")) {
                    under = Global.getSector().getStarSystem("Underspace");
                    if (under!= null && Math.abs(fleet.getLocation().getX()) <= 250 && Math.abs(fleet.getLocation().getY()) <= 250 && under.getEntityById(PrepareAbyss.undergateID) != null) {
                        iSecond.advance(amount);
                        if (iSecond.intervalElapsed()) {
                            targ = under.getEntityById(PrepareAbyss.undergateID);
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
