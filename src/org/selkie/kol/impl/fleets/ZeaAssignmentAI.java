package org.selkie.kol.impl.fleets;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class ZeaAssignmentAI implements EveryFrameScript {

	protected StarSystemAPI homeSystem;
	protected CampaignFleetAPI fleet;
	protected SectorEntityToken source;


	public ZeaAssignmentAI(CampaignFleetAPI fleet, StarSystemAPI homeSystem, SectorEntityToken source) {
		this.fleet = fleet;
		this.homeSystem = homeSystem;
		this.source = source;
		
		giveInitialAssignments();
	}
	
	protected void giveInitialAssignments() {
		boolean playerInSameLocation = fleet.getContainingLocation() == Global.getSector().getCurrentLocation();
		
		// launch from source if player is in-system, or sometimes
		if ((playerInSameLocation || (float) Math.random() < 0.1f) && source != null) {
			fleet.setLocation(source.getLocation().x, source.getLocation().y);
			fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, source, 3f + (float) Math.random() * 2f);
		} else {
			// start at random location
			SectorEntityToken target = ZeaFleetManager.pickEntityToGuard(new Random(), homeSystem, fleet);
			if (target != null) {
				Vector2f loc = Misc.getPointAtRadius(target.getLocation(), target.getRadius() + 100f);
				fleet.setLocation(loc.x, loc.y);
			} else {
				Vector2f loc = Misc.getPointAtRadius(new Vector2f(), 5000f);
				fleet.setLocation(loc.x, loc.y);
			}
			pickNext();
		}
	}
	
	protected void pickNext() {
		boolean standDown = source != null && (float) Math.random() < 0.2f;
		if (!standDown) {
			SectorEntityToken target = ZeaFleetManager.pickEntityToGuard(new Random(), homeSystem, fleet);
			if (target != null) {
				float speed = Misc.getSpeedForBurnLevel(8);
				float dist = Misc.getDistance(fleet.getLocation(), target.getLocation());
				float seconds = dist / speed;
				float days = seconds / Global.getSector().getClock().getSecondsPerDay();
				days += 5f + 5f * (float) Math.random();
				fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target, days, "patrolling");
				return;
			} else {
				float days = 5f + 5f * (float) Math.random();
				fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, null, days, "patrolling");
			}
		}
		
		if (source != null) {
			float dist = Misc.getDistance(fleet.getLocation(), source.getLocation());
			if (dist > 1000) {
				fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, source, 3f, "returning from patrol");
			} else {
				fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, source, 3f + (float) Math.random() * 2f, "standing down");
				fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 5f);
			}
		}
		
	}

	public void advance(float amount) {
		if (fleet.getCurrentAssignment() == null) {
			pickNext();
		}
	}

	
	
	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}
	
	

}










