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
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.kol.impl.listeners.ReportTransit;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;
import org.selkie.kol.impl.world.PrepareDarkDeeds;
import org.selkie.kol.listeners.UpdateRelationships;
import org.selkie.kol.world.GenerateKnights;

import exerelin.campaign.SectorManager;

import java.util.Arrays;
import java.util.Set;

public class KOL_ModPlugin extends BaseModPlugin {


	public static final boolean haveNex = Global.getSettings().getModManager().isModEnabled(ZeaStaticStrings.NEXERELIN);
	public static final boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled(ZeaStaticStrings.SHADER_LIB);
	public static final boolean hasKOLGraphics = Global.getSettings().getModManager().isModEnabled(ZeaStaticStrings.KNIGHTS_OF_LUDD_MAPS);
	public static final boolean hasMMM = Global.getSettings().getModManager().isModEnabled(ZeaStaticStrings.MORE_MILITARY_MISSIONS);



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
            BL.addAll(Arrays.asList(ZeaStaticStrings.factionIDs));
		}

		//LunaRefitManager.addRefitButton(new DuskCoreRefitButton());
	}

	@Override
	public void onGameLoad(boolean newGame) {
		if (!haveNex || SectorManager.getManager().isCorvusMode()) {
			if(!Global.getSector().getMemoryWithoutUpdate().contains(ZeaMemKeys.MEMKEY_KOL_INTIALIZED)) {
				addToOngoingGame();
				Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.MEMKEY_KOL_INTIALIZED, true);
			}
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(ZeaStaticStrings.kolFactionID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(ZeaStaticStrings.kolFactionID);
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
		while (!Global.getSector().getListenerManager().getListeners(UpdateRelationships.class).isEmpty()) {
			Global.getSector().getListenerManager().removeListenerOfClass(UpdateRelationships.class);
		}

		Global.getSector().addTransientListener(new UpdateRelationships(false));
	}

	@Override
	public void onNewGame() {
		if (!haveNex || SectorManager.getManager().isCorvusMode()) {
			GenerateKnights.genCorvus();
		}
		Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.MEMKEY_KOL_INTIALIZED, true);
	}

	@Override
	public void onNewGameAfterProcGen() {
		PrepareAbyss.generate();
		if (!haveNex || SectorManager.getManager().isCorvusMode()) {
			PrepareDarkDeeds.andBegin();
			if (!Global.getSector().getListenerManager().hasListenerOfClass(ReportTransit.class)) Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(ZeaStaticStrings.kolFactionID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(ZeaStaticStrings.kolFactionID);
		}
		Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.MEMKEY_ZEA_INTIALIZED, true);
		Global.getSector().addTransientListener(new UpdateRelationships(false));
		//Global.getSector().addTransientScript(new SpoilersNotif());
	}

	@Override
	public void onNewGameAfterTimePass() {
		GenerateKnights.genAlways();
	}

	protected void addToOngoingGame() {

		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(ZeaStaticStrings.kolFactionID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(ZeaStaticStrings.kolFactionID);
		}
		Global.getSector().addTransientListener(new UpdateRelationships(false));
		if (!haveNex || SectorManager.getManager().isCorvusMode()) {
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
