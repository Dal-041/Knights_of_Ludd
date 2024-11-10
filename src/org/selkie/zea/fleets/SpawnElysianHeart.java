package org.selkie.zea.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.loading.VariantSource;
import org.magiclib.util.MagicCampaign;
import org.selkie.zea.helpers.ZeaStaticStrings;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaEntities;
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;
import org.selkie.zea.helpers.ZeaUtils;


public class SpawnElysianHeart {

	public static void SpawnElysianHeart() {

		PersonAPI elysianBossCaptain = ZeaFleetManager.createAICaptain(ZeaStaticStrings.elysianID);
		elysianBossCaptain.setName(new FullName("Corrupting", "Heart", FullName.Gender.ANY));
		elysianBossCaptain.setPortraitSprite(Global.getSettings().getSpriteName(GfxCat.CHARACTERS, ZeaStaticStrings.portraitElysianBoss));

		SectorEntityToken token;
		if (Math.random() > 0.7f) {
			token = Global.getSector().getStarSystem(ZeaStaticStrings.elysiaSysName).getEntityById(ZeaEntities.ZEA_ELYSIA_ABYSS);
		} else {
			token = Global.getSector().getStarSystem(ZeaStaticStrings.elysiaSysName).getEntityById(ZeaEntities.ZEA_ELYSIA_GAZE);
		}
		CampaignFleetAPI elysianHeartFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Corrupting Heart")
		        .setFleetFaction(ZeaStaticStrings.elysianID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Corrupting Heart")
		        .setFlagshipVariant(ZeaStaticStrings.ZEA_BOSS_CORRUPTINGHEART_UNHOLY)
		        .setCaptain(elysianBossCaptain)
		        .setMinFP(440) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.ORBIT_AGGRESSIVE)
				.setSpawnLocation(token)
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		elysianHeartFleet.setDiscoverable(true);
		elysianHeartFleet.getFleetData().ensureHasFlagship();
		elysianHeartFleet.getMemoryWithoutUpdate().set(ZeaMemKeys.ZEA_CORRUPTING_HEART_BOSS_FLEET, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);

		elysianHeartFleet.removeAbility(Abilities.EMERGENCY_BURN);
		//fleet.removeAbility(Abilities.SENSOR_BURST);
		elysianHeartFleet.removeAbility(Abilities.GO_DARK);

		// populate the fleet with escorts
		ZeaUtils.ZeaBossGenFleetWeaver(elysianHeartFleet, 440);
		elysianHeartFleet.getFleetData().sort();

		// make sure the escorts have cores
		ZeaFleetManager.setAICaptains(elysianHeartFleet);

		// setup and permalock the flagship variant
		FleetMemberAPI flagship = elysianHeartFleet.getFlagship();
		flagship.setVariant(flagship.getVariant().clone(), false, false);
		flagship.getVariant().setSource(VariantSource.REFIT);
		flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		flagship.getVariant().addTag(ZeaMemKeys.ZEA_BOSS_TAG);
		flagship.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);

		flagship.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		flagship.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName(GfxCat.CHARACTERS, ZeaStaticStrings.portraitElysianBoss));


		// to make sure they attack the player on sight when player's transponder is off
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		//elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

		// add boss core to cargo
		CargoAPI cargo = Global.getFactory().createCargo(true);
		cargo.addCommodity(ZeaStaticStrings.BossCore.ELYSIAN_CORE.itemID, 1);
		BaseSalvageSpecial.addExtraSalvage(elysianHeartFleet, cargo);

		// tag to exclude terrain effects
		elysianHeartFleet.addTag(ZeaStaticStrings.ZEA_EXCLUDE_TAG);

		// manage the wreck
		elysianHeartFleet.addEventListener(new ManageElysianCorruptingheart());

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
		elysianHeartFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, FID);
	}
}
