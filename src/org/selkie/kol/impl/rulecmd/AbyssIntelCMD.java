package org.selkie.kol.impl.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.intel.TriTachBreadcrumbIntel;
import org.selkie.kol.impl.world.PrepareDarkDeeds;

import java.util.List;
import java.util.Map;

public class AbyssIntelCMD extends BaseCommandPlugin {
    SectorEntityToken TTStationBoss2;
    SectorEntityToken TTBoss3System = null;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        SectorEntityToken entity = dialog.getInteractionTarget();
        if (entity.getMarket() != null && !entity.getMarket().isPlanetConditionMarketOnly()) {
            PlanetAPI planet = entity.getMarket().getPlanetEntity();
            if (planet != null) {
                entity = planet;
            }
        }

        if ("addIntelTTBoss1".equals(command)) {
            if (Global.getSector().getEntityById("zea_boss_station_tritachyon") != null) {
                TTStationBoss2 = Global.getSector().getEntityById("zea_boss_station_tritachyon");
            } else {
                return false;
            }
            TriTachBreadcrumbIntel intel = new TriTachBreadcrumbIntel("Recovered Tri-Tachyon Navigation log", "", TTStationBoss2);
            Global.getSector().getIntelManager().addIntel(intel, false, dialog.getTextPanel());
            return true;
        } else if ("addIntelTTBoss2".equals(command)) {
            for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                if (system.getMemoryWithoutUpdate().contains(PrepareDarkDeeds.TTBOSS3_SYSTEM_KEY)) {
                    TTBoss3System = system.getCenter();
                }
            }
            if (TTBoss3System == null) return false;
            TriTachBreadcrumbIntel intel = new TriTachBreadcrumbIntel("Recovered Project Dusk Datacore", "", TTBoss3System);
            Global.getSector().getIntelManager().addIntel(intel, false, dialog.getTextPanel());
            return true;
        } else if ("addIntelTTBoss3".equals(command)) {

        } else if ("endMusic".equals(command)) {
            Global.getSoundPlayer().restartCurrentMusic();
            return true;
        }

        return false;
    }
}
