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
    protected IntervalUtil iMain = new IntervalUtil(20, 20);
    protected IntervalUtil iSecond = new IntervalUtil(5, 5); //multiplied by iMain
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
                if (fleet.getContainingLocation().getId().equalsIgnoreCase(PrepareAbyss.elysiaSysName)) {
                    under = Global.getSector().getStarSystem(PrepareAbyss.nullspaceSysName);
                    if (under!= null && Math.abs(fleet.getLocation().getX()) <= 350 && Math.abs(fleet.getLocation().getY()) <= 350 && under.getEntityById(PrepareAbyss.nullgateID) != null) {
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
                }
            }
        }
    }
}
