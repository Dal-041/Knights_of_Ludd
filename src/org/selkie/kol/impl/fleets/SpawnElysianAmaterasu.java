package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.fleets.ManageElysianAmaterasu;
import org.selkie.kol.impl.world.PrepareAbyss;

public class SpawnElysianAmaterasu {
	
	public static boolean SpawnElysianAmaterasu() {

		PersonAPI elysianBossCaptain = MagicCampaign.createCaptainBuilder(PrepareAbyss.dawnID).create();

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
		String variant = "abyss_boss_amaterasu_Blinding";
		CampaignFleetAPI elysianBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Amaterasu")
		        .setFleetFaction(PrepareAbyss.elysianID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("00000001")
		        .setFlagshipVariant(variant)
		        .setCaptain(elysianBossCaptain)
		        .setMinFP(240) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
				.setSpawnLocation(Global.getSector().getStarSystem("Elysia").getEntityById("abyss_elysia_abyss"))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		elysianBossFleet.setDiscoverable(true);
		for(String support : PrepareAbyss.elysianBossSupportingFleet) {
			elysianBossFleet.getFleetData().addFleetMember(support);
		}
		elysianBossFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		elysianBossFleet.getFleetData().sort();
		elysianBossFleet.addTag("abyss_rulesfortheebutnotforme");
		elysianBossFleet.addEventListener(new ManageElysianAmaterasu());

		return true;
	}
}
