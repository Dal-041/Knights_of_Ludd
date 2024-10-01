package org.selkie.kol.impl.fleets;

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
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnElysianHeart {
	public final static String MEMKEY_ELYSIAN_BOSS_FLEET_2 = "$zea_corruptingheart";
	public static void SpawnElysianHeart() {

		PersonAPI elysianBossCaptain = ZeaFleetManager.createAICaptain(PrepareAbyss.elysianID);
		elysianBossCaptain.setName(new FullName("Corrupting", "Heart", FullName.Gender.ANY));
		elysianBossCaptain.setPortraitSprite(Global.getSettings().getSpriteName("characters", ZeaStaticStrings.portraitElysianBoss));

        String variant = "zea_boss_corruptingheart_Unholy";
		SectorEntityToken token;
		if (Math.random() > 0.7f) {
			token = Global.getSector().getStarSystem(PrepareAbyss.elysiaSysName).getEntityById("zea_elysia_abyss");
		} else {
			token = Global.getSector().getStarSystem(PrepareAbyss.elysiaSysName).getEntityById("zea_elysia_gaze");
		}
		CampaignFleetAPI elysianHeartFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Corrupting Heart")
		        .setFleetFaction(PrepareAbyss.elysianID)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("Corrupting Heart")
		        .setFlagshipVariant(variant)
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
		elysianHeartFleet.getMemoryWithoutUpdate().set(MEMKEY_ELYSIAN_BOSS_FLEET_2, true);

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
		flagship.getVariant().addTag(ZeaStaticStrings.BOSS_TAG);
		flagship.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);

		flagship.getStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat("NoNormalRecovery", -2000);
		flagship.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("characters", ZeaStaticStrings.portraitElysianBoss));


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
		elysianHeartFleet.addTag(excludeTag);

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
