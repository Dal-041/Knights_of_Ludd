package org.selkie.kol.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import org.selkie.kol.listeners.updateRelationships;
import org.selkie.kol.world.Generate;
import org.selkie.kol.world.SpawnInvictus;
import org.selkie.kol.world.SpawnRetribution;

import exerelin.campaign.SectorManager;

public class KOL_ModPlugin extends BaseModPlugin {
	public static String ModID = "Knights of Ludd";
	public boolean haveNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
	
	public static String MEMKEY_KOL_INVICTUS_SPAWNED = "$kol_lp_invictus_spawned";
	public static String MEMKEY_KOL_RETRIBUTION_SPAWNED = "$kol_lp_retribution_spawned";
	
	@Override
	public void onGameLoad(boolean newGame) {
		Global.getSector().addTransientListener(new updateRelationships(true));
		if (!haveNex || (haveNex && SectorManager.getManager().isCorvusMode())) {
			if(!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_INVICTUS_SPAWNED)) {
				SpawnInvictus.spawnInvictus();
				Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_INVICTUS_SPAWNED, true);
			}
			if(!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_RETRIBUTION_SPAWNED)) {
				SpawnRetribution.spawnRetribution();
				Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_RETRIBUTION_SPAWNED, true);
			}
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains("knights of selkie")) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction("knights_of_selkie");
		}
	}
	
	@Override
	public void onNewGameAfterEconomyLoad() {
		if (!haveNex || (haveNex && SectorManager.getManager().isCorvusMode())) {
			Generate.zugg();
			SpawnInvictus.spawnInvictus();
			SpawnRetribution.spawnRetribution();
			Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_INVICTUS_SPAWNED, true);
			Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_RETRIBUTION_SPAWNED, true);
		}
		Global.getSector().addTransientListener(new updateRelationships(true));
	}
}
