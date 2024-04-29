package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnElysianAmaterasu {
	
	public static boolean SpawnElysianAmaterasu() {

		PersonAPI elysianBossCaptain = ZeaFleetManager.createAICaptain(PrepareAbyss.elysianID);
		elysianBossCaptain.setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/zea_boss_amaterasu.png");
		elysianBossCaptain.setName(new FullName("Amaterasu", "", FullName.Gender.ANY));

		/**
		* Creates a fleet with a defined flagship and optional escort
		*
		* @param fleetName
		* @param fleetFaction
		* @param fleetType
		* campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
		* @param flagshipName
		* Optional flagship name
		* @param flagshipVariant
		* @param captain
		* PersonAPI, can be NULL for random captain, otherwise use createCaptain()
		* @param supportFleet
		* Optional escort ship VARIANTS and their NUMBERS
		* @param minFP
		* Minimal fleet size, can be used to adjust to the player's power, set to 0 to ignore
		* @param reinforcementFaction
		* Reinforcement faction, if the fleet faction is a "neutral" faction without ships
		* @param qualityOverride
		* Optional ship quality override, default to 2 (no D-mods) if null or <0
		* @param spawnLocation
		* Where the fleet will spawn, default to assignmentTarget if NULL
		* @param assignment
		* campaign.FleetAssignment, default to orbit aggressive
		* @param assignementTarget
		* @param isImportant
		* @param transponderOn
		* @return
		*/
		String variant = "zea_boss_amaterasu_Blinding";
		CampaignFleetAPI elysianBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Amaterasu")
		        .setFleetFaction(PrepareAbyss.elysianID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Amaterasu")
		        .setFlagshipVariant(variant)
		        .setCaptain(elysianBossCaptain)
		        .setMinFP(0) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
				.setSpawnLocation(Global.getSector().getStarSystem(PrepareAbyss.elysiaSysName).getCustomEntitiesWithTag("edf_Hypershunt").get(0))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		elysianBossFleet.setDiscoverable(true);
		elysianBossFleet.getFleetData().ensureHasFlagship();
		elysianBossFleet.getMemoryWithoutUpdate().set("$zea_amaterasu", true);
		elysianBossFleet.getFlagship().getVariant().addTag(ZeaUtils.BOSS_TAG);
		elysianBossFleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
		elysianBossFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);

		ZeaUtils.ZeaBossGenFleetWeaver(elysianBossFleet, 440);

		for(String support : ZeaUtils.elysianBossSupportingFleet) {
			//elysianBossFleet.getFleetData().addFleetMember(support);
		}

		ZeaFleetManager.setAICaptains(elysianBossFleet);
		elysianBossFleet.getFlagship().getCaptain().setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/zea_boss_amaterasu.png");
		elysianBossFleet.getFleetData().sort();

		elysianBossFleet.removeAbility(Abilities.EMERGENCY_BURN);
		//fleet.removeAbility(Abilities.SENSOR_BURST);
		elysianBossFleet.removeAbility(Abilities.GO_DARK);
		// to make sure they attack the player on sight when player's transponder is off
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);

		elysianBossFleet.addTag(excludeTag);
		elysianBossFleet.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		elysianBossFleet.addEventListener(new ManageElysianAmaterasu());
		ZeaUtils.ZeaBossGenFIDConfig FID = new ZeaUtils.ZeaBossGenFIDConfig();
		FID.setAlwaysAttack(true);
		FID.setAlwaysPursue(true);
		FID.setLeaveAlwaysAvailable(false);
		FID.setWithSalvage(true);
		FID.aiRetreatToggle = false;
		FID.deployallToggle = true;
		FID.objectivesToggle = true;
		FID.fttlToggle = true;



		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, FID);

		return true;
	}
}
