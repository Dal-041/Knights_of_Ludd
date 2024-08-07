package org.selkie.kol.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation;
import org.selkie.kol.plugins.KOL_ModPlugin;

public class kol_battlestar extends OrbitalStation {
    @Override
    public boolean canDowngrade() {
        return false;
    }

    @Override
    public boolean canUpgrade() {
        if (Global.getSector() != null && Global.getSector().getPlayerFaction() != null) {
            if (Global.getSector().getPlayerFaction().getId().equals(KOL_ModPlugin.kolID)) return true;
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
