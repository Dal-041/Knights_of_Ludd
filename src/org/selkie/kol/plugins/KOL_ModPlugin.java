package org.selkie.kol.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;

import mmm.missions.DefenseMission;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.selkie.kol.impl.campaign.AICoreCampaignPlugin;
import org.selkie.kol.impl.campaign.ZeaCampaignPlugin;
import org.selkie.kol.impl.campaign.cores.AICoreDropReplacerScript;
import org.selkie.kol.impl.campaign.cores.AICoreReplacerScript;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.listeners.ReportTransit;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.impl.world.PrepareDarkDeeds;
import org.selkie.kol.listeners.UpdateRelationships;
import org.selkie.kol.world.GenerateKnights;

import exerelin.campaign.SectorManager;

import java.util.Objects;
import java.util.Set;

public class KOL_ModPlugin extends BaseModPlugin {
	public static final String ModID = "Knights of Ludd";
	public static final String kolID = "knights_of_selkie";

	public static boolean haveNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
	public static boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
	public static boolean hasKOLGraphics = Global.getSettings().getModManager().isModEnabled("knights_of_ludd_maps");
	public static boolean hasMMM = Global.getSettings().getModManager().isModEnabled("MoreMilitaryMissions");

	public static final String MEMKEY_KOL_INTIALIZED = "$kol_initialized";
	public static final String MEMKEY_ZEA_INTIALIZED = "$zea_initialized";
	public static final String MEMKEY_CURRENT_VERSION = "$kol_version";

	@Override
	public void onApplicationLoad() {
		if (hasGraphicsLib) {
			ShaderLib.init();
			LightData.readLightDataCSV("data/lights/kol_light_data.csv");
			TextureData.readTextureDataCSV("data/lights/kol_glib_normals.csv");
			if (hasKOLGraphics) {
				TextureData.readTextureDataCSV("data/lights/kol_glib_extended.csv");
			}
		}
		if (hasMMM) { //Remove after week 1
			Set<String> BL = DefenseMission.FACTION_BLACKLIST;
			for (String faction : ZeaStaticStrings.factionIDs) {
				if (!BL.contains(faction)) BL.add(faction);
			}
		}

		//LunaRefitManager.addRefitButton(new DuskCoreRefitButton());
	}

	@Override
	public void onGameLoad(boolean newGame) {
		if (!haveNex || (haveNex && SectorManager.getManager().isCorvusMode())) {
			if(!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_INTIALIZED)) {
				addToOngoingGame();
				Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_INTIALIZED, true);
			}
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(kolID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(kolID);
		}
		GenerateKnights.copyChurchEquipment();
		GenerateKnights.addKoLIntel();
		ZeaUtils.checkAbyssalFleets();
		ZeaUtils.copyHighgradeEquipment();
		PrepareDarkDeeds.andContinue();

		if (!Global.getSector().getListenerManager().hasListenerOfClass(ReportTransit.class)) Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
		//Global.getSector().addTransientScript(new SpoilersNotif());

		Global.getSector().registerPlugin(new ZeaCampaignPlugin());
		Global.getSector().registerPlugin(new AICoreCampaignPlugin());
		Global.getSector().addTransientScript(new AICoreReplacerScript());
		Global.getSector().addTransientListener(new AICoreDropReplacerScript());

		//Handle very silly bug duplicating listeners
		while (Global.getSector().getListenerManager().getListeners(UpdateRelationships.class).size() > 0) {
			Global.getSector().getListenerManager().removeListenerOfClass(UpdateRelationships.class);
		}

		Global.getSector().addTransientListener(new UpdateRelationships(false));

		// handle version updates
		if(Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_CURRENT_VERSION)){
			String saveGameVersion = (String) Global.getSector().getMemoryWithoutUpdate().get(MEMKEY_CURRENT_VERSION);
			if (Objects.equals(saveGameVersion, "1.3")) return;
		} else{
			ZeaUtils.versionUpdate1_2to1_3();
			Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_CURRENT_VERSION, "1.3");
		}
	}

	@Override
	public void onNewGame() {
		if (!haveNex || (haveNex && SectorManager.getManager().isCorvusMode())) {
			GenerateKnights.genCorvus();
		}
		Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_INTIALIZED, true);
		Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_CURRENT_VERSION, "1.3");
	}

	@Override
	public void onNewGameAfterProcGen() {
		PrepareAbyss.generate();
		if (!haveNex || (haveNex && SectorManager.getManager().isCorvusMode())) {
			PrepareDarkDeeds.andBegin();
			if (!Global.getSector().getListenerManager().hasListenerOfClass(ReportTransit.class)) Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(kolID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(kolID);
		}
		Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_ZEA_INTIALIZED, true);
		Global.getSector().addTransientListener(new UpdateRelationships(false));
		//Global.getSector().addTransientScript(new SpoilersNotif());
	}

	@Override
	public void onNewGameAfterTimePass() {
		GenerateKnights.genAlways();
	}

	protected void addToOngoingGame() {
		Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_CURRENT_VERSION, "1.3");

		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(kolID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(kolID);
		}
		Global.getSector().addTransientListener(new UpdateRelationships(false));
		if (!haveNex || haveNex && SectorManager.getManager().isCorvusMode()) {
			GenerateKnights.genCorvus();
			PrepareAbyss.generate();
			if (!Global.getSector().getListenerManager().hasListenerOfClass(ReportTransit.class)) Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
			PrepareDarkDeeds.andBegin();
			PrepareDarkDeeds.andContinue();
		}
		GenerateKnights.genAlways();
	}

	@Override
	public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
		return KOL_ShipAITweaker.pickShipAI(member, ship);
	}
}
