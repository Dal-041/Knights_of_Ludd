package org.selkie.kol.impl.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class ZeaMechanicIntel extends BaseIntelPlugin {

    private String name;
    private String icon;
    private String desc;
    private String[] descHighlights;
    public String ID;

    private static String[] dawn = {
            "The hulking Dawntide ships employ %s. Their armor and shield drones make them tough nuts to crack. "
                    + "Be wary of their %s or you may find yourself crushed under the unyielding tide.",
            "Dawntide ships possess a special %s system which increases their threat potential.\n"
                    + "At full hull strength they receive a %s; at 0%% hull their weapons receive a %s, "
                    + "with a %s from range to ROF as their ship takes more damage."
    };
    private static ArrayList<String[]> dawnHLs = new ArrayList<>();
    static {
        String[] hl1 = { "heavy defensive measures", "bursts of speed and many fighter drones" };
        dawnHLs.add(hl1);
        String[] hl2 = { "Cascade Targeting Protocol", "15% bonus to weapon range", "20% firerate boost", "smooth transition" };
        dawnHLs.add(hl2);
    };
    private static String[] dusk = {
            "The Duskborne eschew direct confrontation, specializing in %s and %s. They employ many phase ships, strike weapons, and %s to attack, damage, and disable any unshielded hull their foes leave exposed. Do %s let your guard down."
    };
    private static ArrayList<String[]> duskHLs = new ArrayList<>();
    static {
        String[] hl1 = {"skirmishes", "heavy strikes", "ubiquitous motes", "NOT"};
        duskHLs.add(hl1);
    };
    private static String[] edf = {
            "Elysian fleets appear numerous in terms of both fleets and vessels. It's %s to %s. It's best to %s and explore without them ever noticing.\n%s",
            "The Elysian vessels appear to %s to empower their combat systems. The systems, \"%s\", as our engineers have dubbed them, recharge during combat %s. " +
                    "\n When fully charged, we estimate they can provide a %s increase in the performance of their %s. The charge depletes when they build flux by firing their weapons." +
                    "\n Combat with these vessels will be %s."
    };
    private static ArrayList<String[]> edfHLs = new ArrayList<>();
    static {
            String[] hl1 = { "strongly recommended", "avoid direct combat with their fleets", "avoid detection", "They are likely to win combat on their terms." };
            edfHLs.add(hl1);
            String[] hl2 = { "harvest energy from nearby stars", "Coronal Capacitors", "depending where in the system they are", "30%",
            "weapons and engines", "much easier the farther from local stars that we are." };
            edfHLs.add(hl2);
    };
    private static String[] TT = {
            "phase!"
    };
private static ArrayList<String[]> TTHLs = new ArrayList<>();
    static {};
    private static String[] KoL = {
            "Knights ships make heavy use of modules. Should you engage them in combat, %s to take them down swiftly. They will also be more vulnerable to %s.",
            "The Knights of Ludd use an outdated but efficient shield generator design. Their shields %s but %s. If their shields fully deplete they %s until they've fully charged, leaving them vulernable."
    };
private static ArrayList<String[]> KoLHLs = new ArrayList<>();
    static {
        String[] hl1 = { "fire between the gaps in their armor", "heavy torpedos" };
        KoLHLs.add(hl1);
        String[] hl2 = { "can withstand modern firepower", "they can only be raised for up to 10 seconds before needing to recharge", "won't be able to raise them again" };
        KoLHLs.add(hl2);
    };

    public ZeaMechanicIntel(String id, String icon, String name) {
        try {
            Global.getSettings().loadTexture(icon);
        } catch (IOException e) {

        }
        this.ID = id;
        this.name = name;
        this.icon = icon;
    }

    public ZeaMechanicIntel(String id, String icon, String name, String desc, String[] descHighlights) {
        try {
            Global.getSettings().loadTexture(icon);
        } catch (IOException e) {

        }
        this.ID = id;
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

        info.addSpacer(3f);
        info.addPara("An important tactical analysis", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "tactical analysis");
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color gray = Misc.getGrayColor();
        info.addSpacer(10f);
        TooltipMakerAPI imageTooltip = info.beginImageWithText(icon, 32f);
        imageTooltip.addPara(desc,0f, Misc.getTextColor(), Misc.getHighlightColor(), descHighlights);
        info.addImageWithText(0f);
        info.addSpacer(10f);
    }

    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        if (!name.contains("Knights")) tags.add(ZeaUtils.IntelLoreTag);
        else tags.add("Knights of Ludd");
        return tags;
    }

    @Override
    public boolean canTurnImportantOff() {
        return true;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public String getID() {
        return ID;
    }

    //Could combine these, maybe later
    public static int unknownMechanics(String id) {
        int count = 0;
        int total = 0;
        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(ZeaMechanicIntel.class)) {
            ZeaMechanicIntel thisIntel = (ZeaMechanicIntel) intel;
            if (thisIntel.ID == null) return 0;
            if (thisIntel.getID().equals(id)) count++;
        }

        if (id.equals(PrepareAbyss.duskID)) total = dusk.length;
        else if (id.equals(PrepareAbyss.dawnID)) total = dawn.length;
        else if (id.equals(PrepareAbyss.elysianID)) total = edf.length;
        else if (id.equals(Factions.TRITACHYON)) total = TT.length;
        else if (id.equals(KOL_ModPlugin.kolID)) total = KoL.length;

        if (count < total) {
            return total - count;
        }
        return 0;
    }

    public static ZeaMechanicIntel getNextMechanicIntel(String id) {
        String[] desc;
        ArrayList<String[]> hl;
        switch (id) {
            case PrepareAbyss.duskID:
                desc = dusk;
                hl = duskHLs;
                break;
            case PrepareAbyss.dawnID:
                desc = dawn;
                hl = dawnHLs;
                break;
            case PrepareAbyss.elysianID:
                desc = edf;
                hl = edfHLs;
                break;
            case Factions.TRITACHYON:
                desc = TT;
                hl = TTHLs;
                break;
            case KOL_ModPlugin.kolID:
                desc = KoL;
                hl = KoLHLs;
                break;
            default:
                return null;
        }
        int unknown = unknownMechanics(id);
        if (unknown == 0) return null;
        int entry = desc.length - unknown;
        String title = Global.getSector().getFaction(id).getDisplayName() + " Assessment #" + Integer.toString(entry+1);

        return new ZeaMechanicIntel(id, Global.getSector().getFaction(id).getCrest(), title, desc[entry], hl.get(entry));
    }
}
