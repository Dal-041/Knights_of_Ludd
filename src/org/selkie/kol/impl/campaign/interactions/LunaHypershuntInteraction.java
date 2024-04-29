package org.selkie.kol.impl.campaign.interactions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

public class LunaHypershuntInteraction implements InteractionDialogPlugin {

    InteractionDialogAPI dialog;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;

        dialog.getVisualPanel().showImageVisual(dialog.getInteractionTarget().getCustomInteractionDialogImageVisual());

        String text = Global.getSettings().getDescription(dialog.getInteractionTarget().getCustomDescriptionId(),  Description.Type.CUSTOM).getText1();
        dialog.getTextPanel().addPara(text);

        Boolean foundAma = foundAmaterasu(dialog);

        if (foundAma) {
            dialog.getTextPanel().addPara("Interference by a local fleet with the transponder signature \"Amaterasu\" makes it impossible to access the Hypershunt directly.",
                    Misc.getTextColor(), Misc.getHighlightColor(), "\"Amaterasu\"");

            dialog.getOptionPanel().addOption("Leave", "LEAVE");
            dialog.getOptionPanel().setShortcut("LEAVE", Keyboard.KEY_ESCAPE, false, false, false, false);
        }
        else if (dialog.getInteractionTarget().getMemoryWithoutUpdate().contains("$usable")){
            dialog.getTextPanel().addPara("It is as of now in active operation and enables any colony within " + ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS + " light-years to establish an additional industry, given that it has a hypershunt tap installed.",
                    Misc.getTextColor(), Misc.getHighlightColor(), "" + ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS, "hypershunt tap");

            dialog.getOptionPanel().addOption("Leave", "LEAVE");
            dialog.getOptionPanel().setShortcut("LEAVE", Keyboard.KEY_ESCAPE, false, false, false, false);
        } else {
            dialog.getTextPanel().addPara("A quick scan indicates that, surprisingly, the structure is still functional, though at the moment shut down. " +
                    "Turning it back on enables any colony within " + ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS + " light-years to establish an additional industry, as long as a hypershunt tap is installed within the colony.",
                    Misc.getTextColor(), Misc.getHighlightColor(), "" + ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS, "hypershunt tap");

            dialog.getOptionPanel().addOption("Bring it online", "ONLINE");

            dialog.getOptionPanel().addOption("Leave", "LEAVE");
            dialog.getOptionPanel().setShortcut("LEAVE", Keyboard.KEY_ESCAPE, false, false, false, false);
        }

    }

    @NotNull
    private static Boolean foundAmaterasu(InteractionDialogAPI dialog) {
        boolean foundAma = false;

        List<CampaignFleetAPI> fleets = dialog.getInteractionTarget().getContainingLocation().getFleets();
        for (CampaignFleetAPI fleet : fleets) {
            if (fleet == Global.getSector().getPlayerFleet()) continue;
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                if (member.getHullSpec().getBaseHullId().equals("zea_boss_amaterasu")) {
                    foundAma = true;
                }
            }
        }
        return foundAma;
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {

        if (optionData.equals("ONLINE")) {
            dialog.getOptionPanel().clearOptions();

            dialog.getInteractionTarget().getMemoryWithoutUpdate().set("$usable", true);

            dialog.getTextPanel().addPara("You give your crew the command to turn the megastructure back on running, and due to domain-level software optimisation, the job is done faster than one would expect.");

            dialog.getOptionPanel().addOption("Leave", "LEAVE");
            dialog.getOptionPanel().setShortcut("LEAVE", Keyboard.KEY_ESCAPE, false, false, false, false);
        }

        if (optionData.equals("LEAVE")) {
            dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
