package org.selkie.kol.campaign;

import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Pair;


public class kol_garden extends BaseIndustry implements MarketImmigrationModifier {

    public void apply() {
        super.apply(true);

        int size = market.getSize();

        demand(Commodities.MARINES, size - 3);
        demand(Commodities.CREW, size - 3);
        supply(Commodities.FOOD, size - 2);

        Pair<String, Integer> deficit = getMaxDeficit(Commodities.CREW);
        //applyDeficitToProduction(0, deficit, Commodities.FOOD, Commodities.ORGANICS);
        //applyDeficitToProduction(0, deficit, Commodities.FOOD);

        modifyStabilityWithBaseMod();

        if (!isFunctional()) {
            supply.clear();
            unmodifyStabilityWithBaseMod();
        }
    }

    @Override
    public boolean isFunctional() {
        return super.isFunctional();
    }

    @Override
    public void unapply() {
        super.unapply();
    }


    @Override
    public boolean isAvailableToBuild() {
        if (!super.isAvailableToBuild()) return false;

        if (market.getFactionId() == "knights_of_selkie" && market.hasCondition(Conditions.OUTPOST)) {
            return true;
        }

        return false;
    }


    @Override
    public boolean showWhenUnavailable() {
        return false;
    }


    @Override
    public String getUnavailableReason() {
        if (!super.isAvailableToBuild()) return super.getUnavailableReason();
        return "Not a Selkian";
    }


    @Override
    public void createTooltip(IndustryTooltipMode mode, TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltip(mode, tooltip, expanded);
    }


    public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
        incoming.add(Factions.LUDDIC_CHURCH, 10f);
    }


    @Override
    public String getCurrentImage() {
        return super.getCurrentImage();
    }

    @Override
    protected boolean canImproveToIncreaseProduction() {
        return false;
    }

    @Override
    public boolean showShutDown() {
        return false;
    }

    @Override
    protected int getBaseStabilityMod() {
        return 1;
    }

    @Override
    public boolean canInstallAICores() {
        return false;
    }
}
