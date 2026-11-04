package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Brimstone: wide-cone flamethrower built on the vanilla Cryoflamer trail technique.
 * Each fired shot is replaced by a spray of rays across a cone; the rays themselves are invisible
 * hitboxes (see kol_brimestone_shot.proj) and every visual comes from the fire and smoke particles here.
 */
public class BrimstoneEffect extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin {
    private static final String WEAPON_ID = "kol_brimstone";
    private static final String SPRAY_SPLIT_ID = "kol_brimstone_spray_split";
    private static final String SHIELD_DAMAGE_ID = "kol_brimstone_shield_damage";
    private static final float SHIELD_DAMAGE_MULT = 2f / 3f;

    private static final int RAYS_PER_SHOT = 3;
    private static final float CONE_HALF_ANGLE = 11f;
    private static final float SPEED_JITTER = 0.15f; // each ray flies at 1 +- this times the weapon's speed
    private static final float MUZZLE_JITTER = 12f;

    private static final int FIRE_PARTICLES = 4;
    private static final int SMOKE_PARTICLES = 2;
    // Particle size as a multiple of the projectile width, at the muzzle and at max range.
    private static final float FIRE_SCALE_START = 0.3f;
    private static final float FIRE_SCALE_END = 1.8f;
    private static final float SMOKE_SCALE_START = 0.7f;
    private static final float SMOKE_SCALE_END = 2.6f;

    // Fire color over the flame's life: hot yellow at the muzzle, through orange, to a dim ember red.
    // Additive particles stack, so blue stays low: dense overlaps saturate to yellow instead of white.
    private static final Color[] FIRE_RAMP = {
            new Color(245, 150, 35, 36),
            new Color(235, 105, 20, 40),
            new Color(205, 60, 12, 34),
            new Color(130, 25, 8, 22),
    };
    private static final Color SMOKE_COLOR = new Color(45, 32, 26, 70);
    private static final int FLAME_OVER_PARTICLES = 3;

    private static final int CORE_PARTICLES = 2;
    // White-hot spine inside the flame: small, on-axis, additive, so it saturates white while the outer fire stays orange.
    private static final float CORE_SCALE_START = 0.35f;
    private static final float CORE_SCALE_END = 1f;
    private static final Color CORE_COLOR = new Color(255, 238, 190);

    private boolean init = false;
    private boolean active = false;
    private ShipAPI ship;

    protected List<BrimstoneEffect> trails;

    public BrimstoneEffect() {
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (engine.isPaused() || weapon == null) {
            return;
        }
        if (!init) {
            ship = weapon.getShip();
            if (ship != null && !ship.hasListenerOfClass(ShieldDamageMod.class)) {
                ship.addListener(new ShieldDamageMod());
            }
            init = true;
        }

        if (ship == null || !ship.isAlive() || ship.getSystem() == null) {
            assert true; //Lazy negation, would return but cryoflux effect still needs to run.
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

        if (trails == null) return;

        Iterator<BrimstoneEffect> iter = trails.iterator();
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
            for (BrimstoneEffect curr : trails) {
                if (curr.proj != null) {
                    Vector2f.add(com, curr.proj.getLocation(), com);
                    weight += curr.proj.getBrightness();
                    totalDist += Misc.getDistance(source, curr.proj.getLocation());
                }
            }
            if (weight > 0.1f) {
                com.scale(1f / weight);
                float volume = Math.min(weight, 1f);
                if (!trails.isEmpty()) {
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
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Vector2f vel = new Vector2f();
        if (target instanceof ShipAPI) {
            vel.set(target.getVelocity());
        }

        float size = projectile.getProjectileSpec().getWidth();
        float sizeMult = Misc.getHitGlowSize(100f, projectile.getDamage().getBaseDamage(), damageResult) / 100f;
        Color c = Misc.scaleAlpha(FIRE_RAMP[2], projectile.getBrightness());
        engine.addNebulaParticle(point, vel, size, 5f + 3f * sizeMult, 0f, 0f, 1f, c);

        // Flames pour over the hull: particles ride the target and slide along its surface,
        // so the flame appears to wash across the ship instead of stopping dead at the impact point.
        Vector2f normal = Vector2f.sub(point, target.getLocation(), new Vector2f());
        Vector2f tangent = new Vector2f(-normal.y, normal.x);
        if (tangent.lengthSquared() > 1f) {
            tangent.normalise();
            Vector2f jet = projectile.getVelocity();
            if (Vector2f.dot(tangent, jet) < 0f) tangent.negate();
            float slide = Math.min(jet.length(), 900f) * 0.4f;
            for (int i = 0; i < FLAME_OVER_PARTICLES; i++) {
                float along = size * (0.4f + 1.6f * Misc.random.nextFloat()) * (i + 1) / FLAME_OVER_PARTICLES;
                Vector2f at = new Vector2f(point.x + tangent.x * along, point.y + tangent.y * along);
                Vector2f pvel = new Vector2f(vel);
                float drift = slide * (0.4f + 0.6f * Misc.random.nextFloat());
                pvel.x += tangent.x * drift;
                pvel.y += tangent.y * drift;
                engine.addNebulaParticle(at, pvel, size * 0.7f,
                        2.5f + 2f * Misc.random.nextFloat(), 0.15f, 0.5f,
                        0.55f + 0.45f * Misc.random.nextFloat(),
                        Misc.scaleAlpha(FIRE_RAMP[1], projectile.getBrightness()));
            }
        }

        Misc.playSound(damageResult, point, vel,
                "cryoflamer_hit_shield_light",
                "cryoflamer_hit_shield_solid",
                "cryoflamer_hit_shield_heavy",
                "cryoflamer_hit_light",
                "cryoflamer_hit_solid",
                "cryoflamer_hit_heavy");
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ShipAPI source = weapon.getShip();
        Vector2f origin = new Vector2f(projectile.getLocation());
        float facing = projectile.getFacing();
        float speed = weapon.getProjectileSpeed();
        Vector2f shipVel = source.getVelocity();

        // The fired shot only marks the muzzle; the spray below replaces it.
        engine.removeEntity(projectile);

        for (int i = 0; i < RAYS_PER_SHOT; i++) {
            // Stratify across the cone so every shot covers it, then jitter within each slice.
            float slice = 2f * CONE_HALF_ANGLE / RAYS_PER_SHOT;
            float angle = facing - CONE_HALF_ANGLE + slice * (i + Misc.random.nextFloat());
            Vector2f point = Misc.getPointWithinRadius(origin, MUZZLE_JITTER);
            CombatEntityAPI spawned = engine.spawnProjectile(source, weapon, weapon.getId(), point, angle, shipVel);
            if (!(spawned instanceof DamagingProjectileAPI)) continue;
            DamagingProjectileAPI proj = (DamagingProjectileAPI) spawned;

            Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
            vel.scale(speed * (1f + SPEED_JITTER * (2f * Misc.random.nextFloat() - 1f)));
            Vector2f.add(vel, shipVel, proj.getVelocity());
            proj.getDamage().getModifier().modifyMult(SPRAY_SPLIT_ID, 1f / RAYS_PER_SHOT);

            BrimstoneEffect trail = new BrimstoneEffect(proj);
            CombatEntityAPI e = engine.addLayeredRenderingPlugin(trail);
            e.getLocation().set(proj.getLocation());

            if (trails == null) {
                trails = new ArrayList<>();
            }
            trails.add(trail);
        }
    }

    public static class ShieldDamageMod implements DamageDealtModifier {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit || !(param instanceof DamagingProjectileAPI)) return null;
            WeaponAPI weapon = ((DamagingProjectileAPI) param).getWeapon();
            if (weapon == null || !WEAPON_ID.equals(weapon.getId())) return null;
            damage.getModifier().modifyMult(SHIELD_DAMAGE_ID, SHIELD_DAMAGE_MULT);
            return SHIELD_DAMAGE_ID;
        }
    }

    public static class ParticleData {
        public final SpriteAPI sprite;
        public final boolean smoke;
        public final boolean core;
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();
        public float sizeJitter;
        public float turnDir;
        public float angle;
        public final FaderUtil fader;

        public ParticleData(DamagingProjectileAPI proj, boolean smoke, boolean core) {
            this.smoke = smoke;
            this.core = core;
            sprite = Global.getSettings().getSprite(GfxCat.MISC, "nebula_particles");
            float i = Misc.random.nextInt(4);
            float j = Misc.random.nextInt(4);
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(i * 0.25f);
            sprite.setTexY(j * 0.25f);
            if (smoke) {
                sprite.setNormalBlend();
            } else {
                sprite.setAdditiveBlend();
            }

            angle = (float) Math.random() * 360f;
            sizeJitter = 0.75f + 0.5f * (float) Math.random();
            turnDir = Math.signum((float) Math.random() - 0.5f) * 90f * (float) Math.random();

            // Flames billow outward as they travel.
            float maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
            vel = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
            vel.scale(proj.getProjectileSpec().getWidth() / maxDur * (core ? 0.1f : smoke ? 0.6f : 0.4f));

            fader = new FaderUtil(0f, smoke ? 0.4f : 0.1f, 0.5f);
            fader.fadeIn();
        }

        public void advance(float amount) {
            offset.x += vel.x * amount;
            offset.y += vel.y * amount;
            angle += turnDir * amount;
            fader.advance(amount);
        }
    }

    protected final List<ParticleData> particles = new ArrayList<>();

    protected DamagingProjectileAPI proj;
    protected float baseFacing;
    protected float maxDur;

    public BrimstoneEffect(DamagingProjectileAPI proj) {
        this.proj = proj;
        baseFacing = proj.getFacing();
        maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();

        float length = proj.getProjectileSpec().getLength();
        float width = proj.getProjectileSpec().getWidth();
        for (int i = 0; i < SMOKE_PARTICLES + FIRE_PARTICLES + CORE_PARTICLES; i++) {
            boolean core = i >= SMOKE_PARTICLES + FIRE_PARTICLES;
            ParticleData p = new ParticleData(proj, i < SMOKE_PARTICLES, core);
            // Spread along the ray body, behind the head.
            Vector2f back = Misc.getUnitVectorAtDegreeAngle(baseFacing + 180f);
            back.scale(length * (float) Math.random());
            p.offset = Misc.getPointWithinRadius(back, width * (core ? 0.06f : 0.3f));
            particles.add(p);
        }
    }

    public float getRenderRadius() {
        return 500f;
    }

    protected final EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);

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

        for (ParticleData p : particles) {
            p.advance(amount);
        }
    }

    public boolean isExpired() {
        return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    private static Color fireColor(float life) {
        float scaled = Math.min(Math.max(life, 0f), 0.999f) * (FIRE_RAMP.length - 1);
        int index = (int) scaled;
        return Misc.interpolateColor(FIRE_RAMP[index], FIRE_RAMP[index + 1], scaled - index);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;

        float life = Math.min(proj.getElapsed() / maxDur, 1f);
        float b = proj.getBrightness() * viewport.getAlphaMult();
        float width = proj.getProjectileSpec().getWidth();
        Color fire = fireColor(life);
        // Smoke only shows once the flame has cooled.
        float smokeAlpha = Math.max(0f, (life - 0.35f) / 0.65f);

        float diff = Misc.getAngleDiff(baseFacing, proj.getFacing());
        for (ParticleData p : particles) {
            float alpha;
            float size;
            Color color;
            if (p.core) {
                size = width * (CORE_SCALE_START + (CORE_SCALE_END - CORE_SCALE_START) * (float) Math.sqrt(life));
                alpha = 1f - 0.6f * life; // white along the spine, cooling only at the tail
                color = CORE_COLOR;
            } else if (p.smoke) {
                size = width * (SMOKE_SCALE_START + (SMOKE_SCALE_END - SMOKE_SCALE_START) * life);
                alpha = smokeAlpha;
                color = SMOKE_COLOR;
            } else {
                size = width * (FIRE_SCALE_START + (FIRE_SCALE_END - FIRE_SCALE_START) * (float) Math.sqrt(life));
                alpha = 1f;
                color = fire;
            }
            size *= p.sizeJitter;

            Vector2f offset = p.offset;
            if (Math.abs(diff) > 0.1f) {
                offset = Misc.rotateAroundOrigin(offset, diff);
            }

            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * alpha * p.fader.getBrightness());
            p.sprite.setColor(color);
            p.sprite.renderAtCenter(x + offset.x, y + offset.y);
        }
    }
}
