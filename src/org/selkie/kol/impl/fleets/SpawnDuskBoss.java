package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnDuskBoss {
	
	public static boolean SpawnDuskBoss() {

		PersonAPI duskBossCaptain = ZeaFleetManager.createAICaptain(PrepareAbyss.duskID);
		//Songtress, an experitmental AI who was once human.
		FullName name = new FullName("Songtress", "", FullName.Gender.FEMALE);
		duskBossCaptain.setName(name);
		duskBossCaptain.setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/zea_boss_alphaplus.png");

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
		String variant = "zea_boss_yukionna_Ultimate";
		CampaignFleetAPI duskBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Yukionna")
		        .setFleetFaction(PrepareAbyss.duskID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Yukionna")
		        .setFlagshipVariant(variant)
		        .setCaptain(duskBossCaptain)
		        .setMinFP(0) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.DEFEND_LOCATION)
				.setSpawnLocation(Global.getSector().getStarSystem(PrepareAbyss.nullspaceSysName).getCenter())
		        .setIsImportant(true)
		        .setTransponderOn(false)
		        .create();
		duskBossFleet.setDiscoverable(true);
		duskBossFleet.getFleetData().ensureHasFlagship();
		duskBossFleet.getMemoryWithoutUpdate().set("$zea_yukionna", true);
		duskBossFleet.getFlagship().getVariant().addTag(ZeaUtils.BOSS_TAG); //Now confirmed by fleet rule.
		duskBossFleet.getFlagship().getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		duskBossFleet.getFlagship().getVariant().addTag(Tags.VARIANT_UNBOARDABLE); //Now confirmed by fleet rule.
		duskBossFleet.getFlagship().getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		//duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);

		ZeaUtils.ZeaBossGenFleetWeaver(duskBossFleet, 360);

		for(String support : ZeaUtils.duskBossSupportingFleet) {
			duskBossFleet.getFleetData().addFleetMember(support);
		}

		ZeaFleetManager.setAICaptains(duskBossFleet);
		duskBossFleet.getFlagship().getCaptain().setPortraitSprite("data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/portraits/zea_boss_alphaplus.png");

		duskBossFleet.getFleetData().sort();

		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		duskBossFleet.addTag(excludeTag);
		duskBossFleet.addEventListener(new ManageDuskBoss());
		ZeaUtils.ZeaBossGenFIDConfig FID = new ZeaUtils.ZeaBossGenFIDConfig();
		FID.setAlwaysAttack(false);
		FID.setAlwaysPursue(true);
		FID.setLeaveAlwaysAvailable(false);
		FID.setWithSalvage(true);
		FID.aiRetreatToggle = false;
		FID.deployallToggle = true;
		FID.objectivesToggle = true;
		FID.fttlToggle = false;
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, FID);

		return true;
	}
}
