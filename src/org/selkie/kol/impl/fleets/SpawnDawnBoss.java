package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.fleets.ManageDawnBoss;
import org.selkie.kol.impl.world.PrepareAbyss;

public class SpawnDawnBoss {
	
	public static boolean SpawnDuskBoss() {

		PersonAPI dawnBossCaptain = MagicCampaign.createCaptainBuilder("kol_dawn").create();

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
		String variant = "abyss_boss_dokkaebi_Fiend";
		CampaignFleetAPI dawnBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("The Fiend")
		        .setFleetFaction(PrepareAbyss.dawnID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("00000011")
		        .setFlagshipVariant(variant)
		        .setCaptain(dawnBossCaptain)
		        .setMinFP(300) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
				.setSpawnLocation(Global.getSector().getStarSystem("Luna Sea").getEntityById("abyss_lunasea_jp"))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		dawnBossFleet.setDiscoverable(true);
		dawnBossFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		dawnBossFleet.getFleetData().sort();
		dawnBossFleet.addTag("abyss_rulesfortheebutnotforme");
		dawnBossFleet.addEventListener(new ManageDawnBoss());

		return true;
	}
}
