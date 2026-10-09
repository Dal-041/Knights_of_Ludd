package org.selkie.kol.campaign.movement;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.campaign.fleet.SmoothMovementModule;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.ReflectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Movement kit: lets any location entity (bare station token, market entity, ...)
 * move like a campaign fleet, per the traveling-market notes (tools/traveling-market.md, section 4).
 *
 * <p>Wiring copied verbatim from {@code CampaignFleet} (constructor line 330, advance lines 750-755):
 * a {@link SmoothMovementModule} owns the position/velocity and the entity's live
 * {@code getLocation()} / {@code getVelocity()} vectors are synced from it each frame.
 * {@code BaseLocation.advance} then integrates {@code location += velocity * dt} exactly as it does
 * for fleets, so a moved entity is indistinguishable in behavior from a fleet cruising under power.
 *
 * <p>{@code SmoothMovementModule} is an engine-internal class ({@code com.fs.starfarer.campaign.fleet})
 * but is marked {@code DoNotObfuscate} (mod-stable names) and this mod is pinned to 0.98a; treat it
 * as a soft dependency if the mod ever supports other builds.
 *
 * <p>Notes:
 * <ul>
 *   <li>The target must be in the entity's <em>current</em> coordinate frame
 *       (system-local if the entity is in a system, hyperspace coordinates if it is in hyperspace).</li>
 *   <li>In a system the entity's orbit advances after this sync and would overwrite the position;
 *       call {@code setOrbit(null)} first (this is the state fleets live in).</li>
 *   <li>The mover itself is transient state (a transient script); the entity it drives is normal
 *       saved sector content. Re-attach after a load if the movement must survive saving.</li>
 * </ul>
 *
 * <p>Usage: {@link #attach(SectorEntityToken, float)}, then point it with
 * {@link #setFixedTarget(Vector2f)} or {@link #setTargetProvider(TargetProvider)}, drive
 * {@link #setMaxSpeed(float)} from the fleet it is emulating if desired, and {@link #detach()}.
 */
public class KolEntityMover {

    /** Supplies the frame's movement target, in the entity's current coordinate frame. */
    public interface TargetProvider {
        ReadableVector2f getTarget();
    }

    /**
     * Type-specific after-relocation handling (e.g. re-slots a moved star into the
     * destination system's star slot, re-points a moon's parent). The generic membership
     * change is handled by {@link #relocate}; this hook is where a specific entity type's
     * extra bookkeeping goes.
     */
    public interface RelocateHook {
        void onRelocated(SectorEntityToken entity, LocationAPI oldLocation, LocationAPI newLocation);
    }

    /** Per-frame callback, run before the integrator advances (for policy checks). */
    public interface FrameHook {
        void tick(float dt);
    }

    private static final Set<KolEntityMover> movers = new HashSet<>();
    private static final Vector2f ZERO = new Vector2f(0f, 0f);

    private final SectorEntityToken entity;
    // 3-arg ctor enables the smooth speed cap; null delegate -> cap is whatever setMaxSpeed() says
    private final SmoothMovementModule module = new SmoothMovementModule(1f, 2f, null);
    private TargetProvider targetProvider;
    private Vector2f fixedTarget;
    private float maxSpeed;
    private RelocateHook relocateHook;
    private FrameHook frameHook;

    private KolEntityMover(SectorEntityToken entity, float maxSpeed) {
        this.entity = entity;
        this.maxSpeed = maxSpeed;
        // Seed the integrator from the entity's live state. The module starts at (0,0);
        // without this, the first sync snaps the entity to the system origin (the vanilla
        // CampaignFleet wiring avoids this by constructing the module at the fleet's position).
        module.getLocation().set(entity.getLocation());
        module.getVelocity().set(entity.getVelocity());
    }

    /**
     * Starts driving the entity. Idempotency is the caller's concern; each attach returns a
     * fresh mover, so call {@link #detach()} on the old one first.
     */
    public static KolEntityMover attach(SectorEntityToken entity, float maxSpeed) {
        KolEntityMover mover = new KolEntityMover(entity, maxSpeed);
        movers.add(mover);
        ensureDriver();
        return mover;
    }

    public SectorEntityToken getEntity() {
        return entity;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    /** Moves toward this point (a copy is taken each frame, the vector may be reused). */
    public void setFixedTarget(Vector2f target) {
        this.fixedTarget = target;
        this.targetProvider = null;
    }

    /** Moves toward wherever the provider points each frame; takes precedence over a fixed target. */
    public void setTargetProvider(TargetProvider provider) {
        this.targetProvider = provider;
        this.fixedTarget = null;
    }

    /** Type-specific after-relocation handling; see {@link RelocateHook}. */
    public void setRelocateHook(RelocateHook hook) {
        this.relocateHook = hook;
    }

    /** Per-frame policy callback; see {@link FrameHook}. */
    public void setFrameHook(FrameHook hook) {
        this.frameHook = hook;
    }

    /**
     * Moves the entity to a different location (a "jump"): removes it from its current
     * location and adds it to {@code destination}, placing it at {@code localPosition}
     * (destination's coordinate frame) with zero velocity.
     *
     * <p>Membership is the public {@code LocationAPI.removeEntity/addEntity} pair
     * (synchronous and immediate - see tools/traveling-market.md, section 5). Ordering
     * matters: remove first, then add. The entity's orbit must already be null (the kit
     * owns the position). The PD integrator is reset to the new position so movement
     * resumes cleanly instead of swooping from the old system's coordinates.
     *
     * <p>Every other token in the old location whose orbit focuses on the entity (moons,
     * stations, wrecks, asteroids, ...) is moved with it, preserving its relative offset
     * (recursively, so moons-of-moons follow). Their orbits keep driving them in the new
     * location. Entering a star system also fades the entity's indicator back in.
     */
    public void relocate(LocationAPI destination, ReadableVector2f localPosition) {
        LocationAPI oldLocation = entity.getContainingLocation();
        Vector2f oldPosition = new Vector2f(entity.getLocation());
        if (oldLocation != destination) {
            if (oldLocation != null) {
                oldLocation.removeEntity(entity);
            }
            destination.addEntity(entity);
        }
        entity.getLocation().set(localPosition.getX(), localPosition.getY());
        entity.getVelocity().set(0f, 0f);
        module.getLocation().set(localPosition.getX(), localPosition.getY());
        module.getVelocity().set(0f, 0f);
        Vector2f newPosition = new Vector2f(localPosition.getX(), localPosition.getY());
        if (oldLocation != null) {
            moveDependents(oldLocation, destination, entity, oldPosition, newPosition);
        }
        if (destination instanceof StarSystemAPI) {
            entity.fadeInIndicator();
        }
        if (relocateHook != null) {
            relocateHook.onRelocated(entity, oldLocation, destination);
        }
    }

    /**
     * All tokens in {@code location} (any type) whose orbit's focus is {@code focus}.
     * The orbit is read reflectively: the API only exposes {@code getOrbit()} on
     * {@code PlanetAPI}, while every campaign entity (moons, stations, wrecks, asteroids,
     * hazards, ...) inherits the stable public {@code getOrbit()} from
     * {@code BaseCampaignEntity}.
     */
    public static List<SectorEntityToken> getOrbitDependents(LocationAPI location, SectorEntityToken focus) {
        List<SectorEntityToken> dependents = new ArrayList<>();
        for (SectorEntityToken token : location.getAllEntities()) {
            if (token == focus) {
                continue;
            }
            OrbitAPI orbit = getOrbit(token);
            if (orbit != null && orbit.getFocus() == focus) {
                dependents.add(token);
            }
        }
        return dependents;
    }

    /**
     * Moves {@code focus}'s orbit dependents from {@code oldLocation} to {@code destination},
     * preserving each one's offset relative to the focus (whose position moves from
     * {@code oldFocusPosition} to {@code newFocusPosition}); recurses so nested dependents
     * (moons of moons, stations on moons, ...) follow. Orbit state is re-synced from the
     * new position so the orbit continues smoothly.
     */
    private static void moveDependents(LocationAPI oldLocation, LocationAPI destination,
                                       SectorEntityToken focus, Vector2f oldFocusPosition,
                                       Vector2f newFocusPosition) {
        for (SectorEntityToken dependent : getOrbitDependents(oldLocation, focus)) {
            Vector2f oldPosition = new Vector2f(dependent.getLocation());
            float offsetx = oldPosition.getX() - oldFocusPosition.getX();
            float offsety = oldPosition.getY() - oldFocusPosition.getY();
            Vector2f target = new Vector2f(newFocusPosition.getX() + offsetx, newFocusPosition.getY() + offsety);
            oldLocation.removeEntity(dependent);
            destination.addEntity(dependent);
            dependent.getLocation().set(target);
            OrbitAPI orbit = getOrbit(dependent);
            if (orbit != null) {
                orbit.updateLocation();
            }
            moveDependents(oldLocation, destination, dependent, oldPosition, target);
        }
    }

    private static boolean orbitLookupWarned;

    /**
     * Reflective no-arg {@code getOrbit()} (stable public name on BaseCampaignEntity).
     * The security classloader blocks {@code java.lang.reflect} for mod code, so the call
     * goes through the mod's {@link ReflectionUtils}, which never names the blocked types.
     * Fail-soft: if the shape is ever gone, dependents simply aren't moved.
     */
    private static OrbitAPI getOrbit(SectorEntityToken token) {
        try {
            // Kotlin vararg + default param: from Java the signature is invoke(String, Object, Object[], boolean)
            return (OrbitAPI) ReflectionUtils.INSTANCE.invoke("getOrbit", token, new Object[0], false);
        } catch (Throwable t) {
            if (!orbitLookupWarned) {
                orbitLookupWarned = true;
                Global.getLogger(KolEntityMover.class).warn("Reflective getOrbit() failed; orbit dependents will not be moved", t);
            }
            return null;
        }
    }

    /** Stops driving the entity. The entity keeps its last position and velocity. */
    public void detach() {
        movers.remove(this);
    }

    public boolean isAttached() {
        return movers.contains(this);
    }

    /** Advances the integrator and syncs the entity; called by the shared driver, not by mods. */
    void advance(float dt) {
        if (dt <= 0f) {
            return;
        }
        if (frameHook != null) {
            frameHook.tick(dt);
        }
        ReadableVector2f target = (targetProvider != null) ? targetProvider.getTarget() : fixedTarget;
        if (target == null) {
            return;
        }
        // CampaignFleet wiring: accel floor of 10 keeps the PD response snappy at low speeds
        module.setAcceleration(Math.max(10f, maxSpeed));
        module.setMaxSpeed(maxSpeed);
        module.advance(null, new Vector2f(target), ZERO, dt);
        entity.getLocation().set(module.getLocation());
        entity.getVelocity().set(module.getVelocity());
    }

    private static void ensureDriver() {
        if (!Global.getSector().hasTransientScript(Driver.class)) {
            Global.getSector().addTransientScript(new Driver());
        }
    }

    /**
     * Call from the mod plugin's {@code onGameLoad}: the driver is a transient script and the
     * mover registry a JVM static, so both must be rebuilt when the sector is rebuilt.
     */
    public static void onGameLoad() {
        movers.clear();
        ensureDriver();
    }

    /**
     * Shared per-frame driver. Transient: registered via {@link #onGameLoad()} and lazily on
     * the first {@link #attach} after a load.
     */
    public static class Driver implements EveryFrameScript {
        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float dt) {
            for (Iterator<KolEntityMover> it = movers.iterator(); it.hasNext(); ) {
                KolEntityMover mover = it.next();
                if (!mover.isAttached()) {
                    it.remove();
                } else {
                    mover.advance(dt);
                }
            }
        }
    }
}
