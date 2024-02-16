package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.intel.ZeaAbilityIntel;
import org.selkie.kol.impl.helpers.ZeaUtils;

public class ManageElysianCorruptingheart implements FleetEventListener {
	public final String MEMKEY_KOL_ELYSIAN_BOSS2_DONE = "$kol_elysian_boss2_done";

	//Totally not adapted from Diable or anything :>
	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

		if(Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_KOL_ELYSIAN_BOSS2_DONE)
	                && Global.getSector().getMemoryWithoutUpdate().getBoolean(MEMKEY_KOL_ELYSIAN_BOSS2_DONE)) {
	            return;
		}

		boolean salvaged1 = false;
		boolean killed1 = false;
		if(fleet.getFlagship()==null || !fleet.getFlagship().getHullSpec().getBaseHullId().startsWith("zea_boss_corruptingheart")) {

			if(Global.getSector().getMemoryWithoutUpdate().contains("$kol_elysian_boss1_done")
					&& Global.getSector().getMemoryWithoutUpdate().getBoolean("$kol_elysian_boss1_done")) {
				if (!Global.getSector().getPlayerStats().getGrantedAbilityIds().contains(ZeaUtils.abilityJumpElysia)) {
					Global.getSector().getPlayerFleet().addAbility(ZeaUtils.abilityJumpElysia);
					Global.getSector().getCharacterData().getMemoryWithoutUpdate().set("$ability:" + ZeaUtils.abilityJumpElysia, true, 0);
					Global.getSector().getCharacterData().addAbility(ZeaUtils.abilityJumpElysia);

					ZeaAbilityIntel notif = new ZeaAbilityIntel(fleet.getFaction().getCrest(), "Elysia");
					Global.getSector().getIntelManager().addIntel(notif);
					notif.endAfterDelay(14);
				}
			}

			//remove the fleet if flag is dead
			if (!fleet.getMembersWithFightersCopy().isEmpty()) {
				SectorEntityToken source = fleet.getCurrentAssignment().getTarget();
				fleet.clearAssignments();
				fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 9999);
			}

			//boss is dead,
			killed1 = true;
		}

		if (killed1) {
			for (FleetMemberAPI f : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
				if (f.getHullId().startsWith("zea_boss_corruptingheart")) {
					salvaged1 = true;
					//set memkey that the wreck must never spawn
					Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_ELYSIAN_BOSS2_DONE, true);
					f.getVariant().removeTag(ZeaUtils.BOSS_TAG);
				}
			}

			//spawn a derelict if it wasn't salvaged
			if (!salvaged1) {
				//make sure there is a valid location to avoid spawning in the sun
				Vector2f location = fleet.getLocation();
				if(location == null){
					location = primaryWinner.getLocation();
				}
				//spawn the derelict object
				SectorEntityToken wreck = MagicCampaign.createDerelict(
						"zea_boss_corruptingheart_Unholy",
						ShipRecoverySpecial.ShipCondition.WRECKED,
						false,
						-1,
						true,
						//orbitCenter,angle,radius,period);
						fleet.getStarSystem().getCenter(), VectorUtils.getAngle(new Vector2f(), location), location.length(), 360);
				//MagicCampaign.placeOnStableOrbit(wreck, true);
				wreck.setName("Wreck of the Elysian flagship");
				wreck.setFacing((float) Math.random() * 360f);
				wreck.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);

				ZeaUtils.bossWreckCleaner(wreck, true);

				//set memkey that the wreck exist
				Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_KOL_ELYSIAN_BOSS2_DONE, true);
			}

			//Replacement cache
			LocationAPI system = primaryWinner.getContainingLocation();
			SectorEntityToken wreck = system.addCustomEntity(null, "Ejected Cache", Entities.EQUIPMENT_CACHE_SMALL, Factions.NEUTRAL);
			wreck.setFacing((float)Math.random()*360f);
			wreck.addDropRandom("guaranteed_alpha", 1);
			wreck.addDropRandom("zea_weapons_high", 6);
			wreck.addDropRandom("zea_weapons_high", 6);
			wreck.addDropRandom("techmining_first_find", 6);
			wreck.addDropRandom("omega_weapons_small", 3);
			wreck.addDropRandom("omega_weapons_medium", 2);
			wreck.addDropRandom("omega_weapons_large", 1);
			wreck.getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);
			wreck.setLocation(primaryWinner.getLocation().getX(), primaryWinner.getLocation().getY());
		}
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	    fleet.removeEventListener(this);
	}
}

