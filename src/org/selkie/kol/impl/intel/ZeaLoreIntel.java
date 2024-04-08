package org.selkie.kol.impl.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

public class ZeaLoreIntel extends BaseIntelPlugin {

    private String name;
    private String icon;
    private String desc;
    private String[] descHighlights;

    public ZeaLoreIntel(String icon, String name) {
        try {
            Global.getSettings().loadTexture(icon);
        } catch (IOException e) {

        }
        this.name = name;
        this.icon = icon;
    }

    public ZeaLoreIntel(String icon, String name, String desc, String[] descHighlights) {
        try {
            Global.getSettings().loadTexture(icon);
        } catch (IOException e) {

        }
        this.name = name;
        this.icon = icon;
        this.desc = desc;
        this.descHighlights = descHighlights;
    }

    @Override
    protected String getName() {
        return name;
    }

    public void setName(String text, boolean append) {
        if (append) {
            name.concat(" " + text);
            return;
        }
        this.name = text;
    }

    public void setDesc(String text, boolean append) {
        if (append) {
            desc.concat(" " + text);
            return;
        }
        this.desc = text;
    }

    public void setDesc(String text, boolean append, String[] highlights) {
        if (append) {
            desc.concat(" " + text);
            int oldSize = this.descHighlights.length;
            int newSize = this.descHighlights.length+highlights.length;
            if (newSize > oldSize) {
                String[] temp = new String[newSize];
                for (int i = 0; i < newSize; i++) {
                    if (i < oldSize) {
                        temp[i] = this.descHighlights[i];
                    } else {
                        temp[i] = highlights[i];
                    }
                }
                this.descHighlights = temp;
            }
            return;
        }
        this.desc = text;
        this.descHighlights = highlights;
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {

        Color gray = Misc.getGrayColor();

        info.addSpacer(3f);
        info.addPara("A fragment of hidden knowledge", 0f);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color gray = Misc.getGrayColor();
        info.addSpacer(10f);
        TooltipMakerAPI imageTooltip = info.beginImageWithText(icon, 32f);
        imageTooltip.addPara(desc,
                0f, Misc.getTextColor(), Misc.getHighlightColor(), descHighlights);
        info.addImageWithText(0f);
        info.addSpacer(10f);
    }

    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(ZeaUtils.IntelLoreTag);
        return tags;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public boolean canTurnImportantOff() {
        return true;
    }
}
