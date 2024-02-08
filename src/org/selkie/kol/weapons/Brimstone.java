package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class Brimstone extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin {
    private boolean init = false;
    private boolean active = false;
    private ShipAPI ship;

    protected List<Brimstone> trails;

    public Brimstone() {
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }
        if (!init) {
            ship = weapon.getShip();
            init = true;
        }

        if (ship == null || !ship.isAlive() || ship.getSystem() == null) {
            //Lazy negation, would return but cryoflux effect still needs to run.
        } else {
            if (!active && ship.getSystem().getState().equals(ShipSystemAPI.SystemState.ACTIVE)) {
                active = true;
                int curr = weapon.getAmmo();
                int max = weapon.getMaxAmmo();
                if (max > curr) {
                    weapon.setAmmo(Math.min(curr + (weapon.getMaxAmmo() / 2), weapon.getMaxAmmo()));
                }
            }

            if (active && ship.getSystem().getState().equals(ShipSystemAPI.SystemState.IDLE)) {
                active = false;
            }
        }

        //Cryoflux stuff follows
        if (trails == null) return;

        Iterator<Brimstone> iter = trails.iterator();
        while (iter.hasNext()) {
            if (iter.next().isExpired()) iter.remove();
        }

        // sound loop playback
        if (weapon.getShip() != null) {
            float maxRange = weapon.getRange();
            ShipAPI ship = weapon.getShip();
            Vector2f com = new Vector2f();
            float weight = 0f;
            float totalDist = 0f;
            Vector2f source = weapon.getLocation();
            for (Brimstone curr : trails) {
                if (curr.proj != null) {
                    Vector2f.add(com, curr.proj.getLocation(), com);
                    weight += curr.proj.getBrightness();
                    totalDist += Misc.getDistance(source, curr.proj.getLocation());
                }
            }
            if (weight > 0.1f) {
                com.scale(1f / weight);
                float volume = Math.min(weight, 1f);
                if (trails.size() > 0) {
                    totalDist /= (float) trails.size();
                    float mult = totalDist / Math.max(maxRange, 1f);
                    mult = 1f - mult;
                    if (mult > 1f) mult = 1f;
                    if (mult < 0f) mult = 0f;
                    mult = (float) Math.sqrt(mult);
                    volume *= mult;
                }
                Global.getSoundPlayer().playLoop("kol_brimstone_loop", ship, 1f, volume, com, ship.getVelocity());
            }
        }


        //System.out.println("Trails: " + trails.size());
        float numIter = 1f; // more doesn't actually change anything
        amount /= numIter;
        // drag along the previous projectile, starting with the most recently launched; new ones are added at the start
        // note: prev is fired before and so is in front of proj
        for (int i = 0; i < numIter; i++) {
            for (Brimstone trail : trails) {
                //trail.proj.setFacing(trail.proj.getFacing() + 180f * amount);
                if (trail.prev != null && !trail.prev.isExpired() && Global.getCombatEngine().isEntityInPlay(trail.prev)) {
                    float dist1 = Misc.getDistance(trail.prev.getLocation(), trail.proj.getLocation());
                    if (dist1 < trail.proj.getProjectileSpec().getLength() * 1f) {
                        float maxSpeed = trail.prev.getMoveSpeed() * 0.5f;// * Math.max(0.5f, 1f - trail.prev.getElapsed() * 0.5f);
                        // goal here is to prevent longer shot series (e.g. from Paragon) from moving too unnaturally
                        float e = trail.prev.getElapsed();
                        float t = 0.5f;
                        if (e > t) {
                            maxSpeed *= Math.max(0.25f, 1f - (e - t) * 0.5f);
                        }
                        if (dist1 < 20f && e > t) {
                            maxSpeed *= dist1 / 20f;
                        }

                        Vector2f driftTo = Misc.closestPointOnLineToPoint(trail.proj.getLocation(), trail.proj.getTailEnd(), trail.prev.getLocation());
                        float dist = Misc.getDistance(driftTo, trail.prev.getLocation());
                        Vector2f diff = Vector2f.sub(driftTo, trail.prev.getLocation(), new Vector2f());
                        diff = Misc.normalise(diff);
                        diff.scale(Math.min(dist, maxSpeed * amount));
                        Vector2f.add(trail.prev.getLocation(), diff, trail.prev.getLocation());
                        Vector2f.add(trail.prev.getTailEnd(), diff, trail.prev.getTailEnd());
                    }
                }
            }
        }
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Color color = projectile.getProjectileSpec().getFringeColor();
//		Color inverted = NSLanceEffect.getColorForDarkening(color);
//		inverted = Misc.setAlpha(inverted, 50);
//		Color inverted = new Color(255, 255, 100, 50);

        Vector2f vel = new Vector2f();
        if (target instanceof ShipAPI) {
            vel.set(target.getVelocity());
        }

        float size = projectile.getProjectileSpec().getWidth() * 1f;
        //size = Misc.getHitGlowSize(size, projectile.getDamage().getBaseDamage(), damageResult);
        float sizeMult = Misc.getHitGlowSize(100f, projectile.getDamage().getBaseDamage(), damageResult) / 100f;
//		sizeMult = 1.5f;
//		System.out.println(sizeMult);
        float dur = 1f;
        float rampUp = 0f;
        Color c = Misc.scaleAlpha(color, projectile.getBrightness());
        engine.addNebulaParticle(point, vel, size, 5f + 3f * sizeMult,
                rampUp, 0f, dur, c);
//		engine.addNegativeNebulaParticle(point, vel, size, 2f,
//										rampUp, 0f, dur, inverted);
//		engine.addNegativeParticle(point, vel, size,
//								   rampUp, dur, inverted);

        Misc.playSound(damageResult, point, vel,
                "cryoflamer_hit_shield_light",
                "cryoflamer_hit_shield_solid",
                "cryoflamer_hit_shield_heavy",
                "cryoflamer_hit_light",
                "cryoflamer_hit_solid",
                "cryoflamer_hit_heavy");

    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        String prevKey = "cryo_prev_" + weapon.getShip().getId() + "_" + weapon.getSlot().getId();
        DamagingProjectileAPI prev = (DamagingProjectileAPI) engine.getCustomData().get(prevKey);

        Brimstone trail = new Brimstone(projectile, prev);
        CombatEntityAPI e = engine.addLayeredRenderingPlugin(trail);
        e.getLocation().set(projectile.getLocation());

        engine.getCustomData().put(prevKey, projectile);

        if (trails == null) {
            trails = new ArrayList<Brimstone>();
        }
        trails.add(0, trail);
    }



    public static class ParticleData {
        public SpriteAPI sprite;
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();
        public float scale = 1f;
        public DamagingProjectileAPI proj;
        public float scaleIncreaseRate = 1f;
        public float turnDir = 1f;
        public float angle = 1f;
        public FaderUtil fader;

        public ParticleData(DamagingProjectileAPI proj) {
            this.proj = proj;
            sprite = Global.getSettings().getSprite("misc", "nebula_particles");
            //sprite = Global.getSettings().getSprite("misc", "dust_particles");
            float i = Misc.random.nextInt(4);
            float j = Misc.random.nextInt(4);
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(i * 0.25f);
            sprite.setTexY(j * 0.25f);
            sprite.setAdditiveBlend();

            angle = (float) Math.random() * 360f;

            float maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
            scaleIncreaseRate = 2f / maxDur;
            scale = 1f;
//			scale = 0.1f;
//			scaleIncreaseRate = 2.9f / maxDur;
//			scale = 0.1f;
//			scaleIncreaseRate = 2.5f / maxDur;
//			scale = 0.5f;

            turnDir = Math.signum((float) Math.random() - 0.5f) * 60f * (float) Math.random();
            //turnDir = 0f;

            float driftDir = (float) Math.random() * 360f;
            vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
            vel.scale(proj.getProjectileSpec().getWidth() / maxDur * 0.33f);

//			offset.x += vel.x * 1f;
//			offset.y += vel.y * 1f;
            fader = new FaderUtil(0f, 0.25f, 0.5f);
            fader.fadeIn();
        }

        public void advance(float amount) {
            scale += scaleIncreaseRate * amount;
            if (scale < 1f) {
                scale += scaleIncreaseRate * amount * 1f;
            }

            offset.x += vel.x * amount;
            offset.y += vel.y * amount;

            angle += turnDir * amount;

            fader.advance(amount);
        }
    }

    protected List<Brimstone.ParticleData> particles = new ArrayList<Brimstone.ParticleData>();

    protected DamagingProjectileAPI proj;
    protected DamagingProjectileAPI prev;
    protected float baseFacing = 0f;
    public Brimstone(DamagingProjectileAPI proj, DamagingProjectileAPI prev) {
        this.proj = proj;
        this.prev = prev;

        baseFacing = proj.getFacing();

        int num = 7;
        for (int i = 0; i < num; i++) {
            particles.add(new Brimstone.ParticleData(proj));
        }

        float length = proj.getProjectileSpec().getLength();
        float width = proj.getProjectileSpec().getWidth();

        float index = 0;
        for (Brimstone.ParticleData p : particles) {
            float f = index / (particles.size() - 1);
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
            dir.scale(length * f);
            Vector2f.add(p.offset, dir, p.offset);

            p.offset = Misc.getPointWithinRadius(p.offset, width * 0.5f);
            //p.scale = 0.25f + 0.75f * (1 - f);

            index++;
        }
    }

    public float getRenderRadius() {
        return 300f;
    }


    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return layers;
    }


    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        entity.getLocation().set(proj.getLocation());

        for (Brimstone.ParticleData p : particles) {
            p.advance(amount);
        }
    }


    public boolean isExpired() {
        return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;

        //Color color = new Color(100,150,255,50);
        Color color = proj.getProjectileSpec().getFringeColor();
        color = Misc.setAlpha(color, 50);
        float b = proj.getBrightness();
        b *= viewport.getAlphaMult();

        for (Brimstone.ParticleData p : particles) {
            float size = proj.getProjectileSpec().getWidth() * 0.6f;
            size *= p.scale;

            float alphaMult = 1f;
            Vector2f offset = p.offset;
            float diff = Misc.getAngleDiff(baseFacing, proj.getFacing());
            if (Math.abs(diff) > 0.1f) {
                offset = Misc.rotateAroundOrigin(offset, diff);
            }
            Vector2f loc = new Vector2f(x + offset.x, y + offset.y);

            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * alphaMult * p.fader.getBrightness());
            p.sprite.setColor(color);
            p.sprite.renderAtCenter(loc.x, loc.y);
        }
    }

}
