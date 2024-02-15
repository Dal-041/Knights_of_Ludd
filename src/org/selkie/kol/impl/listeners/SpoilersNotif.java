package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.intel.ZeaLoreIntel;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.helpers.ZeaUtils.KEY_ZEA_SPOILERS;

public class SpoilersNotif implements EveryFrameScript {

    public static IntervalUtil check = new IntervalUtil(8,11);
    public static IntervalUtil delay = new IntervalUtil(15,15);
    public static boolean fire = false;

    public static final String pingSprite = Global.getSettings().getSpriteName("icons", "kol_USC_ping");
    public static final String desc = "Hello! %s\n You may have already realized that Knights of Ludd contains a *%s* of hidden content. We worked very hard on it and hope you enjoy it very much.\n\nWe ask that you %s for the first couple weeks after release. We'd really appreciate it if most users could discover it organically.\n\n Thanks, and once again enjoy Our Song, %s and %s.\n- The Knights of Ludd team";
    public static final String[] descHLs = { "Welcome to the Elysian Abyss.", "lot", "please don't post openly about the secrets", "The Knights of Ludd", "The Elysian Abyss" };

    @Override
    public boolean isDone() {
        return Global.getSector().getMemoryWithoutUpdate().contains(KEY_ZEA_SPOILERS) && (boolean)Global.getSector().getMemoryWithoutUpdate().get(KEY_ZEA_SPOILERS);
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        check.advance(amount);
        if (check.intervalElapsed()) {
            if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null
                    && Global.getSector().getPlayerFleet().getContainingLocation().hasTag(ZeaUtils.THEME_ZEA)) {
                fire = true;
            }
        }
        if (fire) {
            delay.advance(amount);
            if (delay.intervalElapsed() && !Global.getSector().getMemoryWithoutUpdate().contains(KEY_ZEA_SPOILERS)) {
                ZeaLoreIntel intel = new ZeaLoreIntel(pingSprite, "A quick message from the KoL team", desc, descHLs);
                Global.getSector().getIntelManager().addIntel(intel);
                Global.getSector().getMemoryWithoutUpdate().set(KEY_ZEA_SPOILERS, true);
            }
        }
    }
}
