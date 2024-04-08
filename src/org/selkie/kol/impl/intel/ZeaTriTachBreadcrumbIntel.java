package org.selkie.kol.impl.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.helpers.ZeaUtils;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZeaTriTachBreadcrumbIntel extends BaseIntelPlugin {

    protected SectorEntityToken entity;
    protected String rewardID;
    protected String name;
    public String desc;
    public String descShort;
    protected String icon = Global.getSector().getFaction(Factions.TRITACHYON).getLogo();

    public ZeaTriTachBreadcrumbIntel(String name, String desc, SectorEntityToken target) {
        this.entity = target;
        //this.rewardID = ID;
        this.name = name;
        this.desc = desc;
        this.descShort = desc;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        //Color c = getTitleColor(mode);
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;

        info.addImage(Global.getSector().getFaction(Factions.TRITACHYON).getLogo(), width, 128, opad);

        String name = "";
        String shortName = "";
        String isOrAre = "is";
        String aOrAn = "a";

        if (entity.getCustomEntitySpec() != null) {
            name = entity.getCustomEntitySpec().getNameInText();
            shortName = entity.getCustomEntitySpec().getShortName();
            isOrAre = entity.getCustomEntitySpec().getIsOrAre();
            aOrAn = entity.getCustomEntitySpec().getAOrAn();
        } else {
            name = entity.getName();
            shortName = entity.getName();
        }

        Color gray = Misc.getGrayColor();
        info.addSpacer(10f);
        //TooltipMakerAPI imageTooltip = info.beginImageWithText(Global.getSector().getFaction(Factions.TRITACHYON).getCrest(), 24f);
        //imageTooltip.addPara(desc, 0f, Misc.getTextColor(), Misc.getHighlightColor(), null);
        //info.addImageWithText(0f);
        info.addSpacer(10f);

        String loc = BreadcrumbSpecial.getLocatedString(entity, true);
        info.addPara("The " + shortName + " " + isOrAre + " " + loc + ".", opad);
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {

        Color gray = Misc.getGrayColor();

        String name = "";
        String shortName = "";
        String isOrAre = "is";
        String aOrAn = "a";

        if (entity.getCustomEntitySpec() != null) {
            name = entity.getCustomEntitySpec().getNameInText();
            shortName = entity.getCustomEntitySpec().getShortName();
            isOrAre = entity.getCustomEntitySpec().getIsOrAre();
            aOrAn = entity.getCustomEntitySpec().getAOrAn();
        } else {
            name = entity.getName();
            shortName = entity.getName();
        }
        String loc = BreadcrumbSpecial.getLocatedString(entity, true);

        info.addSpacer(3f);
        //info.addPara("", 0f, Misc.getTextColor(), Misc.getHighlightColor(), null);
        info.addPara("The " + shortName + " " + isOrAre + " " + loc + ".", 10f);
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        info.addPara(name, c, 0f);

        addBulletPoints(info, mode);
    }

    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(ZeaUtils.IntelBreadcrumbTag);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (entity != null && entity.isDiscoverable() && entity.getStarSystem() != null) {
            return entity.getStarSystem().getCenter();
        }
        return entity;
    }

    public SectorEntityToken getEntity() {
        return entity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public boolean canTurnImportantOff() {
        return true;
    }

    @Override
    public boolean callEvent(String ruleId, final InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String action = params.get(0).getString(memoryMap);

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        CargoAPI cargo = playerFleet.getCargo();
        SpecialItemData item = new SpecialItemData(rewardID, "Recovered a Tri-Tachyon data recorder");

        if (action.equals("zea_BossStationTT_Salvage2")) {
            Global.getSector().getPlayerStats().addXP(200000, dialog.getTextPanel());
            ReputationActionResponsePlugin.ReputationAdjustmentResult result = Global.getSector().adjustPlayerReputation(
                    new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_SUCCESS, -5,
                            null, dialog.getTextPanel(), true, false),
                    Factions.TRITACHYON);
            endAfterDelay();
        }

        if (action.equals("zea_AfterNinevehDefeat")) {

            Global.getSector().getPlayerStats().addXP(250000, dialog.getTextPanel());

            ReputationActionResponsePlugin.ReputationAdjustmentResult result = Global.getSector().adjustPlayerReputation(
                    new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.MISSION_SUCCESS, -5,
                            null, dialog.getTextPanel(), true, false),
                    Factions.TRITACHYON);
            endAfterDelay();
        }

        return true;
    }
}
