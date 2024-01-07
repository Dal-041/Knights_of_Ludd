package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.fleets.ManageElysianCorruptingheart;
import org.selkie.kol.impl.world.PrepareAbyss;

public class SpawnElysianHeart {
	
	public static boolean SpawnElysianHeart() {

		PersonAPI elysianBossCaptain = AbyssalFleetManager.createAbyssalCaptain(PrepareAbyss.elysianID);

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
		String variant = "abyss_boss_corruptingheart_Unholy";
		CampaignFleetAPI elysianHeartFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Corrupting Heart")
		        .setFleetFaction(PrepareAbyss.elysianID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("00000001")
		        .setFlagshipVariant(variant)
		        .setCaptain(elysianBossCaptain)
		        .setMinFP(240) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
				.setSpawnLocation(Global.getSector().getStarSystem("Elysia").getEntityById("abyss_elysia_silence"))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		elysianHeartFleet.setDiscoverable(true);
		for(String support : PrepareAbyss.elysianBossSupportingFleet) {
			elysianHeartFleet.getFleetData().addFleetMember(support);
		}
		elysianHeartFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		elysianHeartFleet.getFleetData().sort();

		elysianHeartFleet.removeAbility(Abilities.EMERGENCY_BURN);
		//fleet.removeAbility(Abilities.SENSOR_BURST);
		elysianHeartFleet.removeAbility(Abilities.GO_DARK);

		// to make sure they attack the player on sight when player's transponder is off
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

		elysianHeartFleet.addTag("abyss_rulesfortheebutnotforme");
		elysianHeartFleet.addEventListener(new ManageElysianCorruptingheart());

		return true;
	}
}
