package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.fleets.ManageDuskBoss;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.plugins.AbyssUtils.fixVariant;
import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnDuskBoss {
	
	public static boolean SpawnDuskBoss() {

		PersonAPI duskBossCaptain = AbyssalFleetManager.createAbyssalCaptain(PrepareAbyss.duskID);
		//Songtress, an experitmental AI who was once human.
		FullName name = new FullName("Songtress", "", FullName.Gender.FEMALE);
		duskBossCaptain.setName(name);
		duskBossCaptain.setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/abyss_boss_alphaplus.png");

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
		String variant = "abyss_boss_yukionna_Ultimate";
		CampaignFleetAPI duskBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Yukionna")
		        .setFleetFaction("kol_dusk")
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("00000000")
		        .setFlagshipVariant(variant)
		        .setCaptain(duskBossCaptain)
		        .setMinFP(180) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.DEFEND_LOCATION)
				.setSpawnLocation(Global.getSector().getStarSystem("Underspace").getCenter())
		        .setIsImportant(true)
		        .setTransponderOn(false)
		        .create();
		duskBossFleet.setDiscoverable(true);

		for(String support : PrepareAbyss.duskBossSupportingFleet) {
			duskBossFleet.getFleetData().addFleetMember(support);
		}

		AbyssalFleetManager.setAbyssalCaptains(duskBossFleet);
		duskBossFleet.getFlagship().getCaptain().setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/abyss_boss_alphaplus.png");

		duskBossFleet.getFleetData().sort();

		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		duskBossFleet.addTag(excludeTag);
		duskBossFleet.addEventListener(new ManageDuskBoss());

		return true;
	}
}
