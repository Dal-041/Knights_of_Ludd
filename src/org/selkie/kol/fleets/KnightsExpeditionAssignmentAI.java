package org.selkie.kol.fleets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI;

public class KnightsExpeditionAssignmentAI extends RouteFleetAssignmentAI {

	public KnightsExpeditionAssignmentAI(CampaignFleetAPI fleet, RouteData route) {
		super(fleet, route);
	}

	
	protected String getTravelActionText(RouteSegment segment) {
		SectorEntityToken dest = segment.getDestination();
		if (segment.getId() == KnightsExpeditionsManager.ROUTE_TRAVEL) {
			return "bravely venturing into danger";
		}
		if (segment.getId() == KnightsExpeditionsManager.ROUTE_RETURN) {
			return "returning home from a valiant expedition";
		}
		return "bravely going";
	}
	
	protected String getInSystemActionText(RouteSegment segment) {
		return "searching out unholy enemies";
	}
	
	protected String getStartingActionText(RouteSegment segment) {
		return "preparing for a righteous expedition";
	}
	
	protected String getEndingActionText(RouteSegment segment) {
		return "standing down from a righteous expedition";
	}
	
}
