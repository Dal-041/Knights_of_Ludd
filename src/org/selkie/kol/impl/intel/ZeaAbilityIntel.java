package org.selkie.kol.impl.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.io.IOException;

public class ZeaAbilityIntel extends BaseIntelPlugin {

    private String name;
    private String icon;

    public ZeaAbilityIntel(String icon, String name) {
        try {
            Global.getSettings().loadTexture(icon);
        } catch (IOException e) {

        }
        this.name = name;
        this.icon = icon;
    }

    @Override
    protected String getName() {
        return "Fracture jump protocol: " + name;
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {

        Color gray = Misc.getGrayColor();

        info.addSpacer(3f);
        info.addPara("Right click on an %s to view or select the %s.", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "ability slot", "new ability");
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color gray = Misc.getGrayColor();
        info.addSpacer(10f);
        TooltipMakerAPI imageTooltip = info.beginImageWithText(icon, 64f);
        imageTooltip.addPara("Your decryption team has recovered a novel transverse jump protocol from the AI flagship's wreckage. " +
                "This one will allow you to travel from %s directly to %s, with a long cooldown",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "anywhere in hyperspace", name);
        info.addImageWithText(0f);
        info.addSpacer(10f);

        info.addPara("Right click on an ability slot to view or select the new ability. %s, as conditions upon arrival may be hostile.", 0f, Misc.getTextColor(), Misc.getNegativeHighlightColor(), "Use with caution");
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public boolean canTurnImportantOff() {
        return true;
    }
}
