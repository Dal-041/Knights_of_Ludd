package org.selkie.kol.impl.terrain;

import java.awt.Color;

import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class AbyssAsteroidImpact implements EveryFrameScript {

    //public static float SAFE_BURN_LEVEL = 5.01f;
//	public static float IMPACT_SPEED_DELTA = Global.getSettings().getSpeedPerBurnLevel();
    public static float DURATION_SECONDS = 0.2f;

    protected CampaignFleetAPI fleet;
    protected float elapsed;
    //	protected float angle;
//	protected float impact = IMPACT_SPEED_DELTA;
    protected Vector2f dV;

    public AbyssAsteroidImpact(CampaignFleetAPI fleet, boolean dealDamage) {
        this.fleet = fleet;

        if (fleet.hasTag("zea_rulesfortheebutnotforme")) {
            dV = new Vector2f();
            dV.x = 0;
            dV.y = 0;
            return;
        }

        //System.out.println("ADDING IMPACT TO " + fleet.getName() + " " + fleet.getId());

        Vector2f v = fleet.getVelocity();
        float angle = Misc.getAngleInDegrees(v);
        float speed = v.length();
        if (speed < 10) angle = fleet.getFacing();

        float mult = Misc.getFleetRadiusTerrainEffectMult(fleet);

        //float arc = 120f;
        float arc = 120f - 60f * mult; // larger fleets suffer more direct collisions that slow them down more

        angle += (float) Math.random() * arc - arc/2f;
        angle += 180f;

        //if (fleet.getCurrBurnLevel() <= SAFE_BURN_LEVEL || mult <= 0) {
        if (Misc.isSlowMoving(fleet) || mult <= 0) {
            elapsed = DURATION_SECONDS;
            dV = new Vector2f();
        } else if (fleet.isInCurrentLocation()) {
            if (dealDamage) {
                WeightedRandomPicker<FleetMemberAPI> targets = new WeightedRandomPicker<FleetMemberAPI>();
                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    float w = 1f;
                    switch (member.getHullSpec().getHullSize()) {
                        case CAPITAL_SHIP: w = 20f; break;
                        case CRUISER: w = 10f; break;
                        case DESTROYER: w = 5f; break;
                        case FRIGATE: w = 1f; break;
                    }
                    targets.add(member, w);
                }

                FleetMemberAPI member = targets.pick();
                if (member != null) {
                    float damageMult = fleet.getCurrBurnLevel() - Misc.getGoSlowBurnLevel(fleet);
                    if (damageMult < 1) damageMult = 1;
                    Misc.applyDamage(member, null, damageMult, true, "asteroid_impact", "Asteroid impact",
                            true, null, member.getShipName() + " suffers damage from an asteroid impact");
                } else {
                    dealDamage = false;
                }
            }

            if (!dealDamage && fleet.isPlayerFleet()) {
                Global.getSector().getCampaignUI().addMessage("Asteroid impact on drive bubble", Misc.getNegativeHighlightColor());
            }

            Vector2f test = Global.getSector().getPlayerFleet().getLocation();
            float dist = Misc.getDistance(test, fleet.getLocation());
            if (dist < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE) {
//				float volumeMult = 1f - (dist / HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE);
//				volumeMult = (float) Math.sqrt(volumeMult);
                //volumeMult *= 0.75f;
                float volumeMult = 0.75f;
                volumeMult *= 0.5f + 0.5f * mult;
                if (volumeMult > 0) {
                    if (dealDamage) {
                        Global.getSoundPlayer().playSound("hit_heavy", 1f, 1f * volumeMult, fleet.getLocation(), Misc.ZERO);
                    } else {
                        Global.getSoundPlayer().playSound("hit_shield_heavy_gun", 1f, 1f * volumeMult, fleet.getLocation(), Misc.ZERO);
                    }
                }
            }

//			if (fleet.isPlayerFleet()) {
//				System.out.println("wefwefwef");
//			}
            //mult = 2f;
//			if (fleet.isPlayerFleet()) {
//				System.out.println("SPAWNED ---------");
//			}

//			int num = (int) (6f + (float) Math.random() * 5f);
//			for (int i = 0; i < num; i++) {
//				angle = angle + (float) Math.random() * 30f - 15f;
//				float size = 10f + (float) Math.random() * 6f;
//				float size = 10f + (float) Math.random() * 6f;
//				size = 4f + 4f * (float) Math.random();
//
//				AsteroidAPI asteroid = fleet.getContainingLocation().addAsteroid(size);
//				asteroid.setFacing((float) Math.random() * 360f);
//				Vector2f av = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
//				av.scale(fleet.getVelocity().length() + (20f + 20f * (float) Math.random()) * mult);
//				asteroid.getVelocity().set(av);
//				Vector2f al = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
//				//al.scale(fleet.getRadius() + asteroid.getRadius());
//				al.scale(fleet.getRadius());
//				Vector2f.add(al, fleet.getLocation(), al);
//				asteroid.setLocation(al.x, al.y);
//
//				float sign = Math.signum(asteroid.getRotation());
//				asteroid.setRotation(sign * (50f + 50f * (float) Math.random()));
//
//				Misc.fadeInOutAndExpire(asteroid, 0.2f, 1.5f + 1f * (float) Math.random(), 1f);
//			}

            float size = 10f + (float) Math.random() * 6f;
            size *= 0.67f;
            AsteroidAPI asteroid = fleet.getContainingLocation().addAsteroid(size);
            asteroid.setFacing((float) Math.random() * 360f);
            Vector2f av = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
            av.scale(fleet.getVelocity().length() + (20f + 20f * (float) Math.random()) * mult);
            asteroid.getVelocity().set(av);
            Vector2f al = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
            //al.scale(fleet.getRadius() + asteroid.getRadius());
            al.scale(fleet.getRadius());
            Vector2f.add(al, fleet.getLocation(), al);
            asteroid.setLocation(al.x, al.y);

            float sign = Math.signum(asteroid.getRotation());
            asteroid.setRotation(sign * (50f + 50f * (float) Math.random()));

            Misc.fadeInOutAndExpire(asteroid, 0.2f, 1f + 1f * (float) Math.random(), 1f);

            //mult = 1f;
            Vector2f iv = fleet.getVelocity();
            iv = new Vector2f(iv);
            iv.scale(0.7f);
            float glowSize = 100f + 100f * mult + 50f * (float) Math.random();
            Color color = new Color(255, 165, 100, 255);
            Misc.addHitGlow(fleet.getContainingLocation(), al, iv, glowSize, color);
        }

        dV = Misc.getUnitVectorAtDegreeAngle(angle);

        float impact = speed * 1f * (0.5f + mult * 0.5f);
        dV.scale(impact);
        dV.scale(1f / DURATION_SECONDS);

//		if (fleet.isPlayerFleet()) {
//			fleet.addFloatingText("Impact!", Misc.getNegativeHighlightColor(), 0.5f);
//		}
    }

    public void advance(float amount) {

        fleet.setOrbit(null);

//		if (fleet.isPlayerFleet()) {
//			System.out.println("wefwefwef");
//		}

        Vector2f v = fleet.getVelocity();
        fleet.setVelocity(v.x + dV.x * amount, v.y + dV.y * amount);

//		Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);
//
//		//dir.scale(IMPACT_SPEED_DELTA * mult * amount * 75f * (0.5f + (float) Math.random() * 0.5f));
//
//		float mult = Misc.getFleetRadiusTerrainEffectMult(fleet);
//		mult = 1f;
////		if (mult > 1f || amount ) {
////			System.out.println("wefwefwefe");
////			mult = Misc.getFleetRadiusTerrainEffectMult(fleet);
////		}
//		dir.scale(IMPACT_SPEED_DELTA * mult * amount *
//				(Math.min(20f, fleet.getCurrBurnLevel()) * 50f) * (0.5f + (float) Math.random() * 0.5f));
//
//		Vector2f v = fleet.getVelocity();
//		fleet.setVelocity(v.x + dir.x, v.y + dir.y);
//
////		Vector2f loc = fleet.getLocation();
////		fleet.setLocation(loc.x + dir.x, loc.y + dir.y);

        elapsed += amount;

    }

    public boolean isDone() {
        return elapsed >= DURATION_SECONDS;
    }

    public boolean runWhilePaused() {
        return false;
    }

}
