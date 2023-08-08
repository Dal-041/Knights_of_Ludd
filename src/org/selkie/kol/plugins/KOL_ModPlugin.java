package org.selkie.kol.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.listeners.UpdateRelationships;
import org.selkie.kol.world.GenerateKnights;
import org.selkie.kol.world.SpawnInvictus;
import org.selkie.kol.world.SpawnRetribution;

import exerelin.campaign.SectorManager;

public class KOL_ModPlugin extends BaseModPlugin {
	public static final String ModID = "Knights of Ludd";
	public static final String kolID = "knights_of_selkie";
	public static final String duskID = "kol_dusk";

	public static boolean haveNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
	boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
	
	public static final String MEMKEY_KOL_INVICTUS_SPAWNED = "$kol_lp_invictus_spawned";
	public static final String MEMKEY_KOL_RETRIBUTION_SPAWNED = "$kol_lp_retribution_spawned";

	@Override
	public void onApplicationLoad() {
		if (hasGraphicsLib) {
			ShaderLib.init();
			LightData.readLightDataCSV("data/lights/abyss_light_data.csv");
			TextureData.readTextureDataCSV("data/lights/abyss_texture_data.csv");
		}
	}

	@Override
	public void onGameLoad(boolean newGame) {
		Global.getSector().addTransientListener(new UpdateRelationships(true));
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
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(kolID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(kolID);
		}
	}
	
	@Override
	public void onNewGameAfterEconomyLoad() {
		if (!haveNex || (haveNex && SectorManager.getManager().isCorvusMode())) {
			GenerateKnights.zugg();
			PrepareAbyss.generate(Global.getSector());
			SpawnInvictus.spawnInvictus();
			SpawnRetribution.spawnRetribution();
			Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_INVICTUS_SPAWNED, true);
			Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_RETRIBUTION_SPAWNED, true);
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(kolID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(kolID);
		}
		Global.getSector().addTransientListener(new UpdateRelationships(true));
	}
}
