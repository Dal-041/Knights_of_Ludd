package org.selkie.zea.listeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.GateTransitListener;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.GateCMD;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.BaseScript;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import lunalib.lunaUtil.campaign.LunaCampaignRenderer;
import org.selkie.kol.ReflectionUtils;
import org.selkie.zea.campaign.NullspaceGateTransferVFXRenderer;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaEntities;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ReportTransit implements GateTransitListener {

	String memoryKey = "$kol_nullspace_gate_glitch";

	@Override
	public void reportFleetTransitingGate(CampaignFleetAPI fleet, SectorEntityToken gateFrom, SectorEntityToken gateTo) {
		StarSystemAPI destSys = Global.getSector().getStarSystem(ZeaStaticStrings.nullspaceSysName);
		if (Global.getSector().getMemoryWithoutUpdate().getKeys().contains(memoryKey)) return; //Dont trigger multiple times.
		if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$gaATG_missionCompleted")) return; //Prevent it from happening during the story-jump

		if (!fleet.isPlayerFleet() || gateFrom == null || Global.getSector().getClock().getCycle() <= 206 || gateFrom.getContainingLocation() == destSys) return;

		if (/*true*/ Math.random() <= 0.065f) { //0.05f, (Lukas) Increased it to 0.065 since it can no longer happen multiple times per save
			if (destSys == null) return;
			SectorEntityToken dest = destSys.getEntityById(ZeaEntities.ZEA_NULLGATE_DUSK);
			float dist = Misc.getDistanceLY(dest, gateTo);
			if (dist < 12f) {

				//Old Version

				/*fleet.getContainingLocation().removeEntity(fleet);
				dest.getContainingLocation().addEntity(fleet);
				Global.getSector().setCurrentLocation(dest.getContainingLocation());
				fleet.setLocation(dest.getLocation().x,
						dest.getLocation().y);
				fleet.setNoEngaging(1.0f);
				fleet.clearAssignments();*/







				//Remove the Transverse Script added by the GateCMD
				for (EveryFrameScript script : new ArrayList<>(Global.getSector().getScripts())) {
					if (ReflectionUtils.INSTANCE.hasVariableOfName("untilCanWarpOut", script)) {
						Global.getSector().removeScript(script);
					}
				}
				ReflectionUtils.INSTANCE.invoke("setInJumpTransition", fleet, new Object[]{false}, false);

				//Start own Transverse
				Global.getSector().doHyperspaceTransition(fleet, gateFrom, new JumpPointAPI.JumpDestination(dest, ""), 7.5f);

				//Add the VFX Renderer, will be removed after the jump.
				LunaCampaignRenderer.addRenderer(new NullspaceGateTransferVFXRenderer(gateFrom, true));

				//Disable VFX on the Gate Itself
				GateEntityPlugin plugin = (GateEntityPlugin) gateFrom.getCustomPlugin();
				FaderUtil fader = (FaderUtil) ReflectionUtils.get("beingUsedFader", plugin);
				plugin.showBeingUsed(0f, 0f);
				fader.forceOut();


				GateCMD.notifyScanned(dest);
				dest.getMemoryWithoutUpdate().set(GateEntityPlugin.GATE_SCANNED, true);

				Global.getSector().getMemoryWithoutUpdate().set(memoryKey, true);
			}
		}
	}
}
