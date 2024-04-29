package org.selkie.kol.impl.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.impl.combat.EscapeRevealPlugin;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Random;

public class NinayaBattleCreationPlugin extends BattleCreationPluginImpl {

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        this.context = context;
        this.loader = loader;
        CampaignFleetAPI playerFleet = context.getPlayerFleet();
        CampaignFleetAPI otherFleet = context.getOtherFleet();
        FleetGoal playerGoal = context.getPlayerGoal();
        FleetGoal enemyGoal = context.getOtherGoal();

        // doesn't work for consecutive engagements; haven't investigated why
        //Random random = Misc.getRandom(Misc.getNameBasedSeed(otherFleet), 23);

        Random random = Misc.getRandom(Misc.getSalvageSeed(otherFleet) *
                (long)otherFleet.getFleetData().getNumMembers(), 23);
        //System.out.println("RNG: " + random.nextLong());
        //random = new Random(1213123L);
        //Random random = Misc.random;

        escape = playerGoal == FleetGoal.ESCAPE || enemyGoal == FleetGoal.ESCAPE;

        int maxFP = (int) Global.getSettings().getFloat("maxNoObjectiveBattleSize");
        int fpOne = 0;
        int fpTwo = 0;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.canBeDeployedForCombat() || playerGoal == FleetGoal.ESCAPE) {
                fpOne += member.getUnmodifiedDeploymentPointsCost();
            }
        }
        for (FleetMemberAPI member : otherFleet.getFleetData().getMembersListCopy()) {
            if (member.canBeDeployedForCombat() || playerGoal == FleetGoal.ESCAPE) {
                fpTwo += member.getUnmodifiedDeploymentPointsCost();
            }
        }

        int smaller = Math.min(fpOne, fpTwo);

        boolean withObjectives = smaller > maxFP;
        if (!context.objectivesAllowed) {
            withObjectives = false;
        }

        int numObjectives = 0;
        if (withObjectives) {
//			if (fpOne + fpTwo > maxFP + 70) {
//				numObjectives = 3
//				numObjectives = 3 + (int)(Math.random() * 2.0);
//			} else {
//				numObjectives = 2 + (int)(Math.random() * 2.0);
//			}
            if (fpOne + fpTwo > maxFP + 70) {
                numObjectives = 4;
                //numObjectives = 3 + (int)(Math.random() * 2.0);
            } else {
                numObjectives = 3 + random.nextInt(2);
                //numObjectives = 2 + (int)(Math.random() * 2.0);
            }
        }

        // shouldn't be possible, but..
        if (numObjectives > 4) {
            numObjectives = 4;
        }

        int baseCommandPoints = (int) Global.getSettings().getFloat("startingCommandPoints");

        //
        loader.initFleet(FleetSide.PLAYER, "ISS", playerGoal, false,
                context.getPlayerCommandPoints() - baseCommandPoints,
                (int) playerFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);
        loader.initFleet(FleetSide.ENEMY, "", enemyGoal, true,
                (int) otherFleet.getCommanderStats().getCommandPoints().getModifiedValue() - baseCommandPoints);

        List<FleetMemberAPI> playerShips = playerFleet.getFleetData().getCombatReadyMembersListCopy();
        if (playerGoal == FleetGoal.ESCAPE) {
            playerShips = playerFleet.getFleetData().getMembersListCopy();
        }
        for (FleetMemberAPI member : playerShips) {
            loader.addFleetMember(FleetSide.PLAYER, member);
        }


        List<FleetMemberAPI> enemyShips = otherFleet.getFleetData().getCombatReadyMembersListCopy();
        if (enemyGoal == FleetGoal.ESCAPE) {
            enemyShips = otherFleet.getFleetData().getMembersListCopy();
        }
        for (FleetMemberAPI member : enemyShips) {
            loader.addFleetMember(FleetSide.ENEMY, member);
        }

        /*width = 18000f;
        height = 18000f;*/

        width = 12000f;
        height = 12000f;

       /* if (escape) {
            width = 18000f;
            //height = 24000f;
            height = 18000f;
        } else if (withObjectives) {
            width = 24000f;
            if (numObjectives == 2) {
                height = 14000f;
            } else {
                height = 18000f;
            }
        }*/

        createMap(random);

        context.setInitialDeploymentBurnDuration(1.5f);
        context.setNormalDeploymentBurnDuration(6f);
        context.setEscapeDeploymentBurnDuration(1.5f);

        xPad = 2000f;
        yPad = 3000f;

        if (escape) {
//			addEscapeObjectives(loader, 4);
//			context.setInitialEscapeRange(7000f);
//			context.setFlankDeploymentDistance(9000f);
            addEscapeObjectives(loader, 2, random);
//			context.setInitialEscapeRange(4000f);
//			context.setFlankDeploymentDistance(8000f);

            context.setInitialEscapeRange(Global.getSettings().getFloat("escapeStartDistance"));
            context.setFlankDeploymentDistance(Global.getSettings().getFloat("escapeFlankDistance"));

            loader.addPlugin(new EscapeRevealPlugin(context));
        } else {
            if (withObjectives) {
                addObjectives(loader, numObjectives, random);
                context.setStandoffRange(height - 4500f);
            } else {
                context.setStandoffRange(6000f);
            }

            context.setFlankDeploymentDistance(height/2f); // matters for Force Concentration
        }
    }
}
