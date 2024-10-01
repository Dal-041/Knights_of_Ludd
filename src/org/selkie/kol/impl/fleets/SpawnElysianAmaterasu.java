package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.loading.VariantSource;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaUtils;


public class SpawnElysianAmaterasu {
	
	public static void SpawnElysianAmaterasu() {

		PersonAPI elysianBossCaptain = ZeaFleetManager.createAICaptain(ZeaStaticStrings.elysianID);
		elysianBossCaptain.setName(new FullName("Amaterasu", "", FullName.Gender.ANY));
		elysianBossCaptain.setPortraitSprite(Global.getSettings().getSpriteName(ZeaStaticStrings.CHARACTERS, ZeaStaticStrings.portraitAmaterasuBoss));

		CampaignFleetAPI elysianBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Amaterasu")
		        .setFleetFaction(ZeaStaticStrings.elysianID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Amaterasu")
		        .setFlagshipVariant(ZeaStaticStrings.ZEA_BOSS_AMATERASU_BLINDING)
		        .setCaptain(elysianBossCaptain)
		        .setMinFP(0) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
				.setSpawnLocation(Global.getSector().getStarSystem(ZeaStaticStrings.elysiaSysName).getEntityById(ZeaStaticStrings.ZEA_EDF_CORONAL_TAP))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		elysianBossFleet.setDiscoverable(true);
		elysianBossFleet.getFleetData().ensureHasFlagship();
		elysianBossFleet.getMemoryWithoutUpdate().set(ZeaStaticStrings.MemKeys.MEMKEY_ZEA_AMATERASU_BOSS_FLEET, true);

		elysianBossFleet.removeAbility(Abilities.EMERGENCY_BURN);
		//fleet.removeAbility(Abilities.SENSOR_BURST);
		elysianBossFleet.removeAbility(Abilities.GO_DARK);

		// populate the fleet with escorts
		ZeaUtils.ZeaBossGenFleetWeaver(elysianBossFleet, 440);
		elysianBossFleet.getFleetData().sort();

		// make sure the escorts have cores
		ZeaFleetManager.setAICaptains(elysianBossFleet);

		// setup and permalock the flagship variant
		FleetMemberAPI flagship = elysianBossFleet.getFlagship();
		flagship.setVariant(flagship.getVariant().clone(), false, false);
		flagship.getVariant().setSource(VariantSource.REFIT);
		flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		flagship.getVariant().addTag(ZeaStaticStrings.MemKeys.BOSS_TAG);
		flagship.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);

		flagship.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		flagship.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName(ZeaStaticStrings.CHARACTERS, ZeaStaticStrings.portraitAmaterasuBoss));


		// to make sure they attack the player on sight when player's transponder is off
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		elysianBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

		// tag to exclude terrain effects
		elysianBossFleet.addTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG);

		// manage the wreck
		elysianBossFleet.addEventListener(new ManageElysianAmaterasu());

		// set up the initial interaction
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
	}
}
