package org.selkie.kol.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation;
import org.selkie.kol.helpers.KolStaticStrings;

public class kol_battlestar extends OrbitalStation {
    @Override
    public boolean canDowngrade() {
        return false;
    }

    @Override
    public boolean canUpgrade() {
        if (Global.getSector() != null && Global.getSector().getPlayerFaction() != null) {
            return Global.getSector().getPlayerFaction().getId().equals(KolStaticStrings.kolFactionID);
        }
        return false;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return true;
    }
}
