package org.selkie.kol.campaign;

import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation;

public class kol_battlestar extends OrbitalStation {
    @Override
    public boolean canDowngrade() {
        return false;
    }

    @Override
    public boolean canUpgrade() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }
}
