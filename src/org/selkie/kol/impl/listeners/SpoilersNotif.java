package org.selkie.kol.impl.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.intel.ZeaLoreIntel;

import java.io.IOException;

import static org.selkie.kol.impl.helpers.ZeaUtils.KEY_ZEA_SPOILERS;

public class SpoilersNotif implements EveryFrameScript {

    private final String filename = "kolSpoilersSeen";
    public IntervalUtil check = new IntervalUtil(8,11);
    public IntervalUtil delay = new IntervalUtil(15,15);
    public boolean fire = false;

    public static final String icon = Global.getSettings().getSpriteName("icons", "game_icon");
    public static final String desc = "%s\nYou may have already realized that Knights of Ludd contains a lot of hidden content. " +
            "In truth, it's the vast majority of the mod. We worked very hard on every piece and hope you enjoy it very much.\n\n" +
            "We ask that you %s for the first week or so after release. Its our hope that players can organically discover all that the mod contains.\n\n" +
            "Thanks, and once again please enjoy %s and %s.\n\n" +
            "PS: We have a spoilers-discussion thread on the Unofficial Starsector Discord server, you're welcome to join us.";
    public static final String[] descHLs = { "Welcome to Elysium.", "please don't openly spoil the secrets", "The Knights of Ludd", "Elysium" };
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
        return false;
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
            if (checkFile() || System.currentTimeMillis() > march) {
                Global.getSector().getMemoryWithoutUpdate().set(KEY_ZEA_SPOILERS, true);
                return;
            }
            delay.advance(amount);
            if (delay.intervalElapsed() && !Global.getSector().getMemoryWithoutUpdate().contains(KEY_ZEA_SPOILERS)) {
                if(Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null){
                    return; //check again after 15 sec, player is in area without the ability to have interaction
                }
                doSpoilersTalk();
                writeFile();
                Global.getSector().getMemoryWithoutUpdate().set(KEY_ZEA_SPOILERS, true);
            }
        }
    }

    protected void doSpoilersTalk() {
        //SectorEntityToken dummy = Global.getSector().getPlayerFleet().getContainingLocation().addCustomEntity("zea_spoiler_entity", "dummy", Entities.WARNING_BEACON, Factions.NEUTRAL);
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("zea_spoilers_popup");
        boolean gotDialog = Global.getSector().getCampaignUI().showInteractionDialog(plugin, Global.getSector().getPlayerFleet());
        ZeaLoreIntel intel = new ZeaLoreIntel(icon, "A quick message from the KOL team", desc, descHLs);
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

    protected void writeFile() {
        String text = "Thank you for playing Knights of Ludd!";
        try {
            Global.getSettings().writeTextFileToCommon(filename, text);
        } catch (IOException e) {
            return;
        }
    }

    protected boolean checkFile() {
        String text = "";
        try {
            text = Global.getSettings().readTextFileFromCommon(filename);
        } catch (IOException e) {
            return false;
        }
        if (text.isEmpty()) {
            return false;
        }
        return true;
    }
}
