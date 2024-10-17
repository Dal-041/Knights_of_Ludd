package org.selkie.kol.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import exerelin.campaign.SectorManager;
import lunalib.lunaUtil.campaign.LunaCampaignRenderer;
import mmm.missions.DefenseMission;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.selkie.kol.helpers.KolStaticStrings;
import org.selkie.kol.listeners.UpdateRelationships;
import org.selkie.kol.world.GenerateKnights;
import org.selkie.kol.world.GenerateMGA;
import org.selkie.zea.campaign.AICoreCampaignPlugin;
import org.selkie.zea.campaign.NullspaceVFXRenderer;
import org.selkie.zea.campaign.ZeaCampaignPlugin;
import org.selkie.zea.campaign.cores.AICoreDropReplacerScript;
import org.selkie.zea.campaign.cores.AICoreReplacerScript;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.zea.helpers.ZeaUtils;
import org.selkie.zea.listeners.ReportTransit;
import org.selkie.zea.world.PrepareAbyss;
import org.selkie.zea.world.PrepareDarkDeeds;

import java.util.Arrays;
import java.util.Set;

public class KOL_ModPlugin extends BaseModPlugin {


	public static final boolean haveNex = Global.getSettings().getModManager().isModEnabled(KolStaticStrings.NEXERELIN);
	public static final boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled(KolStaticStrings.SHADER_LIB);
	public static final boolean hasKOLGraphics = Global.getSettings().getModManager().isModEnabled(KolStaticStrings.KNIGHTS_OF_LUDD_MAPS);
	public static final boolean hasMMM = Global.getSettings().getModManager().isModEnabled(KolStaticStrings.MORE_MILITARY_MISSIONS);



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
			if(!Global.getSector().getMemoryWithoutUpdate().contains(KolStaticStrings.KolMemKeys.KOL_INTIALIZED)) {
				addToOngoingGame();
				Global.getSector().getMemoryWithoutUpdate().set(KolStaticStrings.KolMemKeys.KOL_INTIALIZED, true);
			}
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(KolStaticStrings.kolFactionID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(KolStaticStrings.kolFactionID);
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

		LunaCampaignRenderer.addTransientRenderer(new NullspaceVFXRenderer());

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
			GenerateMGA.genCorvus();
		}
		Global.getSector().getMemoryWithoutUpdate().set(KolStaticStrings.KolMemKeys.KOL_INTIALIZED, true);
	}
	

	@Override
	public void onNewGameAfterProcGen() {
		PrepareAbyss.generate();
		if (!haveNex || SectorManager.getManager().isCorvusMode()) {
			PrepareDarkDeeds.andBegin();
			if (!Global.getSector().getListenerManager().hasListenerOfClass(ReportTransit.class)) Global.getSector().getListenerManager().addListener(new ReportTransit(), true);
		}
		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(KolStaticStrings.kolFactionID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(KolStaticStrings.kolFactionID);
		}
		Global.getSector().getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_INTIALIZED, true);
		Global.getSector().addTransientListener(new UpdateRelationships(false));
		//Global.getSector().addTransientScript(new SpoilersNotif());
	}

	@Override
	public void onNewGameAfterTimePass() {
		GenerateKnights.genAlways();
	}

	protected void addToOngoingGame() {

		if (!SharedData.getData().getPersonBountyEventData().getParticipatingFactions().contains(KolStaticStrings.kolFactionID)) {
			SharedData.getData().getPersonBountyEventData().addParticipatingFaction(KolStaticStrings.kolFactionID);
		}
		Global.getSector().addTransientListener(new UpdateRelationships(false));
		if (!haveNex || SectorManager.getManager().isCorvusMode()) {
			GenerateKnights.genCorvus();
			GenerateMGA.genCorvus();
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
