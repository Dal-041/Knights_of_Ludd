package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicCampaign;
import org.selkie.kol.impl.world.PrepareDarkDeeds;

import java.util.HashMap;
import java.util.Map;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class SpawnTT1Boss {
	
	public static boolean SpawnTT1Boss() {

		Map<String, Integer> skills = new HashMap<>();
		skills.put(Skills.HELMSMANSHIP, 2);
		skills.put(Skills.COMBAT_ENDURANCE, 2);
		skills.put(Skills.FIELD_MODULATION, 2);
		skills.put(Skills.TARGET_ANALYSIS, 2);
		skills.put(Skills.SYSTEMS_EXPERTISE, 2);
		skills.put(Skills.GUNNERY_IMPLANTS, 2);
		skills.put(Skills.ENERGY_WEAPON_MASTERY, 2);

		PersonAPI TT1BossCaptain = MagicCampaign.createCaptainBuilder(Factions.TRITACHYON)
				.setLevel(7)
				.setPersonality(Personalities.AGGRESSIVE)
				.setSkillLevels(skills)
				.create();

		TT1BossCaptain.getStats().setSkipRefresh(true);
		TT1BossCaptain.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
		TT1BossCaptain.getStats().setSkipRefresh(false);

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
		String variant = "zea_boss_ninaya_Nightdemon";
		SectorEntityToken token = Global.getSector().getStarSystem("Unknown Location").createToken(11111,11111); //cache loc
		CampaignFleetAPI TT1BossFleet = MagicCampaign.createFleetBuilder()
		        .setFleetName("Unidentified Vessel")
		        .setFleetFaction(Factions.TRITACHYON)
		        .setFleetType(FleetTypes.TASK_FORCE)
		        .setFlagshipName("TTS Ninaya")
		        .setFlagshipVariant(variant)
		        .setCaptain(TT1BossCaptain)
		        .setMinFP(0) //support fleet
		        .setQualityOverride(5f)
		        .setAssignment(FleetAssignment.DEFEND_LOCATION)
				.setSpawnLocation(token)
		        .setIsImportant(true)
		        .setTransponderOn(false)
		        .create();
		TT1BossFleet.setDiscoverable(true);

		/*
		for(String support : PrepareAbyss.duskBossSupportingFleet) {
			duskBossFleet.getFleetData().addFleetMember(support);
		}*/

		TT1BossFleet.setNoFactionInName(true);
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_FLEET, true); // so it keeps transponder on
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
		TT1BossFleet.getMemoryWithoutUpdate().set("$zea_nineveh", true);

		TT1BossFleet.getFleetData().sort();

		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.CAN_ONLY_BE_ENGAGED_WHEN_VISIBLE_TO_PLAYER, true);
		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		TT1BossFleet.addTag(excludeTag);

		//Using FID config instead
		//TT1BossFleet.addEventListener(new ManageTT1Boss());

		TT1BossFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
				new TT1FIDConfig());

		return true;
	}

	public static class TT1FIDConfig implements FleetInteractionDialogPluginImpl.FIDConfigGen {
		public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
			FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

//			config.alwaysAttackVsAttack = true;
//			config.leaveAlwaysAvailable = true;
//			config.showFleetAttitude = false;
			config.showTransponderStatus = false;
			config.showEngageText = false;
			config.alwaysPursue = true;
			config.dismissOnLeave = false;
			//config.lootCredits = false;
			config.withSalvage = false;
			//config.showVictoryText = false;
			config.printXPToDialog = true;

			config.noSalvageLeaveOptionText = "Continue";
//			config.postLootLeaveOptionText = "Continue";
//			config.postLootLeaveHasShortcut = false;

			config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
				public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
					new RemnantSeededFleetManager.RemnantFleetInteractionConfigGen().createConfig().delegate.
							postPlayerSalvageGeneration(dialog, context, salvage);
				}
				public void notifyLeave(InteractionDialogAPI dialog) {

					SectorEntityToken other = dialog.getInteractionTarget();
					if (!(other instanceof CampaignFleetAPI)) {
						dialog.dismiss();
						return;
					}
					CampaignFleetAPI fleet = (CampaignFleetAPI) other;

					if (!fleet.isEmpty()) {
						dialog.dismiss();
						return;
					}

					//Global.getSector().getMemoryWithoutUpdate().set(DEFEATED_NINAYA_KEY, true);

					ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData("zea_boss_ninaya_Nightdemon", ShipRecoverySpecial.ShipCondition.WRECKED, 0f);
					ship.shipName = "TTS Ninaya";
					DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
					CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
							fleet.getContainingLocation(),
							Entities.WRECK, Factions.NEUTRAL, params);
					Misc.makeImportant(entity, "zea_ninaya");

					entity.getLocation().x = fleet.getLocation().x + (50f - (float) Math.random() * 100f);
					entity.getLocation().y = fleet.getLocation().y + (50f - (float) Math.random() * 100f);

					ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
					data.notNowOptionExits = true;
					data.noDescriptionText = true;
					DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
					ShipRecoverySpecial.PerShipData copy = (ShipRecoverySpecial.PerShipData) dsep.getData().ship.clone();
					copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
					copy.variantId = null;
					copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
					copy.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
					data.addShip(copy);

					Misc.setSalvageSpecial(entity, data);

					dialog.setInteractionTarget(entity);
					RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("zea_AfterNinevehDefeat");
					dialog.setPlugin(plugin);
					plugin.init(dialog);
				}

				public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
					bcc.aiRetreatAllowed = false;
					bcc.objectivesAllowed = false;
					bcc.fightToTheLast = true;
					bcc.enemyDeployAll = true;
				}
			};
			return config;
		}
	}

}
