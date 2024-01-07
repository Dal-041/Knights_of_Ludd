package org.selkie.kol.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.selkie.kol.impl.listeners.ReportTransit;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.listeners.UpdateRelationships;
import org.selkie.kol.world.GenerateKnights;
import org.selkie.kol.fleets.SpawnInvictus;
import org.selkie.kol.fleets.SpawnRetribution;

import exerelin.campaign.SectorManager;

public class KOL_ModPlugin extends BaseModPlugin {
	public static final String ModID = "Knights of Ludd";
	public static final String kolID = "knights_of_selkie";

	public static boolean haveNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
	boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
	boolean hasKOLGraphics = Global.getSettings().getModManager().isModEnabled("knights_of_ludd_maps");

	public static final String MEMKEY_KOL_INVICTUS_SPAWNED = "$kol_lp_invictus_spawned";
	public static final String MEMKEY_KOL_RETRIBUTION_SPAWNED = "$kol_lp_retribution_spawned";

	@Override
	public void onApplicationLoad() {
		if (hasGraphicsLib && hasKOLGraphics) {
			ShaderLib.init();
			LightData.readLightDataCSV("data/lights/kol_light_data.csv");
			TextureData.readTextureDataCSV("data/lights/kol_texture_data.csv");
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
		Global.getSector().addTransientListener(new UpdateRelationships(true));
		Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
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
		Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
	}

	@Override
	public void onNewGameAfterTimePass() {
		GenerateKnights.genKnightsExpeditions();
	}
}
