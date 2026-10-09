package org.selkie.kol.campaign.movement;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Test case for {@link KolEntityMover}: makes the planet in the player's system that is
 * closest to the player track the player's fleet at a bounded max rate, and follow the
 * player across hyperspace the way a fleet would - jumping to whichever system the player
 * arrives in.
 *
 * <p>Console: {@code kol_follow <maxSpeed>}, {@code kol_follow stop} (see
 * {@code data/console/commands.csv}).
 *
 * <p>Behavior (via {@link KolChaseDriver}, the way a fleet would):
 * <ul>
 *   <li>In-system: the planet tracks the player fleet at up to {@code maxSpeed} u/s.</li>
 *   <li>Cross-system: the planet heads to the exit jump point, jumps out into hyperspace
 *       (chasing the player across hyperspace while in transit), and enters the player's
 *       system through its entry jump point. Point-to-point only - the systems must be
 *       directly connected, otherwise the planet holds.</li>
 * </ul>
 *
 * <p>The tracked planet's orbit is nulled for the duration (the kit owns the position,
 * a live orbit would overwrite it every frame). Stars are excluded as targets (moving one
 * dangles the system's star slot). On {@link #stop()} the velocity is zeroed and the
 * planet is deliberately left where it is - observing that drift is the point of the
 * test. All of this persists in the save (relocation included); this is a save-altering
 * test.
 */
public final class KolPlanetFollowTest {

    private static KolEntityMover mover;
    private static KolChaseDriver chase;
    private static PlanetAPI planet;
    private static OrbitAPI previousOrbit;

    private KolPlanetFollowTest() {
    }

    /** @return true if the test is currently running. */
    public static boolean isActive() {
        return mover != null && planet != null;
    }

    /**
     * Starts tracking: the closest non-star planet in the player's system (by local
     * distance to the player) begins following the player's fleet at up to
     * {@code maxSpeed} game units per second, and follows the player across systems via
     * jump points (see {@link KolChaseDriver}).
     *
     * @return the tracked planet, or null if the player is not in a star system, the
     *         system has no non-star planets, or the test is already running.
     */
    public static PlanetAPI start(float maxSpeed) {
        if (isActive()) {
            return null;
        }
        SectorEntityToken player = Global.getSector().getPlayerFleet();
        if (player == null) {
            return null;
        }
        if (!(player.getContainingLocation() instanceof StarSystemAPI)) {
            return null; // the player must start in a star system
        }

        // Closest non-star planet in the player's system, by local distance to the player.
        PlanetAPI closest = null;
        float bestDistance = Float.MAX_VALUE;
        for (PlanetAPI candidate : player.getContainingLocation().getPlanets()) {
            if (candidate == null || candidate.isStar()) {
                continue; // stars are slotted on the system; relocating one dangles the slot
            }
            float distance = Misc.getDistance(player.getLocation(), candidate.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = candidate;
            }
        }
        if (closest == null) {
            return null;
        }
        planet = closest;

        // The kit owns the position; a live orbit would overwrite it every frame.
        previousOrbit = planet.getOrbit();
        planet.setOrbit(null);

        mover = KolEntityMover.attach(planet, maxSpeed);
        chase = new KolChaseDriver(mover, player);
        mover.setTargetProvider(chase);
        mover.setFrameHook(chase);
        System.out.println("[KolPlanetFollowTest] started: planet " + planet.getId()
                + " in " + planet.getContainingLocation().getId() + ", max speed " + maxSpeed + " u/s");
        return planet;
    }

    /**
     * Stops the movement and leaves the planet where it is: velocity is zeroed (the
     * engine's per-frame loop would otherwise keep integrating it), but the drifted
     * position is kept on purpose - observing the drift is the point of the test. It
     * persists in the save.
     */
    public static void stop() {
        if (mover != null) {
            mover.detach();
            mover = null;
        }
        chase = null;
        if (planet != null) {
            planet.getVelocity().set(0f, 0f);
            //planet.setOrbit(previousOrbit);
        }
        planet = null;
        previousOrbit = null;
    }

    /** @return the planet currently being tracked, or null. */
    public static PlanetAPI getPlanet() {
        return planet;
    }

    /**
     * Statics go stale across save/load: clear them and re-register the kit's transient
     * driver. Called from the mod plugin's {@code onGameLoad}.
     */
    public static void onGameLoad() {
        mover = null;
        chase = null;
        planet = null;
        previousOrbit = null;
        KolEntityMover.onGameLoad();
    }
}
