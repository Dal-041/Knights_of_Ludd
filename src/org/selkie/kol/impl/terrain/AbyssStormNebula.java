package org.selkie.kol.impl.terrain;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl.NebulaTextureProvider;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.awt.Color;
import java.util.List;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.helpers.ZeaUtils;
import org.selkie.kol.impl.world.PrepareAbyss;

import static org.selkie.kol.impl.world.PrepareAbyss.excludeTag;

public class AbyssStormNebula extends HyperspaceTerrainPlugin implements NebulaTextureProvider {
    public static final float NULL_NEBULA_SENSOR_RANGE_MULT = 0.5f;
    public static final float NULLSTORM_SENSOR_RANGE_MULT = 0.5f;
    public static final float CHARGED_STORM_VISIBILITY_FLAT = 0f;

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

            boolean inCloud = isInClouds(fleet);
            int[] tile = getTilePreferStorm(fleet.getLocation(), fleet.getRadius());
            CellStateTracker cell = null;
            if (tile != null) {
                cell = activeCells[tile[0]][tile[1]];
            }

            if (!inCloud || fleet.isInHyperspaceTransition()) {
                // open, do nothing
            } else {
                // deep
                fleet.getStats().removeTemporaryMod(getModId() + "_storm_sensor");
                if (NULL_NEBULA_SENSOR_RANGE_MULT != 1) {
                    fleet.getStats().addTemporaryModMult(0.1f, getModId() + "_sensor",
                            "Inside Nullstorm", NULL_NEBULA_SENSOR_RANGE_MULT,
                            fleet.getStats().getSensorRangeMod());
                }

                float penalty = Misc.getBurnMultForTerrain(fleet);
                fleet.getStats().removeTemporaryMod(getModId() + "_storm_speed");
                fleet.getStats().addTemporaryModMult(0.1f, getModId() + "_speed",
                        "Inside Nullstorm", penalty,
                        fleet.getStats().getFleetwideMaxBurnMod());
                if (cell != null && cell.isSignaling() && cell.signal < 0.2f) {
                    cell.signal = 0; // go to storm as soon as a fleet enters, if it's close to storming already
                }

                if (cell != null && cell.isStorming() && !Misc.isSlowMoving(fleet)) {
                    // storm
                    fleet.getStats().removeTemporaryMod(getModId() + "_sensor");
                    fleet.getStats().addTemporaryModMult(0.1f, getModId() + "_storm_sensor",
                            "Inside Nullstorm",
                            NULLSTORM_SENSOR_RANGE_MULT,
                            fleet.getStats().getSensorRangeMod());
                    applyStormStrikes(cell, fleet, days);
                }
            }
        }
    }

    @Override
    protected void applyStormStrikes(CellStateTracker cell, CampaignFleetAPI fleet, float days) {
        if (cell.flicker != null && cell.flicker.getWait() > 0) {
            cell.flicker.setNumBursts(0);
            cell.flicker.setWait(0);
            cell.flicker.newBurst();
        }

        if (cell.flicker == null || !cell.flicker.isPeakFrame() || fleet.hasTag(excludeTag)) {
            return;
        }

        fleet.addScript(new AbyssChargedStorm(cell, fleet));

        String key = "$zea_chargedStormStrikeTimeout";
        MemoryAPI mem = fleet.getMemoryWithoutUpdate();
        if (mem.contains(key)) {
            return;
        }
        mem.set(key, true, (float) (STORM_MIN_TIMEOUT + (STORM_MAX_TIMEOUT - STORM_MIN_TIMEOUT) * Math.random()));

        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        if (members.isEmpty()) {
            return;
        }

        float totalValue = 0;
        for (FleetMemberAPI member : members) {
            totalValue += member.getStats().getSuppliesToRecover().getModifiedValue();
        }
        if (totalValue <= 0) {
            return;
        }

        float strikeValue = totalValue * STORM_DAMAGE_FRACTION * (0.5f + (float) Math.random() * 0.5f);

        WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<>();
        for (FleetMemberAPI member : members) {
            float w = 1f;
            if (member.isMothballed()) {
                w *= 0.1f;
            }
            if (member.isPhaseShip()) {
                w *= 0.2f;
            }
            picker.add(member, w);
        }
        FleetMemberAPI member = picker.pick();
        if (member == null) {
            return;
        }

        float crPerDep = member.getDeployCost();
        float suppliesPerDep = member.getStats().getSuppliesToRecover().getModifiedValue();
        if (suppliesPerDep <= 0 || crPerDep <= 0) {
            return;
        }

        float strikeDamage = crPerDep * strikeValue / suppliesPerDep;
        if (strikeDamage < STORM_MIN_STRIKE_DAMAGE) {
            strikeDamage = STORM_MIN_STRIKE_DAMAGE;
        }

        float resistance = member.getStats().getDynamic().getValue(Stats.CORONA_EFFECT_MULT);
        strikeDamage *= resistance;
        if (member.isPhaseShip()) strikeDamage *= 0.3f;

        if (strikeDamage > STORM_MAX_STRIKE_DAMAGE) {
            strikeDamage = STORM_MAX_STRIKE_DAMAGE;
        }

        float currCR = member.getRepairTracker().getBaseCR();
        float crDamage = Math.min(currCR, strikeDamage);
        if (crDamage > 0) {
            member.getRepairTracker().applyCREvent(-crDamage, "zea_nullstorm", "Nullstorm strike");
        }

        float hitStrength = member.getStats().getArmorBonus().computeEffective(member.getHullSpec().getArmorRating());
        hitStrength *= strikeDamage / crPerDep;
        if (hitStrength > 0) {
            member.getStatus().applyDamage(hitStrength);
            if (member.getStatus().getHullFraction() < 0.01f) {
                member.getStatus().setHullFraction(0.01f);
            }
        }

        if (fleet.isPlayerFleet()) {
            String verb = "suffers";
            Color c = Misc.getNegativeHighlightColor();
            if (hitStrength <= 0) {
                verb = "avoids";
                c = Misc.getTextColor();
            }
            Global.getSector().getCampaignUI().addMessage(
                    member.getShipName() + " " + verb + " damage from the storm", c);
        }
    }

    @Override
    public boolean containsEntity(SectorEntityToken other) {
        return isInClouds(other);
    }

    @Override
    public boolean containsPoint(Vector2f test, float r) {
        return isInClouds(test, r);
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        float small = 5f;
        Color highlight = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        int[] tile = getTilePreferStorm(player.getLocation(), player.getRadius());
        CellStateTracker cell = null;
        if (tile != null) {
            cell = activeCells[tile[0]][tile[1]];
        }

        tooltip.addTitle(getTerrainName());
        if (cell == null || !cell.isStorming()) {
            // deep
            tooltip.addPara(Global.getSettings().getDescription(getTerrainId() + "_normal", Type.TERRAIN).getText1(),
                    pad);
        } else if (cell.isStorming()) {
            // storm
            tooltip.addPara(Global.getSettings().getDescription(getTerrainId() + "_storm", Type.TERRAIN).getText1(), pad);
        }

        float nextPad = pad;
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad);
            nextPad = small;
        }

        tooltip.addPara("Reduces the range at which fleets inside can be detected by %s.",
                pad,
                highlight,
                "" + (int) ((1f - NULL_NEBULA_SENSOR_RANGE_MULT) * 100) + "%"
        );

        tooltip.addPara("Reduces the speed of fleets inside by up to %s. Larger fleets are slowed down more.",
                nextPad,
                highlight,
                "" + (int) ((Misc.BURN_PENALTY_MULT) * 100f) + "%"
        );

        float penalty = Misc.getBurnMultForTerrain(Global.getSector().getPlayerFleet());
        tooltip.addPara("Your fleet's speed is reduced by %s.", pad,
                highlight,
                "" + (int) Math.round((1f - penalty) * 100) + "%"
        );

        tooltip.addSectionHeading("Charged storms", Alignment.MID, pad);

        Color stormDescColor = Misc.getTextColor();
        if (cell != null && cell.isStorming()) {
            stormDescColor = bad;
        }
        tooltip.addPara("Being caught in a storm causes storm strikes to damage ships "
                + "and reduce their combat readiness. "
                + "Larger fleets attract more damaging strikes.", stormDescColor, pad);

        tooltip.addPara("In addition, storm strikes toss the fleet's drive bubble about "
                + "with great violence, often causing a loss of control.", Misc.getTextColor(), pad);

        tooltip.addPara("\"Slow-moving\" fleets do not attract storm strikes.", Misc.getTextColor(), pad);

        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad);
            tooltip.addPara("Numerous patches of nebula present on the battlefield, but the medium is not dense enough to affect ships moving at combat speeds.", small);
        }
    }

    @Override
    public int getNumMapSamples() {
        return 10;
    }

    @Override
    public float getStormCellTimeMultOutsideBaseArea() {
        return 0f; //0
    }

    @Override
    public float getExtraDistanceAroundPlayerToAdvanceStormCells() {
        return 0f; //0
    }

    @Override
    public String getTerrainName() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        int[] tile = getTilePreferStorm(player.getLocation(), player.getRadius());
        CellStateTracker cell = null;
        if (tile != null) {
            cell = activeCells[tile[0]][tile[1]];
        }

        String n = "Nullstorm Nebula";
        if (cell != null && cell.isStorming()) {
            n = "Nullstorm";
        }
        return n;
    }

   /* @Override
    public float getTileSize() {
        return TILE_SIZE * 2f;
    }

    @Override
    public float getTileRenderSize() {
        return getTileSize() * 2.5f;
    }

    @Override
    public float getTileContainsSize() {
        //return TILE_SIZE + 200f;
        return getTileSize() * 1.5f;
    }*/

    @Override
    protected float[] getThetaAndRadius(Random rand, float width, float height) {
        if (temp == null) {
            temp = new float[2];
        }

        float speedFactor = 0.10f; //0.5f

        float time = elapsed * Global.getSector().getClock().getSecondsPerDay();
        float min = -360f * (rand.nextFloat() * 3f + 1f) * Misc.RAD_PER_DEG;
        float max = 360f * (rand.nextFloat() * 3f + 1f) * Misc.RAD_PER_DEG;
        float rate = (30f + 70f * rand.nextFloat()) * Misc.RAD_PER_DEG;
        rate *= speedFactor;
        float period = 2f * (max - min) / rate;
        float progress = rand.nextFloat() + time / period;
        progress -= (int) progress;

        float theta, radius;
        if (progress < 0.5f) {
            theta = min + (max - min) * progress * 2f;
        } else {
            theta = min + (max - min) * (1f - progress) * 2f;
        }
        temp[0] = theta;

        min = 0f;
        max = (width + height) * 0.03f; //0.025f, 0.03f
        rate = max * 0.5f;
        rate *= speedFactor;

        period = 2f * (max - min) / rate;
        progress = rand.nextFloat() + time / period;
        progress -= (int) progress;
        if (progress < 0.5f) {
            radius = min + (max - min) * progress * 2f;
        } else {
            radius = min + (max - min) * (1f - progress) * 2f;
        }
        temp[1] = radius;

        return temp;
    }

    @Override
    public void turnOffStorms(Vector2f loc, float radius) {
        setTileState(loc, radius, CellState.OFF, -1f, -1f);
    }

    @Override
    public void advance(float amount) {

        if (!clearedCellsPostLoad && Global.getSector().getPlayerFleet() != null) {
            clearCellsNotNearPlayer(this);
            clearedCellsPostLoad = true;
        }

        if (Global.getSector()!= null && Global.getSector().getCurrentLocation() != null
                && !Global.getSector().getCurrentLocation().hasTag(ZeaUtils.THEME_STORM)) {
            return;
        }

        playStormStrikeSoundsIfNeeded();

        float days = Global.getSector().getClock().convertToDays(amount);
//		for (int i = 0; i < 100; i++) {
//			auto.advance(days * 10f);
//		}
        auto.advance(days * 1f);

        int [][] cells = auto.getCells();

//		int count = 0;
//		for (int i = 0; i < activeCells.length; i++) {
//			for (int j = 0; j < activeCells[0].length; j++) {
//				if (tiles[i][j] < 0) continue;
//				CellStateTracker curr = activeCells[i][j];
//				if (curr != null) {
//					count++;
//				}
//			}
//		}
//		System.out.println("Count: " + count + "(out of " + (activeCells.length * activeCells[0].length));

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Vector2f test = new Vector2f();
        if (playerFleet != null) {
            test = playerFleet.getLocation();
        }

        float x = this.entity.getLocation().x;
        float y = this.entity.getLocation().y;
        float size = getTileSize();

        float w = tiles.length * size;
        float h = tiles[0].length * size;
        x -= w/2f;
        y -= h/2f;
        int xIndex = (int) ((test.x - x) / size);
        int yIndex = (int) ((test.y - y) / size);
        if (xIndex < 0) xIndex = 0;
        if (yIndex < 0) yIndex = 0;
        if (xIndex >= tiles.length) xIndex = tiles.length - 1;
        if (yIndex >= tiles[0].length) yIndex = tiles[0].length - 1;

        float subgridDist = 10000f + extraDistanceAroundPlayerToAdvanceStormCells;
        float baseSubgridDist = 10000f;

        int subgridSize = (int) ((subgridDist / size + 1) * 2f);

        int minX = Math.max(0, xIndex - subgridSize/2);
        int maxX = xIndex + subgridSize/2 ;
        int minY = Math.max(0, yIndex - subgridSize/2);
        int maxY = yIndex + subgridSize/2;

        int baseSubgridSize = (int) ((baseSubgridDist / size + 1) * 2f);

        int baseMinX = Math.max(0, xIndex - baseSubgridSize/2);
        int baseMaxX = xIndex + baseSubgridSize/2 ;
        int baseMinY = Math.max(0, yIndex - baseSubgridSize/2);
        int baseMaxY = yIndex + baseSubgridSize/2;

        // clean up area around the "active" area so that as the player moves around,
        // they don't leave frozen storm cells behind (which would then make it into the savefile)
        int pad = 4;
        for (int i = minX - pad; i <= maxX + pad && i < tiles.length; i++) {
            for (int j = minY - pad; j <= maxY + pad && j < tiles[0].length; j++) {
                if (i < minX || j < minY || i > maxX || j > maxY) {
                    if (i >= 0 && j >= 0) {
                        activeCells[i][j] = null;
                    }
                }
            }
        }

        for (int i = minX; i <= maxX && i < tiles.length; i++) {
            for (int j = minY; j <= maxY && j < tiles[0].length; j++) {
//		for (int i = 0; i < activeCells.length; i++) {
//			for (int j = 0; j < activeCells[0].length; j++) {
                if (tiles[i][j] < 0) continue;

                CellStateTracker curr = activeCells[i][j];
                int val = cells[i][j];
                float interval = auto.getInterval().getIntervalDuration();

                if (val == 1 && curr == null) {
                    curr = activeCells[i][j] = new CellStateTracker(i, j,
                            interval * 0f + interval * 1.5f * (float) Math.random(),
                            interval * 0.5f + interval * 0.5f * (float) Math.random());
//							interval * 0f + interval * 0.5f * (float) Math.random(),
//							interval * 0.25f + interval * 0.25f * (float) Math.random());
                }

                if (curr != null) {
                    if (val != 1 && curr.isStorming() && !curr.isWaning()) {
                        //curr.wane(interval * 0.25f + interval * 0.25f * (float) Math.random());
                        curr.wane(interval * 0.5f + interval * 0.5f * (float) Math.random());
//						curr.wane(interval * 0.5f * (float) Math.random() +
//								  interval * 0.25f + interval * 0.25f * (float) Math.random());
                    }
                    float timeMult = 1f;
                    if (extraDistanceAroundPlayerToAdvanceStormCells > 0 && stormCellTimeMultOutsideBaseArea > 0) {
                        if (i < baseMinX || j < baseMinY || i > baseMaxX || j > baseMaxY) {
                            timeMult = stormCellTimeMultOutsideBaseArea;
                        }
                    }
                    curr.advance(days * timeMult);
                    if (curr.isOff()) {
                        activeCells[i][j] = null;
                    }
                }
            }
        }

        //stormCellTimeMultOutsideBaseArea = 0f;
        //extraDistanceAroundPlayerToAdvanceStormCells = 0f;
    }

}
