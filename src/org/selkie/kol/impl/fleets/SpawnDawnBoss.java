package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnDawnBoss {
	
	public static boolean SpawnDawnBoss() {

		PersonAPI dawnBossCaptain = ZeaFleetManager.createAbyssalCaptain(PrepareAbyss.dawnID);
		//A "slightly" rampant ALLMOTHER copy.
		FullName name = new FullName("XLLM01H3R", "", FullName.Gender.ANY);
		dawnBossCaptain.setName(name);
		dawnBossCaptain.setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/zea_boss_nian.png");

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
		String variant = "zea_boss_nian_Salvation";
		CampaignFleetAPI dawnBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("The Devourer")
		        .setFleetFaction(PrepareAbyss.dawnID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("00000010")
		        .setFlagshipVariant(variant)
		        .setCaptain(dawnBossCaptain)
		        .setMinFP(500) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.PATROL_SYSTEM)
				.setSpawnLocation(Global.getSector().getStarSystem(PrepareAbyss.lunaSeaSysName).getEntityById("zea_lunasea_four"))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		dawnBossFleet.setDiscoverable(true);

		//dawnBossFleet.removeAbility(Abilities.EMERGENCY_BURN);
		//fleet.removeAbility(Abilities.SENSOR_BURST);
		dawnBossFleet.removeAbility(Abilities.GO_DARK);
		// to make sure they attack the player on sight when player's transponder is off
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		//dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);

		ZeaFleetManager.setAbyssalCaptains(dawnBossFleet);
		dawnBossFleet.getFleetData().getCommander().setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/zea_boss_nian.png");

		dawnBossFleet.getFleetData().sort();
		dawnBossFleet.addTag(excludeTag);
		dawnBossFleet.getFlagship().getVariant().addTag("kol_boss");
		dawnBossFleet.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		dawnBossFleet.addEventListener(new ManageDawnBoss());

		return true;
	}
}
