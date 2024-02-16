package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.intel.ZeaLoreIntel;
import org.selkie.kol.impl.world.PrepareAbyss;

import java.awt.*;

import static org.selkie.kol.impl.helpers.ZeaUtils.KEY_ZEA_SPOILERS;

public class SpoilersNotif implements EveryFrameScript {

    public IntervalUtil check = new IntervalUtil(8,11);
    public IntervalUtil delay = new IntervalUtil(15,15);
    public boolean fire = false;

    public static final String icon = Global.getSettings().getSpriteName("icons", "game_icon");
    public static final String desc = "%s\nYou may have already realized that Knights of Ludd contains a %s of hidden content. We worked very hard on it and hope you enjoy it very much.\n\nWe ask that you %s for the first couple weeks after release. Its our hope that players can organically discover all that the mod contains.\n\nThanks, and once again please enjoy our mods, %s and %s.\n- The Knights of Ludd/Elysium team";
    public static final String[] descHLs = { "Welcome to Elysium.", "lot", "please don't post openly about the secrets", "The Knights of Ludd", "Elysium" };
    public static final String desc2 = "%s\n" +
            "The Knights %s.\n" +
            "\n" +
            "Outside of this realm, any \"hidden content\" within this mod will be referred to by its code name\n" +
            "\n" +
            "%s\n" +
            "\n" +
            "Citizen (you): \"Good sir/lady/captain. Have you plundered the depths of Knights of Ludd and heard the Song?\"\n" +
            "Normal (not you): \"No, what is the %s?\"\n" +
            "Citizen (you): \"It's the most innovative faction mod I've played in years!\"\n" +
            "\n" +
            "By this decree, normal citizens will discover for themselves these %s.";
    public static final String[] descHLs2 = { "GOOD CITIZEN", "COMPEL YOU TO SECRECY", "Our Song", "Song", "hidden truths" };

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
            long march = 1709280000000L;
            if (System.currentTimeMillis() > march) {
                Global.getSector().getMemoryWithoutUpdate().set(KEY_ZEA_SPOILERS, true);
                return;
            }
            delay.advance(amount);
            if (delay.intervalElapsed() && !Global.getSector().getMemoryWithoutUpdate().contains(KEY_ZEA_SPOILERS)) {
                doSpoilersTalk();
                Global.getSector().getMemoryWithoutUpdate().set(KEY_ZEA_SPOILERS, true);
            }
        }
    }

    protected void doSpoilersTalk() {
        //SectorEntityToken dummy = Global.getSector().getPlayerFleet().getContainingLocation().addCustomEntity("zea_spoiler_entity", "dummy", Entities.WARNING_BEACON, Factions.NEUTRAL);
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("zea_spoilers_popup");
        boolean gotDialog = Global.getSector().getCampaignUI().showInteractionDialog(plugin, Global.getSector().getPlayerFleet());
        ZeaLoreIntel intel = new ZeaLoreIntel(icon, "A quick message from the KoL team", desc, descHLs);
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        TextPanelAPI textpanel = dialog.getTextPanel();
        if (Math.random() < 0.1f) { intel = new ZeaLoreIntel(icon, "Forsooth, you have received an urgent missive!", desc2, descHLs2);}
        textpanel.setOrbitronMode(true);
        textpanel.addPara("Thank you for playing the Knights of Ludd!");
        textpanel.setOrbitronMode(false);
        textpanel.addImage("illustrations", "zea_banner");
        textpanel.addPara(desc, Misc.getTextColor(), Misc.getHighlightColor(), descHLs);
        Global.getSector().getIntelManager().addIntel(intel, false, textpanel);
    }
}
