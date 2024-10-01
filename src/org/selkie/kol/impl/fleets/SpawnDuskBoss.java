package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.loading.VariantSource;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaUtils;


public class SpawnDuskBoss {

	public static void SpawnDuskBoss() {

		PersonAPI duskBossCaptain = ZeaFleetManager.createAICaptain(ZeaStaticStrings.duskID);
		//Songtress, an experitmental AI who was once human.
		duskBossCaptain.setName(new FullName("Songtress", "", FullName.Gender.FEMALE));
		duskBossCaptain.setPortraitSprite(Global.getSettings().getSpriteName(ZeaStaticStrings.CHARACTERS, ZeaStaticStrings.portraitDuskBoss));

		CampaignFleetAPI duskBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Yukionna")
		        .setFleetFaction(ZeaStaticStrings.duskID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Yukionna")
		        .setFlagshipVariant(ZeaStaticStrings.ZEA_BOSS_YUKIONNA_ULTIMATE)
		        .setCaptain(duskBossCaptain)
		        .setMinFP(0) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.DEFEND_LOCATION)
				.setSpawnLocation(Global.getSector().getStarSystem(ZeaStaticStrings.nullspaceSysName).getCenter())
		        .setIsImportant(true)
		        .setTransponderOn(false)
		        .create();
		duskBossFleet.setDiscoverable(true);
		duskBossFleet.getFleetData().ensureHasFlagship();
		duskBossFleet.getMemoryWithoutUpdate().set(ZeaStaticStrings.MemKeys.MEMKEY_ZEA_DUSK_BOSS_FLEET, true);
		duskBossFleet.getMemoryWithoutUpdate().set(ZeaStaticStrings.MemKeys.BOSS_TAG, true);

		// populate the fleet with escorts
		ZeaUtils.ZeaBossGenFleetWeaver(duskBossFleet, 360);
		for(String support : ZeaStaticStrings.duskBossSupportingFleet) {
			duskBossFleet.getFleetData().addFleetMember(support);
		}
		duskBossFleet.getFleetData().sort();

		// make sure the escorts have cores
		ZeaFleetManager.setAICaptains(duskBossFleet);

		// setup and permalock the flagship variant
		FleetMemberAPI flagship = duskBossFleet.getFlagship();
		flagship.setVariant(flagship.getVariant().clone(), false, false);
		flagship.getVariant().setSource(VariantSource.REFIT);
		flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		flagship.getVariant().addTag(ZeaStaticStrings.MemKeys.BOSS_TAG); //Now confirmed by fleet rule.
		flagship.getVariant().addTag(Tags.VARIANT_UNBOARDABLE); //Now confirmed by fleet rule.

		flagship.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		flagship.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName(ZeaStaticStrings.CHARACTERS, ZeaStaticStrings.portraitDuskBoss));

		// to make sure they attack the player on sight when player's transponder is off
		//duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		//duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		//duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		duskBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

		// add boss core to cargo
		CargoAPI cargo = Global.getFactory().createCargo(true);
		cargo.addCommodity(ZeaStaticStrings.BossCore.DUSK_CORE.itemID, 1);
		BaseSalvageSpecial.addExtraSalvage(duskBossFleet, cargo);

		// tag to exclude terrain effects
		duskBossFleet.addTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG);

		// set up the initial interaction
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

	}
}
