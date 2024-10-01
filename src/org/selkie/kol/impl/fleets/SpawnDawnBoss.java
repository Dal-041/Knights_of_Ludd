package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.loading.VariantSource;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnDawnBoss {
	public final static String MEMKEY_DAWN_BOSS_FLEET = "$zea_nian";
	public static void SpawnDawnBoss() {

		PersonAPI dawnBossCaptain = ZeaFleetManager.createAICaptain(PrepareAbyss.dawnID);
		//A "slightly" rampant ALLMOTHER copy.
		dawnBossCaptain.setName(new FullName("XLLM01H3R", "", FullName.Gender.ANY));
		dawnBossCaptain.setPortraitSprite(Global.getSettings().getSpriteName("characters", ZeaStaticStrings.portraitDawnBoss));

        String variant = "zea_boss_nian_Salvation";
		CampaignFleetAPI dawnBossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("The Devourer")
		        .setFleetFaction(PrepareAbyss.dawnID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Nian")
		        .setFlagshipVariant(variant)
		        .setCaptain(dawnBossCaptain)
		        .setMinFP(0) //support fleet
		        .setQualityOverride(2f)
		        .setAssignment(FleetAssignment.PATROL_SYSTEM)
				.setSpawnLocation(Global.getSector().getStarSystem(PrepareAbyss.lunaSeaSysName).getEntityById(ZeaStaticStrings.ZEA_LUNASEA_PLANET_FOUR))
		        .setIsImportant(true)
		        .setTransponderOn(true)
		        .create();
		dawnBossFleet.setDiscoverable(true);
		dawnBossFleet.getFleetData().ensureHasFlagship();
		dawnBossFleet.getMemoryWithoutUpdate().set(MEMKEY_DAWN_BOSS_FLEET, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(ZeaStaticStrings.BOSS_TAG, true);

		//dawnBossFleet.removeAbility(Abilities.EMERGENCY_BURN);
		//fleet.removeAbility(Abilities.SENSOR_BURST);
		dawnBossFleet.removeAbility(Abilities.GO_DARK);

		// populate the fleet with escorts
		ZeaUtils.ZeaBossGenFleetWeaver(dawnBossFleet, 500);
		dawnBossFleet.getFleetData().sort();

		// make sure the escorts have cores
		ZeaFleetManager.setAICaptains(dawnBossFleet);

		// setup and permalock the flagship variant
		FleetMemberAPI flagship = dawnBossFleet.getFlagship();
		flagship.setVariant(flagship.getVariant().clone(), false, false);
		flagship.getVariant().setSource(VariantSource.REFIT);
		flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		flagship.getVariant().addTag(ZeaStaticStrings.BOSS_TAG);
		flagship.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);

		flagship.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		flagship.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("characters", ZeaStaticStrings.portraitDawnBoss));


		// to make sure they attack the player on sight when player's transponder is off
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		//dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);

		// add boss core to cargo
		CargoAPI cargo = Global.getFactory().createCargo(true);
		cargo.addCommodity(ZeaStaticStrings.BossCore.DAWN_CORE.itemID, 1);
		BaseSalvageSpecial.addExtraSalvage(dawnBossFleet, cargo);

		// tag to exclude terrain effects
		dawnBossFleet.addTag(excludeTag);

		// set up the initial interaction
		dawnBossFleet.addEventListener(new ManageDawnBoss());
		ZeaUtils.ZeaBossGenFIDConfig FID = new ZeaUtils.ZeaBossGenFIDConfig();
		FID.setAlwaysAttack(true);
		FID.setAlwaysPursue(true);
		FID.setLeaveAlwaysAvailable(false);
		FID.setWithSalvage(true);
		FID.aiRetreatToggle = false;
		FID.deployallToggle = true;
		FID.objectivesToggle = true;
		FID.fttlToggle = true;
		dawnBossFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, FID);
	}
}
